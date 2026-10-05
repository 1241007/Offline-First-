"""Course service for business logic"""

from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from app.repositories.course_repository import CourseRepository
from app.schemas.course import CourseListResponse, CourseDetailResponse, ModuleResponse, LessonResponse


class CourseService:
    """Service for course-related business logic"""
    
    def __init__(self, db: AsyncSession):
        self.repo = CourseRepository(db)
    
    async def get_courses(
        self,
        featured: Optional[bool] = None,
        category: Optional[str] = None
    ) -> List[CourseListResponse]:
        """Get all courses with optional filtering"""
        courses = await self.repo.get_all_courses(
            featured_only=featured or False,
            category_slug=category
        )
        
        return [
            CourseListResponse(
                id=course.id,
                name=course.name,
                description=course.description,
                icon=course.icon,
                accent=course.accent_color
            )
            for course in courses
        ]
    
    async def get_course_detail(self, course_id: str) -> Optional[CourseDetailResponse]:
        """Get course with modules and lessons"""
        course = await self.repo.get_course_by_id(course_id)
        
        if not course:
            return None
        
        modules = [
            ModuleResponse(
                id=module.id,
                title=module.title,
                description=module.description,
                lessons=[
                    LessonResponse(
                        id=lesson.id,
                        title=lesson.title,
                        content_type=lesson.content_type,
                        duration_minutes=lesson.duration_minutes
                    )
                    for lesson in module.lessons
                ]
            )
            for module in course.modules
        ]
        
        return CourseDetailResponse(
            id=course.id,
            name=course.name,
            description=course.description,
            icon=course.icon,
            accent=course.accent_color,
            level=course.level,
            duration_hours=course.duration_hours,
            modules=modules
        )
