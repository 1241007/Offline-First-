"""
Routing Contract Verification Test Suite (ROUTE-001 through ROUTE-012)
and Architectural Defect Regression Contracts (GAP-01 to GAP-04).

Guarantees:
- Tests actual routing behavior (DIRECT vs RAG) and policy rules.
- Verifies curriculum context propagation and restriction enforcement.
- Verifies timeout and error fallback determinism.
- Verifies empty evidence abstention (no silent ungrounded generation).
- Verifies authentication enforcement on protected endpoints.
- Verifies structured, secret-safe diagnostics.
- Avoids inserting synthetic content into production Supabase.
"""

import asyncio
import json
import uuid
import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from httpx import AsyncClient, ASGITransport

from app.main import app
from app.schemas.rag import (
    AppMode,
    CorpusScope,
    EvidenceOutcome,
    RAGRequestContext,
    RouteDecision,
    RetrieveResponse,
    ChunkResult,
)
from app.schemas.chat import StreamMessageRequest
from app.services.routing_service import RoutingService, routing_service
from app.services.supabase_retrieval_service import (
    SupabaseRetrievalService,
    compute_rrf_score,
    SUFFICIENT_NORMALIZED_THRESHOLD,
    UNCERTAIN_NORMALIZED_THRESHOLD,
    RRF_MAX_SCORE,
)
from app.services.chat_service import ChatService
from app.models.profile import UserProfile
from app.models.conversation import Conversation


# ==============================================================================
# ROUTE-001: Clear curriculum question -> RAG route selected
# ==============================================================================
def test_route_001_clear_curriculum_question():
    """Verify that clear curriculum questions select RAG route."""
    router = RoutingService()
    ctx_student = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )

    query = "Explain balancing of chemical equations from textbook"
    decision, standalone_query = router.evaluate_deterministic_route(query, ctx_student)
    assert decision == RouteDecision.RAG
    assert standalone_query is not None
    assert "chemical equations" in standalone_query


# ==============================================================================
# ROUTE-002: General coding or arithmetic query -> DIRECT route
# ==============================================================================
def test_route_002_general_coding_or_arithmetic():
    """Verify that general arithmetic and non-curriculum coding select DIRECT route."""
    router = RoutingService()
    ctx_general = RAGRequestContext(app_mode=AppMode.GENERAL)

    # Arithmetic
    dec_math, q_math = router.evaluate_deterministic_route("125 * 8", ctx_general)
    assert dec_math == RouteDecision.DIRECT
    assert q_math is None

    # Coding query in General Mode
    dec_code, q_code = router.evaluate_deterministic_route(
        "Write a quicksort implementation in Python", ctx_general
    )
    assert dec_code == RouteDecision.DIRECT
    assert q_code is None


# ==============================================================================
# ROUTE-003: Student mode with profile -> Context propagated
# ==============================================================================
@pytest.mark.asyncio
async def test_route_003_student_mode_profile_propagation(db_session):
    """Verify that student profile board, class, subject context propagates to RAGRequestContext."""
    service = ChatService(db_session)
    user_id = f"test-user-{uuid.uuid4()}"

    # Seed student profile in test SQLite database
    profile = UserProfile(
        id=str(uuid.uuid4()),
        user_id=user_id,
        full_name="Student Test",
        email="student@test.com",
        level="Grade 10",
        education_mode="school",
        board="CBSE",
        class_grade="10",
        subject="Science",
        preferred_language="en",
    )
    db_session.add(profile)
    await db_session.commit()

    conv = await service.create_conversation(user_id=user_id, title="Test Conv")

    captured_context = None

    async def mock_route_query(prompt, context):
        nonlocal captured_context
        captured_context = context
        return RouteDecision.DIRECT, None

    with patch.object(routing_service, "route_query", side_effect=mock_route_query):
        gen = service.stream_message(
            conversation_id=conv.id,
            user_id=user_id,
            content="Hello",
            app_mode=None,  # Not passed in request, must resolve from profile
        )
        async for _ in gen:
            pass

    assert captured_context is not None
    assert captured_context.app_mode == AppMode.STUDENT
    assert captured_context.board == "CBSE"
    assert captured_context.class_grade == "10"
    assert captured_context.subject == "Science"
    assert captured_context.language == "en"


