import pytest
from unittest.mock import patch, AsyncMock, MagicMock
import httpx
from app.services.openrouter_service import (
    OpenRouterService,
    _format_messages_for_openrouter,
    _normalize_model_name,
)


def test_normalize_model_name():
    assert _normalize_model_name("gemini-3.8-flash") == "google/gemini-3.8-flash"
    assert _normalize_model_name("google/gemini-3.8-flash") == "google/gemini-3.8-flash"
    assert _normalize_model_name("openai/gpt-4o") == "openai/gpt-4o"
    assert _normalize_model_name("") == "google/gemini-3.8-flash"


def test_format_messages_for_openrouter():
    # Legacy Gemini history format
    gemini_history = [
        {"role": "user", "parts": ["Hello there"]},
        {"role": "model", "parts": ["Hi, how can I help?"]},
        {"role": "user", "parts": ["What is physics?"]},
    ]
    formatted = _format_messages_for_openrouter(gemini_history, "You are a teacher.")
    assert len(formatted) == 4
    assert formatted[0] == {"role": "system", "content": "You are a teacher."}
    assert formatted[1] == {"role": "user", "content": "Hello there"}
    assert formatted[2] == {"role": "assistant", "content": "Hi, how can I help?"}
    assert formatted[3] == {"role": "user", "content": "What is physics?"}


def test_format_messages_with_standard_content():
    chat_history = [
        {"role": "user", "content": "Question 1"},
        {"role": "assistant", "content": "Answer 1"},
    ]
    formatted = _format_messages_for_openrouter(chat_history, "System prompt")
    assert len(formatted) == 3
    assert formatted[0] == {"role": "system", "content": "System prompt"}
    assert formatted[1] == {"role": "user", "content": "Question 1"}
    assert formatted[2] == {"role": "assistant", "content": "Answer 1"}


@pytest.mark.asyncio
async def test_generate_response_success():
    service = OpenRouterService()
    service._api_key = "test-key"

    fake_resp = MagicMock()
    fake_resp.status_code = 200
    fake_resp.json.return_value = {
        "choices": [{"message": {"content": "OpenRouter test response"}}]
    }

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=fake_resp):
        res = await service.generate_response(
            [{"role": "user", "content": "Hello"}], explanation_mode="general"
        )
        assert res == "OpenRouter test response"


@pytest.mark.asyncio
async def test_generate_response_stream_success():
    service = OpenRouterService()
    service._api_key = "test-key"

    async def mock_aiter_lines():
        lines = [
            'data: {"choices": [{"delta": {"content": "Hello"}}]}',
            'data: {"choices": [{"delta": {"content": " "}}]}',
            'data: {"choices": [{"delta": {"content": "world!"}}]}',
            'data: [DONE]',
        ]
        for line in lines:
            yield line

    fake_stream_response = MagicMock()
    fake_stream_response.status_code = 200
    fake_stream_response.aiter_lines = mock_aiter_lines

    class FakeStreamContext:
        async def __aenter__(self):
            return fake_stream_response

        async def __aexit__(self, exc_type, exc_val, exc_tb):
            pass

    with patch("httpx.AsyncClient.stream", return_value=FakeStreamContext()):
        tokens = []
        async for token in service.generate_response_stream(
            [{"role": "user", "content": "Hello"}], explanation_mode="teacher"
        ):
            tokens.append(token)

        assert "".join(tokens) == "Hello world!"


@pytest.mark.asyncio
async def test_auth_error_not_retried():
    service = OpenRouterService()
    service._api_key = "invalid-key"

    fake_resp = MagicMock()
    fake_resp.status_code = 401
    fake_resp.text = "Unauthorized: Invalid API Key"

    call_count = 0

    async def mock_post(*args, **kwargs):
        nonlocal call_count
        call_count += 1
        return fake_resp

    with patch("httpx.AsyncClient.post", side_effect=mock_post):
        with pytest.raises(RuntimeError) as exc_info:
            await service.generate_response([{"role": "user", "content": "Hi"}])

        assert "authentication failed" in str(exc_info.value).lower()
        # Should NOT retry when 401
        assert call_count == 1


@pytest.mark.asyncio
async def test_transient_error_falls_back_to_next_model():
    service = OpenRouterService()
    service._api_key = "test-key"

    # Model 1 fails with 503, Model 2 succeeds
    call_index = 0

    async def mock_post(*args, **kwargs):
        nonlocal call_index
        call_index += 1
        resp = MagicMock()
        if call_index <= 2:
            resp.status_code = 503
        else:
            resp.status_code = 200
            resp.json.return_value = {
                "choices": [{"message": {"content": "Fallback succeeded"}}]
            }
        return resp

    with patch("httpx.AsyncClient.post", side_effect=mock_post):
        res = await service.generate_response([{"role": "user", "content": "Hi"}])
        assert res == "Fallback succeeded"
        assert call_index >= 3
