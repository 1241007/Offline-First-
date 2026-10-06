import pytest
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_courses_pagination(client: AsyncClient):
    # Test offset and limit query parameters
    resp = await client.get("/api/v1/courses?limit=2&offset=0")
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)
    assert len(resp.json()) <= 2


@pytest.mark.asyncio
async def test_roadmaps_pagination(client: AsyncClient):
    # Test roadmaps with limit and offset
    resp = await client.get("/api/v1/roadmaps?limit=2&offset=0")
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)
    assert len(resp.json()) <= 2
