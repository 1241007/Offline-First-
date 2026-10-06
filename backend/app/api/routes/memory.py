import logging
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.deps import get_current_user_id
from app.services.memory_service import MemoryService
from app.schemas.memory import (
    MemoryCreateRequest,
    MemoryUpdateRequest,
    MemoryResponse,
    MemoryListResponse,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/memory", tags=["memory"])


@router.get("", response_model=MemoryListResponse)
async def list_memories(
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = MemoryService(db)
    memories = await service.list_user_memories(user_id=user_id)
    return MemoryListResponse(
        memories=[
            MemoryResponse(
                id=m.id,
                user_id=m.user_id,
                category=m.category,
                content=m.content,
                importance=m.importance,
                confidence=m.confidence,
                source_conversation_id=m.source_conversation_id,
                created_at=m.created_at,
                updated_at=m.updated_at,
                last_used_at=m.last_used_at,
                active=m.active,
            )
            for m in memories
        ]
    )


@router.post("", response_model=MemoryResponse, status_code=201)
async def create_memory(
    request: MemoryCreateRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    if not request.content.strip():
        raise HTTPException(status_code=400, detail="Memory content cannot be empty")

    service = MemoryService(db)
    m = await service.create_user_memory(
        user_id=user_id,
        category=request.category,
        content=request.content.strip(),
        importance=request.importance,
        confidence=request.confidence,
        source_conversation_id=request.source_conversation_id,
    )
    return MemoryResponse(
        id=m.id,
        user_id=m.user_id,
        category=m.category,
        content=m.content,
        importance=m.importance,
        confidence=m.confidence,
        source_conversation_id=m.source_conversation_id,
        created_at=m.created_at,
        updated_at=m.updated_at,
        last_used_at=m.last_used_at,
        active=m.active,
    )


@router.patch("/{memory_id}", response_model=MemoryResponse)
async def update_memory(
    memory_id: str,
    request: MemoryUpdateRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = MemoryService(db)
    m = await service.update_user_memory(
        memory_id=memory_id,
        user_id=user_id,
        content=request.content,
        importance=request.importance,
        confidence=request.confidence,
        active=request.active,
    )
    if not m:
        raise HTTPException(status_code=404, detail="Memory not found")

    return MemoryResponse(
        id=m.id,
        user_id=m.user_id,
        category=m.category,
        content=m.content,
        importance=m.importance,
        confidence=m.confidence,
        source_conversation_id=m.source_conversation_id,
        created_at=m.created_at,
        updated_at=m.updated_at,
        last_used_at=m.last_used_at,
        active=m.active,
    )


@router.delete("/{memory_id}", response_model=dict)
async def delete_memory(
    memory_id: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = MemoryService(db)
    success = await service.delete_user_memory(memory_id=memory_id, user_id=user_id)
    if not success:
        raise HTTPException(status_code=404, detail="Memory not found")
    return {"status": "ok"}


@router.delete("", response_model=dict)
async def clear_all_memories(
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = MemoryService(db)
    count = await service.clear_all_user_memories(user_id=user_id)
    return {"status": "ok", "deleted_count": count}