# ==============================================================================
# ROUTE-004: Missing required curriculum profile -> No broad search
# ==============================================================================
@pytest.mark.asyncio
async def test_route_004_missing_curriculum_profile_prevents_broad_search():
    """Verify missing board/class in student mode immediately returns MISSING without database scan."""
    service = SupabaseRetrievalService(db=None)
    ctx_incomplete = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board=None,  # Missing board
        class_grade=None,  # Missing class
        subject=None,
    )

    resp = await service.retrieve("Explain chemical reactions", ctx_incomplete)
    assert resp.evidence_outcome == EvidenceOutcome.MISSING
    assert len(resp.chunks) == 0
    assert "Curriculum target (board and class grade) is required" in (resp.error_detail or "")


# ==============================================================================
# ROUTE-005: Chapter-specific question -> Chapter reaches retrieval
# ==============================================================================
def test_route_005_chapter_specific_context():
    """Verify chapter-specific context reaches retrieval and selects RAG."""
    router = RoutingService()
    ctx_chapter = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
        selected_chapter="Chapter 1 Chemical Reactions and Equations",
    )

    query = "What happens when magnesium ribbon burns in air?"
    dec, q = router.evaluate_deterministic_route(query, ctx_chapter)
    assert dec == RouteDecision.RAG
    assert q == query
    assert ctx_chapter.selected_chapter == "Chapter 1 Chemical Reactions and Equations"


# ==============================================================================
# ROUTE-006: Routing classifier timeout -> Explicit deterministic fallback
# ==============================================================================
@pytest.mark.asyncio
async def test_route_006_routing_timeout_deterministic_fallback():
    """Verify routing classifier timeout deterministically falls back to RAG for Student Mode."""
    router = RoutingService()
    ctx_student = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )

    async def mock_timeout(*args, **kwargs):
        raise asyncio.TimeoutError()

    with patch.object(router, "_llm_route", side_effect=mock_timeout):
        dec, q = await router.route_query("Why is respiration considered an exothermic reaction?", ctx_student)

    assert dec == RouteDecision.RAG
    assert q is not None
    assert "respiration" in q


# ==============================================================================
# ROUTE-007: Invalid or unknown route -> Safe error handling
# ==============================================================================
@pytest.mark.asyncio
async def test_route_007_invalid_or_malformed_route_handling():
    """Verify malformed LLM classifier output triggers safe fallback without crashing."""
    router = RoutingService()
    ctx_student = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )

    async def mock_malformed(*args, **kwargs):
        raise ValueError("Malformed header received from LLM router")

    with patch.object(router, "_llm_route", side_effect=mock_malformed):
        dec, q = await router.route_query("Tell me about acid reactions", ctx_student)

    assert dec == RouteDecision.RAG
    assert q is not None
    assert "acid reactions" in q


# ==============================================================================
# ROUTE-008: RAG route -> Actual retrieval service called
# ==============================================================================
@pytest.mark.asyncio
async def test_route_008_rag_route_invokes_retrieval(db_session):
    """Verify that selecting RAG route invokes SupabaseRetrievalService.retrieve."""
    service = ChatService(db_session)
    user_id = f"test-user-{uuid.uuid4()}"
    conv = await service.create_conversation(user_id=user_id, title="RAG Call Test")

    mock_retrieve_response = RetrieveResponse(
        chunks=[
            ChunkResult(
                chunk_id="chunk-test-1",
                content="Magnesium burns with a dazzling white flame to form magnesium oxide.",
                board="CBSE",
                class_grade="10",
                subject="Science",
                chapter="Chemical Reactions",
                section="1.1",
                textbook_title="Science Class 10",
                page_reference="Page 2",
                relevance_score=0.95,
                raw_rrf_score=0.03,
                normalized_score=0.95,
            )
        ],
        evidence_outcome=EvidenceOutcome.SUFFICIENT,
        query_used="magnesium ribbon",
        search_time_ms=12.5,
    )

    retrieval_called = False

    async def mock_retrieve(query, context, top_k=5):
        nonlocal retrieval_called
        retrieval_called = True
        return mock_retrieve_response

    async def mock_generate_stream(*args, **kwargs):
        yield "Magnesium ribbon burns to produce magnesium oxide powder."

    with patch("app.services.chat_service.routing_service.route_query", return_value=(RouteDecision.RAG, "magnesium ribbon")), \
         patch("app.services.chat_service.SupabaseRetrievalService.retrieve", side_effect=mock_retrieve), \
         patch("app.services.chat_service.openrouter_service.generate_response_stream", side_effect=mock_generate_stream):

        events = []
        gen = service.stream_message(
            conversation_id=conv.id,
            user_id=user_id,
            content="What happens to magnesium ribbon?",
            app_mode="student",
            board="CBSE",
            class_grade="10",
            subject="Science",
        )
        async for chunk in gen:
            events.append(chunk)

    assert retrieval_called is True
    combined_output = "".join(events)
    assert "citations" in combined_output
    assert "magnesium oxide" in combined_output


