"""Deterministic and context-aware routing service for RAG vs DIRECT decisions."""

import asyncio
import logging
import re
from typing import Optional, Tuple
import httpx

from app.core.config import settings
from app.schemas.rag import AppMode, RAGRequestContext, RouteDecision
from app.services.route_header_parser import RouteHeaderParser

logger = logging.getLogger(__name__)

# Rule patterns
GREETINGS_PATTERN = re.compile(
    r"^\s*(hi|hello|hey|good\s+(morning|afternoon|evening)|howdy|greetings|thanks|thank\s+you|bye|goodbye)\b[!\.\s]*$",
    re.IGNORECASE,
)
ARITHMETIC_PATTERN = re.compile(
    r"^\s*(?:what\s+is\s+)?(\d+(?:\.\d+)?)\s*([\+\-\*\/×÷])\s*(\d+(?:\.\d+)?)\s*\??\s*$",
    re.IGNORECASE,
)
APP_COMMAND_PATTERN = re.compile(
    r"^\s*(help|settings|clear\s+chat|reset|switch\s+to\s+.*|profile)\s*$",
    re.IGNORECASE,
)
CURRICULUM_KEYWORDS_PATTERN = re.compile(
    r"\b(chapter|exercise|ncert|cbse|icse|syllabus|curriculum|textbook|page|section|theorem|experiment|formula|definition|explain\s+topic)\b",
    re.IGNORECASE,
)


