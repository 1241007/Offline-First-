"""Typed WebSocket event schemas for EduNova Personalized Roadmap Agent"""

from enum import Enum
from typing import Any, Dict, List, Optional, Union
from pydantic import BaseModel, Field


class RoadmapClientEventType(str, Enum):
    START_ROADMAP = "start_roadmap"
    SUBMIT_ANSWER = "submit_answer"
    CORRECT_PROFILE = "correct_profile"
    REQUEST_CLARIFICATION = "request_clarification"
    GENERATE_ROADMAP = "generate_roadmap"
    RESUME_SESSION = "resume_session"
    CANCEL_SESSION = "cancel_session"
    PING = "ping"


class RoadmapServerEventType(str, Enum):
    SESSION_READY = "session_ready"
    ASSISTANT_MESSAGE = "assistant_message"
    NEXT_QUESTION = "next_question"
    ASSESSMENT_UPDATE = "assessment_update"
    LEARNER_PROFILE_UPDATED = "learner_profile_updated"
    ROADMAP_GENERATION_STARTED = "roadmap_generation_started"
    ROADMAP_READY = "roadmap_ready"
    SESSION_RESTORED = "session_restored"
    SESSION_COMPLETE = "session_complete"
    ERROR = "error"
    PONG = "pong"


class SkillStatusEnum(str, Enum):
    KNOWN = "known"
    MASTERED = "mastered"
    PARTIAL = "partial"
    WEAK = "weak"
    UNVERIFIED = "unverified"
    IRRELEVANT = "irrelevant"


class EvidenceSourceEnum(str, Enum):
    VERIFIED_RECORD = "verified_record"
    LEARNER_REPORTED = "learner_reported"
    DIAGNOSTIC_VERIFIED = "diagnostic_verified"
    INFERRED_ESTIMATE = "inferred_estimate"
    UNKNOWN = "unknown"


# --- Skill Inventory Record ---

class SkillRecordDto(BaseModel):
    skill_id: str
    name: str
    description: str = ""
    status: SkillStatusEnum = SkillStatusEnum.UNVERIFIED
    confidence: float = Field(default=0.0, ge=0.0, le=1.0)
    source: EvidenceSourceEnum = EvidenceSourceEnum.UNKNOWN
    evidence_notes: str = ""
    attempts: int = 0
    misconceptions: List[str] = Field(default_factory=list)
    prerequisite_ids: List[str] = Field(default_factory=list)


# --- Diagnostic Question DTO ---

class DiagnosticQuestionDto(BaseModel):
    id: str
    prompt: str
    question_type: str = "multiple_choice"  # multiple_choice, code_output, debugging, conceptual
    options: List[str] = Field(default_factory=list)
    code_snippet: Optional[str] = None
    skill_tested_id: str
    skill_tested_name: str
    difficulty: str = "Intermediate"  # Beginner, Intermediate, Advanced
    selection_rationale: str = ""


# --- Client Payloads ---

class StartRoadmapPayload(BaseModel):
    domain: str = "Python"
    goal: Optional[str] = None
    target_level: Optional[str] = None
    weekly_hours: Optional[float] = None
    target_timeline_weeks: Optional[int] = None


class SubmitAnswerPayload(BaseModel):
    message_id: str
    answer_text: Optional[str] = None
    quiz_selected_index: Optional[int] = None
    question_id: Optional[str] = None


class CorrectProfilePayload(BaseModel):
    field: str  # goal, level, weekly_hours, timeline
    value: Any


class RequestClarificationPayload(BaseModel):
    question_id: str
    query: str


class ResumeSessionPayload(BaseModel):
    session_id: str


# Generic envelope for client messages
class RoadmapClientEvent(BaseModel):
    event: RoadmapClientEventType
    session_id: Optional[str] = None
    message_id: Optional[str] = None
    payload: Optional[Dict[str, Any]] = None


# Generic envelope for server messages
class RoadmapServerEvent(BaseModel):
    event: RoadmapServerEventType
    session_id: str
    message_id: Optional[str] = None
    timestamp: Optional[str] = None
    payload: Dict[str, Any] = Field(default_factory=dict)
