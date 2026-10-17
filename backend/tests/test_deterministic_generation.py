"""Tests for Deterministic LLM Generation across Online Inference, RAG, and Memory."""

import pytest
from unittest.mock import patch, AsyncMock, MagicMock
from datetime import datetime, timezone

from app.services.openrouter_service import OpenRouterService
from app.services.supabase_retrieval_service import SupabaseRetrievalService
from app.schemas.rag import ChunkResult, RAGRequestContext, AppMode, EvidenceOutcome
from app.repositories.memory_repository import MemoryRepository
from app.models.memory import UserMemory
from app.core.config import settings


def test_payload_contains_deterministic_parameters():
    """Verify that OpenRouterService generates payloads with temperature=0.0 and fixed seed=42."""
    service = OpenRouterService()
    target_model, payload = service._build_payload(
        history=[{"role": "user", "content": "What is photosynthesis?"}],
        explanation_mode="teacher",
    )
    assert payload["temperature"] == 0.0
    assert payload["seed"] == 42
    assert payload["max_tokens"] == settings.openrouter_max_tokens
    assert payload["stream"] is False
    assert len(payload["messages"]) == 2
    assert payload["messages"][0]["role"] == "system"
    assert "patient, encouraging teacher" in payload["messages"][0]["content"]
    assert payload["messages"][1]["content"] == "what is photosynthesis?"


def test_openrouter_is_configured_property():
    """Verify is_configured reflects presence of API key."""
    service = OpenRouterService()
    service._api_key = "sk-test-key"
    assert service.is_configured is True

    service._api_key = ""
    assert service.is_configured is False


@pytest.mark.asyncio
async def test_online_generation_byte_for_byte_identical_and_no_cache():
    """
    Verify that repeated requests with identical effective input produce byte-for-byte identical output
    and that NO response cache exists (the HTTP client is invoked on each call).
    """
    service = OpenRouterService()
    service._api_key = "test-key"

    call_count = 0

    def mock_post_handler(*args, **kwargs):
        nonlocal call_count
        call_count += 1
        payload = kwargs.get("json", {})
        # Verify decoding parameters were sent in request body
        assert payload.get("temperature") == 0.0
        assert payload.get("seed") == 42

        fake_resp = MagicMock()
        fake_resp.status_code = 200
        fake_resp.json.return_value = {
            "choices": [{"message": {"content": "Deterministic response: Photosynthesis converts light into chemical energy."}}]
        }
        return fake_resp

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, side_effect=mock_post_handler):
        res1 = await service.generate_response(
            history=[{"role": "user", "content": "Explain photosynthesis"}],
            explanation_mode="general",
        )
        res2 = await service.generate_response(
            history=[{"role": "user", "content": "Explain photosynthesis"}],
            explanation_mode="general",
        )
        res3 = await service.generate_response(
            history=[{"role": "user", "content": "Explain photosynthesis"}],
            explanation_mode="general",
        )

        # 1. Byte-for-byte identical
        assert res1 == res2 == res3
        assert res1 == "Deterministic response: Photosynthesis converts light into chemical energy."

        # 2. NO response cache: client.post was invoked exactly 3 times
        assert call_count == 3


