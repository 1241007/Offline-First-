"""Repository for Roadmap Assessment Session database operations"""

from typing import Optional, List
from datetime import datetime, timezone
from sqlalchemy import select, desc
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.roadmap_assessment import RoadmapAssessmentSession


class RoadmapAssessmentRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_session(
        self,
        user_id: str,
        state: str = "INITIALIZING",
        goal: Optional[str] = None,
        collected_profile: Optional[dict] = None,
        messages: Optional[list] = None,
        pending_quiz: Optional[dict] = None,
    ) -> RoadmapAssessmentSession:
        session = RoadmapAssessmentSession(
            user_id=user_id,
            state=state,
            goal=goal,
            collected_profile=collected_profile or {},
            messages=messages or [],
            pending_quiz=pending_quiz,
        )
        self.db.add(session)
        await self.db.flush()
        await self.db.refresh(session)
        return session

    async def get_session_by_id(
        self, session_id: str, user_id: str
    ) -> Optional[RoadmapAssessmentSession]:
        query = select(RoadmapAssessmentSession).where(
            RoadmapAssessmentSession.id == session_id,
            RoadmapAssessmentSession.user_id == user_id,
        )
        result = await self.db.execute(query)
        return result.scalar_one_or_none()

    async def get_active_session_for_user(
        self, user_id: str
    ) -> Optional[RoadmapAssessmentSession]:
        query = (
            select(RoadmapAssessmentSession)
            .where(
                RoadmapAssessmentSession.user_id == user_id,
                RoadmapAssessmentSession.state.not_in(["COMPLETED", "ERROR"]),
            )
            .order_by(desc(RoadmapAssessmentSession.created_at))
            .limit(1)
        )
        result = await self.db.execute(query)
        return result.scalar_one_or_none()

    async def update_session(
        self,
        session: RoadmapAssessmentSession,
        state: Optional[str] = None,
        goal: Optional[str] = None,
        target_level: Optional[str] = None,
        target_timeline: Optional[str] = None,
        weekly_hours: Optional[float] = None,
        collected_profile: Optional[dict] = None,
        messages: Optional[list] = None,
        pending_quiz: Optional[dict] = None,
        roadmap_id: Optional[str] = None,
    ) -> RoadmapAssessmentSession:
        if state is not None:
            session.state = state
        if goal is not None:
            session.goal = goal
        if target_level is not None:
            session.target_level = target_level
        if target_timeline is not None:
            session.target_timeline = target_timeline
        if weekly_hours is not None:
            session.weekly_hours = weekly_hours
        if collected_profile is not None:
            session.collected_profile = collected_profile
        if messages is not None:
            session.messages = messages
        if pending_quiz is not None or "pending_quiz" in locals():
            session.pending_quiz = pending_quiz
        if roadmap_id is not None:
            session.roadmap_id = roadmap_id

        session.updated_at = datetime.now(timezone.utc)
        await self.db.flush()
        await self.db.refresh(session)
        return session
