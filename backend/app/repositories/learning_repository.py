"""Learning progress repository for database operations"""

from typing import List, Optional
from datetime import date, datetime, timezone
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload
from sqlalchemy.ext.asyncio import AsyncSession
from app.models import (
    Course, CourseModule, Lesson,
    UserCourseProgress, UserLessonProgress
)


class LearningRepository:
    """Repository for learning progress operations"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def get_user_course_progress(
        self,
        user_id: str,
        status: Optional[str] = None
    ) -> List[UserCourseProgress]:
        """Get user's course progress records"""
        query = select(UserCourseProgress).where(
            UserCourseProgress.user_id == user_id
        ).options(
            selectinload(UserCourseProgress.course).selectinload(Course.modules).selectinload(CourseModule.lessons)
        )
        
        if status:
            query = query.where(UserCourseProgress.status == status)
        
        query = query.order_by(UserCourseProgress.last_accessed.desc())
        
        result = await self.db.execute(query)
        return list(result.scalars().all())
    
    async def get_user_course_progress_by_course(
        self,
        user_id: str,
        course_id: str
    ) -> Optional[UserCourseProgress]:
        """Get specific user course progress"""
        query = select(UserCourseProgress).where(
            UserCourseProgress.user_id == user_id,
            UserCourseProgress.course_id == course_id
        )
        
        result = await self.db.execute(query)
        return result.scalar_one_or_none()
    
    async def create_course_enrollment(
        self,
        user_id: str,
        course_id: str
    ) -> UserCourseProgress:
        """Create new course enrollment"""
        progress = UserCourseProgress(
            user_id=user_id,
            course_id=course_id,
            enrollment_date=date.today(),
            last_accessed=datetime.now(timezone.utc),
            completion_percentage=0.0,
            status="not_started"
        )
        
        self.db.add(progress)
        await self.db.flush()
        return progress
    
    async def get_user_lesson_progress(
        self,
        user_id: str,
        lesson_id: str
    ) -> Optional[UserLessonProgress]:
        """Get user's progress for a specific lesson"""
        query = select(UserLessonProgress).where(
            UserLessonProgress.user_id == user_id,
            UserLessonProgress.lesson_id == lesson_id
        )
        
        result = await self.db.execute(query)
        return result.scalar_one_or_none()
    
    async def mark_lesson_complete(
        self,
        user_id: str,
        lesson_id: str
    ) -> UserLessonProgress:
        """Mark a lesson as complete"""
        # Check if progress exists
        existing = await self.get_user_lesson_progress(user_id, lesson_id)
        
        if existing:
            existing.completed = True
            existing.completion_date = datetime.now(timezone.utc)
            return existing
        else:
            # Create new progress record
            progress = UserLessonProgress(
                user_id=user_id,
                lesson_id=lesson_id,
                completed=True,
                completion_date=datetime.now(timezone.utc)
            )
            self.db.add(progress)
            await self.db.flush()
            return progress
    
    async def get_total_lessons_in_course(self, course_id: str) -> int:
        """Get total number of lessons in a course"""
        query = select(func.count(Lesson.id)).select_from(
            Lesson
        ).join(
            CourseModule
        ).where(
            CourseModule.course_id == course_id
        )
        
        result = await self.db.execute(query)
        return result.scalar() or 0
    
    async def get_completed_lessons_count(self, user_id: str, course_id: str) -> int:
        """Get count of completed lessons for user in a course"""
        query = select(func.count(UserLessonProgress.id)).select_from(
            UserLessonProgress
        ).join(
            Lesson
        ).join(
            CourseModule
        ).where(
            UserLessonProgress.user_id == user_id,
            UserLessonProgress.completed == True,
            CourseModule.course_id == course_id
        )
        
        result = await self.db.execute(query)
        return result.scalar() or 0
    
    async def update_course_progress(
        self,
        user_id: str,
        course_id: str
    ) -> None:
        """Recalculate and update course progress percentage"""
        progress = await self.get_user_course_progress_by_course(user_id, course_id)
        
        if not progress:
            return
        
        total_lessons = await self.get_total_lessons_in_course(course_id)
        if total_lessons == 0:
            return
        
        completed_lessons = await self.get_completed_lessons_count(user_id, course_id)
        
        completion_percentage = (completed_lessons / total_lessons) * 100.0
        
        progress.completion_percentage = completion_percentage
        progress.last_accessed = datetime.now(timezone.utc)
        
        # Update status based on completion
        if completion_percentage == 0:
            progress.status = "not_started"
        elif completion_percentage >= 100:
            progress.status = "completed"
        else:
            progress.status = "in_progress"
        
        await self.db.flush()
