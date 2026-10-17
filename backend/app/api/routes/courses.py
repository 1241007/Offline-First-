"""Course API routes"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.deps import get_current_user_id
from app.services.course_service import CourseService
from app.schemas.course import (
    CourseListResponse,
    CourseDetailResponse,
    SavedCourseResponse,
    SavedCourseActionResponse,
)

router = APIRouter(prefix="/api/v1", tags=["courses"])



@router.get("/courses", response_model=List[CourseListResponse])
async def get_courses(
    featured: Optional[bool] = Query(None, description="Filter for featured courses"),
    category: Optional[str] = Query(None, description="Filter by category slug"),
    limit: Optional[int] = Query(None, ge=1, le=100, description="Page limit"),
    offset: int = Query(0, ge=0, description="Page offset"),
    db: AsyncSession = Depends(get_db)
):
    """
    Get active courses with optional filtering and pagination.
    
    Optionally filter by:
    - featured: Show only featured courses
    - category: Filter by category slug
    - limit: Page size (1-100)
    - offset: Number of records to skip
    """
    service = CourseService(db)
    return await service.get_courses(featured=featured, category=category, limit=limit, offset=offset)


@router.get("/courses/saved", response_model=List[SavedCourseResponse])
async def get_saved_courses(
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    List all saved courses for the authenticated user.
    """
    service = CourseService(db)
    return await service.get_saved_courses(user_id=current_user_id)


@router.post("/courses/{course_id}/save", response_model=SavedCourseResponse)
async def save_course(
    course_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Save a course for offline access (idempotent).
    """
    service = CourseService(db)
    return await service.save_course(user_id=current_user_id, course_id=course_id)


@router.delete("/courses/{course_id}/save", response_model=SavedCourseActionResponse)
async def unsave_course(
    course_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Unsave a course for the authenticated user (idempotent).
    """
    service = CourseService(db)
    return await service.unsave_course(user_id=current_user_id, course_id=course_id)


@router.get("/courses/{course_id}", response_model=CourseDetailResponse)
async def get_course_detail(
    course_id: str,
    db: AsyncSession = Depends(get_db)
):
    """
    Get course details with modules and lessons.
    """
    service = CourseService(db)
    course = await service.get_course_detail(course_id)
    
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")
    
    return course

