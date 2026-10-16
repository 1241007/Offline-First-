"""
Regression test suite proving that restricted / unapproved documents
(including the 13 historical NCERT Class 10 Science textbook chapters)
cannot be served through any online RAG retrieval path or offline sync endpoints.
"""

import asyncio
import pytest
from unittest.mock import patch, MagicMock
from sqlalchemy import text
from httpx import AsyncClient, ASGITransport

from app.main import app
from app.core.config import settings
from app.core.database import get_session_factory
from app.core.security import create_access_token
from app.schemas.rag import AppMode, CorpusScope, EvidenceOutcome, RAGRequestContext
from app.services.supabase_retrieval_service import SupabaseRetrievalService


@pytest.mark.asyncio
async def test_online_rag_never_returns_restricted_ncert_documents():
    """
    Empirically verifies that SupabaseRetrievalService strictly filters out
    restricted NCERT textbook documents (source_url LIKE '%ncert.nic.in%')
    and only serves authorized statutory CBSE curriculum documents.
    """
    factory = get_session_factory()
    try:
        async with factory() as db:
            await db.execute(text("SELECT 1;"))
            
            # Verify that NCERT documents actually exist in DB as negative control
            ncert_doc_count = await db.scalar(
                text("SELECT count(*) FROM syllabus_documents WHERE source_url LIKE '%ncert.nic.in%';")
            )
            assert ncert_doc_count > 0, "Negative control check: NCERT docs must exist in DB for this test to be valid"

            svc = SupabaseRetrievalService(db=db)
            ctx = RAGRequestContext(
                execution_mode="ONLINE",
                app_mode=AppMode.STUDENT,
                corpus_scope=CorpusScope.CURRICULUM,
                board="CBSE",
                class_grade="10",
                subject="Science",
            )

            # Test multiple queries: chemical reactions, acids & bases, electricity, life processes
            test_queries = [
                "Ohm's law resistance current voltage in electric circuits",
                "chemical reactions and equations balancing types",
                "life processes nutrition respiration transportation excretion",
            ]

            orig_timeout = settings.retrieval_timeout_seconds
            settings.retrieval_timeout_seconds = 15.0
            try:
                for q in test_queries:
                    resp = await svc.retrieve(q, ctx, top_k=10)
                    for chunk in resp.chunks:
                        # Check document source_url for every retrieved chunk
                        doc_url = await db.scalar(
                            text(f"SELECT source_url FROM syllabus_documents WHERE id = '{chunk.source_id}';")
                        )
                        assert doc_url is not None
                        assert "cbseacademic.nic.in" in doc_url, f"Restricted doc leaked: {doc_url}"
                        assert "ncert.nic.in" not in doc_url, f"NCERT textbook chunk leaked into online RAG: {chunk.chunk_id}"
            finally:
                settings.retrieval_timeout_seconds = orig_timeout
    except (ConnectionError, OSError, asyncio.TimeoutError) as err:
        pytest.skip(f"Live Supabase remote network connection unavailable: {err}")


@pytest.mark.asyncio
async def test_syllabus_sync_packs_exclude_restricted_ncert_chapters(client: AsyncClient, db_session):
    """
    Verifies that syllabus sync endpoints (/packs, /manifest, /chunks)
    strictly exclude restricted NCERT chapters from downloadable offline packs,
    even when both authorized CBSE frameworks and restricted NCERT chapters coexist in the DB.
    """
    from app.models.syllabus import SyllabusDocument, SyllabusChunk

    # Ensure embedding column exists on syllabus_chunks in SQLite
    try:
        await db_session.execute(text("ALTER TABLE syllabus_chunks ADD COLUMN embedding TEXT"))
    except Exception:
        pass

    # Insert 1 authorized CBSE curriculum document and 1 restricted NCERT chapter
    auth_doc_id = "doc-auth-cbse-10"
    restr_doc_id = "doc-restr-ncert-10"

    auth_doc = SyllabusDocument(
        id=auth_doc_id,
        title="CBSE Class 10 Science Curriculum 2026-27",
        board="CBSE",
        class_grade="10",
        subject="Science",
        language="en",
        source_url="https://cbseacademic.nic.in/web_material/CurriculumMain27/SecPart1/Science_SecP1_2026-27.pdf",
        ingestion_status="completed",
        chunk_count=2,
    )
    restr_doc = SyllabusDocument(
        id=restr_doc_id,
        title="Chapter 01 Chemical Reactions And Equations",
        board="CBSE",
        class_grade="10",
        subject="Science",
        language="en",
        source_url="https://ncert.nic.in/textbook.php?jesc1=1-16",
        ingestion_status="completed",
        chunk_count=3,
    )
    db_session.add_all([auth_doc, restr_doc])

    # Add chunks for both
    chunks = [
        SyllabusChunk(id="c-auth-1", document_id=auth_doc_id, content="CBSE Class 10 Science learning outcomes.", chunk_index=0, board="CBSE", class_grade="10", subject="Science", chapter="Curriculum", language="en", source_id=auth_doc_id, corpus_version="1.0", content_hash="hash-auth-1"),
        SyllabusChunk(id="c-auth-2", document_id=auth_doc_id, content="CBSE Class 10 Science assessment framework.", chunk_index=1, board="CBSE", class_grade="10", subject="Science", chapter="Curriculum", language="en", source_id=auth_doc_id, corpus_version="1.0", content_hash="hash-auth-2"),
        SyllabusChunk(id="c-restr-1", document_id=restr_doc_id, content="NCERT Chapter 1 full text passage 1.", chunk_index=0, board="CBSE", class_grade="10", subject="Science", chapter="Chemical Reactions", language="en", source_id=restr_doc_id, corpus_version="1.0", content_hash="hash-restr-1"),
        SyllabusChunk(id="c-restr-2", document_id=restr_doc_id, content="NCERT Chapter 1 full text passage 2.", chunk_index=1, board="CBSE", class_grade="10", subject="Science", chapter="Chemical Reactions", language="en", source_id=restr_doc_id, corpus_version="1.0", content_hash="hash-restr-2"),
        SyllabusChunk(id="c-restr-3", document_id=restr_doc_id, content="NCERT Chapter 1 full text passage 3.", chunk_index=2, board="CBSE", class_grade="10", subject="Science", chapter="Chemical Reactions", language="en", source_id=restr_doc_id, corpus_version="1.0", content_hash="hash-restr-3"),
    ]
    db_session.add_all(chunks)
    await db_session.commit()

    await db_session.execute(text("UPDATE syllabus_chunks SET embedding = '[0.1, 0.2]'"))
    await db_session.commit()

    # 1. Fetch available packs
    resp = await client.get("/api/v1/syllabus/packs")
    assert resp.status_code == 200
    packs = resp.json()["packs"]
    assert len(packs) == 1
    pack = packs[0]
    assert pack["pack_id"] == "cbse-10-science"
    # Chunk count MUST reflect only the authorized 2 chunks, not 2 + 3 = 5
    assert pack["chunk_count"] == 2

    # 2. Fetch manifest
    m_resp = await client.get(f"/api/v1/syllabus/packs/{pack['pack_id']}/manifest")
    assert m_resp.status_code == 200
    manifest = m_resp.json()
    assert manifest["chunk_count"] == 2

    # 3. Fetch chunks
    c_resp = await client.get(f"/api/v1/syllabus/packs/{pack['pack_id']}/chunks?limit=10")
    assert c_resp.status_code == 200
    chunks_data = c_resp.json()["chunks"]
    assert len(chunks_data) == 2
    for ch in chunks_data:
        assert ch["source_id"] == auth_doc_id
        assert "NCERT" not in ch["content"]
