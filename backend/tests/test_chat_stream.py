import pytest
from httpx import AsyncClient
from unittest.mock import patch

@pytest.mark.asyncio
async def test_stream_message_sse(client: AsyncClient):
    # 1. Create a conversation
    create_resp = await client.post("/api/v1/chat/conversations")
    assert create_resp.status_code == 201
    conv_id = create_resp.json()["id"]

    # Mock generator for OpenRouter streaming
    async def mock_stream_tokens(*args, **kwargs):
        yield "Hello"
        yield " "
        yield "world!"

    with patch(
        "app.services.chat_service.openrouter_service.generate_response_stream",
        side_effect=mock_stream_tokens
    ):
        response = await client.post(
            f"/api/v1/chat/conversations/{conv_id}/messages/stream",
            json={
                "content": "Say hello world",
                "explanation_mode": "general",
                "client_message_id": "test-client-msg-123"
            }
        )

    assert response.status_code == 200
    assert "text/event-stream" in response.headers.get("content-type", "")

    # Parse SSE events from response text
    body = response.text
    assert "data: " in body
    assert "metadata" in body
    assert "token" in body
    assert "world!" in body
    assert "done" in body

    # Verify message was saved to database
    conv_resp = await client.get(f"/api/v1/chat/conversations/{conv_id}")
    assert conv_resp.status_code == 200
    detail = conv_resp.json()
    messages = detail["messages"]
    assert len(messages) == 2
    assert messages[0]["content"] == "Say hello world"
    assert messages[0]["id"] == "test-client-msg-123"  # stable client ID
    assert messages[1]["content"] == "Hello world!"
    assert messages[1]["role"] == "assistant"


@pytest.mark.asyncio
async def test_stream_message_interrupted_persists_partial(client: AsyncClient):
    """Verifies Stop Generation saves partial output."""
    create_resp = await client.post("/api/v1/chat/conversations")
    assert create_resp.status_code == 201
    conv_id = create_resp.json()["id"]

    async def mock_failing_stream(*args, **kwargs):
        yield "Partial"
        yield " response"
        raise RuntimeError("Client aborted / Stop generation")

    with patch(
        "app.services.chat_service.openrouter_service.generate_response_stream",
        side_effect=mock_failing_stream
    ):
        response = await client.post(
            f"/api/v1/chat/conversations/{conv_id}/messages/stream",
            json={
                "content": "Tell me a story",
                "explanation_mode": "general",
            }
        )

    assert response.status_code == 200
    body = response.text
    assert "error" in body

    # Check that partial content was saved to DB in finally block
    conv_resp = await client.get(f"/api/v1/chat/conversations/{conv_id}")
    messages = conv_resp.json()["messages"]
    assert len(messages) == 2
    assert messages[1]["content"] == "Partial response"
    assert messages[1]["role"] == "assistant"
