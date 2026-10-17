"""Pydantic schemas for Personalized Roadmap Assessment and Generation"""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, ConfigDict, Field
from app.schemas.roadmap import RoadmapItemResponse


class AssessmentQuizPublicDto(BaseModel):
    """Diagnostic quiz presented to the client. Never contains correct_index or answers!"""
    question: str
    options: List[str]
    skill_tested: Optional[str] = Field(None, alias="skillTested")
    difficulty: Optional[str] = None
    selection_rationale: Optional[str] = Field(None, alias="selectionRationale")

    model_config = ConfigDict(populate_by_name=True)


class AssessmentMessageItem(BaseModel):
    """Single turn in an assessment session"""
    sender: str  # "mentor" or "learner"
    text: str
    options: List[str] = Field(default_factory=list)
    quiz: Optional[AssessmentQuizPublicDto] = None
    step_id: Optional[str] = Field(None, alias="stepId")
    timestamp: Optional[str] = None

    model_config = ConfigDict(populate_by_name=True)


class AssessmentSessionResponse(BaseModel):
    """Full assessment session state response for the client"""
    id: str
    state: str
    domain: str = "Python"
    current_step_id: Optional[str] = Field(None, alias="currentStepId")
    completed_steps: List[str] = Field(default_factory=list, alias="completedSteps")
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


class ProfileCorrectionRequest(BaseModel):
    """In-place correction of a specific profile field before generation"""
    field: str  # "goal", "target_level", "weekly_hours", "target_timeline"
    value: Any

    model_config = ConfigDict(populate_by_name=True)


# --- Structured Roadmap Schemas ---

class PersonalizedTaskSchema(BaseModel):
    id: str
    title: str
    description: Optional[str] = ""
    instructions: str = ""
    estimated_hours: float = Field(1.0, alias="estimatedHours")
    resources: List[str] = Field(default_factory=list)
    completion_criteria: str = Field("", alias="completionCriteria")
    is_completed: bool = Field(False, alias="isCompleted")
    dependencies: List[str] = Field(default_factory=list)
    # Curriculum-grounded attributes
    topic_id: Optional[str] = Field(None, alias="topicId")
    status: str = "UNLOCKED"  # "LOCKED", "UNLOCKED", "IN_PROGRESS", "COMPLETED", "SKIPPED"
    subtopics: List[str] = Field(default_factory=list)
    learning_objectives: List[str] = Field(default_factory=list, alias="learningObjectives")
    code_example: Optional[str] = Field(None, alias="codeExample")
    skip_reason: Optional[str] = Field(None, alias="skipReason")

    model_config = ConfigDict(populate_by_name=True)


class PersonalizedMilestoneSchema(BaseModel):
    title: str
    completion_criteria: List[str] = Field(default_factory=list, alias="completionCriteria")
    assessment: str = ""
    passing_criteria: str = Field(..., alias="passingCriteria")

    model_config = ConfigDict(populate_by_name=True)


class PersonalizedPhaseSchema(BaseModel):
    title: str
    objective: str
    duration_weeks: int = Field(1, alias="durationWeeks")
    topics: List[str] = Field(default_factory=list)
    tasks: List[PersonalizedTaskSchema] = Field(default_factory=list)
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


class SkillGapItemSchema(BaseModel):
    skill: str
    status: str = "developing"  # "verified_mastery", "partial_mastery", "needs_learning", "not_assessed", "demonstrated", "developing"
    source: str = "diagnostic_verified"  # "diagnostic_verified", "course_completion", "self_reported", "inferred"
    confidence: float = 1.0
    rationale: Optional[str] = None
    topic_id: Optional[str] = Field(None, alias="topicId")

    model_config = ConfigDict(populate_by_name=True)


class AssessmentSummarySchema(BaseModel):
    strengths: List[str] = Field(default_factory=list)
    skill_gaps: List[str] = Field(default_factory=list, alias="skillGaps")
    verified_evidence: List[str] = Field(default_factory=list, alias="verifiedEvidence")
    self_reported_information: List[str] = Field(default_factory=list, alias="selfReportedInformation")
    unknowns: List[str] = Field(default_factory=list)
    skill_gap_breakdown: List[SkillGapItemSchema] = Field(default_factory=list, alias="skillGapBreakdown")
    curriculum_rationale: Optional[str] = Field(None, alias="curriculumRationale")

    model_config = ConfigDict(populate_by_name=True)


