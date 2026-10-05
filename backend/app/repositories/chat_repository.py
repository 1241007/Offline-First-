import uuid
import logging
from datetime import datetime, timezone
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.models.conversation import Conversation
from app.models.message import Message
from typing import Optional

logger = logging.getLogger(__name__)

class ChatRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_conversation(self, user_id: str, title: str = "New Conversation") -> Conversation:
        conv = Conversation(
            id=str(uuid.uuid4()),
            user_id=user_id,
            title=title,
            created_at=datetime.now(timezone.utc),
            updated_at=datetime.now(timezone.utc)
        )
        self.db.add(conv)
        await self.db.commit()
        await self.db.refresh(conv)
        logger.info(f"Created conversation id={conv.id} for user_id={user_id}")
        return conv

    async def get_conversation(self, conversation_id: str) -> Optional[Conversation]:
        result = await self.db.execute(
            select(Conversation)
            .where(Conversation.id == conversation_id)
            .options(selectinload(Conversation.messages))
        )
        return result.scalar_one_or_none()

    async def list_conversations(self, user_id: str, limit: int = 50) -> list[Conversation]:
        result = await self.db.execute(
            select(Conversation)
            .where(Conversation.user_id == user_id)
            .order_by(Conversation.updated_at.desc())
            .limit(limit)
        )
        return list(result.scalars().all())

    async def save_message(self, conversation_id: str, role: str, content: str) -> Message:
        msg = Message(
            id=str(uuid.uuid4()),
            conversation_id=conversation_id,
            role=role,
            content=content,
            created_at=datetime.now(timezone.utc)
        )
        self.db.add(msg)
        await self.db.commit()
        await self.db.refresh(msg)
        return msg

    async def get_recent_messages(self, conversation_id: str, limit: int) -> list[Message]:
        result = await self.db.execute(
            select(Message)
            .where(Message.conversation_id == conversation_id)
            .order_by(Message.created_at.desc())
            .limit(limit)
        )
        msgs = list(result.scalars().all())
        return list(reversed(msgs))  # chronological order

    async def update_conversation(self, conversation_id: str, title: str = None) -> None:
        values = {"updated_at": datetime.now(timezone.utc)}
        if title:
            values["title"] = title
        await self.db.execute(
            update(Conversation)
            .where(Conversation.id == conversation_id)
            .values(**values)
        )
        await self.db.commit()
