"""Unit and integration tests for syllabus synchronization and ingestion pipeline."""

import pytest
from pathlib import Path
from unittest.mock import AsyncMock, patch, MagicMock
from httpx import AsyncClient, ASGITransport

from app.main import app
from app.core.security import create_access_token
from scripts.ingest_syllabus import (
    compute_sha256,
    chunk_text,
    parse_metadata_from_path,
    SyllabusIngestionPipeline,
)


def test_compute_sha256():
    """Verify SHA-256 hash generation for chunk deduplication."""
    text1 = "Photosynthesis is the process used by plants."
    text2 = "Photosynthesis is the process used by plants."
    text3 = "Cellular respiration produces ATP."

    hash1 = compute_sha256(text1)
    hash2 = compute_sha256(text2)
    hash3 = compute_sha256(text3)

    assert hash1 == hash2
    assert hash1 != hash3
    assert len(hash1) == 64


def test_chunk_text():
    """Verify text chunking preserves paragraphs and creates retrieval-sized passages."""
    short_text = "Chapter 1: Real Numbers.\n\nEvery composite number can be factored as a product of primes."
    chunks = chunk_text(short_text, target_chunk_chars=500, overlap_chars=50)
    assert len(chunks) == 1
    assert "Real Numbers" in chunks[0]

    # Longer multi-paragraph text
    paras = [f"Paragraph {i}: This is educational content describing concepts in detail." * 5 for i in range(10)]
    long_text = "\n\n".join(paras)
    long_chunks = chunk_text(long_text, target_chunk_chars=600, overlap_chars=100)
    assert len(long_chunks) > 1
    for c in long_chunks:
        assert len(c) > 0


def test_parse_metadata_from_path():
    """Verify curriculum metadata extraction from directory structure and filenames."""
    base_dir = Path("/data/syllabus_sources")
    file_path = base_dir / "CBSE" / "10" / "Science" / "Chapter_06_Life_Processes.txt"

    meta = parse_metadata_from_path(file_path, base_dir)
    assert meta["board"] == "CBSE"
    assert meta["class_grade"] == "10"
    assert meta["subject"] == "Science"
    assert "Life Processes" in meta["chapter"]


@pytest.mark.asyncio
async def test_syllabus_pipeline_empty_directory():
    """Verify pipeline reports pending sources when directory is empty or missing."""
    pipeline = SyllabusIngestionPipeline(db_session=None)
    res = await pipeline.ingest_directory(Path("/nonexistent/directory"), dry_run=True)
    assert res["status"] == "pending_sources"
    assert res["documents_processed"] == 0
    assert "pending until authorized syllabus sources" in res["message"]


@pytest.mark.asyncio
async def test_syllabus_sync_packs_unauthenticated():
    """Verify GET /api/v1/syllabus/packs requires Bearer token."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/api/v1/syllabus/packs")
        assert response.status_code == 401


@pytest.mark.asyncio
async def test_syllabus_sync_packs_authenticated():
    """Verify GET /api/v1/syllabus/packs succeeds with authenticated user."""
    token = create_access_token(user_id="test-user-sync-123", email="test@example.com")

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get(
            "/api/v1/syllabus/packs",
            headers={"Authorization": f"Bearer {token}"}
        )
        assert response.status_code == 200
        data = response.json()
        assert "packs" in data
        assert "total_count" in data
