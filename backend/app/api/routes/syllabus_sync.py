"""Authenticated syllabus pack synchronization endpoints for EduNova."""

import json
import logging
from typing import Any, List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.deps import get_current_user_id, get_db
from app.repositories.profile_repository import ProfileRepository
from app.schemas.syllabus_sync import (
    SyllabusChunkDTO,
    SyllabusChunksResponse,
    SyllabusManifestResponse,
    SyllabusPackListResponse,
    SyllabusPackSummary,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/syllabus", tags=["Syllabus Sync"])


def format_pack_id(board: str, class_grade: str, subject: str) -> str:
    """Creates a deterministic pack identifier."""
    return f"{board.strip().lower()}-{class_grade.strip().lower()}-{subject.strip().lower().replace(' ', '-')}"


def parse_vector_string(val: Any) -> Optional[List[float]]:
    """Safely converts pgvector string or list representation to List[float]."""
    if val is None:
        return None
    if isinstance(val, (list, tuple)):
        return [float(x) for x in val]
    if isinstance(val, str):
        cleaned = val.strip().strip("[]()")
        if not cleaned:
            return None
        return [float(x.strip()) for x in cleaned.split(",") if x.strip()]
    return None


@router.get("/packs", response_model=SyllabusPackListResponse)
async def list_available_packs(
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
) -> SyllabusPackListResponse:
    """
    Lists available syllabus curriculum packs stored in the Supabase knowledge base.
    Highlights or filters packs compatible with the authenticated user's profile.
    """
    profile_repo = ProfileRepository(db)
    profile = await profile_repo.get_by_user_id(user_id)

    sql = """
        SELECT board, class_grade, subject, language, corpus_version,
               embedding_model, embedding_version,
               COUNT(*) as chunk_count, MAX(updated_at) as last_updated
        FROM syllabus_chunks
        GROUP BY board, class_grade, subject, language, corpus_version, embedding_model, embedding_version
        ORDER BY board, class_grade, subject;
    """

    try:
        res = await db.execute(text(sql))
        rows = res.fetchall()
    except Exception as exc:
        logger.debug(f"Syllabus chunks table not yet queried or empty: {exc}")
        return SyllabusPackListResponse(packs=[], total_count=0)

    packs: List[SyllabusPackSummary] = []
    for r in rows:
        pack_id = format_pack_id(r.board, r.class_grade, r.subject)
        packs.append(
            SyllabusPackSummary(
                pack_id=pack_id,
                board=r.board,
                class_grade=r.class_grade,
                subject=r.subject,
                language=r.language or "en",
                chunk_count=r.chunk_count,
                corpus_version=r.corpus_version or "1.0",
                embedding_model=r.embedding_model or "intfloat/multilingual-e5-small",
                embedding_version=r.embedding_version or "1.0",
                last_updated=r.last_updated,
            )
        )

    # If user profile specifies board/class/subject, sort matching packs to the top
    if profile:
        def pack_priority(p: SyllabusPackSummary) -> int:
            score = 0
            if profile.board and p.board.lower() == profile.board.lower():
                score += 4
            if profile.class_grade and p.class_grade.lower() == profile.class_grade.lower():
                score += 2
            if profile.subject and p.subject.lower() in profile.subject.lower():
                score += 1
            return -score

        packs.sort(key=pack_priority)

    return SyllabusPackListResponse(packs=packs, total_count=len(packs))


@router.get("/packs/{pack_id}/manifest", response_model=SyllabusManifestResponse)
async def get_pack_manifest(
    pack_id: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
) -> SyllabusManifestResponse:
    """
    Returns the pack manifest including chunk count, model version, and embedding configuration.
    """
    sql = """
        SELECT board, class_grade, subject, language, corpus_version,
               embedding_model, embedding_version,
               COUNT(*) as chunk_count, MAX(updated_at) as last_updated
        FROM syllabus_chunks
        GROUP BY board, class_grade, subject, language, corpus_version, embedding_model, embedding_version;
    """
    res = await db.execute(text(sql))
    rows = res.fetchall()

    matched_row = None
    for r in rows:
        if format_pack_id(r.board, r.class_grade, r.subject) == pack_id.lower().strip():
            matched_row = r
            break

    if not matched_row:
        raise HTTPException(
            status_code=404,
            detail=f"Syllabus pack '{pack_id}' not found or has no ingested chunks.",
        )

    return SyllabusManifestResponse(
        pack_id=pack_id,
        board=matched_row.board,
        class_grade=matched_row.class_grade,
        subject=matched_row.subject,
        language=matched_row.language or "en",
        chunk_count=matched_row.chunk_count,
        corpus_version=matched_row.corpus_version or "1.0",
        embedding_model=matched_row.embedding_model or "intfloat/multilingual-e5-small",
        embedding_version=matched_row.embedding_version or "1.0",
        embedding_dimension=384,
        last_updated=matched_row.last_updated,
    )


@router.get("/packs/{pack_id}/chunks", response_model=SyllabusChunksResponse)
async def get_pack_chunks(
    pack_id: str,
    cursor: Optional[str] = Query(None, description="Cursor for pagination (chunk id)"),
    limit: int = Query(200, ge=1, le=1000, description="Max chunks to return per page"),
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
) -> SyllabusChunksResponse:
    """
    Paginated retrieval of syllabus chunks including precomputed 384-dimensional
    embeddings for on-device vector search.
    """
    # Find matching curriculum parameters
    manifest_sql = """
        SELECT board, class_grade, subject, corpus_version, embedding_model, embedding_version, COUNT(*) as total
        FROM syllabus_chunks
        GROUP BY board, class_grade, subject, corpus_version, embedding_model, embedding_version;
    """
    res = await db.execute(text(manifest_sql))
    rows = res.fetchall()

    matched = None
    for r in rows:
        if format_pack_id(r.board, r.class_grade, r.subject) == pack_id.lower().strip():
            matched = r
            break

    if not matched:
        raise HTTPException(
            status_code=404,
            detail=f"Syllabus pack '{pack_id}' not found.",
        )

    # Query chunks with cursor pagination
    query_parts = [
        """
        SELECT id, content, board, class_grade, subject, chapter, section,
               language, textbook_title, page_reference, source_id, corpus_version,
               content_hash, embedding_model, embedding_version,
               CAST(embedding AS text) as embedding_text
        FROM syllabus_chunks
        WHERE board = :board AND class_grade = :class_grade AND subject = :subject
        """
    ]
    params = {
        "board": matched.board,
        "class_grade": matched.class_grade,
        "subject": matched.subject,
        "limit": limit + 1,
    }

    if cursor:
        query_parts.append("AND id > :cursor")
        params["cursor"] = cursor

    query_parts.append("ORDER BY id ASC LIMIT :limit")
    sql = "\n".join(query_parts)

    chunk_res = await db.execute(text(sql), params)
    chunk_rows = chunk_res.fetchall()

    has_more = len(chunk_rows) > limit
    returned_rows = chunk_rows[:limit]
    next_cursor = returned_rows[-1].id if has_more and returned_rows else None

    dtos: List[SyllabusChunkDTO] = []
    for r in returned_rows:
        emb = parse_vector_string(r.embedding_text)
        dtos.append(
            SyllabusChunkDTO(
                chunk_id=str(r.id),
                content=r.content,
                board=r.board,
                class_grade=r.class_grade,
                subject=r.subject,
                chapter=r.chapter,
                section=r.section,
                language=r.language or "en",
                textbook_title=r.textbook_title,
                page_reference=r.page_reference,
                source_id=str(r.source_id) if r.source_id else None,
                corpus_version=r.corpus_version,
                content_hash=r.content_hash,
                embedding_model=r.embedding_model or "intfloat/multilingual-e5-small",
                embedding_version=r.embedding_version or "1.0",
                embedding=emb,
            )
        )

    return SyllabusChunksResponse(
        pack_id=pack_id,
        chunks=dtos,
        next_cursor=next_cursor,
        has_more=has_more,
        total_chunks=matched.total,
        embedding_model=matched.embedding_model or "intfloat/multilingual-e5-small",
        embedding_version=matched.embedding_version or "1.0",
        embedding_dimension=384,
    )
