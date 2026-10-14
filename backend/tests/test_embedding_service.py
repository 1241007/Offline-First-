"""Tests for provider-consistent embedding configuration and validation."""

import pytest
from unittest.mock import AsyncMock, patch, MagicMock
import httpx

from app.services.embedding_service import (
    EmbeddingService,
    EmbeddingConfigurationError,
    EmbeddingDimensionMismatchError,
)


def test_reject_openrouter_key_at_openai_endpoint():
    """Verify that an OpenRouter key cannot be used with the OpenAI embeddings endpoint."""
    with pytest.raises(EmbeddingConfigurationError, match="OpenRouter API key cannot be used with OpenAI API endpoint"):
        EmbeddingService(
            provider="openai",
            base_url="https://api.openai.com/v1",
            api_key="sk-or-v1-abcdef1234567890",
            dimensions=1536,
        )


def test_unconfigured_embedding_service():
    """Verify is_configured is False when api_key is missing."""
    service = EmbeddingService(
        provider="openai",
        base_url="https://api.openai.com/v1",
        api_key="",
        dimensions=1536,
    )
    assert not service.is_configured


@pytest.mark.asyncio
async def test_embed_query_unconfigured_raises():
    """Attempting embed_query without configuration raises error."""
    service = EmbeddingService(
        provider="openai",
        base_url="https://api.openai.com/v1",
        api_key="",
        dimensions=1536,
    )
    with pytest.raises(EmbeddingConfigurationError, match="Embedding service is not configured"):
        await service.embed_query("test query")


@pytest.mark.asyncio
async def test_embed_query_dimension_mismatch_raises():
    """Verify dimension mismatch triggers EmbeddingDimensionMismatchError."""
    service = EmbeddingService(
        provider="openai",
        base_url="https://api.openai.com/v1",
        api_key="sk-test-valid-openai-key",
        dimensions=1536,
    )

    # Return vector of 384 dimensions instead of expected 1536
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "data": [{"embedding": [0.1] * 384}]
    }

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock) as mock_post:
        mock_post.return_value = mock_response
        with pytest.raises(EmbeddingDimensionMismatchError, match="Embedding dimension mismatch: expected 1536, got 384"):
            await service.embed_query("test query")


@pytest.mark.asyncio
async def test_embed_query_timeout_enforcement():
    """Verify timeout is properly caught and raised as TimeoutError."""
    service = EmbeddingService(
        provider="openai",
        base_url="https://api.openai.com/v1",
        api_key="sk-test-valid-openai-key",
        dimensions=1536,
        timeout_seconds=0.5,
    )

    with patch("httpx.AsyncClient.post", side_effect=httpx.TimeoutException("Timeout exceeded")):
        with pytest.raises(TimeoutError, match="timed out after 0.5s"):
            await service.embed_query("test query")


@pytest.mark.asyncio
async def test_embed_query_success_with_dimensions():
    """Verify successful embedding extraction with exact 1536 dimensions."""
    service = EmbeddingService(
        provider="openai",
        base_url="https://api.openai.com/v1",
        api_key="sk-test-valid-openai-key",
        dimensions=1536,
    )

    expected_vector = [0.05] * 1536
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "data": [{"embedding": expected_vector}]
    }

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock) as mock_post:
        mock_post.return_value = mock_response
        res = await service.embed_query("Explain photosynthesis")
        assert len(res) == 1536
        assert res == expected_vector
