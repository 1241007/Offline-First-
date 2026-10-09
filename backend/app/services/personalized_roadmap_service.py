"""Personalized Roadmap Assessment and Generation Service"""

import json
import logging
import re
import uuid
from typing import Optional, List, Dict, Any, Tuple
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.config import settings
from app.models import (
    RoadmapAssessmentSession,
    Roadmap,
    RoadmapItem,
    Course,
    CourseCategory,
    UserProfile,
    UserCourseProgress,
    UserLessonProgress,
    UserMemory,
)
from app.repositories.roadmap_assessment_repository import RoadmapAssessmentRepository
from app.repositories.roadmap_repository import RoadmapRepository
from app.repositories.course_repository import CourseRepository
from app.repositories.profile_repository import ProfileRepository
from app.repositories.learning_repository import LearningRepository
from app.repositories.memory_repository import MemoryRepository
from app.schemas.personalized_roadmap import (
    AssessmentQuizPublicDto,
    AssessmentMessageItem,
    AssessmentSessionResponse,
    PersonalizedRoadmapSchema,
    PersonalizedRoadmapDetailResponse,
)
from app.schemas.roadmap import RoadmapItemResponse
from app.services.openrouter_service import openrouter_service

logger = logging.getLogger(__name__)

_ROADMAP_MODEL = getattr(settings, "openrouter_roadmap_model", "anthropic/claude-sonnet-4.5")


def _extract_json_from_response(raw_text: str) -> Optional[dict]:
    """Extract JSON object from model response text even if surrounded by markdown fences."""
    text = (raw_text or "").strip()
    if not text:
        return None

    # Strip markdown fences if present
    if text.startswith("```"):
        lines = text.splitlines()
        if lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        text = "\n".join(lines).strip()

    try:
        return json.loads(text)
    except Exception:
        pass

    # Try finding the outermost JSON object { ... }
    match = re.search(r"(\{.*\})", text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(1))
        except Exception:
            pass

    return None


