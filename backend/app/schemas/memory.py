from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List


class MemoryCreateRequest(BaseModel):
    category: str
    content: str
    importance: float = 1.0
    confidence: float = 1.0
    source_conversation_id: Optional[str] = None


class MemoryUpdateRequest(BaseModel):
    content: Optional[str] = None
    importance: Optional[float] = None
    confidence: Optional[float] = None
    active: Optional[bool] = None


class MemoryResponse(BaseModel):
    id: str
    user_id: str
    category: str
    content: str
    importance: float
    confidence: float
    source_conversation_id: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    last_used_at: Optional[datetime] = None
    active: bool


class MemoryListResponse(BaseModel):
    memories: List[MemoryResponse]
