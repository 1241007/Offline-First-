import logging
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.schemas.chat import (
    ConversationCreateResponse,
    ConversationSummary,
    ConversationDetail,
    MessageResponse,
    SendMessageRequest,
    SendMessageResponse,
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
    )


def _msg_to_response(msg) -> MessageResponse:
    return MessageResponse(
        id=msg.id,
        conversation_id=msg.conversation_id,
        role=msg.role,
        content=msg.content,
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
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    service = ChatService(db)
    convs = await service.list_conversations(user_id=user_id)
    return [
        ConversationSummary(id=c.id, title=c.title, updated_at=c.updated_at)
        for c in convs
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
        messages=[_msg_to_response(m) for m in conv.messages],
    )


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
    )
    return SendMessageResponse(
        user_message=_msg_to_response(user_msg),
        assistant_message=_msg_to_response(assistant_msg),
    )
