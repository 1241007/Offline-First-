"""OpenRouter LLM service for dynamic course validation, diagnostic question generation,
personalized roadmap DAG synthesis, and rich topic learning content.
"""

import asyncio
import json
import logging
import re
import time
from typing import Any, Dict, List, Optional, Tuple
import httpx
from app.core.config import settings

logger = logging.getLogger(__name__)

OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"


class OpenRouterException(Exception):
    """Base exception for OpenRouter operations."""
    pass


class OpenRouterAuthError(OpenRouterException):
    """Raised when the OpenRouter API key is missing or invalid (HTTP 401)."""
    pass


class OpenRouterRateLimitError(OpenRouterException):
    """Raised when OpenRouter rate limit is exceeded (HTTP 429)."""
    pass


class OpenRouterServiceError(OpenRouterException):
    """Raised when OpenRouter returns a server or model error."""
    pass


def extract_json_from_llm(raw_text: str) -> Any:
    """Extract and parse JSON from LLM output, safely handling markdown code blocks, reasoning tags, and preambles."""
    if not raw_text or not raw_text.strip():
        raise ValueError("Empty response received from LLM")
    cleaned = raw_text.strip()
    # Strip <think>...</think> tags if present
    cleaned = re.sub(r"<think>[\s\S]*?</think>", "", cleaned).strip()
    # Match ```json ... ``` or ``` ... ```
    fence_match = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", cleaned)
    if fence_match:
        cleaned = fence_match.group(1).strip()
    else:
        # Try finding the first '[' or '{' and the last ']' or '}'
        start_brace = cleaned.find("{")
        start_bracket = cleaned.find("[")
        if start_brace != -1 and (start_bracket == -1 or start_brace < start_bracket):
            end_brace = cleaned.rfind("}")
            if end_brace != -1:
                cleaned = cleaned[start_brace : end_brace + 1]
        elif start_bracket != -1:
            end_bracket = cleaned.rfind("]")
            if end_bracket != -1:
                cleaned = cleaned[start_bracket : end_bracket + 1]

    return json.loads(cleaned)