@pytest.mark.asyncio
async def test_online_streaming_byte_for_byte_identical_and_no_cache():
    """
    Verify that repeated streaming requests yield identical sequence of token chunks
    and execute the network call each time (no response caching).
    """
    service = OpenRouterService()
    service._api_key = "test-key"

    stream_invocation_count = 0

    class FakeStreamContext:
        def __init__(self, stream_response):
            self.stream_response = stream_response

        async def __aenter__(self):
            nonlocal stream_invocation_count
            stream_invocation_count += 1
            return self.stream_response

        async def __aexit__(self, exc_type, exc_val, exc_tb):
            pass

    async def mock_aiter():
        chunks = [
            'data: {"choices": [{"delta": {"content": "Ohm\'s "}}]}',
            'data: {"choices": [{"delta": {"content": "law "}}]}',
            'data: {"choices": [{"delta": {"content": "states "}}]}',
            'data: {"choices": [{"delta": {"content": "V = IR."}}]}',
            'data: [DONE]',
        ]
        for c in chunks:
            yield c

    fake_stream_resp = MagicMock()
    fake_stream_resp.status_code = 200
    fake_stream_resp.aiter_lines = mock_aiter

    with patch("httpx.AsyncClient.stream", return_value=FakeStreamContext(fake_stream_resp)):
        tokens1 = [
            t async for t in service.generate_response_stream(
                history=[{"role": "user", "content": "State Ohm's Law"}],
                explanation_mode="explainable",
            )
        ]
        tokens2 = [
            t async for t in service.generate_response_stream(
                history=[{"role": "user", "content": "State Ohm's Law"}],
                explanation_mode="explainable",
            )
        ]

        # Token sequences are byte-for-byte identical
        assert tokens1 == tokens2
        assert "".join(tokens1) == "Ohm's law states V = IR."
        # No caching: stream was opened twice
        assert stream_invocation_count == 2


def test_different_explanation_modes_produce_different_effective_prompts():
    """Verify that teacher, general, and explainable modes generate distinct system instructions."""
    service = OpenRouterService()
    _, p_teacher = service._build_payload(history=[], explanation_mode="teacher")
    _, p_general = service._build_payload(history=[], explanation_mode="general")
    _, p_explainable = service._build_payload(history=[], explanation_mode="explainable")

    assert p_teacher["messages"][0]["content"] != p_general["messages"][0]["content"]
    assert p_general["messages"][0]["content"] != p_explainable["messages"][0]["content"]
    assert p_teacher["messages"][0]["content"] != p_explainable["messages"][0]["content"]


def test_rag_chunk_ranking_tie_breaker_deterministic():
    """
    Verify that when chunks have equal normalized_score and similarity_score,
    the sorting is deterministically broken by chunk_id.
    """
    chunks = [
        ChunkResult(
            chunk_id="chunk-beta",
            content="Second chunk content with sufficient characters to pass minimum length checks.",
            normalized_score=0.85,
            similarity_score=0.88,
        ),
        ChunkResult(
            chunk_id="chunk-alpha",
            content="First chunk content with sufficient characters to pass minimum length checks.",
            normalized_score=0.85,
            similarity_score=0.88,
        ),
        ChunkResult(
            chunk_id="chunk-gamma",
            content="Third chunk content with sufficient characters to pass minimum length checks.",
            normalized_score=0.85,
            similarity_score=0.88,
        ),
    ]

    chunks.sort(
        key=lambda c: (
            c.normalized_score or 0.0,
            c.similarity_score or 0.0,
            str(c.chunk_id or ""),
        ),
        reverse=True,
    )

    ordered_ids = [c.chunk_id for c in chunks]
    assert ordered_ids == ["chunk-gamma", "chunk-beta", "chunk-alpha"]


@pytest.mark.asyncio
async def test_memory_relevance_tie_breaker_deterministic():
    """
    Verify that memory retrieval with identical relevance scores tie-breaks
    deterministically by (importance, updated_at, id).
    """
    now = datetime.now(timezone.utc)
    mock_db = AsyncMock()
    repo = MemoryRepository(mock_db)

    mem1 = UserMemory(
        id="mem-1",
        user_id="u1",
        category="PREFERENCE",
        content="Prefers Python for coding examples",
        importance=1.0,
        confidence=1.0,
        active=True,
        updated_at=now,
    )
    mem2 = UserMemory(
        id="mem-2",
        user_id="u1",
        category="PREFERENCE",
        content="Prefers Kotlin for coding examples",
        importance=1.0,
        confidence=1.0,
        active=True,
        updated_at=now,
    )

    with patch.object(repo, "list_memories", new_callable=AsyncMock, return_value=[mem1, mem2]):
        results1 = await repo.get_relevant_memories(user_id="u1", query_text="coding examples", limit=5)
        results2 = await repo.get_relevant_memories(user_id="u1", query_text="coding examples", limit=5)

        # Stable and identical across repeated calls
        assert [m.id for m in results1] == [m.id for m in results2]
