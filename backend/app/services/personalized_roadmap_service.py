"""Service implementing dynamic multi-subject diagnostic assessment,
OpenRouter LLM synthesis of prerequisite DAG roadmaps, topic learning content, and adaptive progression.
"""

import asyncio
import math
import uuid
import logging
from datetime import datetime, timezone
from typing import Optional, List, Dict, Any, Tuple
from sqlalchemy import select, delete, update
from sqlalchemy.orm.attributes import flag_modified
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.personalized_roadmap import AssessmentSessionModel, PersonalizedRoadmapModel
from app.services.openrouter_service import openrouter_service

# In-memory dictionary for prefetching diagnostic questions between Q2 and Q3
_pending_diag_tasks: Dict[str, asyncio.Task] = {}
_synthesis_locks: Dict[str, asyncio.Lock] = {}
from app.schemas.personalized_roadmap import (
    AssessmentSessionResponse,
    AssessmentQuestionResponse,
    TopicMasteryStateResponse,
    CorrectionPromptResponse,
    PersonalizedRoadmapDetailResponse,
    PersonalizedRoadmapTopicResponse,
    PersonalizedRoadmapEdgeResponse,
    TopicLearningContentResponse,
    TopicCodeExampleResponse,
    TopicCommonMistakeResponse,
    TopicPracticeQuizResponse,
    TopicLearningResourceResponse,
    CompleteTopicResponse,
    ValidateSubjectResponse,
)

logger = logging.getLogger(__name__)