class PersonalizedRoadmapService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.assessment_repo = RoadmapAssessmentRepository(db)
        self.roadmap_repo = RoadmapRepository(db)
        self.course_repo = CourseRepository(db)
        self.profile_repo = ProfileRepository(db)
        self.learning_repo = LearningRepository(db)
        self.memory_repo = MemoryRepository(db)

    async def _gather_learner_context(self, user_id: str) -> Dict[str, Any]:
        """Gathers authentic verified records for the authenticated learner."""
        profile = await self.profile_repo.get_by_user_id(user_id)
        in_progress_courses = await self.learning_repo.get_user_course_progress(
            user_id=user_id, status="in_progress"
        )
        completed_courses = await self.learning_repo.get_user_course_progress(
            user_id=user_id, status="completed"
        )
        memories = await self.memory_repo.list_memories(user_id=user_id, active_only=True)

        # Count completed lessons
        lesson_query = select(UserLessonProgress).where(
            UserLessonProgress.user_id == user_id,
            UserLessonProgress.completed == True,
        )
        lesson_result = await self.db.execute(lesson_query)
        completed_lessons_count = len(list(lesson_result.scalars().all()))

        verified_evidence = []
        for c in completed_courses:
            c_name = c.course.name if c.course else "Course"
            verified_evidence.append(f"Completed course: {c_name} (100% progress)")
        for ip in in_progress_courses:
            c_name = ip.course.name if ip.course else "Course"
            pct = int(ip.completion_percentage)
            verified_evidence.append(f"In-progress course: {c_name} ({pct}% complete)")
        if completed_lessons_count > 0:
            verified_evidence.append(f"Completed {completed_lessons_count} individual lesson(s)")

        learner_info = {
            "name": profile.full_name if profile else "Learner",
            "education_mode": profile.education_mode if profile else "general",
            "stated_level": profile.level if profile else "Beginner",
            "interests": profile.interests if profile else "",
            "verified_evidence": verified_evidence,
            "memories": [f"{m.category}: {m.content}" for m in memories],
        }
        return learner_info

    async def _get_catalog_summary(self) -> List[Dict[str, Any]]:
        """Retrieves real EduNova catalog summary to prevent hallucinating courses."""
        courses = await self.course_repo.get_all_courses(limit=50)
        return [
            {
                "id": c.id,
                "name": c.name,
                "level": c.level,
                "description": c.description[:120] if c.description else "",
            }
            for c in courses
        ]

    async def start_assessment_session(self, user_id: str) -> AssessmentSessionResponse:
        """Starts a new assessment session and generates the mentor's greeting and first question."""
        context = await self._gather_learner_context(user_id)
        catalog = await self._get_catalog_summary()

        initial_profile = {
            "verified_evidence": context["verified_evidence"],
            "self_reported_information": [],
            "strengths": [],
            "skill_gaps": [],
            "unknowns": ["primary_goal", "target_timeline", "weekly_hours", "specific_skills_to_master"],
            "education_mode": context["education_mode"],
            "stated_level": context["stated_level"],
            "interests": context["interests"],
            "memories": context["memories"],
        }

        # Prompt AI to craft an initial welcoming message acknowledging verified background if present
        system_prompt = (
            "You are EduNova's AI Learning Advisor. Your goal is to guide learners to a tailored roadmap.\n"
            "You ask ONE focused question at a time. Do not overwhelm the learner.\n"
            "Acknowledge the learner's existing verified progress naturally if any is available.\n"
            "You must return ONLY valid JSON in this exact structure:\n"
            "{\n"
            '  "mentor_message": "Warm, personalized greeting + first question about their primary learning/career goal.",\n'
            '  "quick_options": ["Option 1", "Option 2", "Option 3", "Option 4"],\n'
            '  "assessment_state": "COLLECTING_GOALS",\n'
            '  "completeness_percentage": 15\n'
            "}"
        )

        user_prompt = (
            f"Learner Context:\n"
            f"- Name: {context['name']}\n"
            f"- Mode: {context['education_mode']}\n"
            f"- Level: {context['stated_level']}\n"
            f"- Interests: {context['interests']}\n"
            f"- Verified Progress:\n" + "\n".join(f"  * {e}" for e in context["verified_evidence"]) + "\n"
            f"- Known Memories:\n" + "\n".join(f"  * {m}" for m in context["memories"]) + "\n\n"
            "Initiate the assessment session with a concise, warm message and ask the first question."
        )

        mentor_text = f"Hi {context['name']}! I'm your EduNova learning mentor. I'll help you build a personalized roadmap tailored to your goals and pace. What is your primary learning or career goal?"
        quick_opts = ["Land a Software Developer Job", "Master Android & AI", "Prepare for Technical Exams", "Build Full-Stack Projects"]
        state = "COLLECTING_GOALS"
        completeness = 15

        try:
            raw_resp = await openrouter_service.generate_response(
                history=[{"role": "user", "content": user_prompt}],
                explanation_mode="general",
                memory_context=system_prompt,
            )
            parsed = _extract_json_from_response(raw_resp)
            if parsed and "mentor_message" in parsed:
                mentor_text = parsed["mentor_message"]
                quick_opts = parsed.get("quick_options", quick_opts)
                state = parsed.get("assessment_state", state)
                completeness = parsed.get("completeness_percentage", completeness)
        except Exception as e:
            logger.warning(f"Failed AI initial generation, using robust fallback greeting: {e}")

        initial_messages = [
            {
                "sender": "mentor",
                "text": mentor_text,
                "options": quick_opts,
                "quiz": None,
                "timestamp": None,
            }
        ]

        session = await self.assessment_repo.create_session(
            user_id=user_id,
            state=state,
            collected_profile=initial_profile,
            messages=initial_messages,
            pending_quiz=None,
        )
        await self.db.commit()

        return self._format_session_response(session)

    async def get_active_session(self, user_id: str) -> Optional[AssessmentSessionResponse]:
        session = await self.assessment_repo.get_active_session_for_user(user_id)
        if not session:
            return None
        return self._format_session_response(session)

    async def get_session_by_id(
        self, session_id: str, user_id: str
    ) -> Optional[AssessmentSessionResponse]:
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            return None
        return self._format_session_response(session)

    async def process_learner_response(
        self,
        user_id: str,
        session_id: str,
        answer: str,
        quiz_selected_index: Optional[int] = None,
    ) -> AssessmentSessionResponse:
        """Processes learner response, scores diagnostic quizzes, and generates the next step."""
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            raise ValueError("Assessment session not found or unauthorized")

        if session.state in ("COMPLETED", "ERROR"):
            raise ValueError(f"Session is in {session.state} state and cannot accept further messages")

        collected_profile = session.collected_profile or {}
        messages = list(session.messages or [])
        pending_quiz = session.pending_quiz

        # 1. Record user message in history
        user_turn_text = answer.strip()
        quiz_feedback_note = ""

        # 2. Evaluate pending quiz if one was answered
        if pending_quiz is not None and quiz_selected_index is not None:
            correct_idx = pending_quiz.get("correct_index", 0)
            is_correct = quiz_selected_index == correct_idx
            skill = pending_quiz.get("skill_tested", "Skill check")
            explanation = pending_quiz.get("explanation", "")

            if is_correct:
                quiz_feedback_note = f"[QUIZ RESULT: CORRECT for '{skill}'. Explanation: {explanation}]"
                collected_profile.setdefault("strengths", []).append(f"Demonstrated proficiency in {skill}")
            else:
                quiz_feedback_note = f"[QUIZ RESULT: INCORRECT for '{skill}'. Chosen option #{quiz_selected_index + 1}, correct was #{correct_idx + 1}. Explanation: {explanation}]"
                collected_profile.setdefault("skill_gaps", []).append(f"Needs foundation in {skill}")

            # Clear pending quiz once answered
            pending_quiz = None
        else:
            # Self-reported answer
            collected_profile.setdefault("self_reported_information", []).append(user_turn_text)

        messages.append({
            "sender": "learner",
            "text": user_turn_text,
            "options": [],
            "quiz": None,
            "timestamp": None,
        })

        # 3. Prompt OpenRouter Claude Sonnet to evaluate and choose next action
        system_prompt = (
            "You are EduNova's AI Learning Advisor managing an interactive assessment conversation.\n"
            "Rules:\n"
            "1. Ask ONE question at a time.\n"
            "2. If the user answered a quiz, acknowledge their result with encouragement and clear explanation.\n"
            "3. If their skill level on a core prerequisite is unclear or they express uncertainty, create a diagnostic multiple-choice quiz (4 options) to test them.\n"
            "4. Progress through states: COLLECTING_GOALS -> COLLECTING_PROGRESS -> ASSESSING_SKILLS -> CLARIFYING_GAPS -> READY_FOR_GENERATION.\n"
            "5. When you have collected Goal, Proficiency Level, Strengths/Gaps, and Weekly Study Hours (or have enough context to make reasonable assumptions), set assessment_state to 'READY_FOR_GENERATION'.\n"
            "6. You must return ONLY valid JSON in this exact schema:\n"
            "{\n"
            '  "mentor_message": "Your response to the learner + the single next question or encouragement.",\n'
            '  "quick_options": ["Option 1", "Option 2", "Option 3"],\n'
            '  "diagnostic_quiz": null or {\n'
            '    "question": "Diagnostic question text",\n'
            '    "options": ["A", "B", "C", "D"],\n'
            '    "correct_index": 0,\n'
            '    "explanation": "Why option A is correct",\n'
            '    "skill_tested": "Core concept"\n'
            '  },\n'
            '  "extracted_data": {\n'
            '    "goal": "...",\n'
            '    "target_level": "...",\n'
            '    "weekly_hours": 5.0,\n'
            '    "strengths": ["..."],\n'
            '    "skill_gaps": ["..."],\n'
            '    "unknowns": ["..."]\n'
            '  },\n'
            '  "assessment_state": "COLLECTING_GOALS | COLLECTING_PROGRESS | ASSESSING_SKILLS | CLARIFYING_GAPS | READY_FOR_GENERATION",\n'
            '  "completeness_percentage": 60\n'
            "}"
        )

        history_payload = []
        for m in messages[-8:]:
            role = "assistant" if m["sender"] == "mentor" else "user"
            history_payload.append({"role": role, "content": m["text"]})

        if quiz_feedback_note:
            history_payload.append({"role": "system", "content": quiz_feedback_note})

        user_context_info = f"Gathered Profile so far: {json.dumps(collected_profile)}"
        history_payload.append({"role": "user", "content": f"User replied: '{user_turn_text}'. Current context: {user_context_info}. Produce the next advisor response."})

        # Default fallback values in case of transient model issue
        next_mentor_text = "Got it! Could you also share how many hours per week you can dedicate to studying?"
        next_options = ["2-4 hours/week", "5-10 hours/week", "15+ hours/week"]
        next_state = "ASSESSING_SKILLS"
        completeness = min(90, int(len(messages) * 15))
        new_pending_quiz = None

        try:
            raw_resp = await openrouter_service.generate_response(
                history=history_payload,
                explanation_mode="general",
                memory_context=system_prompt,
            )
            parsed = _extract_json_from_response(raw_resp)
            if parsed and "mentor_message" in parsed:
                next_mentor_text = parsed["mentor_message"]
                next_options = parsed.get("quick_options", next_options)
                next_state = parsed.get("assessment_state", next_state)
                completeness = parsed.get("completeness_percentage", completeness)

                # Check extracted data
                extracted = parsed.get("extracted_data", {})
                if extracted.get("goal"):
                    session.goal = extracted["goal"]
                if extracted.get("target_level"):
                    session.target_level = extracted["target_level"]
                if extracted.get("weekly_hours"):
                    session.weekly_hours = float(extracted["weekly_hours"])
                if extracted.get("strengths"):
                    collected_profile.setdefault("strengths", []).extend(extracted["strengths"])
                if extracted.get("skill_gaps"):
                    collected_profile.setdefault("skill_gaps", []).extend(extracted["skill_gaps"])
                if extracted.get("unknowns"):
                    collected_profile["unknowns"] = extracted["unknowns"]

                # Process diagnostic quiz if AI generated one
                diag_quiz = parsed.get("diagnostic_quiz")
                if diag_quiz and isinstance(diag_quiz, dict) and "question" in diag_quiz and "options" in diag_quiz:
                    # Save answer key on backend
                    new_pending_quiz = {
                        "question": diag_quiz["question"],
                        "options": diag_quiz["options"],
                        "correct_index": diag_quiz.get("correct_index", 0),
                        "explanation": diag_quiz.get("explanation", ""),
                        "skill_tested": diag_quiz.get("skill_tested", "Concept check"),
                    }
        except Exception as e:
            logger.warning(f"Error calling OpenRouter for assessment turn: {e}")

        # Ensure unique items in strengths and skill gaps
        if "strengths" in collected_profile:
            collected_profile["strengths"] = list(dict.fromkeys(collected_profile["strengths"]))
        if "skill_gaps" in collected_profile:
            collected_profile["skill_gaps"] = list(dict.fromkeys(collected_profile["skill_gaps"]))

        # Application-level sanity check for readiness (Mandatory Correction 3)
        # If learner has provided 4+ turns and stated a goal, allow READY_FOR_GENERATION
        if len(messages) >= 6 and session.goal and next_state != "READY_FOR_GENERATION":
            if not collected_profile.get("unknowns") or len(collected_profile["unknowns"]) <= 1:
                next_state = "READY_FOR_GENERATION"
                completeness = 100

        # Build public quiz object for message (without answer or explanation!)
        public_quiz_dto = None
        if new_pending_quiz:
            public_quiz_dto = {
                "question": new_pending_quiz["question"],
                "options": new_pending_quiz["options"],
                "skill_tested": new_pending_quiz.get("skill_tested"),
            }

        messages.append({
            "sender": "mentor",
            "text": next_mentor_text,
            "options": next_options,
            "quiz": public_quiz_dto,
            "timestamp": None,
        })

        session = await self.assessment_repo.update_session(
            session=session,
            state=next_state,
            collected_profile=collected_profile,
            messages=messages,
            pending_quiz=new_pending_quiz,
        )
        await self.db.commit()

        return self._format_session_response(session, completeness)

    async def generate_roadmap_from_assessment(
        self, user_id: str, session_id: str
    ) -> PersonalizedRoadmapDetailResponse:
        """Generates a structured, validated personalized roadmap using Claude Sonnet 4.5."""
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            raise ValueError("Assessment session not found or unauthorized")

        # Idempotency check: if roadmap already generated, return existing roadmap
        if session.roadmap_id:
            existing_roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(
                session.roadmap_id, user_id
            )
            if existing_roadmap and existing_roadmap.structure:
                return self._format_roadmap_detail_response(existing_roadmap)

        # Validate assessment readiness (Mandatory Correction 3)
        if session.state not in ("READY_FOR_GENERATION", "ASSESSING_SKILLS", "CLARIFYING_GAPS") and len(session.messages or []) < 4:
            raise ValueError("Assessment is incomplete. Please answer more assessment questions before generating your roadmap.")

        collected_profile = session.collected_profile or {}
        catalog = await self._get_catalog_summary()
        goal = session.goal or "Master Core Technical Skills"
        level = session.target_level or collected_profile.get("stated_level", "Beginner")
        weekly_hours = session.weekly_hours or 5.0

        catalog_prompt = "\n".join([f"- ID: {c['id']}, Name: {c['name']}, Level: {c['level']}" for c in catalog])

        system_instruction = (
            "You are EduNova's Senior Curriculum Architect. Generate a personalized, production-grade learning roadmap.\n"
            "Requirements:\n"
            "1. Ground the roadmap in the learner's assessed strengths, skill gaps, verified records, and available weekly time.\n"
            "2. Map phases in prerequisite order. Every phase must have clear measurable objectives, topics, practical activities, and milestones.\n"
            "3. Every milestone must have observable completion criteria and measurable passing criteria.\n"
            "4. Match to existing EduNova courses/lesson IDs where appropriate from the catalog provided below. DO NOT invent fake course IDs.\n"
            "5. Provide a weekly study schedule tailored to the learner's available hours.\n"
            "6. Output MUST be valid JSON adhering exactly to this schema:\n"
            "{\n"
            '  "title": "Title of the roadmap",\n'
            '  "goal": "Target outcome",\n'
            '  "startingLevel": "Beginner | Intermediate | Advanced",\n'
            '  "category": "Software Engineering | AI & Data | Mobile Development | Curriculum",\n'
            '  "estimatedDuration": "8 weeks",\n'
            '  "weeklyHours": 6.0,\n'
            '  "assessmentSummary": {\n'
            '    "strengths": ["..."],\n'
            '    "skillGaps": ["..."],\n'
            '    "verifiedEvidence": ["..."],\n'
            '    "selfReportedInformation": ["..."],\n'
            '    "unknowns": []\n'
            '  },\n'
            '  "phases": [\n'
            '    {\n'
            '      "title": "Phase 1: Foundation",\n'
            '      "objective": "...",\n'
            '      "durationWeeks": 2,\n'
            '      "topics": ["Topic 1", "Topic 2"],\n'
            '      "activities": ["Project / Exercise 1"],\n'
            '      "resources": ["Official Documentation"],\n'
            '      "milestones": [\n'
            '        {\n'
            '          "title": "Milestone 1",\n'
            '          "completionCriteria": ["Build X", "Pass quiz Y"],\n'
            '          "assessment": "...",\n'
            '          "passingCriteria": "..."\n'
            '        }\n'
            '      ],\n'
            '      "recommendedCourseIds": []\n'
            '    }\n'
            '  ],\n'
            '  "weeklySchedule": [\n'
            '    {\n'
            '      "dayOrWeek": "Week 1",\n'
            '      "focusTopic": "...",\n'
            '      "estimatedHours": 5.0,\n'
            '      "tasks": ["Task 1", "Task 2"]\n'
            '    }\n'
            '  ],\n'
            '  "assumptions": ["Assumes regular practice and access to IDE"],\n'
            '  "capstoneProject": "Description of final capstone project",\n'
            '  "nextAction": "First concrete step to start now"\n'
            "}"
        )

        user_content = (
            f"Learner Goal: {goal}\n"
            f"Assessed Starting Level: {level}\n"
            f"Weekly Available Hours: {weekly_hours}\n"
            f"Assessed Profile & Gaps: {json.dumps(collected_profile)}\n\n"
            f"EduNova Course Catalog:\n{catalog_prompt}\n\n"
            "Generate the complete personalized roadmap JSON."
        )

        roadmap_json = None
        for attempt in range(2):
            try:
                raw_resp = await openrouter_service.generate_response(
                    history=[{"role": "user", "content": user_content}],
                    explanation_mode="explainable",
                    memory_context=system_instruction,
                )
                parsed = _extract_json_from_response(raw_resp)
                if parsed:
                    # Validate against Pydantic schema
                    validated = PersonalizedRoadmapSchema.model_validate(parsed)
                    roadmap_json = validated.model_dump(by_alias=True)
                    break
            except Exception as exc:
                logger.warning(f"Roadmap generation attempt {attempt + 1} validation error: {exc}")
                user_content += f"\nNote: Previous response had validation errors ({exc}). Please return strictly valid JSON matching the schema."

        # If model failed all attempts, construct a validated fallback grounded in user profile
        if not roadmap_json:
            fallback_schema = PersonalizedRoadmapSchema(
                title=f"{goal} - Personalized Roadmap",
                goal=goal,
                starting_level=level if level in ("Beginner", "Intermediate", "Advanced") else "Beginner",
                category="General",
                estimated_duration="8 weeks",
                weekly_hours=float(weekly_hours),
                assessment_summary={
                    "strengths": collected_profile.get("strengths", ["Motivated learner"]),
                    "skill_gaps": collected_profile.get("skill_gaps", ["Core foundations"]),
                    "verified_evidence": collected_profile.get("verified_evidence", []),
                    "self_reported_information": collected_profile.get("self_reported_information", []),
                    "unknowns": [],
                },
                phases=[
                    {
                        "title": "Phase 1: Foundations & Core Concepts",
                        "objective": f"Establish core proficiency in {goal}",
                        "durationWeeks": 3,
                        "topics": ["Core Fundamentals", "Best Practices", "Hands-on Exercises"],
                        "activities": ["Interactive coding tasks", "Concept review"],
                        "resources": ["EduNova Learning Hub"],
                        "milestones": [
                            {
                                "title": "Core Competency Milestone",
                                "completionCriteria": ["Complete fundamental exercises", "Pass checkpoint assessment"],
                                "assessment": "Practical quiz & coding assessment",
                                "passingCriteria": "Score >= 80% on concepts",
                            }
                        ],
                        "recommendedCourseIds": [catalog[0]["id"]] if catalog else [],
                    },
                    {
                        "title": "Phase 2: Applied Projects & Advanced Skills",
                        "objective": "Build end-to-end practical projects demonstrating mastery",
                        "durationWeeks": 5,
                        "topics": ["Architecture & Design", "Integration & Testing", "Deployment"],
                        "activities": ["Build comprehensive portfolio project"],
                        "resources": ["EduNova Course Materials"],
                        "milestones": [
                            {
                                "title": "Capstone Project Delivery",
                                "completionCriteria": ["Build and test functional project", "Code review"],
                                "assessment": "Project evaluation against rubric",
                                "passingCriteria": "All user stories completed and tested",
                            }
                        ],
                        "recommendedCourseIds": [catalog[1]["id"]] if len(catalog) > 1 else [],
                    },
                ],
                weekly_schedule=[
                    {
                        "dayOrWeek": "Week 1-3",
                        "focusTopic": "Foundational Mastery",
                        "estimatedHours": float(weekly_hours),
                        "tasks": ["Study lessons", "Complete practice exercises"],
                    },
                    {
                        "dayOrWeek": "Week 4-8",
                        "focusTopic": "Project Implementation",
                        "estimatedHours": float(weekly_hours),
                        "tasks": ["Design architecture", "Build feature modules", "Test and polish"],
                    },
                ],
                assumptions=["Assumes consistent weekly study schedule"],
                capstone_project=f"Complete production-ready {goal} project",
                next_action="Start Phase 1: Foundations & Core Concepts",
            )
            roadmap_json = fallback_schema.model_dump(by_alias=True)

        # Convert phases into relational RoadmapItems
        items = []
        for phase in roadmap_json.get("phases", []):
            course_id = None
            if phase.get("recommendedCourseIds"):
                course_id = phase["recommendedCourseIds"][0]
            items.append({
                "title": phase["title"],
                "description": phase.get("objective"),
                "skills": phase.get("topics", []),
                "duration": f"{phase.get('durationWeeks', 2)} weeks",
                "course_id": course_id,
            })

        slug = f"personalized-{uuid.uuid4().hex[:8]}"
        roadmap = await self.roadmap_repo.create_personalized_roadmap(
            user_id=user_id,
            title=roadmap_json["title"],
            slug=slug,
            category=roadmap_json.get("category", "General"),
            description=roadmap_json["goal"],
            level=roadmap_json.get("startingLevel", "Beginner") if roadmap_json.get("startingLevel") in ("Beginner", "Intermediate", "Advanced", "All Levels") else "Beginner",
            duration=roadmap_json.get("estimatedDuration", "8 weeks"),
            icon="school",
            accent_theme="primary",
            structure=roadmap_json,
            items=items,
        )

        # Update session state to COMPLETED and link roadmap_id
        session = await self.assessment_repo.update_session(
            session=session,
            state="COMPLETED",
            roadmap_id=roadmap.id,
        )
        await self.db.commit()

        return self._format_roadmap_detail_response(roadmap)

    async def get_user_personalized_roadmaps(
        self, user_id: str, limit: Optional[int] = None, offset: int = 0
    ) -> List[PersonalizedRoadmapDetailResponse]:
        """Lists all personalized roadmaps created for the user."""
        roadmaps = await self.roadmap_repo.get_user_roadmaps(user_id=user_id, limit=limit, offset=offset)
        return [self._format_roadmap_detail_response(r) for r in roadmaps if r.structure]

    async def get_personalized_roadmap_detail(
        self, roadmap_id: str, user_id: str
    ) -> Optional[PersonalizedRoadmapDetailResponse]:
        roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(roadmap_id, user_id)
        if not roadmap or not roadmap.structure:
            return None
        return self._format_roadmap_detail_response(roadmap)

    def _format_session_response(
        self, session: RoadmapAssessmentSession, completeness: Optional[int] = None
    ) -> AssessmentSessionResponse:
        messages = session.messages or []
        latest_text = messages[-1]["text"] if messages else None
        latest_options = messages[-1].get("options", []) if messages else []
        latest_quiz = None

        if messages and messages[-1].get("quiz"):
            q = messages[-1]["quiz"]
            latest_quiz = AssessmentQuizPublicDto(
                question=q["question"],
                options=q["options"],
                skill_tested=q.get("skill_tested"),
            )

        if completeness is None:
            if session.state == "COMPLETED" or session.state == "READY_FOR_GENERATION":
                completeness = 100
            elif session.state == "COLLECTING_GOALS":
                completeness = 25
            elif session.state == "COLLECTING_PROGRESS":
                completeness = 50
            elif session.state == "ASSESSING_SKILLS":
                completeness = 75
            else:
                completeness = min(90, len(messages) * 15)

        items = [
            AssessmentMessageItem(
                sender=m["sender"],
                text=m["text"],
                options=m.get("options", []),
                quiz=AssessmentQuizPublicDto(
                    question=m["quiz"]["question"],
                    options=m["quiz"]["options"],
                    skill_tested=m["quiz"].get("skill_tested"),
                ) if m.get("quiz") else None,
                timestamp=m.get("timestamp"),
            )
            for m in messages
        ]

        return AssessmentSessionResponse(
            id=session.id,
            state=session.state,
            goal=session.goal,
            targetLevel=session.target_level,
            targetTimeline=session.target_timeline,
            weeklyHours=session.weekly_hours,
            latestMessage=latest_text,
            options=latest_options,
            quiz=latest_quiz,
            completenessPercentage=completeness,
            summary=session.collected_profile,
            roadmapId=session.roadmap_id,
            messages=items,
        )

    def _format_roadmap_detail_response(self, roadmap: Roadmap) -> PersonalizedRoadmapDetailResponse:
        items = [
            RoadmapItemResponse(
                id=item.id,
                title=item.title,
                description=item.description,
                courseId=item.course_id,
                skills=item.skills if isinstance(item.skills, list) else [],
                duration=item.duration,
            )
            for item in roadmap.items
        ]
        structure_obj = PersonalizedRoadmapSchema.model_validate(roadmap.structure)
        return PersonalizedRoadmapDetailResponse(
            id=roadmap.id,
            title=roadmap.title,
            goal=roadmap.description,
            category=roadmap.category,
            level=roadmap.level,
            duration=roadmap.duration,
            stages=len(roadmap.items),
            icon=roadmap.icon,
            accentTheme=roadmap.accent_theme,
            structure=structure_obj,
            items=items,
            createdAt=roadmap.created_at.isoformat() if roadmap.created_at else None,
        )
