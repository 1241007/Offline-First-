import pytest
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_create_conversation(client: AsyncClient):
    response = await client.post("/api/v1/chat/conversations")
    assert response.status_code == 201
    data = response.json()
    assert "id" in data
    assert data["title"] == "New Conversation"
    assert "created_at" in data
    assert "updated_at" in data

@pytest.mark.asyncio
async def test_list_conversations_empty(client: AsyncClient):
    response = await client.get("/api/v1/chat/conversations")
    assert response.status_code == 200
    assert isinstance(response.json(), list)

@pytest.mark.asyncio
async def test_list_conversations_after_create(client: AsyncClient):
    await client.post("/api/v1/chat/conversations")
    response = await client.get("/api/v1/chat/conversations")
    assert response.status_code == 200
    assert len(response.json()) >= 1

@pytest.mark.asyncio
async def test_get_conversation(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]
    response = await client.get(f"/api/v1/chat/conversations/{conv_id}")
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == conv_id
    assert isinstance(data["messages"], list)

@pytest.mark.asyncio
async def test_get_conversation_not_found(client: AsyncClient):
    response = await client.get("/api/v1/chat/conversations/nonexistent-id")
    assert response.status_code == 404
