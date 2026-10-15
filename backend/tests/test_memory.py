import pytest
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_memory_crud_and_user_isolation(client: AsyncClient):
    # Create memory
    create_resp = await client.post(
        "/api/v1/memory",
        json={
            "category": "PREFERENCE",
            "content": "Prefers C++ examples",
            "importance": 1.2,
            "confidence": 0.95
        }
    )
    assert create_resp.status_code == 201
    memory_data = create_resp.json()
    assert memory_data["category"] == "PREFERENCE"
    assert memory_data["content"] == "Prefers C++ examples"
    memory_id = memory_data["id"]

    # List memories
    list_resp = await client.get("/api/v1/memory")
    assert list_resp.status_code == 200
    memories = list_resp.json()["memories"]
    assert any(m["id"] == memory_id for m in memories)

    # Update memory
    patch_resp = await client.patch(
        f"/api/v1/memory/{memory_id}",
        json={"content": "Prefers modern C++20 examples"}
    )
    assert patch_resp.status_code == 200
    assert patch_resp.json()["content"] == "Prefers modern C++20 examples"

    # Delete memory
    del_resp = await client.delete(f"/api/v1/memory/{memory_id}")
    assert del_resp.status_code == 200

    # Verify deleted
    list_resp2 = await client.get("/api/v1/memory")
    assert not any(m["id"] == memory_id for m in list_resp2.json()["memories"])