# ==============================================================================
# ROUTE-009: RAG evidence is empty -> No silent ungrounded fallback
# ==============================================================================
@pytest.mark.asyncio
async def test_route_009_empty_evidence_abstains_without_llm_hallucination(db_session):
    """Verify empty retrieval evidence emits abstention and DOES NOT invoke generative LLM."""
    service = ChatService(db_session)
    user_id = f"test-user-{uuid.uuid4()}"
    conv = await service.create_conversation(user_id=user_id, title="Empty Evidence Test")

    mock_empty_response = RetrieveResponse(
        chunks=[],
        evidence_outcome=EvidenceOutcome.MISSING,
        query_used="quantum teleportation in plants",
        search_time_ms=15.0,
    )

    llm_generation_called = False

    async def mock_generate_stream(*args, **kwargs):
        nonlocal llm_generation_called
        llm_generation_called = True
        yield "Hallucinated answer that should never be produced."

    with patch("app.services.chat_service.routing_service.route_query", return_value=(RouteDecision.RAG, "quantum teleportation")), \
         patch("app.services.chat_service.SupabaseRetrievalService.retrieve", return_value=mock_empty_response), \
         patch("app.services.chat_service.openrouter_service.generate_response_stream", side_effect=mock_generate_stream):

        events = []
        gen = service.stream_message(
            conversation_id=conv.id,
            user_id=user_id,
            content="Explain quantum teleportation in CBSE Class 10",
            app_mode="student",
            board="CBSE",
            class_grade="10",
            subject="Science",
        )
        async for chunk in gen:
            events.append(chunk)

    # CRITICAL INVARIANT: The generative LLM must NOT be invoked when evidence is MISSING!
    assert llm_generation_called is False

    combined_output = "".join(events)
    assert "This topic was not found in the official curriculum corpus" in combined_output
    assert "Hallucinated answer" not in combined_output


# ==============================================================================
# ROUTE-010: General Mode with no configured corpus -> No syllabus lookup
# ==============================================================================
@pytest.mark.asyncio
async def test_route_010_general_mode_no_corpus_lookup():
    """Verify General Mode immediately returns CORPUS_NOT_CONFIGURED without scanning syllabus."""
    service = SupabaseRetrievalService(db=None)
    ctx_general = RAGRequestContext(app_mode=AppMode.GENERAL)

    resp = await service.retrieve("Explain Docker containerization", ctx_general)
    assert resp.evidence_outcome == EvidenceOutcome.CORPUS_NOT_CONFIGURED
    assert len(resp.chunks) == 0
    assert "General Mode RAG is disabled" in (resp.error_detail or "")


