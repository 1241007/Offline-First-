from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class ConversationCreateResponse(BaseModel):
    id: str
    title: str
    created_at: datetime
    updated_at: datetime

class ConversationSummary(BaseModel):
    id: str
    title: str
    updated_at: datetime

class MessageResponse(BaseModel):
    id: str
    conversation_id: str
    role: str
    content: str
    created_at: datetime

class ConversationDetail(BaseModel):
    id: str
    title: str
    created_at: datetime
    updated_at: datetime
    messages: list[MessageResponse]

class SendMessageRequest(BaseModel):
    content: str
    explanation_mode: str = "general"  # "teacher" | "general" | "explainable"

class SendMessageResponse(BaseModel):
    user_message: MessageResponse
    assistant_message: MessageResponse