class PersonalizedRoadmapService:
    def __init__(self, db: AsyncSession):
        self.db = db

    # -----------------------------------------------------------------------
    # Subject Validation
    # -----------------------------------------------------------------------

    async def validate_subject(self, subject: str) -> ValidateSubjectResponse:
        """Validate if the course/skill/technology is supported and legitimate."""
        is_valid, std_name, message = await openrouter_service.validate_course_subject(subject)
        return ValidateSubjectResponse(
            is_valid=is_valid,
            standardized_name=std_name,
            message=message,
        )

    # -----------------------------------------------------------------------
    # Assessment Session Lifecycle
    # -----------------------------------------------------------------------

    async def start_assessment(
        self, domain: str = "Python", user_id: Optional[str] = None
    ) -> AssessmentSessionResponse:
        """Initialize a personalized diagnostic assessment for any subject/domain."""
        # Use fast local lookup only — the frontend already validated via /validate-subject
        # This avoids a redundant 5-15s LLM call on the free tier
        clean_domain = domain.strip() if domain else "Python"
        # Quick standardize from common subjects without LLM
        from app.services.openrouter_service import openrouter_service as _or_svc
        common = _or_svc.validate_course_subject.__func__  # type: ignore
        # Direct common_subjects lookup (no LLM)
        common_subjects = {
            "python": "Python", "java": "Java", "javascript": "JavaScript",
            "js": "JavaScript", "react": "React", "sql": "SQL", "dsa": "Data Structures & Algorithms",
            "machine learning": "Machine Learning", "ml": "Machine Learning",
            "c++": "C++", "cpp": "C++", "c#": "C#", "golang": "Go", "go": "Go",
            "rust": "Rust", "kotlin": "Kotlin", "swift": "Swift", "flutter": "Flutter",
            "docker": "Docker", "kubernetes": "Kubernetes", "devops": "DevOps",
            "typescript": "TypeScript", "ts": "TypeScript", "node.js": "Node.js",
            "nodejs": "Node.js", "fastapi": "FastAPI", "django": "Django",
            "android": "Android Development", "ios": "iOS Development",
            "html": "HTML & CSS", "css": "HTML & CSS", "web development": "Web Development",
            "data science": "Data Science", "deep learning": "Deep Learning",
            "ai": "Artificial Intelligence", "postgresql": "PostgreSQL", "mongodb": "MongoDB",
        }
        lower = clean_domain.lower()
        if lower in common_subjects:
            clean_domain = common_subjects[lower]
        else:
            clean_domain = clean_domain.title()

        session = AssessmentSessionModel(
            domain=clean_domain,
            user_id=user_id,
            phase="profile_collection",
            question_number=1,
            total_questions=7,  # 3 profile + 4 diagnostic
            current_question_data={
                "id": "prof_goal",
                "topic_tag": "profile_goal",
                "question_type": "multiple_choice",
                "prompt": f"What is your primary goal with {clean_domain}?",
                "options": [
                    f"Get a Developer / Software Engineering job in {clean_domain}",
                    f"Build production web apps, tools & services with {clean_domain}",
                    f"Learn {clean_domain} for Data Science, AI & Automation",
                    "Prepare for university exams / academic prep",
                ],
                "explanation": f"Your goal helps us tailor milestone priorities and real-world project themes for {clean_domain}.",
            },
            questions_history=[],
            mastery_map={},
        )
        self.db.add(session)
        await self.db.commit()
        return self._session_to_response(session)

    async def get_active_session(self, user_id: Optional[str] = None) -> Optional[AssessmentSessionResponse]:
        query = select(AssessmentSessionModel).where(
            AssessmentSessionModel.is_complete == False
        ).order_by(AssessmentSessionModel.created_at.desc())

        if user_id:
            query = query.where(AssessmentSessionModel.user_id == user_id)

        result = await self.db.execute(query)
        session = result.scalars().first()
        return self._session_to_response(session) if session else None

    async def get_session(self, session_id: str) -> Optional[AssessmentSessionResponse]:
        result = await self.db.execute(
            select(AssessmentSessionModel).where(AssessmentSessionModel.id == session_id)
        )
        session = result.scalar_one_or_none()
        return self._session_to_response(session) if session else None

    async def submit_answer(
        self, session_id: str, answer: str, quiz_selected_index: Optional[int]
    ) -> AssessmentSessionResponse:
        """Submit response for profile question or diagnostic test question."""
        result = await self.db.execute(
            select(AssessmentSessionModel).where(AssessmentSessionModel.id == session_id)
        )
        session = result.scalar_one_or_none()
        if not session:
            raise ValueError(f"Session {session_id} not found")

        curr_q = session.current_question_data or {}
        topic_tag = curr_q.get("topic_tag", "")
        mastery = dict(session.mastery_map or {})

        if session.phase == "profile_collection":
            # Profile Question 1: Goal
            if topic_tag == "profile_goal":
                session.profile_goal = answer
                session.question_number = 2
                session.current_question_data = {
                    "id": "prof_level",
                    "topic_tag": "profile_level",
                    "question_type": "multiple_choice",
                    "prompt": f"What best describes your current experience level in {session.domain}?",
                    "options": [
                        "Complete Beginner (no prior coding)",
                        "Some Experience (know basics/syntax)",
                        "Intermediate / Advanced (built apps)",
                    ],
                    "explanation": "We will independently verify this with diagnostic checks.",
                }
            # Profile Question 2: Level
            elif topic_tag == "profile_level":
                session.profile_level = answer
                session.question_number = 3
                session.current_question_data = {
                    "id": "prof_hours",
                    "topic_tag": "profile_study_hours",
                    "question_type": "multiple_choice",
                    "prompt": "How many hours per week can you study consistently?",
                    "options": ["3 hours / week", "6 hours / week", "10 hours / week", "15+ hours / week"],
                    "explanation": "Used to accurately estimate completion timeline in weeks.",
                }
                # Background prefetch diagnostic questions while learner reads & answers Q3
                try:
                    task = asyncio.create_task(
                        openrouter_service.generate_diagnostic_questions(
                            domain=session.domain,
                            goal=session.profile_goal,
                            level=session.profile_level,
                            count=4,
                        )
                    )
                    _pending_diag_tasks[session.id] = task
                    logger.info(f"Prefetching diagnostic questions in background for session {session.id}")
                except Exception as e:
                    logger.warning(f"Failed to kick off background diag prefetch: {e}")

            # Profile Question 3: Study hours -> Transition to dynamic diagnostic
            elif topic_tag == "profile_study_hours":
                hours = 5
                if "3" in answer:
                    hours = 3
                elif "6" in answer:
                    hours = 6
                elif "10" in answer:
                    hours = 10
                elif "15" in answer:
                    hours = 15
                session.profile_study_hours_per_week = hours
                session.phase = "diagnostic"
                session.question_number = 4

                # Requirement 1: Generate diagnostic question set ONCE and persist. Never regenerate.
                diag_questions = session.questions_history or []
                if not diag_questions and session.id in _pending_diag_tasks:
                    try:
                        logger.info(f"Awaiting prefetched diagnostic questions for session {session.id}")
                        diag_questions = await _pending_diag_tasks.pop(session.id)
                    except Exception as e:
                        logger.warning(f"Prefetched diag task failed: {e}")

                if not diag_questions:
                    diag_questions = await openrouter_service.generate_diagnostic_questions(
                        domain=session.domain,
                        goal=session.profile_goal,
                        level=session.profile_level,
                        count=4,
                    )
                session.questions_history = diag_questions
                session.total_questions = 3 + len(diag_questions)
                session.current_question_data = diag_questions[0] if diag_questions else None

        elif session.phase == "diagnostic":
            # Grade answer
            correct_idx = curr_q.get("correct_option_index")
            is_correct = (quiz_selected_index == correct_idx) if correct_idx is not None else False
            session.last_answer_correct = is_correct
            session.last_answer_explanation = curr_q.get("explanation", "")

            # Update topic mastery state
            tag = curr_q.get("topic_tag", f"{session.domain.lower()}_core")
            state = mastery.get(
                tag,
                {"topic_tag": tag, "mastery_level": "unknown", "evidence_count": 0, "correct_count": 0},
            )
            ev = state.get("evidence_count", 0) + 1
            corr = state.get("correct_count", 0) + (1 if is_correct else 0)

            if corr >= 2:
                lvl = "strong"
            elif corr >= 1:
                lvl = "adequate"
            else:
                lvl = "weak"

            mastery[tag] = {
                "topic_tag": tag,
                "mastery_level": lvl,
                "evidence_count": ev,
                "correct_count": corr,
            }
            session.mastery_map = mastery

            # Adaptive Next Question selection
            curr_diag_idx = session.question_number - 4  # 0-indexed index within diagnostic list
            diag_list = session.questions_history or []
            next_diag_idx = curr_diag_idx + 1

            if next_diag_idx < len(diag_list):
                session.question_number += 1
                session.current_question_data = diag_list[next_diag_idx]
            else:
                # Diagnostic Complete -> Check consistency & transition to summary
                session.phase = "summary"
                session.is_complete = True
                session.current_question_data = None

                # Inconsistency check: self-reported Advanced, but diagnostic shows foundational weaknesses
                weak_count = sum(1 for m in mastery.values() if m.get("mastery_level") == "weak")
                if "advanced" in session.profile_level.lower() and weak_count >= 2:
                    session.correction_prompt = {
                        "field": "level",
                        "current_value": session.profile_level,
                        "suggested_value": "Intermediate",
                        "reason": f"Diagnostic evidence revealed foundational gaps in {weak_count} core areas of {session.domain}. Starting at Intermediate ensures solid mastery.",
                    }

                # Auto-synthesize roadmap DAG right now so it's already ready!
                try:
                    roadmap_resp = await self._synthesize_and_save_roadmap(session)
                    session.generated_roadmap_id = roadmap_resp.id
                    logger.info(f"Auto-generated roadmap {roadmap_resp.id} for session {session.id}")
                except Exception as e:
                    logger.warning(f"Inline roadmap generation failed: {e}")

        flag_modified(session, "mastery_map")
        flag_modified(session, "questions_history")
        await self.db.commit()
        return self._session_to_response(session)

    async def correct_profile_field(self, session_id: str, field: str, value: str) -> AssessmentSessionResponse:
        result = await self.db.execute(
            select(AssessmentSessionModel).where(AssessmentSessionModel.id == session_id)
        )
        session = result.scalar_one_or_none()
        if not session:
            raise ValueError(f"Session {session_id} not found")

        if field == "level":
            session.profile_level = value
        elif field == "goal":
            session.profile_goal = value
        elif field == "study_hours":
            session.profile_study_hours_per_week = int(value)

        session.correction_prompt = None
        await self.db.commit()
        return self._session_to_response(session)

    # -----------------------------------------------------------------------
    # Personalized Roadmap Generation & Persistence
    # -----------------------------------------------------------------------

    async def _synthesize_and_save_roadmap(self, session: AssessmentSessionModel) -> PersonalizedRoadmapDetailResponse:
        """Helper to synthesize and persist a personalized roadmap DAG for a session with idempotency and concurrency locking."""
        if session.id not in _synthesis_locks:
            _synthesis_locks[session.id] = asyncio.Lock()

        async with _synthesis_locks[session.id]:
            # Double check if generated while waiting for lock
            if session.generated_roadmap_id:
                existing = await self.get_roadmap(session.generated_roadmap_id)
                if existing:
                    return existing

            mastery_map = session.mastery_map or {}
            weak_topics = [k for k, v in mastery_map.items() if v.get("mastery_level") == "weak"]
            strong_topics = [k for k, v in mastery_map.items() if v.get("mastery_level") == "strong"]

            # Construct compact assessment summary (Requirement 4)
            compact_summary = {
                "domain": session.domain,
                "learning_goal": session.profile_goal or f"Master {session.domain}",
                "experience_level": session.profile_level or "Beginner",
                "study_hours_per_week": session.profile_study_hours_per_week or 5,
                "weak_areas": weak_topics,
                "strong_areas": strong_topics,
                "diagnostic_evidence": mastery_map,
                "total_questions_evaluated": len(session.questions_history or []),
            }

            dag = await openrouter_service.generate_personalized_roadmap_dag(
                domain=session.domain,
                goal=session.profile_goal,
                level=session.profile_level,
                study_hours_per_week=session.profile_study_hours_per_week,
                mastery_map=mastery_map,
                compact_summary=compact_summary,
            )

            roadmap_id = str(uuid.uuid4())
            topics_data = dag.get("topics", [])
            edges_data = dag.get("edges", [])
            title = dag.get("title", f"Personalized {session.domain} Roadmap: {session.profile_goal}")
            est_weeks = dag.get("estimated_weeks", 8)

            completed_ids = [t["id"] for t in topics_data if t.get("status") in ("completed", "skipped")]

            roadmap = PersonalizedRoadmapModel(
                id=roadmap_id,
                user_id=session.user_id,
                title=title,
                domain=session.domain,
                goal=session.profile_goal or f"Master {session.domain}",
                level=session.profile_level or "Beginner",
                study_hours_per_week=session.profile_study_hours_per_week,
                estimated_weeks=est_weeks,
                topics=topics_data,
                edges=edges_data,
                assessment_session_id=session.id,
                completed_topic_ids=completed_ids,
                in_progress_topic_ids=[],
            )
            self.db.add(roadmap)
            session.generated_roadmap_id = roadmap.id
            session.phase = "roadmap_ready"
            await self.db.commit()
            return self._roadmap_to_response(roadmap)

    async def generate_personalized_roadmap(self, session_id: str) -> PersonalizedRoadmapDetailResponse:
        """Synthesize a personalized DAG roadmap or return already generated one."""
        result = await self.db.execute(
            select(AssessmentSessionModel).where(AssessmentSessionModel.id == session_id)
        )
        session = result.scalar_one_or_none()
        if not session:
            raise ValueError(f"Session {session_id} not found")

        # If already generated for this session, return it immediately without calling LLM again!
        if session.generated_roadmap_id:
            try:
                existing = await self.get_roadmap(session.generated_roadmap_id)
                if existing:
                    logger.info(f"Returning cached/pregenerated roadmap {existing.id} for session {session_id}")
                    return existing
            except Exception as e:
                logger.warning(f"Error fetching existing roadmap {session.generated_roadmap_id}: {e}")

        # Synthesize roadmap with concurrency safety
        return await self._synthesize_and_save_roadmap(session)

    # -----------------------------------------------------------------------
    # Roadmap Retrieval & Management
    # -----------------------------------------------------------------------

    async def get_roadmaps(
        self, limit: Optional[int] = None, offset: int = 0, user_id: Optional[str] = None
    ) -> List[PersonalizedRoadmapDetailResponse]:
        query = select(PersonalizedRoadmapModel).order_by(PersonalizedRoadmapModel.created_at.desc()).offset(offset)
        if limit is not None:
            query = query.limit(limit)
        if user_id:
            query = query.where(PersonalizedRoadmapModel.user_id == user_id)

        result = await self.db.execute(query)
        roadmaps = result.scalars().all()
        return [self._roadmap_to_response(r) for r in roadmaps]

    async def get_roadmap(self, roadmap_id: str) -> Optional[PersonalizedRoadmapDetailResponse]:
        result = await self.db.execute(
            select(PersonalizedRoadmapModel).where(PersonalizedRoadmapModel.id == roadmap_id)
        )
        roadmap = result.scalar_one_or_none()
        return self._roadmap_to_response(roadmap) if roadmap else None

    async def save_roadmap(self, roadmap_data: Dict[str, Any]) -> PersonalizedRoadmapDetailResponse:
        roadmap_id = roadmap_data.get("id") or str(uuid.uuid4())
        result = await self.db.execute(
            select(PersonalizedRoadmapModel).where(PersonalizedRoadmapModel.id == roadmap_id)
        )
        roadmap = result.scalar_one_or_none()

        if roadmap:
            roadmap.title = roadmap_data.get("title", roadmap.title)
            roadmap.goal = roadmap_data.get("goal", roadmap.goal)
            roadmap.level = roadmap_data.get("level", roadmap.level)
            roadmap.study_hours_per_week = roadmap_data.get("study_hours_per_week", roadmap.study_hours_per_week)
            roadmap.estimated_weeks = roadmap_data.get("estimated_weeks", roadmap.estimated_weeks)
            roadmap.topics = roadmap_data.get("topics", roadmap.topics)
            roadmap.edges = roadmap_data.get("edges", roadmap.edges)
            roadmap.completed_topic_ids = roadmap_data.get("completed_topic_ids", roadmap.completed_topic_ids)
            roadmap.in_progress_topic_ids = roadmap_data.get("in_progress_topic_ids", roadmap.in_progress_topic_ids)
            flag_modified(roadmap, "topics")
            flag_modified(roadmap, "edges")
            flag_modified(roadmap, "completed_topic_ids")
        else:
            roadmap = PersonalizedRoadmapModel(
                id=roadmap_id,
                title=roadmap_data.get("title", "My Personalized Roadmap"),
                domain=roadmap_data.get("domain", "Python"),
                goal=roadmap_data.get("goal", ""),
                level=roadmap_data.get("level", "Beginner"),
                study_hours_per_week=roadmap_data.get("study_hours_per_week", 5),
                estimated_weeks=roadmap_data.get("estimated_weeks", 8),
                topics=roadmap_data.get("topics", []),
                edges=roadmap_data.get("edges", []),
                assessment_session_id=roadmap_data.get("assessment_session_id", ""),
                completed_topic_ids=roadmap_data.get("completed_topic_ids", []),
                in_progress_topic_ids=roadmap_data.get("in_progress_topic_ids", []),
            )
            self.db.add(roadmap)

        await self.db.commit()
        await self.db.refresh(roadmap)
        return self._roadmap_to_response(roadmap)

    async def delete_roadmap(self, roadmap_id: str) -> bool:
        result = await self.db.execute(
            delete(PersonalizedRoadmapModel).where(PersonalizedRoadmapModel.id == roadmap_id)
        )
        await self.db.commit()
        return result.rowcount > 0

    async def rename_roadmap(self, roadmap_id: str, new_title: str) -> Optional[PersonalizedRoadmapDetailResponse]:
        result = await self.db.execute(
            select(PersonalizedRoadmapModel).where(PersonalizedRoadmapModel.id == roadmap_id)
        )
        roadmap = result.scalar_one_or_none()
        if not roadmap:
            return None
        roadmap.title = new_title
        await self.db.commit()
        await self.db.refresh(roadmap)
        return self._roadmap_to_response(roadmap)

    # -----------------------------------------------------------------------
    # Dedicated Topic Learning Content
    # -----------------------------------------------------------------------

    async def get_topic_learning_content(
        self, roadmap_id: str, topic_id: str
    ) -> Optional[TopicLearningContentResponse]:
        """Fetch or synthesize rich topic learning module with code examples, mistakes, and verified resources."""
        result = await self.db.execute(
            select(PersonalizedRoadmapModel).where(PersonalizedRoadmapModel.id == roadmap_id)
        )
        roadmap = result.scalar_one_or_none()
        if not roadmap:
            return None

        # Find topic
        topic_match = None
        for t in roadmap.topics or []:
            if t.get("id") == topic_id:
                topic_match = t
                break

        if not topic_match:
            return None

        # Check if topic already has cached learning content (Requirement 6: lesson caching)
        cached_content = topic_match.get("learning_content")
        if cached_content:
            data = cached_content
        else:
            # Generate topic content via OpenRouter only on first open
            data = await openrouter_service.generate_topic_learning_content(
                domain=roadmap.domain,
                topic_id=topic_id,
                topic_title=topic_match.get("title", "Topic Overview"),
                topic_description=topic_match.get("short_description", ""),
                level=roadmap.level,
            )
            topic_match["learning_content"] = data
            flag_modified(roadmap, "topics")
            await self.db.commit()

        code_examples = [
            TopicCodeExampleResponse(
                title=ex.get("title", "Code Example"),
                code=ex.get("code", ""),
                explanation=ex.get("explanation", ""),
                expected_output=ex.get("expected_output", ""),
            )
            for ex in data.get("code_examples", [])
        ]

        mistakes = [
            TopicCommonMistakeResponse(
                mistake=m.get("mistake", ""),
                why_it_happens=m.get("why_it_happens", ""),
                how_to_fix=m.get("how_to_fix", ""),
            )
            for m in data.get("common_mistakes", [])
        ]

        practice_quiz = None
        if "practice_quiz" in data and data["practice_quiz"]:
            pq = data["practice_quiz"]
            practice_quiz = TopicPracticeQuizResponse(
                question=pq.get("question", ""),
                options=pq.get("options", []),
                correct_option_index=pq.get("correct_option_index", 0),
                explanation=pq.get("explanation", ""),
            )

        resources = [
            TopicLearningResourceResponse(
                title=r.get("title", ""),
                url=r.get("url", ""),
                source_name=r.get("source_name", "Documentation"),
                description=r.get("description", ""),
                is_verified=r.get("is_verified", True),
            )
            for r in data.get("resources", [])
        ]

        return TopicLearningContentResponse(
            topic_id=topic_id,
            topic_title=data.get("topic_title", topic_match.get("title", "")),
            domain=roadmap.domain,
            learning_objectives=data.get("learning_objectives", []),
            beginner_explanation=data.get("beginner_explanation", ""),
            syntax_and_concepts=data.get("syntax_and_concepts", ""),
            code_examples=code_examples,
            common_mistakes=mistakes,
            practice_quiz=practice_quiz,
            resources=resources,
        )

    # -----------------------------------------------------------------------
    # Adaptive Topic Completion & Progression
    # -----------------------------------------------------------------------

    async def complete_topic(
        self, roadmap_id: str, topic_id: str, quiz_passed: bool = True
    ) -> Optional[CompleteTopicResponse]:
        """Mark a topic completed, dynamically re-evaluate DAG prerequisites, unlock eligible topics,
        and calculate the next recommended milestone.
        """
        result = await self.db.execute(
            select(PersonalizedRoadmapModel).where(PersonalizedRoadmapModel.id == roadmap_id)
        )
        roadmap = result.scalar_one_or_none()
        if not roadmap:
            return None

        topics = list(roadmap.topics or [])
        completed_set = set(roadmap.completed_topic_ids or [])
        completed_set.add(topic_id)

        # Mark targeted topic completed
        for t in topics:
            if t.get("id") == topic_id:
                t["status"] = "completed"
                t["mastery_level"] = "strong" if quiz_passed else "adequate"

        # Re-evaluate prerequisites across all topics in DAG
        unlocked_topic_ids: List[str] = []
        for t in topics:
            t_id = t.get("id")
            if t_id in completed_set:
                t["status"] = "completed"
                continue

            prereqs = t.get("prerequisite_topic_ids", [])
            all_satisfied = all(p in completed_set for p in prereqs)
            if all_satisfied:
                if t.get("status") == "locked":
                    unlocked_topic_ids.append(t_id)
                t["status"] = "unlocked"
            else:
                t["status"] = "locked"

        # Next recommended topic: highest priority score among currently unlocked, incomplete topics
        next_topic_id = None
        best_priority = -1
        for t in topics:
            if t.get("status") == "unlocked" and t.get("id") not in completed_set:
                p_score = t.get("priority_score", 0)
                if p_score > best_priority:
                    best_priority = p_score
                    next_topic_id = t.get("id")

        total_topics = len(topics)
        progress_pct = int(len(completed_set) / max(1, total_topics) * 100)

        roadmap.topics = topics
        roadmap.completed_topic_ids = list(completed_set)
        flag_modified(roadmap, "topics")
        flag_modified(roadmap, "completed_topic_ids")

        await self.db.commit()
        await self.db.refresh(roadmap)

        return CompleteTopicResponse(
            roadmap_id=roadmap.id,
            completed_topic_id=topic_id,
            unlocked_topic_ids=unlocked_topic_ids,
            next_recommended_topic_id=next_topic_id,
            overall_progress_percent=progress_pct,
            roadmap=self._roadmap_to_response(roadmap),
        )

    # -----------------------------------------------------------------------
    # Serializers
    # -----------------------------------------------------------------------

    def _session_to_response(self, session: AssessmentSessionModel) -> AssessmentSessionResponse:
        curr_q = None
        if session.current_question_data:
            q = session.current_question_data
            curr_q = AssessmentQuestionResponse(
                id=q.get("id", ""),
                topic_tag=q.get("topic_tag", ""),
                question_type=q.get("question_type", "multiple_choice"),
                prompt=q.get("prompt", ""),
                code_snippet=q.get("code_snippet"),
                options=q.get("options", []),
                correct_option_index=q.get("correct_option_index"),
                difficulty_level=q.get("difficulty_level", 1),
                explanation=q.get("explanation", ""),
            )

        correction = None
        if session.correction_prompt:
            cp = session.correction_prompt
            correction = CorrectionPromptResponse(
                field=cp.get("field", ""),
                current_value=cp.get("current_value", ""),
                suggested_value=cp.get("suggested_value", ""),
                reason=cp.get("reason", ""),
            )

        mastery_dict = {}
        for k, v in (session.mastery_map or {}).items():
            mastery_dict[k] = TopicMasteryStateResponse(
                topic_tag=v.get("topic_tag", k),
                mastery_level=v.get("mastery_level", "unknown"),
                evidence_count=v.get("evidence_count", 0),
                correct_count=v.get("correct_count", 0),
            )

        return AssessmentSessionResponse(
            session_id=session.id,
            domain=session.domain,
            phase=session.phase,
            profile_goal=session.profile_goal,
            profile_level=session.profile_level,
            profile_study_hours_per_week=session.profile_study_hours_per_week,
            profile_timeline_weeks=session.profile_timeline_weeks,
            current_question=curr_q,
            question_number=session.question_number,
            total_questions=session.total_questions,
            mastery_map=mastery_dict,
            is_complete=session.is_complete,
            generated_roadmap_id=session.generated_roadmap_id,
            last_answer_correct=session.last_answer_correct,
            last_answer_explanation=session.last_answer_explanation,
            correction_prompt=correction,
            error_message=None,
        )

    def _roadmap_to_response(self, roadmap: PersonalizedRoadmapModel) -> PersonalizedRoadmapDetailResponse:
        topics_resp = []
        for t in roadmap.topics or []:
            topics_resp.append(
                PersonalizedRoadmapTopicResponse(
                    id=t.get("id", ""),
                    topic_tag=t.get("topic_tag", ""),
                    title=t.get("title", ""),
                    short_description=t.get("short_description", ""),
                    estimated_minutes=t.get("estimated_minutes", 20),
                    prerequisite_topic_ids=t.get("prerequisite_topic_ids", []),
                    status=t.get("status", "locked"),
                    mastery_level=t.get("mastery_level", "unknown"),
                    priority_score=t.get("priority_score", 0),
                    accelerated=t.get("accelerated", False),
                    explanation=t.get("explanation", ""),
                )
            )

        edges_resp = []
        for e in roadmap.edges or []:
            edges_resp.append(
                PersonalizedRoadmapEdgeResponse(
                    from_topic_id=e.get("from_topic_id", ""),
                    to_topic_id=e.get("to_topic_id", ""),
                )
            )

        return PersonalizedRoadmapDetailResponse(
            id=roadmap.id,
            title=roadmap.title,
            domain=roadmap.domain,
            goal=roadmap.goal,
            level=roadmap.level,
            study_hours_per_week=roadmap.study_hours_per_week,
            estimated_weeks=roadmap.estimated_weeks,
            topics=topics_resp,
            edges=edges_resp,
            assessment_session_id=roadmap.assessment_session_id,
            created_at=roadmap.created_at.isoformat() if roadmap.created_at else "",
            updated_at=roadmap.updated_at.isoformat() if roadmap.updated_at else "",
            completed_topic_ids=roadmap.completed_topic_ids or [],
            in_progress_topic_ids=roadmap.in_progress_topic_ids or [],
        )
