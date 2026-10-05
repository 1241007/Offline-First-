"""Course repository for database operations"""

from typing import List, Optional
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from sqlalchemy.ext.asyncio import AsyncSession
from app.models import CourseCategory, Course, CourseModule, Lesson


class CourseRepository:
    """Repository for course-related database operations"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def get_all_courses(
        self,
        featured_only: bool = False,
        category_slug: Optional[str] = None
    ) -> List[Course]:
        """Get all active courses with optional filtering"""
        query = select(Course).where(Course.status == "active")
        
        if featured_only:
            query = query.where(Course.is_featured == True)
        
        if category_slug:
            # Join with category to filter by slug
            query = query.join(CourseCategory).where(CourseCategory.slug == category_slug)
        
        result = await self.db.execute(query.order_by(Course.created_at))
        return list(result.scalars().all())
    
    async def get_course_by_id(self, course_id: str) -> Optional[Course]:
        """Get course with modules and lessons"""
        query = select(Course).where(
            Course.id == course_id,
            Course.status == "active"
        ).options(
            selectinload(Course.modules).selectinload(CourseModule.lessons)
        )
        
        result = await self.db.execute(query)
        return result.scalar_one_or_none()
    
    async def get_course_by_slug(self, slug: str) -> Optional[Course]:
        """Get course by slug"""
        query = select(Course).where(
            Course.slug == slug,
            Course.status == "active"
        )
        
        result = await self.db.execute(query)
        return result.scalar_one_or_none()
