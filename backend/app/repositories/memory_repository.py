import re
import uuid
import logging
from datetime import datetime, timezone
from typing import Optional, List
from sqlalchemy import select, update, delete
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.memory import UserMemory

logger = logging.getLogger(__name__)


class MemoryRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_memory(
        self,
        user_id: str,
        category: str,
        content: str,
        importance: float = 1.0,
        confidence: float = 1.0,
        source_conversation_id: Optional[str] = None,
    ) -> UserMemory:
        # Check if identical memory already exists for this user to avoid duplicates
        existing = await self.db.execute(
            select(UserMemory).where(
                UserMemory.user_id == user_id,
                UserMemory.category == category,
                UserMemory.content == content.strip(),
            )
        )
        mem = existing.scalar_one_or_none()
        now = datetime.now(timezone.utc)
        if mem:
            mem.importance = max(mem.importance, importance)
            mem.confidence = max(mem.confidence, confidence)
            mem.active = True
            mem.updated_at = now
            await self.db.commit()
            await self.db.refresh(mem)
            return mem

        memory = UserMemory(
            id=str(uuid.uuid4()),
            user_id=user_id,
            category=category,
            content=content.strip(),
            importance=importance,
            confidence=confidence,
            source_conversation_id=source_conversation_id,
            created_at=now,
            updated_at=now,
            active=True,
        )
        self.db.add(memory)
        await self.db.commit()
        await self.db.refresh(memory)
        logger.info(f"Created memory id={memory.id} for user_id={user_id} category={category}")
        return memory

    async def get_memory(self, memory_id: str, user_id: str) -> Optional[UserMemory]:
        result = await self.db.execute(
            select(UserMemory).where(
                UserMemory.id == memory_id,
                UserMemory.user_id == user_id,
            )
        )
        return result.scalar_one_or_none()

    async def list_memories(
        self,
        user_id: str,
        active_only: bool = True,
    ) -> List[UserMemory]:
        query = select(UserMemory).where(UserMemory.user_id == user_id)
        if active_only:
            query = query.where(UserMemory.active == True)
        query = query.order_by(UserMemory.importance.desc(), UserMemory.updated_at.desc())
        result = await self.db.execute(query)
        return list(result.scalars().all())

    async def update_memory(
        self,
        memory_id: str,
        user_id: str,
        content: Optional[str] = None,
        importance: Optional[float] = None,
        confidence: Optional[float] = None,
        active: Optional[bool] = None,
    ) -> Optional[UserMemory]:
        memory = await self.get_memory(memory_id, user_id)
        if not memory:
            return None

        if content is not None:
            memory.content = content.strip()
        if importance is not None:
            memory.importance = importance
        if confidence is not None:
            memory.confidence = confidence
        if active is not None:
            memory.active = active

        memory.updated_at = datetime.now(timezone.utc)
        await self.db.commit()
        await self.db.refresh(memory)
        return memory

    async def delete_memory(self, memory_id: str, user_id: str) -> bool:
        memory = await self.get_memory(memory_id, user_id)
        if not memory:
            return False
        await self.db.delete(memory)
        await self.db.commit()
        return True

    async def clear_all_memories(self, user_id: str) -> int:
        result = await self.db.execute(
            delete(UserMemory).where(UserMemory.user_id == user_id)
        )
        await self.db.commit()
        return result.rowcount or 0

    async def get_relevant_memories(
        self,
        user_id: str,
        query_text: str,
        limit: int = 5,
    ) -> List[UserMemory]:
        """
        Structured relevance retrieval:
        Scores memories based on:
        1. Token keyword overlap with the current prompt
        2. Category keyword matches
        3. Stored memory importance and confidence weight
        4. Recency
        """
        all_active = await self.list_memories(user_id=user_id, active_only=True)
        if not all_active:
            return []

        tokens = set(re.findall(r"\w+", query_text.lower()))

        scored: list[tuple[float, UserMemory]] = []
        for mem in all_active:
            mem_tokens = set(re.findall(r"\w+", mem.content.lower()))
            overlap = len(tokens.intersection(mem_tokens))

            # Category boost if relevant to programming, math, exam, style
            cat_lower = mem.category.lower()
            cat_boost = 1.0 if any(t in cat_lower for t in tokens) else 0.0

            # Score = (overlap * 2.0 + cat_boost) * importance * confidence
            score = (overlap * 2.0 + cat_boost + 0.1) * mem.importance * mem.confidence
            scored.append((score, mem))

        # Sort descending by score
        scored.sort(key=lambda x: x[0], reverse=True)
        top = [mem for _, mem in scored[:limit]]

        # Update last_used_at timestamp on selected memories
        now = datetime.now(timezone.utc)
        for m in top:
            m.last_used_at = now
        if top:
            await self.db.commit()

        return top
