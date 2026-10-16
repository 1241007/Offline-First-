"""Pydantic schemas for Personalized Roadmap and Assessment endpoints"""

from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


class AssessmentQuestionResponse(BaseModel):
    id: str
    topic_tag: str
    question_type: str = "multiple_choice"  # multiple_choice, code_output, debugging, free_text
    prompt: str
    code_snippet: Optional[str] = None
    options: List[str] = []
    correct_option_index: Optional[int] = None
    difficulty_level: int = 1
    explanation: str = ""


class TopicMasteryStateResponse(BaseModel):
    topic_tag: str
    mastery_level: str = "unknown"  # unknown, weak, adequate, strong
    evidence_count: int = 0
    correct_count: int = 0


class CorrectionPromptResponse(BaseModel):
    field: str
    current_value: str
    suggested_value: str
    reason: str


class AssessmentSessionResponse(BaseModel):
    session_id: str
    domain: str = "Python"
    phase: str = "profile_collection"  # profile_collection, diagnostic, summary, roadmap_ready
    profile_goal: str = ""
    profile_level: str = ""
    profile_study_hours_per_week: int = 5
    profile_timeline_weeks: Optional[int] = None
    current_question: Optional[AssessmentQuestionResponse] = None
    question_number: int = 0
    total_questions: int = 0
    mastery_map: Dict[str, TopicMasteryStateResponse] = {}
    is_complete: bool = False
    generated_roadmap_id: Optional[str] = None
    last_answer_correct: Optional[bool] = None
    last_answer_explanation: str = ""
    correction_prompt: Optional[CorrectionPromptResponse] = None
    error_message: Optional[str] = None


class SubmitAnswerRequest(BaseModel):
    answer: str
    quiz_selected_index: Optional[int] = None


class CorrectProfileFieldRequest(BaseModel):
    field: str
    value: str


class PersonalizedRoadmapTopicResponse(BaseModel):
    id: str
    topic_tag: str
    title: str
    short_description: str = ""
    estimated_minutes: int = 20
    prerequisite_topic_ids: List[str] = []
    status: str = "locked"  # locked, unlocked, in_progress, completed, skipped
    mastery_level: str = "unknown"  # unknown, weak, adequate, strong
    priority_score: int = 0
    accelerated: bool = False
    explanation: str = ""


class PersonalizedRoadmapEdgeResponse(BaseModel):
    from_topic_id: str
    to_topic_id: str


class PersonalizedRoadmapDetailResponse(BaseModel):
    id: str
    title: str = "My Python Roadmap"
    domain: str = "Python"
    goal: str = ""
    level: str = ""
    study_hours_per_week: int = 5
    estimated_weeks: int = 8
    topics: List[PersonalizedRoadmapTopicResponse] = []
    edges: List[PersonalizedRoadmapEdgeResponse] = []
    assessment_session_id: str = ""
    created_at: str = ""
    updated_at: str = ""
    completed_topic_ids: List[str] = []
    in_progress_topic_ids: List[str] = []


class RenameRoadmapRequest(BaseModel):
    title: str


class ValidateSubjectRequest(BaseModel):
    subject: str


class ValidateSubjectResponse(BaseModel):
    is_valid: bool
    standardized_name: str
    message: str


class StartAssessmentRequest(BaseModel):
    domain: Optional[str] = "Python"
    goal: Optional[str] = None
    level: Optional[str] = None
    study_hours_per_week: Optional[int] = 5


class TopicLearningResourceResponse(BaseModel):
    title: str
    url: str
    source_name: str
    description: str
    is_verified: bool = True


class TopicCodeExampleResponse(BaseModel):
    title: str
    code: str
    explanation: str
    expected_output: str = ""


class TopicCommonMistakeResponse(BaseModel):
    mistake: str
    why_it_happens: str
    how_to_fix: str


class TopicPracticeQuizResponse(BaseModel):
    question: str
    options: List[str]
    correct_option_index: int
    explanation: str


class TopicLearningContentResponse(BaseModel):
    topic_id: str
    topic_title: str
    domain: str
    learning_objectives: List[str] = []
    beginner_explanation: str = ""
    syntax_and_concepts: str = ""
    code_examples: List[TopicCodeExampleResponse] = []
    common_mistakes: List[TopicCommonMistakeResponse] = []
    practice_quiz: Optional[TopicPracticeQuizResponse] = None
    resources: List[TopicLearningResourceResponse] = []


class CompleteTopicRequest(BaseModel):
    quiz_score: Optional[int] = 100
    quiz_passed: bool = True


class CompleteTopicResponse(BaseModel):
    roadmap_id: str
    completed_topic_id: str
    unlocked_topic_ids: List[str] = []
    next_recommended_topic_id: Optional[str] = None
    overall_progress_percent: int = 0
    roadmap: PersonalizedRoadmapDetailResponse

