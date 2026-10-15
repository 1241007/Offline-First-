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
        category: Optional[str] = None,
        limit: Optional[int] = None,
        offset: int = 0
    ) -> List[CourseListResponse]:
        """Get courses with optional filtering and pagination"""
        courses = await self.repo.get_all_courses(
<<<<<<< HEAD
            featured=featured,
            category_slug=category
=======
            featured_only=featured or False,
            category_slug=category,
            limit=limit,
            offset=offset
>>>>>>> origin
        )
        
        response_items = []
        for course in courses:
            modules = course.modules or []
            total_lessons = sum(len(m.lessons or []) for m in modules)
            modules_count = len(modules)
            response_items.append(
                CourseListResponse(
                    id=course.id,
                    name=course.name,
                    slug=course.slug,
                    description=course.description,
                    icon=course.icon,
                    accent_color=course.accent_color,
                    category=course.category.name if course.category else "",
                    category_slug=course.category.slug if course.category else None,
                    level=course.level,
                    duration_hours=course.duration_hours,
                    is_featured=course.is_featured,
                    total_lessons=total_lessons,
                    modules_count=modules_count
                )
            )
        return response_items
    
    async def get_course_detail(self, course_id: str) -> Optional[CourseDetailResponse]:
        """Get course with modules and lessons, ordered by display_order"""
        course = await self.repo.get_course_by_id(course_id)
        
        if not course:
            return None
        
        sorted_modules = sorted(course.modules, key=lambda m: m.display_order)
        modules = [
            ModuleResponse(
                id=module.id,
                title=module.title,
                description=module.description,
                display_order=module.display_order,
                lessons=[
                    LessonResponse(
                        id=lesson.id,
                        title=lesson.title,
                        description=lesson.description,
                        content_type=lesson.content_type,
                        duration_minutes=lesson.duration_minutes,
                        display_order=lesson.display_order,
                        resource_url=lesson.resource_url,
                        resource_provider=lesson.resource_provider,
                        video_url=lesson.video_url,
                        video_provider=lesson.video_provider
                    )
                    for lesson in sorted(module.lessons, key=lambda l: l.display_order)
                ]
            )
            for module in sorted_modules
        ]
        
        total_lessons = sum(len(m.lessons) for m in modules)
        modules_count = len(modules)

        return CourseDetailResponse(
            id=course.id,
            name=course.name,
            slug=course.slug,
            description=course.description,
            icon=course.icon,
            accent_color=course.accent_color,
            category=course.category.name if course.category else "",
            category_slug=course.category.slug if course.category else None,
            level=course.level,
            duration_hours=course.duration_hours,
            is_featured=course.is_featured,
            total_lessons=total_lessons,
            modules_count=modules_count,
            prerequisites=course.prerequisites or [],
            modules=modules
        )
