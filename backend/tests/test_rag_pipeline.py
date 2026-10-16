"""Tests for RAG pipeline routing, context-aware fallback, and prevention of fallthrough."""

import pytest
from unittest.mock import AsyncMock, patch

from app.schemas.rag import AppMode, CorpusScope, EvidenceOutcome, RAGRequestContext, RouteDecision
from app.services.routing_service import RoutingService
from app.services.supabase_retrieval_service import SupabaseRetrievalService
from app.services.embedding_service import embedding_service


def test_deterministic_routing_rules():
    router = RoutingService()

    ctx_general = RAGRequestContext(app_mode=AppMode.GENERAL)
    ctx_student = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )

    # 1. Greetings -> DIRECT
    dec, _ = router.evaluate_deterministic_route("Hello!", ctx_general)
    assert dec == RouteDecision.DIRECT

    dec, _ = router.evaluate_deterministic_route("Good morning", ctx_student)
    assert dec == RouteDecision.DIRECT

    # 2. Arithmetic -> DIRECT
    dec, _ = router.evaluate_deterministic_route("What is 15 * 24?", ctx_general)
    assert dec == RouteDecision.DIRECT

    # 3. Student mode with curriculum query -> RAG
    dec, q = router.evaluate_deterministic_route("Explain Chapter 4 Carbon Compounds", ctx_student)
    assert dec == RouteDecision.RAG
    assert "Carbon Compounds" in q

    # 4. General mode non-curriculum query -> DIRECT
    dec, _ = router.evaluate_deterministic_route("How do I write a binary search in Python?", ctx_general)
    assert dec == RouteDecision.DIRECT


def test_context_aware_router_fallback():
    """Verify that curriculum questions NEVER silently bypass RAG on router timeout or error."""
    router = RoutingService()

    ctx_student = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )
    ctx_general = RAGRequestContext(app_mode=AppMode.GENERAL)

    # Student mode question with timeout MUST fallback to RAG
    prompt = "Explain photosynthesis reactivity in leaves"
    dec, q = router.get_fallback_route(prompt, ctx_student, "router_timeout")
    assert dec == RouteDecision.RAG
    assert q == prompt  # Raw prompt is passed to RAG retrieval

    # General mode question with timeout falls back to DIRECT
    prompt_gen = "Tell me about quantum computing"
    dec_gen, _ = router.get_fallback_route(prompt_gen, ctx_general, "router_timeout")
    assert dec_gen == RouteDecision.DIRECT


@pytest.mark.asyncio
async def test_general_mode_rag_corpus_not_configured():
    """Verify General Mode retrieval strictly returns CORPUS_NOT_CONFIGURED."""
    service = SupabaseRetrievalService(db=None)
    ctx_general = RAGRequestContext(app_mode=AppMode.GENERAL)

    resp = await service.retrieve("Explain neural networks", ctx_general)
    assert resp.evidence_outcome == EvidenceOutcome.CORPUS_NOT_CONFIGURED
    assert len(resp.chunks) == 0
    assert "General Mode RAG is disabled" in (resp.error_detail or "")


@pytest.mark.asyncio
async def test_retrieval_error_when_embedding_not_configured():
    """Verify that unconfigured embedding returns RETRIEVAL_ERROR and does not silently succeed."""
    service = SupabaseRetrievalService(db=None)
    ctx_student = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )

    with patch.object(embedding_service, "api_key", ""):
        resp = await service.retrieve("Explain plant cells", ctx_student)
        assert resp.evidence_outcome == EvidenceOutcome.RETRIEVAL_ERROR
        assert "Embedding service is not configured" in (resp.error_detail or "")
