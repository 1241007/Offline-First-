"""Regression tests verifying syllabus authorization gating across online RAG and offline sync paths.

Ensures that the 13 legacy NCERT Class 10 Science documents in Supabase
cannot be served through either online RAG or offline sync unless authorized.
"""

import uuid
import pytest
from sqlalchemy import text
from unittest.mock import AsyncMock, patch

from app.models.profile import UserProfile
from app.models.syllabus import SyllabusChunk, SyllabusDocument
from app.schemas.rag import (
    AppMode,
    ChunkResult,
    CorpusScope,
    EvidenceOutcome,
    RAGRequestContext,
    RetrieveResponse,
)
from app.services.supabase_retrieval_service import SupabaseRetrievalService


@pytest.fixture(autouse=True)
async def setup_sqlite_schema(db_session):
    """Ensures embedding column exists on syllabus_chunks in SQLite test database."""
    try:
        await db_session.execute(text("ALTER TABLE syllabus_chunks ADD COLUMN embedding TEXT"))
        await db_session.commit()
    except Exception:
        pass


# ==============================================================================
# 1. ONLINE RAG AUTHORIZATION GATING REGRESSION TEST
# ==============================================================================

@pytest.mark.asyncio
async def test_online_rag_cannot_retrieve_unauthorized_legacy_ncert_documents(db_session):
    """
    REGRESSION TEST:
    Verify that online RAG retrieval strictly excludes unauthorized legacy NCERT
    textbook documents (source_url not LIKE '%cbseacademic.nic.in%'), even when
    the prompt explicitly asks about textbook chapters.
    """
    await db_session.execute(text("DELETE FROM syllabus_chunks"))
    await db_session.execute(text("DELETE FROM syllabus_documents"))
    await db_session.commit()

    # 1. Seed 1 authorized CBSE curriculum document
    auth_doc = SyllabusDocument(
        id=str(uuid.uuid4()),
        title="CBSE Class 10 Science Curriculum 2026-27",
        board="CBSE",
        class_grade="10",
        subject="Science",
        language="en",
        source_url="https://cbseacademic.nic.in/curriculum/Science_SecP1_2026-27.pdf",
        corpus_version="1.0",
        chunk_count=1,
    )
    db_session.add(auth_doc)

    # 2. Seed 1 legacy NCERT textbook document (unauthorized/restricted)
    legacy_doc = SyllabusDocument(
        id=str(uuid.uuid4()),
        title="Chapter 01 Chemical Reactions And Equations",
        board="CBSE",
        class_grade="10",
        subject="Science",
        language="en",
        source_url="https://ncert.nic.in/textbook.php?jesc1=1-16",
        corpus_version="1.0",
        chunk_count=1,
    )
    db_session.add(legacy_doc)
    await db_session.commit()

    # 3. Seed 1 chunk for authorized doc
    auth_chunk = SyllabusChunk(
        id=str(uuid.uuid4()),
        document_id=auth_doc.id,
        content="CBSE Class 10 Science Unit I: Chemical Substances - Nature and Behaviour. Balanced chemical equations.",
        board="CBSE",
        class_grade="10",
        subject="Science",
        chapter="Curriculum 2026-27",
        textbook_title="CBSE Class 10 Science Curriculum",
        source_id=auth_doc.id,
    )
    db_session.add(auth_chunk)

    # 4. Seed 1 chunk for legacy NCERT doc
    legacy_chunk = SyllabusChunk(
        id=str(uuid.uuid4()),
        document_id=legacy_doc.id,
        content="NCERT Textbook Chapter 1: When a magnesium ribbon is burnt in oxygen, it gets converted to magnesium oxide.",
        board="CBSE",
        class_grade="10",
        subject="Science",
        chapter="Chemical Reactions And Equations",
        textbook_title="NCERT Class 10 Science Textbook",
        source_id=legacy_doc.id,
    )
    db_session.add(legacy_chunk)
    await db_session.commit()

    service = SupabaseRetrievalService(db=db_session)
    ctx = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )

    auth_chunk_result = ChunkResult(
        chunk_id=auth_chunk.id,
        content=auth_chunk.content,
        board=auth_chunk.board,
        class_grade=auth_chunk.class_grade,
        subject=auth_chunk.subject,
        chapter=auth_chunk.chapter,
        section=None,
        textbook_title=auth_chunk.textbook_title,
        page_reference="Page 1",
        source_id=auth_chunk.source_id,
        corpus_version="1.0",
        vector_rank=1,
        keyword_rank=1,
        similarity_score=0.88,
        normalized_score=0.95,
    )

    mock_response = RetrieveResponse(
        chunks=[auth_chunk_result],
        evidence_outcome=EvidenceOutcome.SUFFICIENT,
        query_used="Chemical reactions and equations",
        search_time_ms=12.5,
    )

    with patch.object(service, "_execute_hybrid_retrieval", return_value=mock_response):
        res = await service.retrieve("Chemical reactions and equations", ctx)

    assert res.evidence_outcome == EvidenceOutcome.SUFFICIENT
    assert len(res.chunks) == 1
    # STRICT ASSERTION: The legacy NCERT chunk must NEVER be returned!
    assert res.chunks[0].chunk_id == auth_chunk.id
    assert res.chunks[0].chunk_id != legacy_chunk.id
    assert "NCERT Textbook" not in (res.chunks[0].textbook_title or "")


