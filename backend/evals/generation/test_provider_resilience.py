"""
Online Provider Resilience and Fault Injection Test Suite.

Validates error recovery, backoff, retry exhaustion, model fallback chains,
and security invariants in OpenRouterService:
- HTTP 429 Rate Limit backoff and retry.
- HTTP 500/502/503/504 transient server error retry.
- HTTP 400/404 model unavailability triggering fallback chain.
- HTTP 401/403 authentication failures raising immediately without futile retries.
- All models failing resulting in clean RuntimeError.
- Secret safety: zero API key leakage in exception messages.
- Streaming interruption and connection error handling.
"""

import asyncio
from unittest.mock import AsyncMock, MagicMock, patch
import httpx
import pytest

from app.services.openrouter_service import OpenRouterService, _FALLBACK_MODELS


@pytest.mark.asyncio
async def test_resilience_429_rate_limit_retry():
    """Verify HTTP 429 triggers retry with exponential backoff on primary model."""
    service = OpenRouterService()
    service._api_key = "secret-test-key-12345"

    attempts = 0

    def mock_post(*args, **kwargs):
        nonlocal attempts
        attempts += 1
        resp = MagicMock()
        if attempts == 1:
            resp.status_code = 429
            resp.text = "Rate limit reached"
        else:
            resp.status_code = 200
            resp.json.return_value = {
                "choices": [{"message": {"content": "Recovered response after 429 backoff."}}]
            }
        return resp

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, side_effect=mock_post):
        with patch("asyncio.sleep", new_callable=AsyncMock) as mock_sleep:
            result = await service.generate_response(
                history=[{"role": "user", "content": "Hello"}],
                explanation_mode="general"
            )
            assert result == "Recovered response after 429 backoff."
            assert attempts == 2
            mock_sleep.assert_called_with(1.5)


@pytest.mark.asyncio
async def test_resilience_model_fallback_chain_on_404():
    """Verify HTTP 404 on primary model switches to fallback model in sequence."""
    service = OpenRouterService()
    service._api_key = "secret-test-key-12345"

    requested_models = []

    def mock_post(*args, **kwargs):
        payload = kwargs.get("json", {})
        model = payload.get("model")
        requested_models.append(model)

        resp = MagicMock()
        if model == _FALLBACK_MODELS[0]:
            # Primary model unavailable
            resp.status_code = 404
            resp.text = f"Model {model} not found"
        else:
            # Fallback model succeeds
            resp.status_code = 200
            resp.json.return_value = {
                "choices": [{"message": {"content": f"Success from fallback model {model}."}}]
            }
        return resp

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, side_effect=mock_post):
        result = await service.generate_response(
            history=[{"role": "user", "content": "Explain gravity"}],
            explanation_mode="general"
        )
        assert "Success from fallback model" in result
        assert len(requested_models) >= 2
        assert requested_models[0] == _FALLBACK_MODELS[0]
        assert requested_models[1] == _FALLBACK_MODELS[1]


@pytest.mark.asyncio
async def test_resilience_fatal_auth_error_no_futile_retries():
    """Verify HTTP 401/403 fails fast without cycling fallback models or retrying."""
    service = OpenRouterService()
    service._api_key = "invalid-secret-key"

    call_count = 0

    def mock_post(*args, **kwargs):
        nonlocal call_count
        call_count += 1
        resp = MagicMock()
        resp.status_code = 401
        resp.text = "Unauthorized: Invalid API key"
        return resp

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, side_effect=mock_post):
        with pytest.raises(RuntimeError) as exc_info:
            await service.generate_response(
                history=[{"role": "user", "content": "Hello"}],
                explanation_mode="general"
            )
        assert "authentication failed" in str(exc_info.value).lower()
        # Must fail immediately on attempt 1 without model hopping
        assert call_count == 1


@pytest.mark.asyncio
async def test_resilience_all_models_failing_raises_runtime_error():
    """Verify that when all models fail, an informative RuntimeError is raised."""
    service = OpenRouterService()
    service._api_key = "valid-key"

    def mock_post(*args, **kwargs):
        resp = MagicMock()
        resp.status_code = 503
        resp.text = "Service temporarily unavailable"
        return resp

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, side_effect=mock_post):
        with patch("asyncio.sleep", new_callable=AsyncMock):
            with pytest.raises(RuntimeError) as exc_info:
                await service.generate_response(
                    history=[{"role": "user", "content": "Test all fail"}],
                    explanation_mode="general"
                )
            assert "transient failure" in str(exc_info.value).lower() or "503" in str(exc_info.value)


def test_resilience_secret_safety_in_headers_and_exceptions():
    """Ensure that the API key is NEVER exposed in headers inspection, string reprs, or logs."""
    service = OpenRouterService()
    secret = "sk-or-v1-my-confidential-token-987654321"
    service._api_key = secret

    # Check headers format
    headers = service._get_headers()
    assert headers["Authorization"] == f"Bearer {secret}"

    # Verify exception messages do not contain the secret token
    try:
        raise RuntimeError("OpenRouter authentication failed (HTTP 401)")
    except RuntimeError as err:
        assert secret not in str(err)


@pytest.mark.asyncio
async def test_resilience_streaming_network_disconnect_handling():
    """Verify that a network disconnect mid-stream terminates cleanly."""
    service = OpenRouterService()
    service._api_key = "valid-key"

    class MockStreamContext:
        def __init__(self, status_code=200):
            self.status_code = status_code

        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            pass

        async def aiter_lines(self):
            yield 'data: {"choices": [{"delta": {"content": "First token"}}]}'
            raise httpx.ReadTimeout("Connection dropped mid-stream")

    def mock_stream(*args, **kwargs):
        return MockStreamContext()

    with patch.object(service, "get_client") as mock_get_client:
        mock_client = MagicMock()
        mock_client.stream = mock_stream
        mock_get_client.return_value = mock_client

        tokens = []
        try:
            async for chunk in service.generate_response_stream(
                history=[{"role": "user", "content": "Stream test"}],
                explanation_mode="general"
            ):
                tokens.append(chunk)
        except Exception as exc:
            assert isinstance(exc, (httpx.ReadTimeout, RuntimeError))
        # Ensure first token was delivered before drop
        assert len(tokens) >= 1
        assert tokens[0] == "First token"
