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


@pytest.mark.asyncio
async def test_local_e5_provider_dimension_and_normalization():
    """Verify local_e5 provider returns 384 dimensions and normalized vector."""
    service = EmbeddingService(
        provider="local_e5",
        dimensions=384,
    )
    assert service.is_configured
    vec = await service.embed_query("Photosynthesis in plants")
    assert len(vec) == 384
    # Vector should be finite and normalized (norm ≈ 1.0)
    norm = sum(x * x for x in vec) ** 0.5
    assert abs(norm - 1.0) < 1e-4


@pytest.mark.asyncio
async def test_embed_passage_local_e5():
    """Verify embed_passage returns 384 dimensions for document chunk."""
    service = EmbeddingService(
        provider="local_e5",
        dimensions=384,
    )
    vec = await service.embed_passage("Chloroplasts contain chlorophyll that absorbs light.")
    assert len(vec) == 384


def test_quantized_onnx_artifact_parity():
    """
    Verify FastAPI uses the exact same dynamic quantized ONNX weights and tokenizer as Android,
    not a separate full-precision (fp32) version.
    """
    from app.services.local_e5_service import (
        ONNX_QUANTIZED_URL,
        ONNX_QUANTIZED_SHA256,
        ONNX_QUANTIZED_SIZE_BYTES,
        TOKENIZER_URL,
        TOKENIZER_SHA256,
        EMBEDDING_DIM,
    )

    # 1. Must specifically point to model_quantized.onnx, NOT model.onnx (fp32)
    assert ONNX_QUANTIZED_URL.endswith("model_quantized.onnx")
    assert "model_quantized" in ONNX_QUANTIZED_URL

    # 2. Must match verified Xenova ARM64-compatible dynamic quantized checksum
    assert ONNX_QUANTIZED_SHA256 == "f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193"
    assert ONNX_QUANTIZED_SIZE_BYTES == 118308185

    # 3. Must match verified tokenizer checksum
    assert TOKENIZER_URL.endswith("tokenizer.json")
    assert TOKENIZER_SHA256 == "0b44a9d7b51c3c62626640cda0e2c2f70fdacdc25bbbd68038369d14ebdf4c39"

    # 4. Dimension must be exactly 384
    assert EMBEDDING_DIM == 384


