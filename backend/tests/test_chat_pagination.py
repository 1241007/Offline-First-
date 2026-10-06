import pytest
from httpx import AsyncClient
from unittest.mock import patch, AsyncMock

@pytest.mark.asyncio
async def test_chat_messages_pagination_and_stable_id(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]

    # Send 3 messages with stable client IDs
    with patch(
        "app.services.chat_service.gemini_service.generate_response",
        new_callable=AsyncMock,
        return_value="AI answer"
    ):
        for i in range(1, 4):
            send_resp = await client.post(
                f"/api/v1/chat/conversations/{conv_id}/messages",
                json={
                    "content": f"Message {i}",
                    "explanation_mode": "general",
                    "client_message_id": f"stable-user-msg-{i}"
                }
            )
            assert send_resp.status_code == 200
            assert send_resp.json()["user_message"]["id"] == f"stable-user-msg-{i}"

    # Fetch with limit=2
    paginated_resp = await client.get(
        f"/api/v1/chat/conversations/{conv_id}/messages?limit=2"
    )
    assert paginated_resp.status_code == 200
    msgs = paginated_resp.json()
    assert len(msgs) == 2
    assert "X-Next-Cursor" in paginated_resp.headers
    assert paginated_resp.headers["X-Has-More"] == "true"

    # Fetch earlier messages using cursor
    cursor = paginated_resp.headers["X-Next-Cursor"]
    earlier_resp = await client.get(
        f"/api/v1/chat/conversations/{conv_id}/messages?limit=10&before_timestamp={cursor}"
    )
    assert earlier_resp.status_code == 200
    earlier_msgs = earlier_resp.json()
    assert len(earlier_msgs) > 0


@pytest.mark.asyncio
async def test_conversations_pagination_and_search(client: AsyncClient):
    # Create conversations
    for i in range(5):
        await client.post("/api/v1/chat/conversations")

    # List with limit=2
    resp = await client.get("/api/v1/chat/conversations?limit=2")
    assert resp.status_code == 200
    items = resp.json()
    assert len(items) == 2
    assert resp.headers["X-Has-More"] == "true"
    assert "X-Next-Cursor" in resp.headers


@pytest.mark.asyncio
async def test_offline_sync_endpoint(client: AsyncClient):
    # Sync a batch of messages
    sync_payload = {
        "messages": [
            {
                "id": "offline-msg-1",
                "conversation_id": "offline-conv-1",
                "role": "user",
                "content": "Offline question 1",
                "created_at": "2026-10-06T10:00:00Z"
            },
            {
                "id": "offline-msg-2",
                "conversation_id": "offline-conv-1",
                "role": "assistant",
                "content": "Offline answer 1",
                "created_at": "2026-10-06T10:00:05Z"
            }
        ]
    }
    sync_resp = await client.post("/api/v1/chat/sync", json=sync_payload)
    assert sync_resp.status_code == 200
    assert "offline-msg-1" in sync_resp.json()["synced_ids"]
    assert "offline-msg-2" in sync_resp.json()["synced_ids"]

    # Verify conversation is queryable
    conv_resp = await client.get("/api/v1/chat/conversations/offline-conv-1")
    assert conv_resp.status_code == 200
    assert len(conv_resp.json()["messages"]) == 2
