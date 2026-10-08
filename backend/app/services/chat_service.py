import uuid
import json
import logging
from datetime import datetime, timezone
from typing import Optional, AsyncIterator
from fastapi import HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.repositories.chat_repository import ChatRepository
from app.services.gemini_service import gemini_service
from app.services.memory_service import MemoryService
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
        self.db = db
        self.repo = ChatRepository(db)
        self.memory_service = MemoryService(db)

    async def create_conversation(
        self,
        user_id: str,
        title: str = "New Conversation",
        conversation_id: Optional[str] = None
    ) -> Conversation:
        return await self.repo.create_conversation(
            user_id=user_id,
            title=title,
            conversation_id=conversation_id
        )

    async def list_conversations(
        self,
        user_id: str,
        limit: int = 20,
        cursor: Optional[str] = None,
        search: Optional[str] = None,
        include_archived: bool = False,
    ):
        cursor_dt = None
        if cursor:
            try:
                cursor_dt = datetime.fromisoformat(cursor)
            except Exception:
                cursor_dt = None

        return await self.repo.list_conversations(
            user_id=user_id,
            limit=limit,
            cursor_updated_at=cursor_dt,
            search=search,
            include_archived=include_archived,
        )

    async def get_conversation(self, conversation_id: str, user_id: str) -> Conversation:
        conv = await self.repo.get_conversation(conversation_id)
        if not conv:
            raise HTTPException(status_code=404, detail="Conversation not found")
        if conv.user_id is not None and conv.user_id != user_id:
            raise HTTPException(status_code=403, detail="Forbidden: conversation does not belong to user")
        return conv

    async def get_messages_paginated(
        self,
        conversation_id: str,
        user_id: str,
        limit: int = 30,
        before_timestamp: Optional[str] = None,
    ):
        await self.get_conversation(conversation_id, user_id)
        before_dt = None
        if before_timestamp:
            try:
                before_dt = datetime.fromisoformat(before_timestamp)
            except Exception:
                before_dt = None

        return await self.repo.get_messages_paginated(
            conversation_id=conversation_id,
            limit=limit,
            before_created_at=before_dt,
        )

    async def send_message(
        self,
        conversation_id: str,
        user_id: str,
        content: str,
        explanation_mode: str = "general",
        client_message_id: Optional[str] = None,
        parent_id: Optional[str] = None,
    ) -> tuple[Message, Message]:
        # 1. Validate conversation exists and belongs to user
        conv = await self.get_conversation(conversation_id, user_id)

        # 2. Extract long-term memories from user statement
        await self.memory_service.extract_and_save_memories(
            user_id=user_id,
            message_text=content,
            conversation_id=conversation_id,
        )

        # 3. Retrieve relevant memory context for the current prompt
        memory_context = await self.memory_service.get_relevant_memory_context(
            user_id=user_id,
            current_prompt=content,
            limit=5,
        )

        # 4. Save user message with stable ID if supplied
        user_msg = await self.repo.save_message(
            conversation_id=conversation_id,
            role="user",
            content=content,
            message_id=client_message_id,
            parent_id=parent_id,
        )
        logger.info(f"Saved user message id={user_msg.id} to conv={conversation_id}")

        # 5. Load bounded context (most recent N messages including the one just saved)
        recent = await self.repo.get_recent_messages(
            conversation_id=conversation_id,
            limit=settings.chat_context_messages,
        )

        # 6. Format as Gemini history
        history = []
        for msg in recent:
            role = "user" if msg.role == "user" else "model"
            history.append({"role": role, "parts": [msg.content]})

        # 7. Call Gemini with injected memory context
        gen_kwargs = {}
        if memory_context:
            gen_kwargs["memory_context"] = memory_context

        try:
            ai_text = await gemini_service.generate_response(
                history=history,
                explanation_mode=explanation_mode,
                **gen_kwargs
            )
        except Exception as exc:
            logger.error(f"Gemini call failed for conv={conversation_id}: {type(exc).__name__}")
            raise HTTPException(
                status_code=502,
                detail="AI service temporarily unavailable. Please try again.",
            ) from exc

        # 8. Save assistant response
        assistant_msg = await self.repo.save_message(
            conversation_id=conversation_id,
            role="assistant",
            content=ai_text,
            parent_id=user_msg.id,
        )
        logger.info(f"Saved assistant message id={assistant_msg.id}")

        # 9. Update conversation metadata (title on first real exchange)
        is_first_message = len(conv.messages) <= 1
        new_title = None
        if conv.title == "New Conversation" and is_first_message:
            new_title = _generate_title(content)
        await self.repo.update_conversation(
            conversation_id=conversation_id,
            title=new_title,
        )

        return user_msg, assistant_msg

    async def stream_message(
        self,
        conversation_id: str,
        user_id: str,
        content: str,
        explanation_mode: str = "general",
        client_message_id: Optional[str] = None,
        parent_id: Optional[str] = None,
    ) -> AsyncIterator[str]:
        """
        Server-Sent Events (SSE) generator for streaming AI responses.
        Handles client disconnection / Stop generation gracefully by saving partial text.
        """
        conv = await self.get_conversation(conversation_id, user_id)

        # Extract memories
        await self.memory_service.extract_and_save_memories(
            user_id=user_id,
            message_text=content,
            conversation_id=conversation_id,
        )

        # Get relevant memory context
        memory_context = await self.memory_service.get_relevant_memory_context(
            user_id=user_id,
            current_prompt=content,
            limit=5,
        )

        # Save user message
        user_msg = await self.repo.save_message(
            conversation_id=conversation_id,
            role="user",
            content=content,
            message_id=client_message_id,
            parent_id=parent_id,
        )

        # Reserve assistant message ID up front for streaming stability
        assistant_id = str(uuid.uuid4())

        # Yield initial metadata
        yield f"data: {json.dumps({'type': 'metadata', 'user_message_id': user_msg.id, 'assistant_message_id': assistant_id})}\n\n"

        # Context
        recent = await self.repo.get_recent_messages(
            conversation_id=conversation_id,
            limit=settings.chat_context_messages,
        )
        history = []
        for msg in recent:
            role = "user" if msg.role == "user" else "model"
            history.append({"role": role, "parts": [msg.content]})

        accumulated_chunks = []
        try:
            async for token in gemini_service.generate_response_stream(
                history=history,
                explanation_mode=explanation_mode,
                memory_context=memory_context,
            ):
                accumulated_chunks.append(token)
                yield f"data: {json.dumps({'type': 'token', 'content': token})}\n\n"
        except Exception as exc:
            logger.warning(f"Stream interrupted or cancelled for conv={conversation_id}: {exc}")
            yield f"data: {json.dumps({'type': 'error', 'detail': str(exc)})}\n\n"
        finally:
            # End-to-end Stop generation handling: persist accumulated content (even if partial)
            final_text = "".join(accumulated_chunks).strip()
            if final_text:
                await self.repo.save_message(
                    conversation_id=conversation_id,
                    role="assistant",
                    content=final_text,
                    message_id=assistant_id,
                    parent_id=user_msg.id,
                )

            # Update title if first message
            is_first_message = len(conv.messages) <= 1
            new_title = None
            if conv.title == "New Conversation" and is_first_message:
                new_title = _generate_title(content)
            await self.repo.update_conversation(
                conversation_id=conversation_id,
                title=new_title,
            )

            yield f"data: {json.dumps({'type': 'done', 'assistant_message_id': assistant_id, 'title': new_title or conv.title})}\n\n"

    async def update_conversation(
        self,
        conversation_id: str,
        user_id: str,
        title: Optional[str] = None,
        is_archived: Optional[bool] = None,
        is_pinned: Optional[bool] = None,
        draft_text: Optional[str] = None,
    ) -> None:
        await self.get_conversation(conversation_id, user_id)
        await self.repo.update_conversation(
            conversation_id=conversation_id,
            title=title,
            is_archived=is_archived,
            is_pinned=is_pinned,
            draft_text=draft_text,
        )

    async def delete_conversation(self, conversation_id: str, user_id: str) -> None:
        deleted = await self.repo.delete_conversation(conversation_id, user_id)
        if not deleted:
            raise HTTPException(status_code=404, detail="Conversation not found")

    async def sync_messages(
        self,
        user_id: str,
        messages: list,
    ) -> list[str]:
        """
        Idempotent offline-first sync handler for batches of messages.
        Ensures local messages are safely committed remotely without duplicates.
        """
        synced_ids = []
        for item in messages:
            try:
                # Ensure conversation exists
                await self.repo.create_conversation(
                    user_id=user_id,
                    conversation_id=item.conversation_id,
                )
                saved = await self.repo.save_message(
                    conversation_id=item.conversation_id,
                    role=item.role,
                    content=item.content,
                    message_id=item.id,
                    parent_id=item.parent_id,
                    created_at=item.created_at,
                )
                synced_ids.append(saved.id)
            except Exception as e:
                logger.error(f"Error syncing message {item.id}: {e}")

        return synced_ids
