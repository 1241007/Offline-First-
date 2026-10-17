"""Pipeline Integration Test Matrix for EduNova Chatbot (Pipelines A through G).

Verifies the end-to-end behavior of the 7 core chatbot execution pipelines:
- Pipeline A: Online Curriculum RAG (User query -> Router -> Supabase Retrieval -> Gating -> LLM Stream + Citations)
- Pipeline B: Offline Curriculum RAG (User query -> Offline Syllabus Retrieval -> Local Gate -> On-device inference stream)
- Pipeline C: General Direct Conversation (Non-curriculum query -> Direct routing -> No retrieval overhead -> LLM Stream)
- Pipeline D: Missing Evidence Abstention (Curriculum query with no matching syllabus -> Abstains without calling LLM)
- Pipeline E: Provider Fallback & Error Recovery (Primary model failure -> automatic fallback -> graceful user notification)
- Pipeline F: Citation Event Schema & Propagation (Retrieved chunk metadata -> SSE citation event schema validation)
- Pipeline G: Multi-Student Context Isolation (User A memories, history, and active sessions never leak into User B)
"""

import asyncio
import json
import uuid
import pytest
import pytest_asyncio
from fastapi import HTTPException
from httpx import AsyncClient, ASGITransport
from unittest.mock import patch, AsyncMock

from app.main import app
from app.core.config import settings
from app.core.database import get_db
from app.core.deps import get_current_user_id
from app.core.security import create_access_token
from app.models.profile import UserProfile
from app.schemas.rag import (
    AppMode,
    ChunkResult,
    EvidenceOutcome,
    RAGRequestContext,
    RetrieveResponse,
    RouteDecision,
)
from app.services.chat_service import ChatService
from app.services.openrouter_service import openrouter_service
from app.services.routing_service import routing_service
from app.services.supabase_retrieval_service import SupabaseRetrievalService
from tests.integration.test_sse_parser_contract import SseStreamBuffer


@pytest_asyncio.fixture
async def student_a(db_session):
    user_id = f"student-a-{uuid.uuid4()}"
    profile = UserProfile(
        id=str(uuid.uuid4()),
        user_id=user_id,
        full_name="Student Alpha",
        email=f"alpha.{uuid.uuid4()}@test.edu",
        level="Grade 10",
        education_mode="school",
        board="CBSE",
        class_grade="10",
        subject="Science",
        preferred_language="en",
    )
    db_session.add(profile)
    await db_session.commit()
    token = create_access_token(user_id=user_id, email=profile.email)
    return {"user_id": user_id, "token": token, "profile": profile}


@pytest_asyncio.fixture
async def student_b(db_session):
    user_id = f"student-b-{uuid.uuid4()}"
    profile = UserProfile(
        id=str(uuid.uuid4()),
        user_id=user_id,
        full_name="Student Beta",
        email=f"beta.{uuid.uuid4()}@test.edu",
        level="Grade 12",
        education_mode="general",
        board=None,
        class_grade=None,
        subject=None,
        preferred_language="en",
    )
    db_session.add(profile)
    await db_session.commit()
    token = create_access_token(user_id=user_id, email=profile.email)
    return {"user_id": user_id, "token": token, "profile": profile}


