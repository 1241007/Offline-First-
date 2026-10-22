"""Roadmap Session Manager for WebSocket Lifecycle and Persistent Assessment State"""

import logging
import uuid
from datetime import datetime, timezone
from typing import Dict, Any, Optional
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import RoadmapAssessmentSession
from app.repositories.roadmap_assessment_repository import RoadmapAssessmentRepository
from app.repositories.roadmap_repository import RoadmapRepository
from app.schemas.roadmap_websocket import (
    RoadmapClientEventType,
    RoadmapServerEventType,
    RoadmapServerEvent,
)
from app.services.learner_context_assembler import LearnerContextAssembler
from app.services.adaptive_assessment_engine import AdaptiveAssessmentEngine
from app.services.roadmap_planner import RoadmapPlanner

logger = logging.getLogger(__name__)


class RoadmapSessionManager:
    """
    Manages WebSocket session lifecycle, message sequencing, idempotency,
    persistent assessment turns, and safe database transaction boundaries.
    """

    def __init__(self, db: AsyncSession):
        self.db = db
        self.assessment_repo = RoadmapAssessmentRepository(db)
        self.roadmap_repo = RoadmapRepository(db)
        self.context_assembler = LearnerContextAssembler(db)
        self.assessment_engine = AdaptiveAssessmentEngine()
        self.planner = RoadmapPlanner()

    async def handle_start_roadmap(
        self, user_id: str, payload: Dict[str, Any]
    ) -> RoadmapServerEvent:
        domain = payload.get("domain", "Python")
        goal = payload.get("goal")
        target_level = payload.get("target_level")
        weekly_hours = payload.get("weekly_hours")

        # 1. Ensure user record exists to preserve foreign key integrity
        from app.models.user import User
        existing_user = await self.db.get(User, user_id)
        if not existing_user:
            self.db.add(User(
                id=user_id,
                email=f"{user_id}@edunova.app",
                hashed_password="placeholder_hash",
                is_active=True,
            ))
            await self.db.flush()

        # 2. Assemble authentic learner context
        context = await self.context_assembler.assemble_context(user_id=user_id, target_domain=domain)

        # 2. Initialize skill inventory
        skill_inventory = self.assessment_engine.initialize_skill_inventory(
            learner_context=context, domain=domain
        )

        # 3. Create persistent assessment session in DB
        session_id = str(uuid.uuid4())
        initial_profile = {
            "domain": domain,
            "goal": goal or "",
            "target_level": target_level or context.get("stated_level", "Beginner"),
            "weekly_hours": weekly_hours or 5.0,
            "skill_inventory": skill_inventory,
            "asked_question_ids": [],
            "quiz_history": [],
            "verified_records": context.get("verified_records", []),
            "memories": context.get("memories", []),
        }

        # 4. Select first diagnostic question
        first_q = self.assessment_engine.select_next_question(
            domain=domain,
            skill_inventory=skill_inventory,
            asked_question_ids=[],
            max_questions=5,
        )

        if first_q:
            initial_profile["asked_question_ids"].append(first_q["question_id"])

        greeting_text = (
            f"Hi {context['name']}! I'm your EduNova AI Learning Advisor. "
            f"Let's assess your {domain} foundation and construct an individualized learning roadmap."
        )

        initial_messages = [
            {
                "sender": "mentor",
                "text": greeting_text,
                "timestamp": datetime.now(timezone.utc).isoformat(),
            }
        ]

        session = await self.assessment_repo.create_session(
            user_id=user_id,
            state="ASSESSING_SKILLS",
            domain=domain,
            collected_profile=initial_profile,
            messages=initial_messages,
            pending_quiz=first_q,
        )
        session.id = session_id
        session.goal = goal or ""
        session.target_level = target_level or context.get("stated_level", "Beginner")
        session.weekly_hours = float(weekly_hours or 5.0)

        await self.db.commit()

        return RoadmapServerEvent(
            event=RoadmapServerEventType.SESSION_READY,
            session_id=session.id,
            timestamp=datetime.now(timezone.utc).isoformat(),
            payload={
                "domain": domain,
                "greeting": greeting_text,
                "currentQuestion": first_q,
                "skillInventory": list(skill_inventory.values()),
                "completenessPercentage": 20,
            },
        )

    async def handle_submit_answer(
        self, user_id: str, session_id: str, payload: Dict[str, Any]
    ) -> RoadmapServerEvent:
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            raise ValueError("Assessment session not found or unauthorized")

        profile = dict(session.collected_profile or {})
        skill_inventory = profile.get("skill_inventory", {})
        asked_q_ids = list(profile.get("asked_question_ids", []))
        quiz_history = list(profile.get("quiz_history", []))
        pending_quiz = session.pending_quiz

        message_id = payload.get("message_id")
        answer_text = payload.get("answer_text", "")
        quiz_selected_index = payload.get("quiz_selected_index")

        # Idempotency check: if last turn already processed this message_id, return current state
        last_history = quiz_history[-1] if quiz_history else {}
        if last_history.get("message_id") == message_id:
            return RoadmapServerEvent(
                event=RoadmapServerEventType.ASSESSMENT_UPDATE,
                session_id=session_id,
                payload={
                    "skillInventory": list(skill_inventory.values()),
                    "feedback": last_history.get("feedback", ""),
                    "isReadyForGeneration": True,
                },
            )

        feedback_note = ""
        is_correct = False

        if pending_quiz:
            is_correct, feedback_note, updated_skill = self.assessment_engine.evaluate_response(
                domain=session.domain,
                question_data=pending_quiz,
                learner_answer=answer_text,
                quiz_selected_index=quiz_selected_index,
                skill_inventory=skill_inventory,
            )
            quiz_history.append({
                "message_id": message_id,
                "question_id": pending_quiz.get("question_id"),
                "is_correct": is_correct,
                "feedback": feedback_note,
            })

        # Check completeness
        is_ready, completeness_pct = self.assessment_engine.determine_completeness(
            skill_inventory=skill_inventory,
            asked_count=len(asked_q_ids),
            target_count=4,
        )

        next_q = None
        if not is_ready:
            next_q = self.assessment_engine.select_next_question(
                domain=session.domain,
                skill_inventory=skill_inventory,
                asked_question_ids=asked_q_ids,
                max_questions=4,
            )
            if next_q:
                asked_q_ids.append(next_q["question_id"])
            else:
                is_ready = True

        profile["skill_inventory"] = skill_inventory
        profile["asked_question_ids"] = asked_q_ids
        profile["quiz_history"] = quiz_history

        session.collected_profile = profile
        session.pending_quiz = next_q
        if is_ready:
            session.state = "READY_FOR_GENERATION"

        await self.db.commit()

        if is_ready:
            return RoadmapServerEvent(
                event=RoadmapServerEventType.SESSION_COMPLETE,
                session_id=session_id,
                timestamp=datetime.now(timezone.utc).isoformat(),
                payload={
                    "message": "Diagnostic assessment complete! Ready to generate your personalized roadmap.",
                    "skillInventory": list(skill_inventory.values()),
                    "feedback": feedback_note,
                    "completenessPercentage": 100,
                    "isReadyForGeneration": True,
                },
            )
        else:
            return RoadmapServerEvent(
                event=RoadmapServerEventType.NEXT_QUESTION,
                session_id=session_id,
                timestamp=datetime.now(timezone.utc).isoformat(),
                payload={
                    "feedback": feedback_note,
                    "nextQuestion": next_q,
                    "skillInventory": list(skill_inventory.values()),
                    "completenessPercentage": completeness_pct,
                },
            )

    async def handle_generate_roadmap(
        self, user_id: str, session_id: str
    ) -> RoadmapServerEvent:
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            raise ValueError("Assessment session not found or unauthorized")

        profile = dict(session.collected_profile or {})
        skill_inventory = profile.get("skill_inventory", {})

        roadmap_dict = self.planner.plan_roadmap(
            domain=session.domain,
            goal=session.goal or profile.get("goal", ""),
            target_level=session.target_level or profile.get("target_level", "Beginner"),
            weekly_hours=session.weekly_hours or profile.get("weekly_hours", 5.0),
            skill_inventory=skill_inventory,
        )

        roadmap_id = str(uuid.uuid4())
        created_roadmap = await self.roadmap_repo.create_personalized_roadmap(
            id=roadmap_id,
            user_id=user_id,
            title=roadmap_dict["title"],
            slug=f"{roadmap_dict['domain'].lower()}-{roadmap_id[:8]}",
            category=roadmap_dict["domain"],
            description=roadmap_dict["goal"],
            level=roadmap_dict["startingLevel"],
            duration=f"{roadmap_dict['estimatedWeeks']} weeks",
            icon="school",
            accent_theme="primary",
            structure=roadmap_dict,
            items=[],
        )

        session.roadmap_id = roadmap_id
        session.state = "COMPLETED"
        await self.db.commit()

        roadmap_detail = {
            "id": created_roadmap.id,
            "title": created_roadmap.title,
            "domain": getattr(created_roadmap, "domain", None) or roadmap_dict["domain"],
            "goal": getattr(created_roadmap, "goal", None) or roadmap_dict["goal"],
            "level": created_roadmap.level,
            "estimatedWeeks": roadmap_dict["estimatedWeeks"],
            "weeklyStudyHours": roadmap_dict["weeklyStudyHours"],
            "structure": roadmap_dict["structure"],
            "schedule": roadmap_dict["schedule"],
            "assessmentSummary": roadmap_dict["assessmentSummary"],
            "createdAt": created_roadmap.created_at.isoformat() if created_roadmap.created_at else "",
        }

        return RoadmapServerEvent(
            event=RoadmapServerEventType.ROADMAP_READY,
            session_id=session_id,
            timestamp=datetime.now(timezone.utc).isoformat(),
            payload={"roadmap": roadmap_detail},
        )

    async def handle_resume_session(
        self, user_id: str, session_id: str
    ) -> RoadmapServerEvent:
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            raise ValueError("Assessment session not found or unauthorized")

        profile = dict(session.collected_profile or {})
        skill_inventory = profile.get("skill_inventory", {})

        return RoadmapServerEvent(
            event=RoadmapServerEventType.SESSION_RESTORED,
            session_id=session_id,
            timestamp=datetime.now(timezone.utc).isoformat(),
            payload={
                "domain": session.domain,
                "state": session.state,
                "currentQuestion": session.pending_quiz,
                "skillInventory": list(skill_inventory.values()),
                "roadmapId": session.roadmap_id,
            },
        )
