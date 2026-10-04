import pytest
from unittest.mock import AsyncMock, patch
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_send_message_to_nonexistent_conversation(client: AsyncClient):
    response = await client.post(
        "/api/v1/chat/conversations/nonexistent/messages",
        json={"content": "Hello", "explanation_mode": "general"}
    )
    assert response.status_code == 404

@pytest.mark.asyncio
async def test_send_empty_message(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]
    response = await client.post(
        f"/api/v1/chat/conversations/{conv_id}/messages",
        json={"content": "   ", "explanation_mode": "general"}
    )
    assert response.status_code == 400

@pytest.mark.asyncio
async def test_send_invalid_mode(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]
    response = await client.post(
        f"/api/v1/chat/conversations/{conv_id}/messages",
        json={"content": "Hello", "explanation_mode": "invalid_mode"}
    )
    assert response.status_code == 400

@pytest.mark.asyncio
async def test_send_message_success(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]

    with patch(
        "app.services.chat_service.gemini_service.generate_response",
        new_callable=AsyncMock,
        return_value="Binary search works by dividing the array in half each step."
    ):
        response = await client.post(
            f"/api/v1/chat/conversations/{conv_id}/messages",
            json={"content": "Explain binary search", "explanation_mode": "teacher"}
        )

    assert response.status_code == 200
    data = response.json()
    assert "user_message" in data
    assert "assistant_message" in data
    assert data["user_message"]["role"] == "user"
    assert data["user_message"]["content"] == "Explain binary search"
    assert data["assistant_message"]["role"] == "assistant"
    assert "Binary search" in data["assistant_message"]["content"]

@pytest.mark.asyncio
async def test_send_message_persists_in_db(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]

    with patch(
        "app.services.chat_service.gemini_service.generate_response",
        new_callable=AsyncMock,
        return_value="This is the AI answer."
    ):
        await client.post(
            f"/api/v1/chat/conversations/{conv_id}/messages",
            json={"content": "Test question", "explanation_mode": "general"}
        )

    conv_resp = await client.get(f"/api/v1/chat/conversations/{conv_id}")
    messages = conv_resp.json()["messages"]
    assert len(messages) == 2
    assert messages[0]["role"] == "user"
    assert messages[1]["role"] == "assistant"

@pytest.mark.asyncio
async def test_conversation_title_generated_from_first_message(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]

    with patch(
        "app.services.chat_service.gemini_service.generate_response",
        new_callable=AsyncMock,
        return_value="Binary search answer here."
    ):
        await client.post(
            f"/api/v1/chat/conversations/{conv_id}/messages",
            json={"content": "Explain binary search", "explanation_mode": "general"}
        )

    conv_resp = await client.get(f"/api/v1/chat/conversations/{conv_id}")
    assert conv_resp.json()["title"] == "Explain binary search"

@pytest.mark.asyncio
async def test_gemini_failure_returns_502(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]

    with patch(
        "app.services.chat_service.gemini_service.generate_response",
        new_callable=AsyncMock,
        side_effect=Exception("Gemini unavailable")
    ):
        response = await client.post(
            f"/api/v1/chat/conversations/{conv_id}/messages",
            json={"content": "Hello", "explanation_mode": "general"}
        )

    assert response.status_code == 502

@pytest.mark.asyncio
async def test_follow_up_context(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]

    with patch(
        "app.services.chat_service.gemini_service.generate_response",
        new_callable=AsyncMock,
        return_value="First answer."
    ):
        await client.post(
            f"/api/v1/chat/conversations/{conv_id}/messages",
            json={"content": "What is Python?", "explanation_mode": "general"}
        )

    captured_history = []
    async def capture_history(history, explanation_mode):
        captured_history.extend(history)
        return "Second answer."

    with patch(
        "app.services.chat_service.gemini_service.generate_response",
        side_effect=capture_history
    ):
        await client.post(
            f"/api/v1/chat/conversations/{conv_id}/messages",
            json={"content": "Give me an example", "explanation_mode": "general"}
        )

    # Context should include the first exchange
    roles = [m["role"] for m in captured_history]
    assert "user" in roles
    assert "model" in roles
