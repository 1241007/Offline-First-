"""Learning progress-related Pydantic schemas"""

from pydantic import BaseModel, ConfigDict


class LearningCourseResponse(BaseModel):
    """User's course progress for learning screens"""
    id: str
    name: str
    lesson: str  # Current/last accessed lesson title
    progress: float  # 0.0 to 1.0
    
    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class EnrollmentResponse(BaseModel):
    """Response after enrolling in a course"""
    message: str


class LessonCompleteResponse(BaseModel):
    """Response after marking lesson complete"""
    message: str