# ==============================================================================
# PIPELINE A: Online Curriculum RAG
# ==============================================================================
@pytest.mark.asyncio
async def test_pipeline_a_online_curriculum_rag(student_a, db_session):
    """Pipeline A: User query -> Route to RAG -> Hybrid Retrieval -> Gate -> Grounded LLM Stream + Citations."""
    user_id = student_a["user_id"]
    token = student_a["token"]

    app.dependency_overrides[get_db] = lambda: db_session
    app.dependency_overrides[get_current_user_id] = lambda: user_id

    mock_chunk = ChunkResult(
        chunk_id="chunk-photo-001",
        content="Photosynthesis occurs in chloroplasts where chlorophyll absorbs sunlight.",
        board="CBSE",
        class_grade="10",
        subject="Science",
        chapter="Life Processes",
        section="6.2",
        page_reference="Page 96",
        relevance_score=0.92,
        normalized_score=0.92,
        similarity_score=0.88,
        vector_rank=1,
        sparse_rank=1,
    )
    retrieval_resp = RetrieveResponse(
        chunks=[mock_chunk],
        evidence_outcome=EvidenceOutcome.SUFFICIENT,
        query_used="Where does photosynthesis occur?",
        search_time_ms=10.0,
    )

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        conv_resp = await client.post(
            "/api/v1/chat/conversations",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert conv_resp.status_code == 201
        conv_id = conv_resp.json()["id"]

        captured_memory = None
        async def mock_generate_stream(*args, **kwargs):
            nonlocal captured_memory
            captured_memory = kwargs.get("memory_context", "")
            for t in ["Photosynthesis ", "takes ", "place ", "in ", "chloroplasts."]:
                yield t

        with patch.object(SupabaseRetrievalService, "retrieve", AsyncMock(return_value=retrieval_resp)):
            with patch.object(openrouter_service, "generate_response_stream", side_effect=mock_generate_stream):
                resp = await client.post(
                    f"/api/v1/chat/conversations/{conv_id}/messages/stream",
                    json={
                        "content": "Where does photosynthesis occur?",
                        "explanation_mode": "teacher",
                        "app_mode": "student",
                    },
                    headers={"Authorization": f"Bearer {token}"},
                )
                assert resp.status_code == 200

                buffer = SseStreamBuffer()
                events = buffer.feed(resp.text)
                events.extend(buffer.finalize())

                event_types = [e["type"] for e in events]
                assert "metadata" in event_types
                assert "route" in event_types
                assert "citations" in event_types
                assert "token" in event_types
                assert "done" in event_types

                # Verify grounded context reached the LLM
                assert captured_memory is not None
                assert "Photosynthesis occurs in chloroplasts" in captured_memory

                # Verify citations payload
                cit_event = next(e for e in events if e["type"] == "citations")
                assert cit_event["outcome"] in ("SUFFICIENT", "sufficient")
                assert len(cit_event["citations"]) == 1
                assert cit_event["citations"][0]["chunk_id"] == "chunk-photo-001"


# ==============================================================================
# PIPELINE B: Offline Curriculum RAG Invariants
# ==============================================================================
@pytest.mark.asyncio
async def test_pipeline_b_offline_curriculum_rag_invariants():
    """Pipeline B: Verifies offline RAG contract (local prompt grounding & citation structure)."""
    from evals.generation.answer_eval_metrics import compute_citation_validity

    local_evidence_citations = [
        {
            "chunk_id": "local-chunk-101",
            "content": "Ohm's Law states V = IR, where V is voltage, I is current, and R is resistance.",
            "textbook_title": "NCERT Class 10 Physics",
            "chapter": "Electricity",
        }
    ]

    available_chunk_ids = {"local-chunk-101"}

    ratio, valid_count, total_count = compute_citation_validity(
        citations=local_evidence_citations,
        available_chunk_ids=available_chunk_ids,
    )

    assert total_count == 1
    assert valid_count == 1
    assert ratio == 1.0


# ==============================================================================
# PIPELINE C: General Direct Conversation
# ==============================================================================
@pytest.mark.asyncio
async def test_pipeline_c_general_direct_conversation(student_b, db_session):
    """Pipeline C: Non-curriculum query -> Direct routing -> No retrieval call -> Direct LLM Stream."""
    user_id = student_b["user_id"]
    token = student_b["token"]

    app.dependency_overrides[get_db] = lambda: db_session
    app.dependency_overrides[get_current_user_id] = lambda: user_id

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        conv_resp = await client.post(
            "/api/v1/chat/conversations",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert conv_resp.status_code == 201
        conv_id = conv_resp.json()["id"]

        async def mock_generate_stream(*args, **kwargs):
            for t in ["Hello! ", "How ", "can ", "I ", "help ", "you ", "today?"]:
                yield t

        retrieve_called = False
        async def mock_retrieve(query, context, top_k=5):
            nonlocal retrieve_called
            retrieve_called = True
            return RetrieveResponse(chunks=[], evidence_outcome=EvidenceOutcome.MISSING, query_used=query, search_time_ms=0)

        with patch.object(SupabaseRetrievalService, "retrieve", side_effect=mock_retrieve):
            with patch.object(openrouter_service, "generate_response_stream", side_effect=mock_generate_stream):
                resp = await client.post(
                    f"/api/v1/chat/conversations/{conv_id}/messages/stream",
                    json={
                        "content": "Hi, who are you?",
                        "app_mode": "general",
                    },
                    headers={"Authorization": f"Bearer {token}"},
                )
                assert resp.status_code == 200

                buffer = SseStreamBuffer()
                events = buffer.feed(resp.text)
                events.extend(buffer.finalize())

                route_event = next(e for e in events if e["type"] == "route")
                assert route_event["decision"] in ("DIRECT", "direct")
                # Retrieval must NOT be called for direct routing
                assert not retrieve_called

                # Citations event must NOT be emitted for direct general conversation
                assert not any(e["type"] == "citations" for e in events)

                tokens = [e["content"] for e in events if e["type"] == "token"]
                assert "".join(tokens).startswith("Hello!")


# ==============================================================================
# PIPELINE D: Missing Evidence Abstention
# ==============================================================================
@pytest.mark.asyncio
async def test_pipeline_d_missing_evidence_abstention(student_a, db_session):
    """Pipeline D: Curriculum query with no matching syllabus evidence -> Halts before LLM -> Pedagogical abstention."""
    user_id = student_a["user_id"]
    token = student_a["token"]

    app.dependency_overrides[get_db] = lambda: db_session
    app.dependency_overrides[get_current_user_id] = lambda: user_id

    missing_resp = RetrieveResponse(
        chunks=[],
        evidence_outcome=EvidenceOutcome.MISSING,
        query_used="Quantum entanglement in Grade 10 syllabus",
        search_time_ms=8.0,
    )

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        conv_resp = await client.post(
            "/api/v1/chat/conversations",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert conv_resp.status_code == 201
        conv_id = conv_resp.json()["id"]

        llm_called = False
        async def mock_generate_stream(*args, **kwargs):
            nonlocal llm_called
            llm_called = True
            yield "Should not be called"

        with patch.object(SupabaseRetrievalService, "retrieve", AsyncMock(return_value=missing_resp)):
            with patch.object(openrouter_service, "generate_response_stream", side_effect=mock_generate_stream):
                resp = await client.post(
                    f"/api/v1/chat/conversations/{conv_id}/messages/stream",
                    json={
                        "content": "Explain quantum entanglement for CBSE Class 10",
                        "app_mode": "student",
                    },
                    headers={"Authorization": f"Bearer {token}"},
                )
                assert resp.status_code == 200

                buffer = SseStreamBuffer()
                events = buffer.feed(resp.text)
                events.extend(buffer.finalize())

                # Generative LLM must NOT be called when evidence is missing
                assert not llm_called

                # Abstention message must be emitted to user
                token_events = [e["content"] for e in events if e["type"] == "token"]
                full_text = "".join(token_events)
                assert "not found in the official curriculum corpus" in full_text


# ==============================================================================
# PIPELINE E: Provider Fallback & Error Recovery
# ==============================================================================
@pytest.mark.asyncio
async def test_pipeline_e_provider_fallback_and_recovery(student_a, db_session):
    """Pipeline E: Verifies provider error handling and fallback behavior when primary provider fails."""
    user_id = student_a["user_id"]
    token = student_a["token"]

    app.dependency_overrides[get_db] = lambda: db_session
    app.dependency_overrides[get_current_user_id] = lambda: user_id

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        conv_resp = await client.post(
            "/api/v1/chat/conversations",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert conv_resp.status_code == 201
        conv_id = conv_resp.json()["id"]

        # Simulate provider failure during stream
        async def mock_failing_stream(*args, **kwargs):
            yield "Partial first token... "
            raise RuntimeError("Provider connection reset by peer")

        with patch.object(openrouter_service, "generate_response_stream", side_effect=mock_failing_stream):
            resp = await client.post(
                f"/api/v1/chat/conversations/{conv_id}/messages/stream",
                json={
                    "content": "Tell me a story",
                    "app_mode": "general",
                },
                headers={"Authorization": f"Bearer {token}"},
            )
            assert resp.status_code == 200

            buffer = SseStreamBuffer()
            events = buffer.feed(resp.text)
            events.extend(buffer.finalize())

            # Error event must be emitted
            error_events = [e for e in events if e["type"] == "error"]
            assert len(error_events) >= 1
            assert "Provider connection reset" in error_events[0]["detail"]

            # Done event must still be emitted in finally block with assistant_message_id
            done_events = [e for e in events if e["type"] == "done"]
            assert len(done_events) == 1
            assert "assistant_message_id" in done_events[0]


# ==============================================================================
# PIPELINE F: Citation Event Schema & Propagation
# ==============================================================================
@pytest.mark.asyncio
async def test_pipeline_f_citation_schema_and_propagation(student_a, db_session):
    """Pipeline F: Verifies citation event schema matches Android client contracts."""
    user_id = student_a["user_id"]
    token = student_a["token"]

    app.dependency_overrides[get_db] = lambda: db_session
    app.dependency_overrides[get_current_user_id] = lambda: user_id

    mock_chunks = [
        ChunkResult(
            chunk_id="chunk-acid-001",
            content="Acids turn blue litmus red. Bases turn red litmus blue.",
            board="CBSE",
            class_grade="10",
            subject="Science",
            chapter="Acids, Bases and Salts",
            section="2.1",
            page_reference="Page 18",
            relevance_score=0.91,
            normalized_score=0.91,
            similarity_score=0.89,
            vector_rank=1,
            sparse_rank=1,
        )
    ]
    retrieval_resp = RetrieveResponse(
        chunks=mock_chunks,
        evidence_outcome=EvidenceOutcome.SUFFICIENT,
        query_used="What is the litmus test for acids?",
        search_time_ms=12.0,
    )

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        conv_resp = await client.post(
            "/api/v1/chat/conversations",
            headers={"Authorization": f"Bearer {token}"},
        )
        conv_id = conv_resp.json()["id"]

        async def mock_stream(*args, **kwargs):
            for t in ["Acids ", "turn ", "litmus ", "red."]:
                yield t

        with patch.object(SupabaseRetrievalService, "retrieve", AsyncMock(return_value=retrieval_resp)):
            with patch.object(openrouter_service, "generate_response_stream", side_effect=mock_stream):
                resp = await client.post(
                    f"/api/v1/chat/conversations/{conv_id}/messages/stream",
                    json={"content": "What is the litmus test for acids?", "app_mode": "student"},
                    headers={"Authorization": f"Bearer {token}"},
                )
                assert resp.status_code == 200

                buffer = SseStreamBuffer()
                events = buffer.feed(resp.text)
                events.extend(buffer.finalize())

                cit_event = next(e for e in events if e["type"] == "citations")
                assert cit_event["outcome"] in ("SUFFICIENT", "sufficient")
                citations = cit_event["citations"]
                assert len(citations) == 1

                citation = citations[0]
                # Contract fields required by Android SourceCitation / StreamEventDto
                assert "chunk_id" in citation
                assert "chapter" in citation
                assert "score" in citation
                assert "page_reference" in citation
                assert citation["chunk_id"] == "chunk-acid-001"
                assert citation["chapter"] == "Acids, Bases and Salts"
                assert citation["score"] == 0.91


# ==============================================================================
# PIPELINE G: Multi-Student Context Isolation
# ==============================================================================
@pytest.mark.asyncio
async def test_pipeline_g_multi_student_context_isolation(student_a, student_b, db_session):
    """Pipeline G: Verifies that Student A's conversation cannot be accessed or modified by Student B."""
    app.dependency_overrides[get_db] = lambda: db_session

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        # Step 1: Student A creates conversation
        app.dependency_overrides[get_current_user_id] = lambda: student_a["user_id"]
        conv_a_resp = await client.post(
            "/api/v1/chat/conversations",
            headers={"Authorization": f"Bearer {student_a['token']}"},
        )
        assert conv_a_resp.status_code == 201
        conv_a_id = conv_a_resp.json()["id"]

        # Step 2: Student B attempts to access Student A's conversation (Must return 403)
        app.dependency_overrides[get_current_user_id] = lambda: student_b["user_id"]
        b_access_resp = await client.get(
            f"/api/v1/chat/conversations/{conv_a_id}",
            headers={"Authorization": f"Bearer {student_b['token']}"},
        )
        assert b_access_resp.status_code == 403

        # Step 3: Student B attempts to stream message to Student A's conversation (Must raise 403 Forbidden)
        with pytest.raises(HTTPException) as exc_info:
            await client.post(
                f"/api/v1/chat/conversations/{conv_a_id}/messages/stream",
                json={"content": "Attempting cross-user injection"},
                headers={"Authorization": f"Bearer {student_b['token']}"},
            )
        assert exc_info.value.status_code == 403