class RoutingService:
    """
    Decides between DIRECT response or RAG retrieval.
    
    Guarantees:
    - Fast deterministic classification for common non-RAG patterns (greetings, math, commands).
    - Curriculum-aware classification for Student Mode.
    - Context-aware fallback on LLM router timeout or malformed output (curriculum questions NEVER silently bypass RAG).
    - Strict deadline enforcement.
    """

    def __init__(self):
        self.last_diagnostic: Optional[dict] = None

    def _record_diagnostic(
        self,
        route: RouteDecision,
        rationale: str,
        context: RAGRequestContext,
        query: Optional[str] = None
    ) -> None:
        app_mode_val = (
            context.app_mode.value.lower()
            if hasattr(context.app_mode, "value")
            else str(context.app_mode).lower()
        )
        self.last_diagnostic = {
            "route": route.value if hasattr(route, "value") else str(route),
            "rationale": rationale,
            "app_mode": app_mode_val,
            "board": context.board,
            "class_grade": context.class_grade,
            "subject": context.subject,
            "query": query,
        }

    def evaluate_deterministic_route(
        self,
        prompt: str,
        context: RAGRequestContext
    ) -> Optional[Tuple[RouteDecision, Optional[str]]]:
        """
        Fast regex/rule-based evaluation before making any network calls.
        Returns (RouteDecision, standalone_query) if deterministic, or None if ambiguous.
        """
        cleaned = prompt.strip()

        # 1. Greetings & Pleasantries -> DIRECT
        if GREETINGS_PATTERN.match(cleaned):
            self._record_diagnostic(RouteDecision.DIRECT, "deterministic_greetings", context)
            return RouteDecision.DIRECT, None

        # 2. Simple Arithmetic -> DIRECT
        if ARITHMETIC_PATTERN.match(cleaned):
            self._record_diagnostic(RouteDecision.DIRECT, "deterministic_arithmetic", context)
            return RouteDecision.DIRECT, None

        # 3. App Commands -> DIRECT
        if APP_COMMAND_PATTERN.match(cleaned):
            self._record_diagnostic(RouteDecision.DIRECT, "deterministic_app_command", context)
            return RouteDecision.DIRECT, None

        # 4. Explicit Student Mode with selected chapter or curriculum keywords
        if context.app_mode == AppMode.STUDENT:
            if context.selected_chapter or CURRICULUM_KEYWORDS_PATTERN.search(cleaned):
                self._record_diagnostic(RouteDecision.RAG, "deterministic_curriculum_keyword", context, cleaned)
                return RouteDecision.RAG, cleaned

        # 5. General Mode without curriculum indicators -> DIRECT
        # (General mode has no document corpus configured in this release)
        if context.app_mode == AppMode.GENERAL and not CURRICULUM_KEYWORDS_PATTERN.search(cleaned):
            self._record_diagnostic(RouteDecision.DIRECT, "deterministic_general_mode", context)
            return RouteDecision.DIRECT, None

        return None

    def get_fallback_route(
        self,
        prompt: str,
        context: RAGRequestContext,
        reason: str
    ) -> Tuple[RouteDecision, Optional[str]]:
        """
        Context-aware fallback when router times out, network fails, or output is malformed.
        
        MANDATORY REQUIREMENT:
        Curriculum-dependent Student Mode questions must NOT silently bypass RAG.
        """
        logger.warning(
            f"Router fallback triggered (reason={reason}) for mode={context.app_mode}, "
            f"board={context.board}, class={context.class_grade}, subject={context.subject}"
        )

        is_student = context.app_mode == AppMode.STUDENT
        has_curriculum_context = bool(
            context.board or context.class_grade or context.subject or context.selected_chapter
        )
        has_curriculum_keywords = bool(CURRICULUM_KEYWORDS_PATTERN.search(prompt))

        if is_student or has_curriculum_context or has_curriculum_keywords:
            # Must invoke RAG with user's raw prompt rather than silently hallucinating generic text
            self._record_diagnostic(RouteDecision.RAG, f"fallback_{reason}", context, prompt.strip())
            return RouteDecision.RAG, prompt.strip()
        else:
            self._record_diagnostic(RouteDecision.DIRECT, f"fallback_{reason}", context)
            return RouteDecision.DIRECT, None

    async def route_query(
        self,
        prompt: str,
        context: RAGRequestContext
    ) -> Tuple[RouteDecision, Optional[str]]:
        """
        Evaluates the optimal route (DIRECT or RAG) for a user prompt.
        """
        # Step 1: Check deterministic rules
        deterministic = self.evaluate_deterministic_route(prompt, context)
        if deterministic is not None:
            return deterministic

        # Step 2: For ambiguous cases, invoke LLM router with strict deadline
        try:
            return await asyncio.wait_for(
                self._llm_route(prompt, context),
                timeout=settings.router_timeout_seconds
            )
        except asyncio.TimeoutError:
            return self.get_fallback_route(prompt, context, "router_timeout")
        except Exception as exc:
            return self.get_fallback_route(prompt, context, f"router_error: {exc}")

    async def _llm_route(
        self,
        prompt: str,
        context: RAGRequestContext
    ) -> Tuple[RouteDecision, Optional[str]]:
        """LLM-based classifier using streaming line protocol."""
        from app.services.openrouter_service import openrouter_service

        if not openrouter_service.is_configured:
            # Offline or unconfigured OpenRouter -> use context-aware fallback
            return self.get_fallback_route(prompt, context, "openrouter_not_configured")

        system_instruction = (
            "You are an educational query router. Output ONLY in one of two formats:\n"
            "Format 1 (if the query requires textbook/curriculum retrieval):\n"
            "ROUTE: RAG\n"
            "QUERY: <specific standalone retrieval query>\n\n"
            "Format 2 (if general conversation, coding, or can be answered directly):\n"
            "ROUTE: DIRECT\n"
            "<direct answer>"
        )

        messages = [
            {"role": "system", "content": system_instruction},
            {"role": "user", "content": f"Mode: {context.app_mode}\nQuery: {prompt}"}
        ]

        parser = RouteHeaderParser()

        # Enforce underlying HTTP client timeout matching router deadline
        async for chunk in openrouter_service.generate_response_stream(
            messages=messages,
            temperature=0.0,
            max_tokens=64,
        ):
            parser.feed(chunk)
            if parser.is_complete:
                break

        parser.finalize()

        if parser.is_rag and parser.query:
            self._record_diagnostic(RouteDecision.RAG, "llm_rag", context, parser.query)
            return RouteDecision.RAG, parser.query
        elif parser.is_direct:
            self._record_diagnostic(RouteDecision.DIRECT, "llm_direct", context)
            return RouteDecision.DIRECT, None
        else:
            raise ValueError("Malformed header received from LLM router")


routing_service = RoutingService()