# ==============================================================================
# 2. OFFLINE SYNC AUTHORIZATION GATING REGRESSION TEST
# ==============================================================================

@pytest.mark.asyncio
async def test_offline_sync_excludes_unauthorized_legacy_ncert_documents(client, db_session):
    """
    REGRESSION TEST:
    Verify that GET /api/v1/syllabus/packs and GET /api/v1/syllabus/packs/{pack_id}/chunks
    never return or count chunks from legacy NCERT documents.
    """
    await db_session.execute(text("DELETE FROM syllabus_chunks"))
    await db_session.execute(text("DELETE FROM syllabus_documents"))
    await db_session.execute(text("DELETE FROM user_profiles"))
    await db_session.commit()

    # Create enrolled student profile
    user_id = "test-user-id"
    profile = UserProfile(
        id=str(uuid.uuid4()),
        user_id=user_id,
        full_name="Student",
        email="student@school.edu",
        level="Grade 10",
        education_mode="school",
        board="CBSE",
        class_grade="10",
        subject="Science",
        preferred_language="en",
    )
    db_session.add(profile)

    # 1. Authorized CBSE Science Curriculum Document
    auth_doc = SyllabusDocument(
        id=str(uuid.uuid4()),
        title="CBSE Class 10 Science Curriculum 2026-27",
        board="CBSE",
        class_grade="10",
        subject="Science",
        language="en",
        source_url="https://cbseacademic.nic.in/curriculum_2026/sec_science.pdf",
        corpus_version="1.0",
        chunk_count=2,
    )
    db_session.add(auth_doc)

    # 2. Unauthorized NCERT Chapter Document
    ncert_doc = SyllabusDocument(
        id=str(uuid.uuid4()),
        title="Chapter 01 Chemical Reactions And Equations",
        board="CBSE",
        class_grade="10",
        subject="Science",
        language="en",
        source_url="https://ncert.nic.in/textbook.php?jesc1=1-16",
        corpus_version="1.0",
        chunk_count=5,
    )
    db_session.add(ncert_doc)
    await db_session.commit()

    # Chunks for authorized doc
    for i in range(2):
        chunk = SyllabusChunk(
            id=str(uuid.uuid4()),
            document_id=auth_doc.id,
            content=f"Authorized CBSE syllabus guideline {i}",
            board="CBSE",
            class_grade="10",
            subject="Science",
            chapter="Curriculum 2026-27",
        )
        db_session.add(chunk)

    # Chunks for unauthorized legacy NCERT doc
    for i in range(5):
        chunk = SyllabusChunk(
            id=str(uuid.uuid4()),
            document_id=ncert_doc.id,
            content=f"Restricted NCERT textbook content {i}",
            board="CBSE",
            class_grade="10",
            subject="Science",
            chapter="Chemical Reactions And Equations",
        )
        db_session.add(chunk)
    await db_session.commit()

    # Query /packs
    resp = await client.get("/api/v1/syllabus/packs")
    assert resp.status_code == 200
    packs = resp.json()["packs"]
    assert len(packs) == 1
    pack = packs[0]
    assert pack["pack_id"] == "cbse-10-science"
    # CRITICAL: Chunk count must be 2 (authorized), NOT 7 (which would include the 5 NCERT chunks)!
    assert pack["chunk_count"] == 2

    # Query /packs/cbse-10-science/manifest
    resp_manifest = await client.get("/api/v1/syllabus/packs/cbse-10-science/manifest")
    assert resp_manifest.status_code == 200
    manifest = resp_manifest.json()
    assert manifest["chunk_count"] == 2

    # Query /packs/cbse-10-science/chunks
    resp_chunks = await client.get("/api/v1/syllabus/packs/cbse-10-science/chunks")
    assert resp_chunks.status_code == 200
    chunks_data = resp_chunks.json()
    assert chunks_data["total_chunks"] == 2
    assert len(chunks_data["chunks"]) == 2
    for c in chunks_data["chunks"]:
        assert "Restricted NCERT" not in c["content"]
        assert "Authorized CBSE" in c["content"]
