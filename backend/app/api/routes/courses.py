"""Course API routes"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.services.course_service import CourseService
from app.schemas.course import CourseListResponse, CourseDetailResponse

router = APIRouter(prefix="/api/v1", tags=["courses"])


@router.get("/courses", response_model=List[CourseListResponse])
async def get_courses(
    featured: Optional[bool] = Query(None, description="Filter for featured courses"),
    category: Optional[str] = Query(None, description="Filter by category slug"),
    db: AsyncSession = Depends(get_db)
):
    """
    Get all active courses.
    
    Optionally filter by:
    - featured: Show only featured courses
    - category: Filter by category slug
    """
    service = CourseService(db)
    return await service.get_courses(featured=featured, category=category)


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