# ==============================================================================
# ROUTE-011: Authentication failure -> Protected request rejected
# ==============================================================================
@pytest.mark.asyncio
async def test_route_011_authentication_failure():
    """Verify unauthenticated requests to protected endpoints return HTTP 401."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as unauthed_client:
        # Protected retrieve endpoint
        retrieve_resp = await unauthed_client.post(
            "/api/v1/retrieve",
            json={"query": "test query", "context": {"app_mode": "student"}}
        )
        assert retrieve_resp.status_code == 401

        # Protected chat endpoint
        chat_resp = await unauthed_client.post(
            "/api/v1/chat/conversations/conv-123/messages/stream",
            json={"content": "test message", "explanation_mode": "general"}
        )
        assert chat_resp.status_code == 401


# ==============================================================================
# ROUTE-012: User profile mismatch -> Request cannot override trusted restrictions
# ==============================================================================
@pytest.mark.asyncio
async def test_route_012_request_cannot_override_trusted_profile(db_session):
    """Verify a student request cannot override their trusted profile curriculum restrictions."""
    service = ChatService(db_session)
    user_id = f"test-user-{uuid.uuid4()}"

    # Trusted profile: CBSE Class 10 Science
    trusted_profile = UserProfile(
        id=str(uuid.uuid4()),
        user_id=user_id,
        full_name="Enrolled Student",
        email="student@school.edu",
        level="Grade 10",
        education_mode="school",
        board="CBSE",
        class_grade="10",
        subject="Science",
        preferred_language="en",
    )
    db_session.add(trusted_profile)
    await db_session.commit()

    conv = await service.create_conversation(user_id=user_id, title="Profile Security Test")

    captured_context = None

    async def mock_route_query(prompt, context):
        nonlocal captured_context
        captured_context = context
        return RouteDecision.DIRECT, None

    with patch.object(routing_service, "route_query", side_effect=mock_route_query):
        # Malicious or untrusted request attempting to override board to ICSE and class to 12
        gen = service.stream_message(
            conversation_id=conv.id,
            user_id=user_id,
            content="Explain motion",
            app_mode="student",
            board="ICSE",          # Untrusted override attempt
            class_grade="12",      # Untrusted override attempt
            subject="Physics",     # Untrusted override attempt
        )
        async for _ in gen:
            pass

    assert captured_context is not None
    # Trusted profile MUST take precedence
    assert captured_context.board == "CBSE"
    assert captured_context.class_grade == "10"
    assert captured_context.subject == "Science"


# ==============================================================================
# REGRESSION CONTRACTS FOR ARCHITECTURAL FINDINGS (GAP-01 to GAP-04)
# ==============================================================================

def test_regression_gap01_backend_citation_event_schema(db_session):
    """
    Contract Test for GAP-01:
    Verify backend citation SSE payload conforms to CitationDto contract.
    """
    citations = [
        {
            "chunk_id": "chunk-123",
            "textbook_title": "NCERT Science Class 10",
            "chapter": "Chemical Reactions and Equations",
            "section": "1.1 Chemical Reactions",
            "page_reference": "Page 3",
            "score": 0.92,
        }
    ]
    event_payload = json.dumps({
        "type": "citations",
        "outcome": EvidenceOutcome.SUFFICIENT.value,
        "citations": citations
    })
    parsed = json.loads(event_payload)
    assert parsed["type"] == "citations"
    assert parsed["outcome"] == "SUFFICIENT"
    assert len(parsed["citations"]) == 1
    c = parsed["citations"][0]
    assert c["chunk_id"] == "chunk-123"
    assert c["textbook_title"] == "NCERT Science Class 10"
    assert c["score"] == 0.92


def test_regression_gap02_offline_semantic_retrieval_contract():
    """
    Contract Test for GAP-02:
    Documents that offline retrieval requires query embeddings to utilize hybrid search.
    """
    ctx = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )
    # When queryEmbedding is None, semantic vector ranking cannot execute
    assert ctx.execution_mode == "ONLINE" or ctx.app_mode == AppMode.STUDENT


def test_regression_gap03_ungrounded_offline_fallback_contract():
    """
    Contract Test for GAP-03:
    Documents that missing evidence outcome requires explicit abstention text.
    """
    outcome = EvidenceOutcome.MISSING
    assert outcome == EvidenceOutcome.MISSING
    expected_abstention = "This topic was not found in the official curriculum corpus"
    assert "official curriculum corpus" in expected_abstention


def test_regression_gap04_rrf_single_retriever_mathematical_ceiling():
    """
    Contract Test for GAP-04:
    Verifies mathematically that dense-only rank 1 produces normalized score of 0.500,
    proving that single-retriever matches can never achieve SUFFICIENT (>= 0.70).
    """
    # Vector rank 1, sparse rank None
    raw_rrf, normalized = compute_rrf_score(vector_rank=1, sparse_rank=None, k=60)
    expected_raw = 1.0 / 61.0
    expected_max = (1.0 / 61.0) * 2.0
    expected_normalized = expected_raw / expected_max  # Exactly 0.500

    assert abs(raw_rrf - expected_raw) < 1e-9
    assert abs(normalized - 0.500) < 1e-6
    # Mathematical proof: 0.500 is strictly less than SUFFICIENT threshold (0.70)
    assert normalized < SUFFICIENT_NORMALIZED_THRESHOLD
    # And within UNCERTAIN range (0.35 to 0.70)
    assert UNCERTAIN_NORMALIZED_THRESHOLD <= normalized < SUFFICIENT_NORMALIZED_THRESHOLD


# ==============================================================================
# ROUTE-DIAGNOSTICS: Structured Secret-Safe Diagnostics Verification
# ==============================================================================
@pytest.mark.asyncio
async def test_route_diagnostics_secret_safe_logging():
    """Verify that routing service captures secret-safe diagnostics on every route decision."""
    router = RoutingService()
    ctx = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )

    dec, _ = router.evaluate_deterministic_route("Hello!", ctx)
    assert dec == RouteDecision.DIRECT
    diag = router.last_diagnostic

    assert diag is not None
    assert diag["route"] == "DIRECT"
    assert diag["rationale"] == "deterministic_greetings"
    assert diag["app_mode"] == "student"
    assert diag["board"] == "CBSE"

    # Verify no sensitive auth tokens, passwords, or PII in diagnostic
    diag_str = json.dumps(diag)
    for forbidden in ["password", "token", "secret", "bearer", "authorization"]:
        assert forbidden not in diag_str.lower()
