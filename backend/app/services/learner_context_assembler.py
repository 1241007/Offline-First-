"""Learner Context Assembler for EduNova Personalized Roadmap"""

from typing import Dict, Any, List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.models import (
    UserProfile,
    UserCourseProgress,
    UserLessonProgress,
    UserMemory,
)
from app.repositories.profile_repository import ProfileRepository
from app.repositories.learning_repository import LearningRepository
from app.repositories.memory_repository import MemoryRepository
from app.repositories.course_repository import CourseRepository
from app.schemas.roadmap_websocket import EvidenceSourceEnum


class LearnerContextAssembler:
    """
    Gathers authentic, verified records for the authenticated learner.
    Ensures strict user-level isolation and clear provenance tagging.
    Never fabricates missing profile fields or assumes unverified mastery.
    """

    def __init__(self, db: AsyncSession):
        self.db = db
        self.profile_repo = ProfileRepository(db)
        self.learning_repo = LearningRepository(db)
        self.memory_repo = MemoryRepository(db)
        self.course_repo = CourseRepository(db)

    async def assemble_context(self, user_id: str, target_domain: str = "Python") -> Dict[str, Any]:
        profile: Optional[UserProfile] = await self.profile_repo.get_by_user_id(user_id)
        
        in_progress_courses = await self.learning_repo.get_user_course_progress(
            user_id=user_id, status="in_progress"
        )
        completed_courses = await self.learning_repo.get_user_course_progress(
            user_id=user_id, status="completed"
        )
        
        memories = await self.memory_repo.list_memories(
            user_id=user_id, active_only=True
        )

        lesson_query = select(UserLessonProgress).where(
            UserLessonProgress.user_id == user_id,
            UserLessonProgress.completed == True,
        )
        lesson_result = await self.db.execute(lesson_query)
        completed_lessons = list(lesson_result.scalars().all())

        verified_records: List[Dict[str, Any]] = []
        for c in completed_courses:
            c_name = c.course.name if c.course else "Course"
            verified_records.append({
                "type": "completed_course",
                "course_id": c.course_id,
                "course_name": c_name,
                "progress_pct": 100,
                "source": EvidenceSourceEnum.VERIFIED_RECORD.value,
                "description": f"Completed course: {c_name} (100% progress)"
            })

        for ip in in_progress_courses:
            c_name = ip.course.name if ip.course else "Course"
            pct = int(ip.completion_percentage)
            verified_records.append({
                "type": "in_progress_course",
                "course_id": ip.course_id,
                "course_name": c_name,
                "progress_pct": pct,
                "source": EvidenceSourceEnum.VERIFIED_RECORD.value,
                "description": f"In-progress course: {c_name} ({pct}% complete)"
            })

        if completed_lessons:
            verified_records.append({
                "type": "completed_lessons",
                "count": len(completed_lessons),
                "source": EvidenceSourceEnum.VERIFIED_RECORD.value,
                "description": f"Completed {len(completed_lessons)} individual lesson(s)"
            })

        learner_reported: List[Dict[str, Any]] = []
        if profile and profile.level:
            learner_reported.append({
                "field": "stated_level",
                "value": profile.level,
                "source": EvidenceSourceEnum.LEARNER_REPORTED.value,
            })
        if profile and profile.interests:
            learner_reported.append({
                "field": "interests",
                "value": profile.interests,
                "source": EvidenceSourceEnum.LEARNER_REPORTED.value,
            })

        relevant_memories = [
            f"{m.category}: {m.content}" for m in memories
        ]

        return {
            "user_id": user_id,
            "target_domain": target_domain,
            "name": profile.full_name if (profile and profile.full_name) else "Learner",
            "education_mode": profile.education_mode if profile else "general",
            "stated_level": profile.level if (profile and profile.level) else "Beginner",
            "interests": profile.interests if (profile and profile.interests) else "",
            "verified_records": verified_records,
            "learner_reported": learner_reported,
            "memories": relevant_memories,
            "has_domain_experience": any(
                target_domain.lower() in rec.get("course_name", "").lower()
                for rec in verified_records
            ),
        }
