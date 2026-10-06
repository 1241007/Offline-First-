import logging
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query, Response
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.schemas.chat import (
    ConversationCreateResponse,
    ConversationSummary,
    ConversationCursorPage,
    ConversationDetail,
    MessageResponse,
    MessageCursorPage,
    SendMessageRequest,
    SendMessageResponse,
    StreamMessageRequest,
    UpdateConversationRequest,
    SyncMessagesRequest,
    SyncMessagesResponse,
)
from app.core.deps import get_current_user_id
from app.services.chat_service import ChatService

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/chat", tags=["chat"])


def _conv_to_response(conv) -> ConversationCreateResponse:
    return ConversationCreateResponse(
        id=conv.id,
        title=conv.title,
        created_at=conv.created_at,
        updated_at=conv.updated_at,
        is_archived=getattr(conv, "is_archived", False),
        is_pinned=getattr(conv, "is_pinned", False),
        draft_text=getattr(conv, "draft_text", ""),
    )


def _msg_to_response(msg) -> MessageResponse:
    return MessageResponse(
        id=msg.id,
        conversation_id=msg.conversation_id,
        role=msg.role,
        content=msg.content,
        parent_id=getattr(msg, "parent_id", None),
        is_edited=getattr(msg, "is_edited", False),
        created_at=msg.created_at,
    )


@router.post("/conversations", response_model=ConversationCreateResponse, status_code=201)
async def create_conversation(
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = ChatService(db)
    conv = await service.create_conversation(user_id=user_id)
    logger.info(f"POST /conversations → id={conv.id} user_id={user_id}")
    return _conv_to_response(conv)


@router.get("/conversations", response_model=list[ConversationSummary])
async def list_conversations(
    response: Response,
    limit: int = Query(20, ge=1, le=100, description="Page limit"),
    cursor: Optional[str] = Query(None, description="Cursor timestamp for keyset pagination"),
    search: Optional[str] = Query(None, description="Filter conversation titles"),
    include_archived: bool = Query(False, description="Include archived conversations"),
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = ChatService(db)
    items, next_cursor, has_more = await service.list_conversations(
        user_id=user_id,
        limit=limit,
        cursor=cursor,
        search=search,
        include_archived=include_archived,
    )
    if next_cursor:
        response.headers["X-Next-Cursor"] = next_cursor
    response.headers["X-Has-More"] = str(has_more).lower()

    return [
        ConversationSummary(
            id=c.id,
            title=c.title,
            updated_at=c.updated_at,
            is_archived=getattr(c, "is_archived", False),
            is_pinned=getattr(c, "is_pinned", False),
            draft_text=getattr(c, "draft_text", ""),
        )
        for c in items
    ]


@router.get("/conversations/{conversation_id}", response_model=ConversationDetail)
async def get_conversation(
    conversation_id: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = ChatService(db)
    conv = await service.get_conversation(conversation_id, user_id=user_id)
    return ConversationDetail(
        id=conv.id,
        title=conv.title,
        created_at=conv.created_at,
        updated_at=conv.updated_at,
        is_archived=getattr(conv, "is_archived", False),
        is_pinned=getattr(conv, "is_pinned", False),
        draft_text=getattr(conv, "draft_text", ""),
        messages=[_msg_to_response(m) for m in conv.messages],
    )


@router.get("/conversations/{conversation_id}/messages", response_model=list[MessageResponse])
async def get_messages(
    conversation_id: str,
    response: Response,
    limit: int = Query(30, ge=1, le=100, description="Page limit"),
    before_timestamp: Optional[str] = Query(None, description="Cursor timestamp for earlier messages"),
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = ChatService(db)
    items, next_cursor, has_more = await service.get_messages_paginated(
        conversation_id=conversation_id,
        user_id=user_id,
        limit=limit,
        before_timestamp=before_timestamp,
    )
    if next_cursor:
        response.headers["X-Next-Cursor"] = next_cursor
    response.headers["X-Has-More"] = str(has_more).lower()

    return [_msg_to_response(m) for m in items]


@router.post(
    "/conversations/{conversation_id}/messages",
    response_model=SendMessageResponse,
)
async def send_message(
    conversation_id: str,
    request: SendMessageRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    if not request.content.strip():
        raise HTTPException(status_code=400, detail="Message content cannot be empty")
    if request.explanation_mode not in ("teacher", "general", "explainable"):
        raise HTTPException(status_code=400, detail="Invalid explanation_mode")

    service = ChatService(db)
    user_msg, assistant_msg = await service.send_message(
        conversation_id=conversation_id,
        user_id=user_id,
        content=request.content.strip(),
        explanation_mode=request.explanation_mode,
        client_message_id=request.client_message_id,
        parent_id=request.parent_id,
    )
    return SendMessageResponse(
        user_message=_msg_to_response(user_msg),
        assistant_message=_msg_to_response(assistant_msg),
    )


@router.post("/conversations/{conversation_id}/messages/stream")
async def stream_message(
    conversation_id: str,
    request: StreamMessageRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Server-Sent Events (SSE) AI generation streaming endpoint.
    Accepts prompt/options in POST request body.
    Streams token events and persists final or partial generation to database.
    """
    if not request.content.strip():
        raise HTTPException(status_code=400, detail="Message content cannot be empty")
    if request.explanation_mode not in ("teacher", "general", "explainable"):
        raise HTTPException(status_code=400, detail="Invalid explanation_mode")

    service = ChatService(db)
    stream_generator = service.stream_message(
        conversation_id=conversation_id,
        user_id=user_id,
        content=request.content.strip(),
        explanation_mode=request.explanation_mode,
        client_message_id=request.client_message_id,
        parent_id=request.parent_id,
    )

    return StreamingResponse(
        stream_generator,
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@router.patch("/conversations/{conversation_id}", response_model=dict)
async def update_conversation(
    conversation_id: str,
    request: UpdateConversationRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = ChatService(db)
    await service.update_conversation(
        conversation_id=conversation_id,
        user_id=user_id,
        title=request.title,
        is_archived=request.is_archived,
        is_pinned=request.is_pinned,
        draft_text=request.draft_text,
    )
    return {"status": "ok"}


@router.delete("/conversations/{conversation_id}", response_model=dict)
async def delete_conversation(
    conversation_id: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = ChatService(db)
    await service.delete_conversation(conversation_id, user_id=user_id)
    return {"status": "ok"}


@router.post("/sync", response_model=SyncMessagesResponse)
async def sync_messages(
    request: SyncMessagesRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Idempotent offline-first sync endpoint for uploading local messages created while offline.
    """
    service = ChatService(db)
    synced_ids = await service.sync_messages(user_id=user_id, messages=request.messages)
    return SyncMessagesResponse(synced_ids=synced_ids)
