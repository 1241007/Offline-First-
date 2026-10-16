"""Course repository for database operations"""

from typing import List, Optional
from sqlalchemy import select, or_
from sqlalchemy.orm import selectinload
from sqlalchemy.ext.asyncio import AsyncSession
from app.models import CourseCategory, Course, CourseModule, Lesson

# Mapping between Android client 7 canonical track IDs and backend slugs
ANDROID_TRACK_SLUG_MAP = {
    "c-cpp": "cpp-programming",
    "c-dsa": "data-structures-algorithms",
    "c-ds": "data-science-python",
    "c-py": "python-fundamentals",
    "c-ml": "machine-learning-fundamentals",
    "c-fs": "full-stack-development",
    "c-sde": "software-engineering-fundamentals",
}


class CourseRepository:
    """Repository for course-related database operations"""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_all_courses(
        self,
        featured_only: bool = False,
        category_slug: Optional[str] = None,
        limit: Optional[int] = None,
        offset: int = 0
    ) -> List[Course]:
        """Get active courses with optional filtering and pagination"""
        query = select(Course).where(Course.status == "active")
        
        if featured_only:
            query = query.where(Course.is_featured == True)
        
        if category_slug:
            # Join with category to filter by slug
            query = query.join(CourseCategory).where(CourseCategory.slug == category_slug)
        
        query = query.order_by(Course.created_at).offset(offset)
        if limit is not None:
            query = query.limit(limit)
        
        result = await self.db.execute(query)
        return list(result.scalars().all())

    async def get_course_by_id(self, course_id: str) -> Optional[Course]:
        """
        Get active course with category, modules, and lessons.
        Supports lookup by primary key UUID, course slug, or canonical Android track alias.
        """
        target_slug = ANDROID_TRACK_SLUG_MAP.get(course_id, course_id)

        query = select(Course).where(
            or_(
                Course.id == course_id,
                Course.slug == course_id,
                Course.slug == target_slug
            ),
            Course.status == "active"
        ).options(
            selectinload(Course.category),
            selectinload(Course.modules).selectinload(CourseModule.lessons)
        )

        result = await self.db.execute(query)
        return result.scalar_one_or_none()

    async def get_course_by_slug(self, slug: str) -> Optional[Course]:
        """Get active course by slug with category, modules, and lessons"""
        target_slug = ANDROID_TRACK_SLUG_MAP.get(slug, slug)

        query = select(Course).where(
            or_(
                Course.slug == slug,
                Course.slug == target_slug
            ),
            Course.status == "active"
        ).options(
            selectinload(Course.category),
            selectinload(Course.modules).selectinload(CourseModule.lessons)
        )

        result = await self.db.execute(query)
        return result.scalar_one_or_none()
