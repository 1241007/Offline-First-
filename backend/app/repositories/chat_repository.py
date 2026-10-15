import uuid
import logging
from datetime import datetime, timezone
from typing import Optional, List, Tuple
from sqlalchemy import select, update, delete
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.models.conversation import Conversation
from app.models.message import Message

logger = logging.getLogger(__name__)


class ChatRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_conversation(
        self,
        user_id: str,
        title: str = "New Conversation",
        conversation_id: Optional[str] = None
    ) -> Conversation:
        conv_id = conversation_id or str(uuid.uuid4())
        # Check if conversation already exists (idempotent create)
        existing = await self.get_conversation(conv_id)
        if existing:
            return existing

        now = datetime.now(timezone.utc)
        conv = Conversation(
            id=conv_id,
            user_id=user_id,
            title=title,
            created_at=now,
            updated_at=now,
            is_archived=False,
            is_pinned=False,
            draft_text="",
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

    async def list_conversations(
        self,
        user_id: str,
        limit: int = 20,
        cursor_updated_at: Optional[datetime] = None,
        search: Optional[str] = None,
        include_archived: bool = False,
    ) -> Tuple[List[Conversation], Optional[str], bool]:
        """
        Keyset/cursor pagination for conversations.
        Orders by is_pinned DESC, updated_at DESC.
        Returns (items, next_cursor, has_more).
        """
        query = select(Conversation).where(Conversation.user_id == user_id)

        if not include_archived:
            query = query.where(Conversation.is_archived == False)

        if search and search.strip():
            query = query.where(Conversation.title.ilike(f"%{search.strip()}%"))

        if cursor_updated_at:
            query = query.where(Conversation.updated_at < cursor_updated_at)

        query = query.order_by(Conversation.is_pinned.desc(), Conversation.updated_at.desc())
        # Query 1 extra item to determine has_more
        query = query.limit(limit + 1)

        result = await self.db.execute(query)
        rows = list(result.scalars().all())

        has_more = len(rows) > limit
        items = rows[:limit]

        next_cursor = None
        if has_more and items:
            next_cursor = items[-1].updated_at.isoformat()

        return items, next_cursor, has_more

    async def save_message(
        self,
        conversation_id: str,
        role: str,
        content: str,
        message_id: Optional[str] = None,
        parent_id: Optional[str] = None,
        is_edited: bool = False,
        created_at: Optional[datetime] = None,
    ) -> Message:
        """
        Idempotent message persistence:
        If message_id is provided and already exists, update/return it without duplicating.
        """
        mid = message_id or str(uuid.uuid4())
        existing = await self.db.execute(select(Message).where(Message.id == mid))
        found = existing.scalar_one_or_none()
        if found:
            found.content = content
            found.is_edited = is_edited
            if parent_id is not None:
                found.parent_id = parent_id
            await self.db.commit()
            await self.db.refresh(found)
            return found

        msg = Message(
            id=mid,
            conversation_id=conversation_id,
            role=role,
            content=content,
            parent_id=parent_id,
            is_edited=is_edited,
            created_at=created_at or datetime.now(timezone.utc),
        )
        self.db.add(msg)
        await self.db.commit()
        await self.db.refresh(msg)
        return msg

    async def get_messages_paginated(
        self,
        conversation_id: str,
        limit: int = 30,
        before_created_at: Optional[datetime] = None,
    ) -> Tuple[List[Message], Optional[str], bool]:
        """
        Keyset cursor pagination for reverse-chronological chat history.
        Retrieves messages older than before_created_at, returned in chronological order.
        """
        query = select(Message).where(Message.conversation_id == conversation_id)
        if before_created_at:
            query = query.where(Message.created_at < before_created_at)

        query = query.order_by(Message.created_at.desc()).limit(limit + 1)
        result = await self.db.execute(query)
        rows = list(result.scalars().all())

        has_more = len(rows) > limit
        items = rows[:limit]

        next_cursor = None
        if has_more and items:
            next_cursor = items[-1].created_at.isoformat()

        # Reverse to chronological order for client display
        return list(reversed(items)), next_cursor, has_more

    async def get_recent_messages(self, conversation_id: str, limit: int) -> List[Message]:
        result = await self.db.execute(
            select(Message)
            .where(Message.conversation_id == conversation_id)
            .order_by(Message.created_at.desc())
            .limit(limit)
        )
        msgs = list(result.scalars().all())
        return list(reversed(msgs))  # chronological order

    async def update_conversation(
        self,
        conversation_id: str,
        title: Optional[str] = None,
        is_archived: Optional[bool] = None,
        is_pinned: Optional[bool] = None,
        draft_text: Optional[str] = None,
    ) -> None:
        values: dict = {"updated_at": datetime.now(timezone.utc)}
        if title is not None:
            values["title"] = title
        if is_archived is not None:
            values["is_archived"] = is_archived
        if is_pinned is not None:
            values["is_pinned"] = is_pinned
        if draft_text is not None:
            values["draft_text"] = draft_text

        await self.db.execute(
            update(Conversation)
            .where(Conversation.id == conversation_id)
            .values(**values)
        )
        await self.db.commit()

    async def delete_conversation(self, conversation_id: str, user_id: str) -> bool:
        conv = await self.get_conversation(conversation_id)
        if not conv:
            return False
        if conv.user_id is not None and conv.user_id != user_id:
            return False
        await self.db.execute(
            delete(Conversation).where(Conversation.id == conversation_id)
        )
        await self.db.commit()
        return True
