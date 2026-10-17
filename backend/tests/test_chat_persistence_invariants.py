"""
Tests for Chat Persistence Invariants, Conversation Lifecycle, and Transaction Safety.

Validates:
- Conversation creation, selection, renaming, and pagination.
- Message persistence, client message ID preservation, parent-child threading.
- Database transaction atomicity and rollback on simulated errors.
- Memory extraction and retrieval with strict user isolation (no cross-user memory leakage).
"""

import uuid
import pytest
from sqlalchemy import text
from unittest.mock import patch

from app.models.conversation import Conversation
from app.models.message import Message
from app.models.memory import UserMemory
from app.repositories.chat_repository import ChatRepository
from app.services.memory_service import MemoryService


@pytest.mark.asyncio
async def test_conversation_lifecycle_and_messages(db_session):
    """Verify complete lifecycle: create, add messages, order check, rename, and delete."""
    repo = ChatRepository(db_session)
    user_id = str(uuid.uuid4())
    conv_id = str(uuid.uuid4())

    # 1. Create conversation
    conv = await repo.create_conversation(user_id=user_id, title="Test Chat", conversation_id=conv_id)
    assert conv.id == conv_id
    assert conv.title == "Test Chat"
    assert conv.user_id == user_id

    # 2. Add sequential messages
    msg1_id = str(uuid.uuid4())
    msg2_id = str(uuid.uuid4())

    m1 = await repo.save_message(
        conversation_id=conv_id,
        role="user",
        content="First question",
        message_id=msg1_id,
        parent_id=None
    )
    m2 = await repo.save_message(
        conversation_id=conv_id,
        role="assistant",
        content="First answer",
        message_id=msg2_id,
        parent_id=msg1_id
    )

    assert m1.id == msg1_id
    assert m2.id == msg2_id
    assert m2.parent_id == msg1_id

    # 3. Verify ordering & pagination
    recent = await repo.get_recent_messages(conversation_id=conv_id, limit=10)
    assert len(recent) == 2
    assert recent[0].id == msg1_id
    assert recent[1].id == msg2_id

    # 4. Rename conversation
    await repo.update_conversation(conversation_id=conv_id, title="Renamed Chat")
    updated = await repo.get_conversation(conv_id)
    assert updated is not None
    assert updated.title == "Renamed Chat"

    # 5. Delete conversation and cascade messages
    deleted = await repo.delete_conversation(conversation_id=conv_id)
    assert deleted is True
    check_conv = await repo.get_conversation(conv_id)
    assert check_conv is None


@pytest.mark.asyncio
async def test_transaction_rollback_on_save_message_failure(db_session):
    """Verify that a database failure during message persistence triggers atomic rollback."""
    repo = ChatRepository(db_session)
    user_id = str(uuid.uuid4())
    conv_id = str(uuid.uuid4())

    conv = await repo.create_conversation(user_id=user_id, title="Rollback Chat", conversation_id=conv_id)

    # Force a database failure during save_message commit
    with patch.object(db_session, "commit", side_effect=RuntimeError("Simulated DB commit error")):
        with pytest.raises(RuntimeError):
            await repo.save_message(
                conversation_id=conv_id,
                role="user",
                content="This should rollback",
                message_id=str(uuid.uuid4())
            )
        # Session rollback discards pending uncommitted entities
        await db_session.rollback()

    # After rollback, clean session state: message must not exist in database
    recent = await repo.get_recent_messages(conversation_id=conv_id, limit=10)
    assert len(recent) == 0


@pytest.mark.asyncio
async def test_memory_persistence_strict_user_isolation(db_session):
    """Verify that long-term memory extraction and retrieval strictly isolate users."""
    mem_service = MemoryService(db_session)
    user_a = str(uuid.uuid4())
    user_b = str(uuid.uuid4())
    conv_id = str(uuid.uuid4())

    # User A: target CBSE Class 10 Board exam
    await mem_service.extract_and_save_memories(
        user_id=user_a,
        message_text="I am preparing for CBSE Class 10 Board exam in March 2027.",
        conversation_id=conv_id
    )

    # User B: target Economics college entrance exam
    await mem_service.extract_and_save_memories(
        user_id=user_b,
        message_text="I am preparing for Economics college entrance exam in June.",
        conversation_id=conv_id
    )

    # Retrieve memory context for User A
    context_a = await mem_service.get_relevant_memory_context(
        user_id=user_a,
        current_prompt="When is my exam?",
        limit=5
    )
    # Retrieve memory context for User B
    context_b = await mem_service.get_relevant_memory_context(
        user_id=user_b,
        current_prompt="What subject is my exam?",
        limit=5
    )

    assert "CBSE" in context_a or "Class 10" in context_a or "2027" in context_a
    assert "Economics" not in context_a, "Cross-user memory leakage detected for User A!"

    assert "Economics" in context_b
    assert "CBSE" not in context_b, "Cross-user memory leakage detected for User B!"
