from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional, List


class ConversationCreateResponse(BaseModel):
    id: str
    title: str
    created_at: datetime
    updated_at: datetime
    is_archived: bool = False
    is_pinned: bool = False
    draft_text: Optional[str] = ""


class ConversationSummary(BaseModel):
    id: str
    title: str
    updated_at: datetime
    is_archived: bool = False
    is_pinned: bool = False
    draft_text: Optional[str] = ""


class ConversationCursorPage(BaseModel):
    items: List[ConversationSummary]
    next_cursor: Optional[str] = None
    has_more: bool = False


class MessageResponse(BaseModel):
    id: str
    conversation_id: str
    role: str
    content: str
    parent_id: Optional[str] = None
    is_edited: bool = False
    created_at: datetime


class MessageCursorPage(BaseModel):
    items: List[MessageResponse]
    next_cursor: Optional[str] = None
    has_more: bool = False


class ConversationDetail(BaseModel):
    id: str
    title: str
    created_at: datetime
    updated_at: datetime
    is_archived: bool = False
    is_pinned: bool = False
    draft_text: Optional[str] = ""
    messages: list[MessageResponse]


class SendMessageRequest(BaseModel):
    content: str
    explanation_mode: str = "general"  # "teacher" | "general" | "explainable"
    client_message_id: Optional[str] = None
    parent_id: Optional[str] = None


class SendMessageResponse(BaseModel):
    user_message: MessageResponse
    assistant_message: MessageResponse


class StreamMessageRequest(BaseModel):
    content: str
    explanation_mode: str = "general"
    client_message_id: Optional[str] = None
    parent_id: Optional[str] = None


class UpdateConversationRequest(BaseModel):
    title: Optional[str] = None
    is_archived: Optional[bool] = None
    is_pinned: Optional[bool] = None
    draft_text: Optional[str] = None


class SyncMessageItem(BaseModel):
    id: str
    conversation_id: str
    role: str
    content: str
    parent_id: Optional[str] = None
    created_at: datetime


class SyncMessagesRequest(BaseModel):
    messages: List[SyncMessageItem]


class SyncMessagesResponse(BaseModel):
    synced_ids: List[str]
