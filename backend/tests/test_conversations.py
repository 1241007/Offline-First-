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


@pytest.mark.asyncio
async def test_update_conversation_title(client: AsyncClient):
    create_resp = await client.post("/api/v1/chat/conversations")
    conv_id = create_resp.json()["id"]

    patch_resp = await client.patch(
        f"/api/v1/chat/conversations/{conv_id}",
        json={"title": "TLE in Software Engineering"}
    )
    assert patch_resp.status_code == 200

    get_resp = await client.get(f"/api/v1/chat/conversations/{conv_id}")
    assert get_resp.status_code == 200
    assert get_resp.json()["title"] == "TLE in Software Engineering"


def test_clean_fallback_title():
    from app.services.openrouter_service import clean_fallback_title
    
    assert clean_fallback_title("What is TLE in software engineering?") == "TLE in software engineering"
    assert clean_fallback_title("Can you explain binary search trees?") == "Binary search trees"
    assert clean_fallback_title("Tell me about photosynthesis in plants.") == "Photosynthesis in plants"
    assert clean_fallback_title("   ") == "New Conversation"


@pytest.mark.asyncio
async def test_llm_title_generation():
    from unittest.mock import AsyncMock, patch
    from app.services.openrouter_service import openrouter_service
    
    with patch.object(openrouter_service, "_api_key", "sk-test-key"):
        with patch.object(
            openrouter_service,
            "generate_response",
            new_callable=AsyncMock,
            return_value='"TLE in Software Engineering"'
        ):
            # Temporarily unset PYTEST_CURRENT_TEST to test the active LLM path
            import os
            pytest_env = os.environ.pop("PYTEST_CURRENT_TEST", None)
            try:
                title = await openrouter_service.generate_title("What is TLE in software engineering?")
                assert title == "TLE in Software Engineering"
            finally:
                if pytest_env:
                    os.environ["PYTEST_CURRENT_TEST"] = pytest_env


