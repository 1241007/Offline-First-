"""Tests for real mode and profile curriculum context propagation."""

import pytest
from unittest.mock import AsyncMock, patch, MagicMock

from app.schemas.chat import StreamMessageRequest
from app.schemas.rag import AppMode, RAGRequestContext
from app.models.profile import UserProfile


def test_stream_message_request_curriculum_deserialization():
    """Verify StreamMessageRequest correctly parses curriculum context fields."""
    payload = {
        "content": "Explain carbon and its compounds",
        "explanation_mode": "teacher",
        "app_mode": "student",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "language": "en",
        "selected_chapter": "Chapter 4 Carbon and its Compounds",
    }
    req = StreamMessageRequest(**payload)
    assert req.content == "Explain carbon and its compounds"
    assert req.explanation_mode == "teacher"
    assert req.app_mode == "student"
    assert req.board == "CBSE"
    assert req.class_grade == "10"
    assert req.subject == "Science"
    assert req.language == "en"
    assert req.selected_chapter == "Chapter 4 Carbon and its Compounds"


def test_rag_request_context_creation():
    """Verify RAGRequestContext holds all curriculum attributes."""
    ctx = RAGRequestContext(
        execution_mode="ONLINE",
        app_mode=AppMode.STUDENT,
        board="ICSE",
        class_grade="9",
        subject="Physics",
        language="hi",
        selected_chapter="Motion",
    )
    assert ctx.app_mode == AppMode.STUDENT
    assert ctx.board == "ICSE"
    assert ctx.class_grade == "9"
    assert ctx.subject == "Physics"
    assert ctx.language == "hi"
    assert ctx.selected_chapter == "Motion"


def test_general_mode_no_cbse_defaults():
    """Verify that General Mode does not inject hardcoded CBSE curriculum defaults."""
    ctx = RAGRequestContext(
        execution_mode="ONLINE",
        app_mode=AppMode.GENERAL,
        board=None,
        class_grade=None,
        subject=None,
    )
    assert ctx.app_mode == AppMode.GENERAL
    assert ctx.board is None
    assert ctx.class_grade is None
    assert ctx.subject is None
