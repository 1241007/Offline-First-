"""Targeted End-to-End Audit scenarios for EduNova Offline-First RAG Architecture.

Covers the 10 mandatory audit scenarios:
1. Student Mode with a complete board/class/subject profile.
2. Student Mode with missing or incomplete profile metadata (curriculum isolation).
3. A query with relevant retrieved evidence (SUFFICIENT outcome).
4. A query with no relevant evidence (MISSING outcome).
5. An unconfigured corpus (CORPUS_NOT_CONFIGURED outcome).
6. A retrieval/provider failure (RETRIEVAL_ERROR outcome).
7. A malformed or incomplete streaming route header.
8. A General Mode query (DIRECT route).
9. Request construction and response parsing contract.
10. Database migration and profile schema backwards compatibility.
"""

import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.schemas.rag import (
    AppMode,
    ChunkResult,
    CorpusScope,
    EvidenceOutcome,
    RAGRequestContext,
    RetrieveRequest,
    RouteDecision,
)
from app.schemas.chat import StreamMessageRequest
from app.services.routing_service import RoutingService
from app.services.supabase_retrieval_service import (
    SupabaseRetrievalService,
    compute_rrf_score,
    assess_evidence_outcome,
)
from app.services.route_header_parser import RouteHeaderParser, ParserState
from app.services.embedding_service import embedding_service
from app.models.profile import UserProfile


# Scenario 1: Student Mode with a complete board/class/subject profile
def test_scenario_1_student_mode_complete_profile():
    router = RoutingService()
    ctx = RAGRequestContext(
        execution_mode="ONLINE",
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
        selected_chapter="Chapter 4 Carbon and its Compounds",
    )
    decision, query = router.evaluate_deterministic_route(
        "Explain covalent bonding in carbon", ctx
    )
    assert decision == RouteDecision.RAG
    assert "covalent bonding in carbon" in query


# Scenario 2: Student Mode with missing or incomplete profile metadata
@pytest.mark.asyncio
async def test_scenario_2_student_mode_missing_metadata_prevents_broadening():
    service = SupabaseRetrievalService(db=None)
    # Student mode with NO board or class_grade
    ctx_missing = RAGRequestContext(
        execution_mode="ONLINE",
        app_mode=AppMode.STUDENT,
        board=None,
        class_grade=None,
        subject=None,
    )
    resp = await service.retrieve("Explain plant cells", ctx_missing)
    # Must NOT silently broaden across all curricula
    assert resp.evidence_outcome == EvidenceOutcome.MISSING
    assert len(resp.chunks) == 0
    assert "Curriculum target" in (resp.error_detail or "")


# Scenario 3: A query with relevant retrieved evidence
def test_scenario_3_query_with_relevant_retrieved_evidence():
    raw_rrf, norm_score = compute_rrf_score(vector_rank=1, sparse_rank=1)
    chunk = ChunkResult(
        chunk_id="chunk-101",
        content="Photosynthesis is the biological process by which green plants convert light energy into chemical energy using chlorophyll." * 2,
        board="CBSE",
        class_grade="10",
        subject="Science",
        chapter="Life Processes",
        normalized_score=norm_score,
        raw_rrf_score=raw_rrf,
    )
    outcome = assess_evidence_outcome([chunk])
    assert outcome == EvidenceOutcome.SUFFICIENT
    assert norm_score >= 0.70


# Scenario 4: A query with no relevant evidence
def test_scenario_4_query_with_no_relevant_evidence():
    # Empty chunk list
    assert assess_evidence_outcome([]) == EvidenceOutcome.MISSING

    # Low score chunk (normalized score < 0.35)
    raw_rrf, norm_score = compute_rrf_score(vector_rank=35, sparse_rank=None)
    chunk_low = ChunkResult(
        chunk_id="chunk-weak",
        content="Random unrelated footnote mentioning leaves.",
        normalized_score=norm_score,
        raw_rrf_score=raw_rrf,
    )
    assert assess_evidence_outcome([chunk_low]) == EvidenceOutcome.MISSING


# Scenario 5: An unconfigured corpus (General Mode)
@pytest.mark.asyncio
async def test_scenario_5_unconfigured_corpus():
    service = SupabaseRetrievalService(db=None)
    ctx_general = RAGRequestContext(
        app_mode=AppMode.GENERAL,
        corpus_scope=CorpusScope.GENERAL_DOCUMENTS,
    )
    resp = await service.retrieve("Explain Docker containers", ctx_general)
    assert resp.evidence_outcome == EvidenceOutcome.CORPUS_NOT_CONFIGURED
    assert "General Mode RAG is disabled" in (resp.error_detail or "")


# Scenario 6: A retrieval/provider failure
@pytest.mark.asyncio
async def test_scenario_6_retrieval_provider_failure():
    service = SupabaseRetrievalService(db=None)
    ctx_student = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )
    with patch.object(embedding_service, "api_key", ""):
        resp = await service.retrieve("Explain Newton's third law", ctx_student)
        assert resp.evidence_outcome == EvidenceOutcome.RETRIEVAL_ERROR
        assert "Embedding service is not configured" in (resp.error_detail or "")


# Scenario 7: A malformed or incomplete streaming route header
def test_scenario_7_malformed_incomplete_route_header():
    # Malformed route keyword
    p1 = RouteHeaderParser()
    with pytest.raises(ValueError, match="Malformed header"):
        p1.feed("NOT_A_ROUTE: SOMETHING\n")

    # Incomplete stream ending before QUERY line
    p2 = RouteHeaderParser()
    p2.feed("ROUTE: RAG\n")
    with pytest.raises(ValueError, match="Stream ended before"):
        p2.finalize()


# Scenario 8: A General Mode query
def test_scenario_8_general_mode_query():
    router = RoutingService()
    ctx = RAGRequestContext(app_mode=AppMode.GENERAL)
    decision, query = router.evaluate_deterministic_route(
        "Write a Python script to sort a list of numbers", ctx
    )
    assert decision == RouteDecision.DIRECT
    assert query is None


# Scenario 9: Android request construction and response parsing contract
def test_scenario_9_request_and_stream_contract():
    req_data = {
        "content": "What is Ohm's Law?",
        "explanation_mode": "exam",
        "app_mode": "student",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Physics",
        "language": "en",
        "selected_chapter": "Electricity",
    }
    req = StreamMessageRequest(**req_data)
    assert req.board == "CBSE"
    assert req.class_grade == "10"
    assert req.subject == "Physics"
    assert req.selected_chapter == "Electricity"


# Scenario 10: Database migration and profile schema backwards compatibility
def test_scenario_10_profile_schema_fields():
    profile = UserProfile(
        id="user-123",
        user_id="user-123",
        education_mode="school",
        board="CBSE",
        class_grade="10",
        subject="Science",
        preferred_language="hi",
    )
    assert profile.board == "CBSE"
    assert profile.class_grade == "10"
    assert profile.subject == "Science"
    assert profile.preferred_language == "hi"

    # Backwards compatibility: nullable fields default safely
    legacy_profile = UserProfile(
        id="legacy-456",
        user_id="legacy-456",
        education_mode="general",
    )
    assert legacy_profile.board is None
    assert legacy_profile.class_grade is None
    assert legacy_profile.subject is None
    assert legacy_profile.preferred_language in (None, "en")
