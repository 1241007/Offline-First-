"""Pydantic schemas for Personalized Roadmap Assessment and Generation"""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, ConfigDict, Field
from app.schemas.roadmap import RoadmapItemResponse


class AssessmentQuizPublicDto(BaseModel):
    """Diagnostic quiz presented to the client. Never contains correct_index or answers!"""
    question: str
    options: List[str]
    skill_tested: Optional[str] = None

    model_config = ConfigDict(populate_by_name=True)


class AssessmentMessageItem(BaseModel):
    """Single turn in an assessment session"""
    sender: str  # "mentor" or "learner"
    text: str
    options: List[str] = Field(default_factory=list)
    quiz: Optional[AssessmentQuizPublicDto] = None
    timestamp: Optional[str] = None

    model_config = ConfigDict(populate_by_name=True)


class AssessmentSessionResponse(BaseModel):
    """Full assessment session state response for the client"""
    id: str
    state: str
    goal: Optional[str] = None
    target_level: Optional[str] = Field(None, alias="targetLevel")
    target_timeline: Optional[str] = Field(None, alias="targetTimeline")
    weekly_hours: Optional[float] = Field(None, alias="weeklyHours")
    latest_message: Optional[str] = Field(None, alias="latestMessage")
    options: List[str] = Field(default_factory=list)
    quiz: Optional[AssessmentQuizPublicDto] = None
    completeness_percentage: int = Field(0, alias="completenessPercentage")
    summary: Optional[Dict[str, Any]] = None
    roadmap_id: Optional[str] = Field(None, alias="roadmapId")
    messages: List[AssessmentMessageItem] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True, from_attributes=True)


class SubmitAssessmentAnswerRequest(BaseModel):
    """Learner's response to the mentor's question or quiz"""
    answer: str
    quiz_selected_index: Optional[int] = Field(None, alias="quizSelectedIndex")

    model_config = ConfigDict(populate_by_name=True)


# --- Structured Roadmap Schemas ---

class PersonalizedMilestoneSchema(BaseModel):
    title: str
    completion_criteria: List[str] = Field(default_factory=list, alias="completionCriteria")
    assessment: str
    passing_criteria: str = Field(..., alias="passingCriteria")

    model_config = ConfigDict(populate_by_name=True)


class PersonalizedPhaseSchema(BaseModel):
    title: str
    objective: str
    duration_weeks: int = Field(1, alias="durationWeeks")
    topics: List[str] = Field(default_factory=list)
    activities: List[str] = Field(default_factory=list)
    resources: List[str] = Field(default_factory=list)
    milestones: List[PersonalizedMilestoneSchema] = Field(default_factory=list)
    recommended_course_ids: List[str] = Field(default_factory=list, alias="recommendedCourseIds")

    model_config = ConfigDict(populate_by_name=True)


class WeeklyScheduleItemSchema(BaseModel):
    day_or_week: str = Field(..., alias="dayOrWeek")
    focus_topic: str = Field(..., alias="focusTopic")
    estimated_hours: float = Field(..., alias="estimatedHours")
    tasks: List[str] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True)


class AssessmentSummarySchema(BaseModel):
    strengths: List[str] = Field(default_factory=list)
    skill_gaps: List[str] = Field(default_factory=list, alias="skillGaps")
    verified_evidence: List[str] = Field(default_factory=list, alias="verifiedEvidence")
    self_reported_information: List[str] = Field(default_factory=list, alias="selfReportedInformation")
    unknowns: List[str] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True)


class PersonalizedRoadmapSchema(BaseModel):
    """Full structured roadmap output validated against AI model JSON generation"""
    title: str
    goal: str
    starting_level: str = Field(..., alias="startingLevel")
    category: str = "General"
    estimated_duration: str = Field(..., alias="estimatedDuration")
    weekly_hours: float = Field(..., alias="weeklyHours")
    assessment_summary: AssessmentSummarySchema = Field(..., alias="assessmentSummary")
    phases: List[PersonalizedPhaseSchema] = Field(default_factory=list)
    weekly_schedule: List[WeeklyScheduleItemSchema] = Field(default_factory=list, alias="weeklySchedule")
    assumptions: List[str] = Field(default_factory=list)
    capstone_project: str = Field(..., alias="capstoneProject")
    next_action: str = Field(..., alias="nextAction")
    completed_milestones: List[str] = Field(default_factory=list, alias="completedMilestones")

    model_config = ConfigDict(populate_by_name=True)


class PersonalizedRoadmapDetailResponse(BaseModel):
    """Detailed response for a saved personalized roadmap"""
    id: str
    title: str
    goal: str
    category: str
    level: str
    duration: str
    stages: int
    icon: str
    accent_theme: str = Field("primary", alias="accentTheme")
    structure: PersonalizedRoadmapSchema
    items: List[RoadmapItemResponse] = Field(default_factory=list)
    created_at: Optional[str] = Field(None, alias="createdAt")

    model_config = ConfigDict(populate_by_name=True, from_attributes=True)


class SavePersonalizedRoadmapRequest(BaseModel):
    """Request to save or sync a personalized roadmap created locally or generated offline"""
    id: Optional[str] = None
    title: str
    goal: str
    category: Optional[str] = "General"
    level: Optional[str] = "Beginner"
    duration: Optional[str] = "8 weeks"
    icon: Optional[str] = "school"
    accent_theme: Optional[str] = Field("primary", alias="accentTheme")
    structure: PersonalizedRoadmapSchema

    model_config = ConfigDict(populate_by_name=True)


class RoadmapMilestonesSyncRequest(BaseModel):
    """Request to toggle or batch sync completed milestones"""
    milestone_key: Optional[str] = Field(None, alias="milestoneKey")
    is_completed: Optional[bool] = Field(None, alias="isCompleted")
    completed_milestones: Optional[List[str]] = Field(None, alias="completedMilestones")

    model_config = ConfigDict(populate_by_name=True)


class RoadmapMilestonesSyncResponse(BaseModel):
    """Current state of completed milestones for a personalized roadmap"""
    roadmap_id: str = Field(..., alias="roadmapId")
    completed_milestones: List[str] = Field(default_factory=list, alias="completedMilestones")

    model_config = ConfigDict(populate_by_name=True)