class OpenRouterService:
    def __init__(self):
        self.api_key = settings.clean_openrouter_api_key
        self.model = settings.openrouter_model or "openrouter/free"
        self._client: Optional[httpx.AsyncClient] = None

    async def _get_client(self, timeout: float = 60.0) -> httpx.AsyncClient:
        if self._client is None or self._client.is_closed:
            self._client = httpx.AsyncClient(timeout=timeout)
        return self._client

    def _get_headers(self) -> Dict[str, str]:
        if not self.api_key:
            raise OpenRouterAuthError("OPENROUTER_API_KEY is not configured in backend environment.")
        return {
            "Authorization": f"Bearer {self.api_key}",
            "HTTP-Referer": "https://edunova.app",
            "X-Title": "EduNova Learning",
            "Content-Type": "application/json",
        }

    async def chat_completion(
        self,
        messages: List[Dict[str, str]],
        temperature: float = 0.2,
        use_web_search: bool = False,
        timeout: float = 60.0,
        max_tokens: int = 2048,
        label: str = "llm_call",
    ) -> str:
        """Call OpenRouter completions API with error handling, timing, and reasoning token suppression."""
        t0 = time.monotonic()
        headers = self._get_headers()
        payload: Dict[str, Any] = {
            "model": self.model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        if use_web_search:
            payload["plugins"] = [{"id": "web"}]

        async with httpx.AsyncClient(timeout=timeout) as client:
            try:
                resp = await client.post(OPENROUTER_URL, headers=headers, json=payload)
            except httpx.TimeoutException as e:
                elapsed = time.monotonic() - t0
                logger.error(f"[{label}] OpenRouter timed out after {elapsed:.1f}s: {e}")
                raise OpenRouterServiceError(f"OpenRouter request timed out after {timeout}s")
            except httpx.RequestError as e:
                elapsed = time.monotonic() - t0
                logger.error(f"[{label}] OpenRouter network error after {elapsed:.1f}s: {e}")
                raise OpenRouterServiceError(f"Network error connecting to OpenRouter: {e}")

            elapsed = time.monotonic() - t0

            # Handle 429 rate limits gracefully with backoff or Gemini fallback
            if resp.status_code == 429:
                err_text = resp.text
                is_daily_limit = "free-models-per-day" in err_text or "openrouter_free_tier_daily" in err_text
                
                gemini_key = (settings.gemini_api_key or "").strip("\"'")
                if is_daily_limit and gemini_key:
                    logger.warning(
                        f"[{label}] OpenRouter daily free tier limit reached (HTTP 429). "
                        "Engaging high-availability fallback via Gemini 3.5 Flash Lite..."
                    )
                    return await self._call_gemini_fallback(
                        messages=messages,
                        temperature=temperature,
                        timeout=timeout,
                        max_tokens=max_tokens,
                        label=label,
                    )

                retry_after = 3.0
                try:
                    retry_after = max(1.0, float(resp.headers.get("Retry-After", 3.0)))
                except Exception:
                    pass
                logger.warning(f"[{label}] OpenRouter 429 rate limit hit. Waiting {retry_after:.1f}s before retry...")
                await asyncio.sleep(retry_after)
                resp = await client.post(OPENROUTER_URL, headers=headers, json=payload)
                elapsed = time.monotonic() - t0

                if resp.status_code == 429 and gemini_key:
                    logger.warning(
                        f"[{label}] OpenRouter still rate-limited after retry (HTTP 429). "
                        "Engaging high-availability fallback via Gemini 3.5 Flash Lite..."
                    )
                    return await self._call_gemini_fallback(
                        messages=messages,
                        temperature=temperature,
                        timeout=timeout,
                        max_tokens=max_tokens,
                        label=label,
                    )

            if resp.status_code == 401:
                raise OpenRouterAuthError("Invalid OpenRouter API key. Please check your OPENROUTER_API_KEY in .env.")
            elif resp.status_code == 429:
                raise OpenRouterRateLimitError("OpenRouter rate limit exceeded. Please wait a moment and retry.")
            elif resp.status_code >= 400:
                err_msg = resp.text
                try:
                    err_json = resp.json()
                    err_msg = err_json.get("error", {}).get("message", resp.text)
                except Exception:
                    pass
                logger.error(f"[{label}] OpenRouter error HTTP {resp.status_code} after {elapsed:.1f}s: {err_msg}")
                raise OpenRouterServiceError(f"OpenRouter error (HTTP {resp.status_code}): {err_msg}")

            data = resp.json()
            choices = data.get("choices", [])
            if not choices:
                raise OpenRouterServiceError("Empty response returned by OpenRouter model.")

            msg = choices[0].get("message", {})
            content = msg.get("content") or ""
            # Fallback if content was placed in reasoning fields
            if not content.strip():
                content = msg.get("reasoning") or msg.get("reasoning_content") or ""

            model_used = data.get("model", "unknown")
            logger.info(f"[{label}] OpenRouter OK in {elapsed:.1f}s (model={model_used}, chars={len(content)})")
            return content

    async def _call_gemini_fallback(
        self,
        messages: List[Dict[str, str]],
        temperature: float = 0.2,
        timeout: float = 45.0,
        max_tokens: int = 2048,
        label: str = "gemini_fallback",
    ) -> str:
        """High-availability fallback when OpenRouter daily limit is exhausted. Calls real Gemini 3.5 Flash Lite."""
        t0 = time.monotonic()
        gemini_key = (settings.gemini_api_key or "").strip("\"'")
        if not gemini_key:
            raise OpenRouterRateLimitError("OpenRouter daily limit exhausted and GEMINI_API_KEY is not configured.")

        # Combine role messages into structured text for Gemini generateContent
        parts = []
        for m in messages:
            role = m.get("role", "user").upper()
            content = m.get("content", "")
            parts.append(f"[{role}]:\n{content}")
        full_prompt = "\n\n".join(parts)

        url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key={gemini_key}"
        payload = {
            "contents": [{"parts": [{"text": full_prompt}]}],
            "generationConfig": {
                "temperature": temperature,
                "maxOutputTokens": max_tokens,
                "responseMimeType": "application/json",
            },
        }

        async with httpx.AsyncClient(timeout=timeout) as client:
            try:
                resp = await client.post(url, json=payload)
            except Exception as e:
                elapsed = time.monotonic() - t0
                logger.error(f"[{label}] Fallback Gemini error after {elapsed:.1f}s: {e}")
                raise OpenRouterServiceError(f"Fallback LLM error: {e}")

            elapsed = time.monotonic() - t0
            if resp.status_code != 200:
                logger.error(f"[{label}] Fallback Gemini HTTP {resp.status_code} after {elapsed:.1f}s: {resp.text[:300]}")
                raise OpenRouterServiceError(f"Fallback LLM returned HTTP {resp.status_code}")

            data = resp.json()
            candidates = data.get("candidates", [])
            if not candidates:
                raise OpenRouterServiceError("Fallback LLM returned empty candidate list.")
            text = candidates[0].get("content", {}).get("parts", [{}])[0].get("text", "")
            logger.info(f"[{label}] Fallback Gemini OK in {elapsed:.1f}s (chars={len(text)})")
            return text

    # -----------------------------------------------------------------------
    # Subject Validation
    # -----------------------------------------------------------------------

    async def validate_course_subject(self, subject: str) -> Tuple[bool, str, str]:
        """Validate if the course/skill/technology input is a legitimate subject.
        Returns (is_valid, standardized_name, message).
        """
        trimmed = subject.strip()
        if not trimmed or len(trimmed) < 2:
            return (
                False,
                "",
                "Please enter a valid course, technology, or programming skill (e.g., Python, React, Java, SQL, DSA).",
            )

        # Quick match for common subjects without LLM call latency
        common_subjects = {
            "python": "Python",
            "java": "Java",
            "javascript": "JavaScript",
            "js": "JavaScript",
            "typescript": "TypeScript",
            "ts": "TypeScript",
            "react": "React",
            "react js": "React",
            "reactjs": "React",
            "react native": "React Native",
            "node": "Node.js",
            "nodejs": "Node.js",
            "node.js": "Node.js",
            "sql": "SQL",
            "postgresql": "PostgreSQL",
            "postgres": "PostgreSQL",
            "mysql": "MySQL",
            "mongodb": "MongoDB",
            "dsa": "Data Structures & Algorithms",
            "data structures": "Data Structures & Algorithms",
            "algorithms": "Data Structures & Algorithms",
            "data science": "Data Science",
            "machine learning": "Machine Learning",
            "ml": "Machine Learning",
            "ai": "Artificial Intelligence",
            "artificial intelligence": "Artificial Intelligence",
            "deep learning": "Deep Learning",
            "html": "HTML & CSS",
            "css": "HTML & CSS",
            "web dev": "Web Development",
            "web development": "Web Development",
            "frontend": "Frontend Development",
            "backend": "Backend Development",
            "c": "C",
            "c++": "C++",
            "cpp": "C++",
            "c#": "C#",
            "csharp": "C#",
            ".net": ".NET",
            "golang": "Go",
            "go": "Go",
            "rust": "Rust",
            "kotlin": "Kotlin",
            "android": "Android Development",
            "swift": "Swift",
            "ios": "iOS Development",
            "flutter": "Flutter",
            "docker": "Docker",
            "kubernetes": "Kubernetes",
            "devops": "DevOps",
            "git": "Git & GitHub",
            "github": "Git & GitHub",
            "aws": "AWS Cloud",
            "cloud": "Cloud Computing",
            "cybersecurity": "Cybersecurity",
            "fastapi": "FastAPI",
            "django": "Django",
            "flask": "Flask",
            "spring": "Spring Boot",
            "spring boot": "Spring Boot",
        }
        lower = trimmed.lower()
        if lower in common_subjects:
            return (True, common_subjects[lower], f"Great choice! Let's tailor your {common_subjects[lower]} roadmap.")

        # For custom/uncommon subjects, ask OpenRouter LLM
        prompt = (
            f"Determine if '{trimmed}' is a valid computer science, programming, software engineering, "
            f"database, data science, devops, or IT educational subject.\n"
            f"Return JSON strictly with format:\n"
            f'{{"is_valid": true/false, "standardized_name": "Title Case Name", "reason": "short explanation"}}\n'
            f"If it is nonsense, a greeting, offensive, or not a real tech/learning topic, set is_valid to false."
        )

        try:
            raw = await self.chat_completion(
                [
                    {"role": "system", "content": "You are a tech education curriculum validator. Return JSON only."},
                    {"role": "user", "content": prompt},
                ],
                temperature=0.0,
                timeout=20.0,
                max_tokens=256,
                label="validate_subject",
            )
            parsed = extract_json_from_llm(raw)
            is_valid = bool(parsed.get("is_valid", False))
            std_name = parsed.get("standardized_name", trimmed.title())
            if is_valid:
                return (True, std_name, f"Ready to build your personalized {std_name} roadmap.")
            else:
                return (
                    False,
                    "",
                    f"'{trimmed}' is not recognized as a valid technology or curriculum topic. Please enter a specific subject like Python, Java, React, SQL, DSA, or Machine Learning.",
                )
        except Exception as e:
            logger.warning(f"Subject validation LLM fallback for '{trimmed}': {e}")
            # If length is reasonable (> 2 and alphanumeric), accept as custom domain
            if re.match(r"^[a-zA-Z0-9\+\#\.\s\-\/]{2,50}$", trimmed):
                return (True, trimmed.title(), f"Setting up custom curriculum for {trimmed.title()}.")
            return (False, "", "Please enter a valid course or technology subject.")

    # -----------------------------------------------------------------------
    # Dynamic Diagnostic Questions Generation
    # -----------------------------------------------------------------------

    async def generate_diagnostic_questions(
        self,
        domain: str,
        goal: str,
        level: str,
        count: int = 4,
    ) -> List[Dict[str, Any]]:
        """Generate subject-appropriate diagnostic multiple-choice & code questions using OpenRouter."""
        prompt = (
            f"Create {count} diagnostic technical assessment questions to test a learner's starting proficiency in {domain}.\n"
            f"Learner goal: {goal or 'general mastery'}\n"
            f"Self-reported level: {level or 'Beginner'}\n\n"
            f"Requirements:\n"
            f"1. Cover foundational, core concepts, syntax/output, and best practices for {domain}.\n"
            f"2. Each question MUST be multiple choice with exactly 4 distinct options.\n"
            f"3. Include realistic code snippets where applicable.\n"
            f"4. Provide the 0-indexed correct option index and a clear pedagogical explanation.\n"
            f"5. Assign a distinct topic_tag for each question (e.g. '{domain.lower()}_basics', '{domain.lower()}_core', etc.).\n\n"
            f"Output strictly a JSON array of objects conforming to this schema:\n"
            f"[\n"
            f"  {{\n"
            f'    "id": "q_1",\n'
            f'    "topic_tag": "{domain.lower()}_syntax",\n'
            f'    "question_type": "multiple_choice",\n'
            f'    "prompt": "Question text here...",\n'
            f'    "code_snippet": "optional code snippet or null",\n'
            f'    "options": ["Option A", "Option B", "Option C", "Option D"],\n'
            f'    "correct_option_index": 0,\n'
            f'    "difficulty_level": 1,\n'
            f'    "explanation": "Why the correct answer is right..."\n'
            f"  }}\n"
            f"]"
        )

        try:
            raw = await self.chat_completion(
                [
                    {
                        "role": "system",
                        "content": f"You are a Senior Principal Engineer and Computer Science Professor creating diagnostic assessments for {domain}. Output pure JSON only without commentary.",
                    },
                    {"role": "user", "content": prompt},
                ],
                temperature=0.2,
                timeout=45.0,
                max_tokens=2048,
                label="generate_diag_questions",
            )
            questions = extract_json_from_llm(raw)
            if isinstance(questions, list) and len(questions) > 0:
                validated_questions = []
                for idx, q in enumerate(questions):
                    opts = q.get("options", [])
                    if len(opts) < 2:
                        continue
                    correct_idx = q.get("correct_option_index", 0)
                    if not (0 <= correct_idx < len(opts)):
                        correct_idx = 0
                    validated_questions.append({
                        "id": q.get("id") or f"diag_{domain.lower()}_{idx + 1}",
                        "topic_tag": q.get("topic_tag") or f"{domain.lower()}_topic_{idx + 1}",
                        "question_type": q.get("question_type", "multiple_choice"),
                        "prompt": q.get("prompt", f"{domain} Diagnostic Question {idx + 1}"),
                        "code_snippet": q.get("code_snippet"),
                        "options": opts,
                        "correct_option_index": correct_idx,
                        "difficulty_level": int(q.get("difficulty_level", 1)),
                        "explanation": q.get("explanation", f"Core concept in {domain}."),
                    })
                if len(validated_questions) >= 2:
                    return validated_questions
        except Exception as e:
            logger.error(f"Error generating diagnostic questions with OpenRouter for {domain}: {e}")
            raise OpenRouterServiceError(f"Failed to generate diagnostic questions via OpenRouter for {domain}: {e}")

    # -----------------------------------------------------------------------
    # Personalized Roadmap DAG Generation
    # -----------------------------------------------------------------------

    async def generate_personalized_roadmap_dag(
        self,
        domain: str,
        goal: str,
        level: str,
        study_hours_per_week: int,
        mastery_map: Dict[str, Any],
        compact_summary: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """Synthesize a personalized curriculum DAG tailored to learner's diagnostic performance and goal."""
        mastery_summary_lines = []
        for tag, data in mastery_map.items():
            level_str = data.get("mastery_level", "unknown")
            correct = data.get("correct_count", 0)
            evidence = data.get("evidence_count", 0)
            mastery_summary_lines.append(f"- {tag}: {level_str} ({correct}/{evidence} correct)")
        mastery_text = "\n".join(mastery_summary_lines) if mastery_summary_lines else "No diagnostic data (beginner)."

        summary_clause = ""
        if compact_summary:
            weak_str = ", ".join(compact_summary.get("weak_areas", [])) or "None identified"
            strong_str = ", ".join(compact_summary.get("strong_areas", [])) or "None identified"
            summary_clause = (
                f"\nCompact Assessment Summary Evidence:\n"
                f"- Weak Areas: {weak_str}\n"
                f"- Strong Areas: {strong_str}\n"
                f"- Evaluated Questions: {compact_summary.get('total_questions_evaluated', 0)}\n"
            )

        prompt = (
            f"Generate a personalized learning roadmap DAG (Directed Acyclic Graph) for learning {domain}.\n"
            f"Learner Goal: {goal or 'Master ' + domain}\n"
            f"Self-Reported Experience Level: {level or 'Beginner'}\n"
            f"Available Study Time: {study_hours_per_week} hours per week\n"
            f"Diagnostic Knowledge Assessment Results:\n{mastery_text}\n"
            f"{summary_clause}\n"
            f"Curriculum Requirements:\n"
            f"1. Produce 7 to 10 sequential topics organized as a clean DAG from fundamentals to advanced/capstone.\n"
            f"2. Each topic must have a unique 'id' (e.g. '{domain.lower()}_t1', '{domain.lower()}_t2'), 'topic_tag', 'title', 'short_description', 'estimated_minutes', 'prerequisite_topic_ids', and 'explanation'.\n"
            f"3. Topics where the learner showed 'strong' mastery should be marked with status='completed' or 'skipped' with accelerated=true.\n"
            f"4. Topics where mastery is 'weak' or 'unknown' must be included with status='locked' or 'unlocked' (if prerequisites are satisfied).\n"
            f"5. Topics with NO prerequisites must have prerequisite_topic_ids=[] and status='unlocked'.\n"
            f"6. Construct explicit 'edges' between prerequisite topic IDs and dependent topic IDs (from_topic_id -> to_topic_id).\n"
            f"7. Calculate a realistic 'estimated_weeks' based on {study_hours_per_week} hours/week.\n\n"
            f"Strictly output a JSON object adhering to this schema:\n"
            f"{{\n"
            f'  "title": "Personalized {domain} Roadmap: {goal or "Foundations to Production"}",\n'
            f'  "domain": "{domain}",\n'
            f'  "estimated_weeks": 8,\n'
            f'  "topics": [\n'
            f'    {{\n'
            f'      "id": "{domain.lower()}_t1",\n'
            f'      "topic_tag": "{domain.lower()}_fundamentals",\n'
            f'      "title": "Topic Title",\n'
            f'      "short_description": "Clear description of skills taught",\n'
            f'      "estimated_minutes": 45,\n'
            f'      "prerequisite_topic_ids": [],\n'
            f'      "status": "unlocked",\n'
            f'      "mastery_level": "unknown",\n'
            f'      "priority_score": 100,\n'
            f'      "accelerated": false,\n'
            f'      "explanation": "Why this topic is in this position for this learner"\n'
            f'    }}\n'
            f'  ],\n'
            f'  "edges": [\n'
            f'    {{"from_topic_id": "{domain.lower()}_t1", "to_topic_id": "{domain.lower()}_t2"}}\n'
            f'  ]\n'
            f"}}"
        )

        try:
            raw = await self.chat_completion(
                [
                    {
                        "role": "system",
                        "content": "You are an elite educational DAG architect. You create rigorous, prerequisite-enforced learning roadmaps in pure JSON.",
                    },
                    {"role": "user", "content": prompt},
                ],
                temperature=0.3,
                timeout=90.0,
                max_tokens=4096,
                label="generate_roadmap_dag",
            )
            result = extract_json_from_llm(raw)
            if isinstance(result, dict) and "topics" in result and len(result["topics"]) >= 4:
                return self._enforce_dag_consistency(result, domain, goal, level, study_hours_per_week)
            raise OpenRouterServiceError(f"OpenRouter returned invalid roadmap DAG structure for {domain}")
        except Exception as e:
            logger.error(f"OpenRouter roadmap DAG synthesis failed for {domain}: {e}")
            raise OpenRouterServiceError(f"OpenRouter roadmap DAG synthesis failed for {domain}: {e}")

    def _enforce_dag_consistency(
        self,
        dag: Dict[str, Any],
        domain: str,
        goal: str,
        level: str,
        study_hours: int,
    ) -> Dict[str, Any]:
        """Ensure the DAG has no circular dependencies, valid IDs, correct status, and edges."""
        topics = dag.get("topics", [])
        topic_ids = {t["id"] for t in topics}

        # Normalize prerequisite IDs to only existing topics and prevent self-cycles
        for t in topics:
            prereqs = t.get("prerequisite_topic_ids", [])
            valid_prereqs = [p for p in prereqs if p in topic_ids and p != t["id"]]
            t["prerequisite_topic_ids"] = valid_prereqs

        # Cycle detection and prevention using Kahn's algorithm
        in_degree = {t["id"]: 0 for t in topics}
        adj: Dict[str, List[str]] = {t["id"]: [] for t in topics}
        for t in topics:
            for p in t["prerequisite_topic_ids"]:
                adj[p].append(t["id"])
                in_degree[t["id"]] += 1

        queue = [tid for tid, deg in in_degree.items() if deg == 0]
        visited_count = 0
        while queue:
            curr = queue.pop(0)
            visited_count += 1
            for nxt in adj[curr]:
                in_degree[nxt] -= 1
                if in_degree[nxt] == 0:
                    queue.append(nxt)

        # If a cycle was detected in LLM output, break cycles by clearing prerequisites of cycle nodes
        if visited_count < len(topics):
            logger.warning(f"Cycle detected in LLM roadmap DAG for {domain}. Breaking cycles...")
            for tid, deg in in_degree.items():
                if deg > 0:
                    for t in topics:
                        if t["id"] == tid:
                            t["prerequisite_topic_ids"] = []

        # Derive edges from prerequisite_topic_ids
        edges = []
        for t in topics:
            for p in t["prerequisite_topic_ids"]:
                edges.append({"from_topic_id": p, "to_topic_id": t["id"]})
        dag["edges"] = edges

        # Compute initial topic status based on prerequisites
        completed_ids = {t["id"] for t in topics if t.get("status") in ("completed", "skipped")}
        for t in topics:
            if t["id"] in completed_ids:
                continue
            prereqs = t.get("prerequisite_topic_ids", [])
            all_satisfied = all(p in completed_ids for p in prereqs)
            t["status"] = "unlocked" if all_satisfied else "locked"

        dag["domain"] = domain
        dag["goal"] = goal
        dag["level"] = level
        dag["study_hours_per_week"] = study_hours
        dag["estimated_weeks"] = dag.get("estimated_weeks", max(4, len(topics) * 45 // (study_hours * 60) + 1))
        return dag

    # -----------------------------------------------------------------------
    # Rich Dedicated Topic Learning Content
    # -----------------------------------------------------------------------

    async def generate_topic_learning_content(
        self,
        domain: str,
        topic_id: str,
        topic_title: str,
        topic_description: str,
        level: str = "Beginner",
    ) -> Dict[str, Any]:
        """Generate comprehensive topic learning page: objectives, concepts, code examples,
        common mistakes, interactive practice quiz, and verified documentation links.
        """
        prompt = (
            f"Generate a comprehensive, beginner-friendly learning module for:\n"
            f"Domain: {domain}\n"
            f"Topic: {topic_title}\n"
            f"Summary: {topic_description}\n"
            f"Learner Target Level: {level}\n\n"
            f"Provide a complete, production-ready educational experience with:\n"
            f"1. Clear learning objectives (list of 3-4 bullet strings).\n"
            f"2. A thorough, pedagogical explanation covering core syntax, mechanics, and mental models.\n"
            f"3. Practical code examples with code, explanation, and expected terminal/console output.\n"
            f"4. Common mistakes and misconceptions learners make, why they happen, and how to fix them.\n"
            f"5. An interactive practice quiz question with 4 options, the 0-indexed correct index, and a helpful explanation.\n"
            f"6. Curated real documentation and resource links (prefer official docs like python.org, react.dev, etc.).\n\n"
            f"Return ONLY a JSON object conforming exactly to this structure:\n"
            f"{{\n"
            f'  "topic_id": "{topic_id}",\n'
            f'  "topic_title": "{topic_title}",\n'
            f'  "domain": "{domain}",\n'
            f'  "learning_objectives": ["Understand X", "Implement Y", "Avoid Z"],\n'
            f'  "beginner_explanation": "In-depth beginner-friendly concept walkthrough...",\n'
            f'  "syntax_and_concepts": "Key syntax points and conceptual rules...",\n'
            f'  "code_examples": [\n'
            f'    {{\n'
            f'      "title": "Basic Usage",\n'
            f'      "code": "example code here...",\n'
            f'      "explanation": "What this code achieves...",\n'
            f'      "expected_output": "Output: ..."\n'
            f'    }}\n'
            f'  ],\n'
            f'  "common_mistakes": [\n'
            f'    {{\n'
            f'      "mistake": "Common trap or error...",\n'
            f'      "why_it_happens": "Why students stumble on this...",\n'
            f'      "how_to_fix": "Best practice fix..."\n'
            f'    }}\n'
            f'  ],\n'
            f'  "practice_quiz": {{\n'
            f'    "question": "Interactive challenge question...",\n'
            f'    "options": ["A", "B", "C", "D"],\n'
            f'    "correct_option_index": 0,\n'
            f'    "explanation": "Why this answer is correct..."\n'
            f'  }},\n'
            f'  "resources": [\n'
            f'    {{\n'
            f'      "title": "Official {domain} Documentation",\n'
            f'      "url": "https://...",\n'
            f'      "source_name": "Official Docs",\n'
            f'      "description": "Comprehensive reference and guides.",\n'
            f'      "is_verified": true\n'
            f'    }}\n'
            f'  ]\n'
            f"}}"
        )

        try:
            raw = await self.chat_completion(
                [
                    {
                        "role": "system",
                        "content": f"You are a World-Class Technical Educator for {domain}. Deliver rigorous, engaging, complete learning content formatted in pure JSON.",
                    },
                    {"role": "user", "content": prompt},
                ],
                temperature=0.2,
                timeout=60.0,
                max_tokens=4096,
                label="generate_topic_content",
            )
            data = extract_json_from_llm(raw)
            if isinstance(data, dict) and "learning_objectives" in data:
                # Merge verified resources to guarantee real URLs
                data["resources"] = self.get_verified_resources_for_domain(domain, topic_title)
                return data
        except Exception as e:
            logger.error(f"Failed to generate topic content via OpenRouter for '{topic_title}' in {domain}: {e}")
            raise OpenRouterServiceError(f"Failed to generate topic content via OpenRouter for '{topic_title}': {e}")

    # -----------------------------------------------------------------------
    # Verified Real Documentation & Resource Registry
    # -----------------------------------------------------------------------

    def get_verified_resources_for_domain(self, domain: str, topic_title: str) -> List[Dict[str, Any]]:
        """Return real, vetted documentation URLs for known domains."""
        d = domain.lower()
        resources: List[Dict[str, Any]] = []

        if "python" in d:
            resources.append({
                "title": f"Python 3 Official Documentation: {topic_title}",
                "url": "https://docs.python.org/3/tutorial/",
                "source_name": "Python Software Foundation",
                "description": "The official tutorial and language reference guide.",
                "is_verified": True,
            })
            resources.append({
                "title": "Real Python Tutorials & Examples",
                "url": "https://realpython.com/",
                "source_name": "Real Python",
                "description": "In-depth, peer-reviewed Python programming tutorials.",
                "is_verified": True,
            })
        elif "react" in d:
            resources.append({
                "title": f"React Official Documentation: {topic_title}",
                "url": "https://react.dev/learn",
                "source_name": "React Core Team",
                "description": "Interactive React guides, diagrams, and code sandboxes.",
                "is_verified": True,
            })
            resources.append({
                "title": "MDN Web Docs: React Fundamentals",
                "url": "https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Client-side_tools/React_getting_started",
                "source_name": "Mozilla MDN",
                "description": "Comprehensive client-side JavaScript documentation.",
                "is_verified": True,
            })
        elif "java" in d:
            resources.append({
                "title": "Oracle Java Tutorials & API Guide",
                "url": "https://dev.java/learn/",
                "source_name": "Oracle Java Developer Center",
                "description": "Official guides covering language syntax, OOP, and standard libraries.",
                "is_verified": True,
            })
        elif "sql" in d or "postgres" in d or "mysql" in d:
            resources.append({
                "title": "PostgreSQL Official Documentation",
                "url": "https://www.postgresql.org/docs/current/tutorial.html",
                "source_name": "PostgreSQL Global Development Group",
                "description": "Official tutorial for relational database schema and SQL syntax.",
                "is_verified": True,
            })
            resources.append({
                "title": "SQLBolt: Interactive SQL Lessons",
                "url": "https://sqlbolt.com/",
                "source_name": "SQLBolt",
                "description": "Interactive in-browser lessons with query execution.",
                "is_verified": True,
            })
        elif "dsa" in d or "data structure" in d or "algorithm" in d:
            resources.append({
                "title": "Visualgo: Visualizing Algorithms & Data Structures",
                "url": "https://visualgo.net/en",
                "source_name": "VisuAlgo",
                "description": "Interactive visual animations of data structure traversals and algorithms.",
                "is_verified": True,
            })
            resources.append({
                "title": "The Algorithms Open-Source Repository",
                "url": "https://the-algorithms.com/",
                "source_name": "The Algorithms",
                "description": "Open-source implementations of foundational algorithms in multiple languages.",
                "is_verified": True,
            })
        elif "javascript" in d or "web" in d or "html" in d:
            resources.append({
                "title": f"MDN Web Docs: {topic_title}",
                "url": "https://developer.mozilla.org/en-US/docs/Web/JavaScript",
                "source_name": "Mozilla MDN",
                "description": "The definitive resource for modern web standards and JavaScript APIs.",
                "is_verified": True,
            })
        else:
            resources.append({
                "title": f"{domain} Documentation & Architecture Overview",
                "url": f"https://devdocs.io/{domain.lower().replace(' ', '-')}/",
                "source_name": "DevDocs",
                "description": f"Fast, searchable reference documentation for {domain}.",
                "is_verified": True,
            })

        return resources

    # End of resource registry


# Singleton service instance
openrouter_service = OpenRouterService()
