"""Learning progress API routes"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.deps import get_current_user_id
from app.services.learning_service import LearningService
from app.schemas.learning import LearningCourseResponse, EnrollmentResponse, LessonCompleteResponse

router = APIRouter(prefix="/api/v1/learning", tags=["learning"])


@router.get("/courses/in-progress", response_model=List[LearningCourseResponse])
async def get_in_progress_courses(
    limit: Optional[int] = Query(None, ge=1, le=100, description="Page limit"),
    offset: int = Query(0, ge=0, description="Page offset"),
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Get user's in-progress courses with optional pagination.
    
    Requires: Bearer token in Authorization header
    """
    service = LearningService(db)
    return await service.get_in_progress_courses(user_id, limit=limit, offset=offset)


@router.get("/courses/completed", response_model=List[LearningCourseResponse])
async def get_completed_courses(
    limit: Optional[int] = Query(None, ge=1, le=100, description="Page limit"),
    offset: int = Query(0, ge=0, description="Page offset"),
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Get user's completed courses with optional pagination.
    
    Requires: Bearer token in Authorization header
    """
    service = LearningService(db)
    return await service.get_completed_courses(user_id, limit=limit, offset=offset)


@router.post("/courses/{course_id}/enroll", response_model=EnrollmentResponse)
async def enroll_in_course(
    course_id: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Enroll user in a course.
    
    Requires: Bearer token in Authorization header
    """
    service = LearningService(db)
    success = await service.enroll_in_course(user_id, course_id)
    
    if not success:
        raise HTTPException(status_code=404, detail="Course not found")
    
    return EnrollmentResponse(message="Enrolled successfully")


@router.post("/lessons/{lesson_id}/complete", response_model=LessonCompleteResponse)
async def complete_lesson(
    lesson_id: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Mark a lesson as complete and update course progress.
    
    Requires: Bearer token in Authorization header
    """
    service = LearningService(db)
    success = await service.complete_lesson(user_id, lesson_id)
    
    if not success:
        raise HTTPException(status_code=404, detail="Lesson not found")
    
    return LessonCompleteResponse(message="Lesson marked complete")
