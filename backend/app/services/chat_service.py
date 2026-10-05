import logging
from fastapi import HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.repositories.chat_repository import ChatRepository
from app.services.gemini_service import gemini_service
from app.models.conversation import Conversation
from app.models.message import Message
from app.core.config import settings

logger = logging.getLogger(__name__)


def _generate_title(first_message: str) -> str:
    """Lightweight title from first user message: trim + truncate."""
    title = " ".join(first_message.split())  # normalize whitespace
    return title[:50].strip() if len(title) > 50 else title


class ChatService:
    def __init__(self, db: AsyncSession):
        self.repo = ChatRepository(db)

    async def create_conversation(self, user_id: str) -> Conversation:
        return await self.repo.create_conversation(user_id=user_id, title="New Conversation")

    async def list_conversations(self, user_id: str) -> list[Conversation]:
        return await self.repo.list_conversations(user_id=user_id, limit=50)

    async def get_conversation(self, conversation_id: str, user_id: str) -> Conversation:
        conv = await self.repo.get_conversation(conversation_id)
        if not conv:
            raise HTTPException(status_code=404, detail="Conversation not found")
        if conv.user_id is not None and conv.user_id != user_id:
            raise HTTPException(status_code=403, detail="Forbidden: conversation does not belong to user")
        return conv

    async def send_message(
        self,
        conversation_id: str,
        user_id: str,
        content: str,
        explanation_mode: str = "general",
    ) -> tuple[Message, Message]:
        # 1. Validate conversation exists and belongs to user
        conv = await self.get_conversation(conversation_id, user_id)

        # 2. Save user message
        user_msg = await self.repo.save_message(
            conversation_id=conversation_id,
            role="user",
            content=content,
        )
        logger.info(f"Saved user message id={user_msg.id} to conv={conversation_id}")

        # 3. Load bounded context (most recent N messages including the one just saved)
        recent = await self.repo.get_recent_messages(
            conversation_id=conversation_id,
            limit=settings.chat_context_messages,
        )

        # 4. Format as Gemini history
        history = []
        for msg in recent:
            role = "user" if msg.role == "user" else "model"
            history.append({"role": role, "parts": [msg.content]})

        # 5. Call Gemini
        try:
            ai_text = await gemini_service.generate_response(
                history=history,
                explanation_mode=explanation_mode,
            )
        except Exception as exc:
            logger.error(f"Gemini call failed for conv={conversation_id}: {type(exc).__name__}")
            # Do not save a fake response
            raise HTTPException(
                status_code=502,
                detail="AI service temporarily unavailable. Please try again.",
            ) from exc

        # 6. Save assistant response
        assistant_msg = await self.repo.save_message(
            conversation_id=conversation_id,
            role="assistant",
            content=ai_text,
        )
        logger.info(f"Saved assistant message id={assistant_msg.id}")

        # 7. Update conversation metadata (title on first real exchange)
        is_first_message = len(conv.messages) == 0
        new_title = None
        if conv.title == "New Conversation" and is_first_message:
            new_title = _generate_title(content)
        await self.repo.update_conversation(
            conversation_id=conversation_id,
            title=new_title,
        )

        return user_msg, assistant_msg
