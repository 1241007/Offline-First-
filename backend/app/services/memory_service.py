import re
import logging
from typing import Optional, List
from sqlalchemy.ext.asyncio import AsyncSession
from app.repositories.memory_repository import MemoryRepository
from app.models.memory import UserMemory

logger = logging.getLogger(__name__)

# Patterns for extracting long-term memories from high-signal user messages
_EXTRACTION_RULES = [
    # Programming language preference
    (
        re.compile(r"\b(?:i\s+prefer|i\s+code\s+in|i\s+use|write\s+in|code\s+in)\s+([A-Za-z\+\#]+)(?:\s+examples?)?", re.IGNORECASE),
        "PREFERENCE",
        lambda m: f"Prefers {m.group(1).upper() if m.group(1).lower() in ('c++', 'c#') else m.group(1).capitalize()} for code examples",
        1.2,
        0.95
    ),
    # Exam / interview preparation goals
    (
        re.compile(r"\b(?:preparing\s+for|studying\s+for|revision\s+for|target\s+is)\s+([A-Za-z0-9\s\-]{2,30}?)(?:\bexam|\binterviews?|\btests?|\bcollege|\.|$)", re.IGNORECASE),
        "GOAL",
        lambda m: f"Preparing for {m.group(1).strip()}",
        1.3,
        0.90
    ),
    # Explanation style preference
    (
        re.compile(r"\b(?:explain\s+(?:with|using)|give\s+(?:me)?)\s+(simple\s+examples?|visuals?|step\s+by\s+step|in-depth|short\s+answers?|real-world\s+analogies?)", re.IGNORECASE),
        "LEARNING_PROFILE",
        lambda m: f"Prefers explanations with {m.group(1).strip().lower()}",
        1.1,
        0.85
    ),
    # Difficulty / Level
    (
        re.compile(r"\bi\s+am\s+a\s+(beginner|intermediate|advanced)\s+(?:in|at|student|learner)", re.IGNORECASE),
        "LEARNING_PROFILE",
        lambda m: f"Self-identified {m.group(1).lower()} level learner",
        1.0,
        0.90
    )
]


class MemoryService:
    def __init__(self, db: AsyncSession):
        self.repo = MemoryRepository(db)

    async def extract_and_save_memories(
        self,
        user_id: str,
        message_text: str,
        conversation_id: Optional[str] = None,
    ) -> List[UserMemory]:
        """
        Scans message for strong preference/goal signals and persists extracted memories.
        Only extracts when high confidence pattern matches.
        """
        extracted = []
        for pattern, category, formatter, importance, confidence in _EXTRACTION_RULES:
            match = pattern.search(message_text)
            if match:
                content = formatter(match)
                try:
                    mem = await self.repo.create_memory(
                        user_id=user_id,
                        category=category,
                        content=content,
                        importance=importance,
                        confidence=confidence,
                        source_conversation_id=conversation_id,
                    )
                    extracted.append(mem)
                    logger.info(f"Extracted memory: [{category}] {content} for user={user_id}")
                except Exception as e:
                    logger.warning(f"Failed to persist extracted memory: {e}")
        return extracted

    async def get_relevant_memory_context(
        self,
        user_id: str,
        current_prompt: str,
        limit: int = 5,
    ) -> str:
        """
        Retrieves top relevant memories and formats them into a compact text block.
        """
        memories = await self.repo.get_relevant_memories(
            user_id=user_id,
            query_text=current_prompt,
            limit=limit,
        )
        if not memories:
            return ""

        lines = [f"- {m.content}" for m in memories]
        return "\n".join(lines)

    async def list_user_memories(self, user_id: str) -> List[UserMemory]:
        return await self.repo.list_memories(user_id=user_id, active_only=False)

    async def create_user_memory(
        self,
        user_id: str,
        category: str,
        content: str,
        importance: float = 1.0,
        confidence: float = 1.0,
        source_conversation_id: Optional[str] = None,
    ) -> UserMemory:
        return await self.repo.create_memory(
            user_id=user_id,
            category=category,
            content=content,
            importance=importance,
            confidence=confidence,
            source_conversation_id=source_conversation_id,
        )

    async def update_user_memory(
        self,
        memory_id: str,
        user_id: str,
        content: Optional[str] = None,
        importance: Optional[float] = None,
        confidence: Optional[float] = None,
        active: Optional[bool] = None,
    ) -> Optional[UserMemory]:
        return await self.repo.update_memory(
            memory_id=memory_id,
            user_id=user_id,
            content=content,
            importance=importance,
            confidence=confidence,
            active=active,
        )

    async def delete_user_memory(self, memory_id: str, user_id: str) -> bool:
        return await self.repo.delete_memory(memory_id=memory_id, user_id=user_id)

    async def clear_all_user_memories(self, user_id: str) -> int:
        return await self.repo.clear_all_memories(user_id=user_id)