class PersonalizedRoadmapSchema(BaseModel):
    """Full structured roadmap output validated against AI model JSON generation"""
    title: str
    goal: str
    domain: str = "Python"
    starting_level: str = Field(..., alias="startingLevel")
    category: str = "General"
    estimated_duration: str = Field(..., alias="estimatedDuration")
    weekly_hours: float = Field(..., alias="weeklyHours")
    assessment_summary: AssessmentSummarySchema = Field(..., alias="assessmentSummary")
    phases: List[PersonalizedPhaseSchema] = Field(default_factory=list)
    topics: List[Dict[str, Any]] = Field(default_factory=list)
    edges: List[Dict[str, Any]] = Field(default_factory=list)
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


class RenamePersonalizedRoadmapRequest(BaseModel):
    """Request to rename a personalized roadmap"""
    title: str = Field(..., min_length=1, max_length=255)

    model_config = ConfigDict(populate_by_name=True)


class StartAssessmentRequest(BaseModel):
    """Request payload to initiate a personalized roadmap assessment session"""
    domain: Optional[str] = "Python"
    goal: Optional[str] = None
    level: Optional[str] = None
    weekly_hours: Optional[float] = Field(None, alias="weeklyHours")

    model_config = ConfigDict(populate_by_name=True)


class ValidateSubjectRequest(BaseModel):
    subject: str


class ValidateSubjectResponse(BaseModel):
    is_valid: bool = Field(True, alias="isValid")
    standardized_name: str = Field(..., alias="standardizedName")
    suggestions: List[str] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True)


class SubmitAnswerRequest(BaseModel):
    answer: str
    quiz_selected_index: Optional[int] = Field(None, alias="quizSelectedIndex")

    model_config = ConfigDict(populate_by_name=True)


class CorrectProfileFieldRequest(BaseModel):
    field: str
    value: Any

    model_config = ConfigDict(populate_by_name=True)


class RenameRoadmapRequest(BaseModel):
    title: str

    model_config = ConfigDict(populate_by_name=True)


class TopicCodeExampleDto(BaseModel):
    title: str
    code: str
    explanation: str
    expected_output: str = Field("", alias="expectedOutput")

    model_config = ConfigDict(populate_by_name=True)


class TopicCommonMistakeDto(BaseModel):
    mistake: str
    why_it_happens: str = Field(..., alias="whyItHappens")
    how_to_fix: str = Field(..., alias="howToFix")

    model_config = ConfigDict(populate_by_name=True)


class TopicPracticeQuizDto(BaseModel):
    question: str
    options: List[str]
    correct_option_index: int = Field(..., alias="correctOptionIndex")
    explanation: str

    model_config = ConfigDict(populate_by_name=True)


class TopicLearningResourceDto(BaseModel):
    title: str
    url: str
    source_name: str = Field("Official", alias="sourceName")
    description: str = ""
    is_verified: bool = Field(True, alias="isVerified")

    model_config = ConfigDict(populate_by_name=True)


class TopicLearningContentResponse(BaseModel):
    topic_id: str = Field(..., alias="topicId")
    topic_title: str = Field(..., alias="topicTitle")
    domain: str = "Python"
    learning_objectives: List[str] = Field(default_factory=list, alias="learningObjectives")
    beginner_explanation: str = Field("", alias="beginnerExplanation")
    syntax_and_concepts: str = Field("", alias="syntaxAndConcepts")
    code_examples: List[TopicCodeExampleDto] = Field(default_factory=list, alias="codeExamples")
    common_mistakes: List[TopicCommonMistakeDto] = Field(default_factory=list, alias="commonMistakes")
    practice_quiz: Optional[TopicPracticeQuizDto] = Field(None, alias="practiceQuiz")
    resources: List[TopicLearningResourceDto] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True)


class CompleteTopicRequest(BaseModel):
    quiz_passed: bool = Field(True, alias="quizPassed")

    model_config = ConfigDict(populate_by_name=True)


class CompleteTopicResponse(BaseModel):
    roadmap_id: str = Field(..., alias="roadmapId")
    topic_id: str = Field(..., alias="topicId")
    is_completed: bool = Field(True, alias="isCompleted")
    progress_percentage: int = Field(..., alias="progressPercentage")
    completed_topic_ids: List[str] = Field(default_factory=list, alias="completedTopicIds")

    model_config = ConfigDict(populate_by_name=True)
