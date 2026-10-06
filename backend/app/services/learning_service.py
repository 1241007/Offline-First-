"""Learning progress service for business logic"""

from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from app.repositories.learning_repository import LearningRepository
from app.repositories.course_repository import CourseRepository
from app.schemas.learning import LearningCourseResponse


class LearningService:
    """Service for learning progress business logic"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
        self.repo = LearningRepository(db)
        self.course_repo = CourseRepository(db)
    
    async def get_in_progress_courses(
        self,
        user_id: str,
        limit: Optional[int] = None,
        offset: int = 0
    ) -> List[LearningCourseResponse]:
        """Get user's in-progress courses with optional pagination"""
        progress_records = await self.repo.get_user_course_progress(
            user_id=user_id,
            status="in_progress",
            limit=limit,
            offset=offset
        )
        
        result = []
        for progress in progress_records:
            course = progress.course
            
            # Find the first incomplete lesson or the last lesson
            current_lesson_title = "Start Learning"
            if course.modules:
                for module in course.modules:
                    if module.lessons:
                        # Find first incomplete lesson
                        for lesson in module.lessons:
                            lesson_progress = await self.repo.get_user_lesson_progress(
                                user_id, lesson.id
                            )
                            if not lesson_progress or not lesson_progress.completed:
                                current_lesson_title = lesson.title
                                break
                        if current_lesson_title != "Start Learning":
                            break
            
            result.append(LearningCourseResponse(
                id=course.id,
                name=course.name,
                lesson=current_lesson_title,
                progress=progress.completion_percentage / 100.0
            ))
        
        return result
    
    async def get_completed_courses(
        self,
        user_id: str,
        limit: Optional[int] = None,
        offset: int = 0
    ) -> List[LearningCourseResponse]:
        """Get user's completed courses with optional pagination"""
        progress_records = await self.repo.get_user_course_progress(
            user_id=user_id,
            status="completed",
            limit=limit,
            offset=offset
        )
        
        return [
            LearningCourseResponse(
                id=progress.course.id,
                name=progress.course.name,
                lesson="Completed",
                progress=1.0
            )
            for progress in progress_records
        ]
    
    async def enroll_in_course(self, user_id: str, course_id: str) -> bool:
        """Enroll user in a course"""
        # Check if course exists
        course = await self.course_repo.get_course_by_id(course_id)
        if not course:
            return False
        
        # Check if already enrolled
        existing = await self.repo.get_user_course_progress_by_course(user_id, course_id)
        if existing:
            return True  # Already enrolled
        
        # Create enrollment
        await self.repo.create_course_enrollment(user_id, course_id)
        await self.db.commit()
        
        return True
    
    async def complete_lesson(self, user_id: str, lesson_id: str) -> bool:
        """Mark lesson as complete and update course progress"""
        # Get lesson to find the course
        from app.models import Lesson, CourseModule
        from sqlalchemy import select
        
        query = select(Lesson).where(Lesson.id == lesson_id)
        result = await self.db.execute(query)
        lesson = result.scalar_one_or_none()
        
        if not lesson:
            return False
        
        # Get module to find course
        query = select(CourseModule).where(CourseModule.id == lesson.module_id)
        result = await self.db.execute(query)
        module = result.scalar_one_or_none()
        
        if not module:
            return False
        
        course_id = module.course_id
        
        # Ensure user is enrolled
        progress = await self.repo.get_user_course_progress_by_course(user_id, course_id)
        if not progress:
            # Auto-enroll if not enrolled
            await self.repo.create_course_enrollment(user_id, course_id)
        
        # Mark lesson complete
        await self.repo.mark_lesson_complete(user_id, lesson_id)
        
        # Update course progress
        await self.repo.update_course_progress(user_id, course_id)
        
        # Commit transaction
        await self.db.commit()
        
        return True
