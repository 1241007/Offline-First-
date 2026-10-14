"""Authenticated RAG retrieval endpoint."""

import logging
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.deps import get_current_user_id, get_db
from app.schemas.rag import RetrieveRequest, RetrieveResponse
from app.services.supabase_retrieval_service import SupabaseRetrievalService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/retrieve", tags=["RAG Retrieval"])


@router.post("", response_model=RetrieveResponse)
async def retrieve_chunks(
    request: RetrieveRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
) -> RetrieveResponse:
    """
    Authenticated endpoint to retrieve relevant curriculum chunks.
    
    Enforces:
    - User authentication
    - Context-aware retrieval
    - General Mode policy (returns CORPUS_NOT_CONFIGURED)
    - Calibrated RRF evidence scoring
    - Retrieval timeout deadlines
    """
    if not request.query.strip():
        raise HTTPException(status_code=400, detail="Query cannot be empty")

    service = SupabaseRetrievalService(db)
    return await service.retrieve(
        query=request.query.strip(),
        context=request.context,
        top_k=request.top_k,
    )
