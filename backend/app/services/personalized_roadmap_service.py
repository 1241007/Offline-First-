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
    SavePersonalizedRoadmapRequest,
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


# Step Identifiers for deterministic assessment state machine
STEP_GOAL = "STEP_GOAL"
STEP_LEVEL = "STEP_LEVEL"
STEP_AVAILABILITY = "STEP_AVAILABILITY"
STEP_SKILLS = "STEP_SKILLS"
STEP_READY = "READY_FOR_GENERATION"

_FALLBACK_DIAGNOSTIC_QUIZZES = {
    "python": {
        "question": "Which keyword in Python is used to define an asynchronous coroutine function?",
        "options": ["async def", "def async", "coroutine def", "yield from"],
        "correct_index": 0,
        "explanation": "'async def' defines native coroutines and asynchronous generators in Python.",
        "skill_tested": "Python Asynchronous Programming",
    },
    "android": {
        "question": "Which Jetpack Compose effect handler is designed for running suspend coroutines tied to a key?",
        "options": ["LaunchedEffect", "rememberCoroutineScope", "DisposableEffect", "SideEffect"],
        "correct_index": 0,
        "explanation": "LaunchedEffect runs suspend code within Compose lifecycle and cancels/restarts on key changes.",
        "skill_tested": "Jetpack Compose Coroutines",
    },
    "ai": {
        "question": "In Retrieval-Augmented Generation (RAG), what is the primary role of a vector embedding?",
        "options": [
            "Represent semantic text meaning as high-dimensional numerical vectors for similarity search",
            "Compress text to save database disk space",
            "Encrypt sensitive user prompts for compliance",
            "Speed up token generation in decoder-only LLMs",
        ],
        "correct_index": 0,
        "explanation": "Vector embeddings project semantic meaning into dense vectors to calculate cosine similarity.",
        "skill_tested": "Vector Embeddings & RAG",
    },
    "general": {
        "question": "Which data structure provides average O(1) time complexity for lookup, insert, and delete operations?",
        "options": ["Hash Table / HashMap", "Binary Search Tree", "Linked List", "Array"],
        "correct_index": 0,
        "explanation": "Hash tables use hash functions to index keys directly into buckets, giving O(1) average lookup.",
        "skill_tested": "Data Structures & Algorithms",
    },
}


def _normalize_experience_level(text: str) -> Optional[str]:
    t = text.lower().strip()
    if re.search(r"\b(advanced|expert|experienced|mastery)\b", t) or "senior level" in t:
        return "Advanced"
    if re.search(r"\b(intermediate|medium|moderate|mid-level|mid level)\b", t) or "some experience" in t:
        return "Intermediate"
    if re.search(r"\b(beginner|novice|starter|basics|entry-level)\b", t) or "no experience" in t or "new to" in t:
        return "Beginner"
    return None


def _normalize_weekly_hours(text: str) -> Optional[float]:
    t = text.lower().strip()
    range_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:-|to|–)\s*(\d+(?:\.\d+)?)", t)
    if range_match:
        low = float(range_match.group(1))
        high = float(range_match.group(2))
        return (low + high) / 2.0
    plus_match = re.search(r"(\d+(?:\.\d+)?)\s*\+", t)
    if plus_match:
        return float(plus_match.group(1))
    hours_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:hours?|hrs?)", t)
    if hours_match:
        return float(hours_match.group(1))
    num_match = re.search(r"\b(\d+(?:\.\d+)?)\b", t)
    if num_match:
        val = float(num_match.group(1))
        if 1.0 <= val <= 80.0:
            return val
    return None


def _normalize_goal(text: str) -> str:
    cleaned = text.strip()
    prefixes = [
        r"^(?:i\s+want\s+to\s+become\s+(?:an?|the)?\s*)",
        r"^(?:i\s+want\s+to\s+learn\s+(?:about\s+)?(?:how\s+to\s+)?(?:an?|the)?\s*)",
        r"^(?:i\s+want\s+to\s+master\s+(?:an?|the)?\s*)",
        r"^(?:i\'?d\s+like\s+to\s+(?:learn|become|master)\s+(?:an?|the)?\s*)",
        r"^(?:learn\s+(?:about\s+)?(?:how\s+to\s+)?(?:an?|the)?\s*)",
        r"^(?:master\s+(?:an?|the)?\s*)",
        r"^(?:become\s+(?:an?|the)?\s*)",
        r"^(?:build\s+(?:an?|the)?\s*)",
    ]
    for p in prefixes:
        cleaned = re.sub(p, "", cleaned, flags=re.IGNORECASE).strip()
    cleaned = cleaned.rstrip(".!?,")
    if not cleaned:
        cleaned = text.strip().rstrip(".!?,")
    if cleaned and cleaned[0].islower():
        cleaned = cleaned[0].upper() + cleaned[1:]
    return cleaned


def _get_fallback_diagnostic_quiz(goal: Optional[str]) -> dict:
    g = (goal or "").lower()
    if "python" in g:
        return _FALLBACK_DIAGNOSTIC_QUIZZES["python"].copy()
    elif "android" in g or "kotlin" in g or "mobile" in g:
        return _FALLBACK_DIAGNOSTIC_QUIZZES["android"].copy()
    elif "ai" in g or "data" in g or "ml" in g or "machine learning" in g:
        return _FALLBACK_DIAGNOSTIC_QUIZZES["ai"].copy()
    return _FALLBACK_DIAGNOSTIC_QUIZZES["general"].copy()


def _determine_next_step(
    session: RoadmapAssessmentSession,
    completed_steps: List[str],
    has_pending_quiz: bool,
    quiz_history_count: int,
) -> str:
    if STEP_GOAL not in completed_steps and not session.goal:
        return STEP_GOAL
    if STEP_LEVEL not in completed_steps and not session.target_level:
        return STEP_LEVEL
    if STEP_AVAILABILITY not in completed_steps and not session.weekly_hours:
        return STEP_AVAILABILITY
    if STEP_SKILLS not in completed_steps and not has_pending_quiz and quiz_history_count == 0:
        return STEP_SKILLS
    return STEP_READY


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
            "current_step_id": STEP_GOAL,
            "completed_step_ids": [],
            "quiz_history": [],
        }

        system_prompt = (
            "You are EduNova's AI Learning Advisor. Your goal is to guide learners to a tailored roadmap.\n"
            "You ask ONE focused question at a time. Do not overwhelm the learner.\n"
            "Acknowledge the learner's existing verified progress naturally if any is available.\n"
            "You must return ONLY valid JSON in this exact structure:\n"
            "{\n"
            '  "mentor_message": "Warm, personalized greeting + first question about their primary learning/career goal.",\n'
            '  "quick_options": ["Option 1", "Option 2", "Option 3", "Option 4"],\n'
            '  "assessment_state": "COLLECTING_GOALS",\n'
            '  "completeness_percentage": 20\n'
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
        completeness = 20

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
                "step_id": STEP_GOAL,
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

        logger.info(
            "ASSESSMENT_TRANSITION: session_id=%s prev_step=START current_step=%s intent=INITIALIZE next_step=%s state=%s completeness=%s",
            session.id,
            STEP_GOAL,
            STEP_GOAL,
            state,
            completeness,
        )

        return self._format_session_response(session, completeness)

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

        user_turn_text = (answer or "").strip()
        if not user_turn_text and quiz_selected_index is None:
            raise ValueError("Answer text or quiz selection cannot be empty")

        collected_profile = dict(session.collected_profile or {})
        messages = list(session.messages or [])
        pending_quiz = session.pending_quiz
        completed_steps = list(collected_profile.get("completed_step_ids", []))
        current_step = collected_profile.get("current_step_id", STEP_GOAL)
        prev_step = current_step

        # Duplicate submission check (Idempotency)
        last_learner_msg = next((m for m in reversed(messages) if m.get("sender") == "learner"), None)
        if (
            last_learner_msg is not None
            and last_learner_msg.get("text") == user_turn_text
            and quiz_selected_index is None
        ):
            return self._format_session_response(session)

        # 1. Intent Classification & Input Processing
        intent = "STEP_ANSWER"
        quiz_feedback_note = ""
        correction_acknowledged_field = None

        # Check if Quiz Answer
        if pending_quiz is not None and (quiz_selected_index is not None or any(opt.lower() == user_turn_text.lower() for opt in pending_quiz.get("options", []))):
            intent = "QUIZ_ANSWER"
            if quiz_selected_index is None:
                # Find matching option index
                for idx, opt in enumerate(pending_quiz.get("options", [])):
                    if opt.lower() == user_turn_text.lower():
                        quiz_selected_index = idx
                        break

            correct_idx = pending_quiz.get("correct_index", 0)
            is_correct = (quiz_selected_index == correct_idx)
            skill = pending_quiz.get("skill_tested", "Skill check")
            explanation = pending_quiz.get("explanation", "")

            # Record in quiz history
            quiz_history = collected_profile.setdefault("quiz_history", [])
            quiz_history.append({
                "question": pending_quiz.get("question"),
                "selected_index": quiz_selected_index,
                "correct_index": correct_idx,
                "is_correct": is_correct,
                "skill_tested": skill,
                "explanation": explanation,
            })

            if is_correct:
                quiz_feedback_note = f"[QUIZ RESULT: CORRECT for '{skill}'. Explanation: {explanation}]"
                collected_profile.setdefault("strengths", []).append(f"Demonstrated proficiency in {skill}")
            else:
                quiz_feedback_note = f"[QUIZ RESULT: INCORRECT for '{skill}'. Chosen option #{((quiz_selected_index or 0) + 1)}, correct was #{correct_idx + 1}. Explanation: {explanation}]"
                collected_profile.setdefault("skill_gaps", []).append(f"Needs foundation in {skill}")

            if STEP_SKILLS not in completed_steps:
                completed_steps.append(STEP_SKILLS)
            pending_quiz = None

        else:
            # Check for explicit corrections
            low_text = user_turn_text.lower()
            is_correction_phrase = any(w in low_text for w in ["actually", "change my", "update my", "i meant", "correct my", "switch to", "make it", "instead of", "not beginner", "not advanced", "not intermediate"])

            norm_level = _normalize_experience_level(user_turn_text)
            norm_hours = _normalize_weekly_hours(user_turn_text)

            if is_correction_phrase:
                if norm_level:
                    session.target_level = norm_level
                    collected_profile["stated_level"] = norm_level
                    if STEP_LEVEL not in completed_steps:
                        completed_steps.append(STEP_LEVEL)
                    correction_acknowledged_field = f"experience level to {norm_level}"
                    intent = "CORRECTION"
                elif norm_hours is not None:
                    session.weekly_hours = norm_hours
                    if STEP_AVAILABILITY not in completed_steps:
                        completed_steps.append(STEP_AVAILABILITY)
                    correction_acknowledged_field = f"weekly study time to {norm_hours} hours/week"
                    intent = "CORRECTION"
                elif "goal" in low_text or "learn" in low_text or "career" in low_text:
                    norm_goal = _normalize_goal(user_turn_text)
                    session.goal = norm_goal
                    if STEP_GOAL not in completed_steps:
                        completed_steps.append(STEP_GOAL)
                    correction_acknowledged_field = f"goal to {norm_goal}"
                    intent = "CORRECTION"

            # If not an explicit correction, process according to current step
            if intent != "CORRECTION":
                # Check for general question or feedback
                is_question = user_turn_text.endswith("?") or any(low_text.startswith(q) for q in ["what is", "can you explain", "how does", "why do", "why should", "could you explain"])
                if is_question and not norm_level and norm_hours is None and current_step != STEP_GOAL:
                    intent = "FEEDBACK_OR_QUESTION"
                    collected_profile.setdefault("self_reported_information", []).append(user_turn_text)
                else:
                    # Normal step answer
                    intent = "STEP_ANSWER"
                    collected_profile.setdefault("self_reported_information", []).append(user_turn_text)

                    if current_step == STEP_GOAL or not session.goal:
                        session.goal = _normalize_goal(user_turn_text)
                        if STEP_GOAL not in completed_steps:
                            completed_steps.append(STEP_GOAL)

                    elif current_step == STEP_LEVEL or (not session.target_level and norm_level):
                        session.target_level = norm_level or "Intermediate"
                        if STEP_LEVEL not in completed_steps:
                            completed_steps.append(STEP_LEVEL)

                    elif current_step == STEP_AVAILABILITY or (session.weekly_hours is None and norm_hours is not None):
                        session.weekly_hours = norm_hours if norm_hours is not None else 5.0
                        if STEP_AVAILABILITY not in completed_steps:
                            completed_steps.append(STEP_AVAILABILITY)

                    elif current_step == STEP_SKILLS:
                        if STEP_SKILLS not in completed_steps:
                            completed_steps.append(STEP_SKILLS)

        # 2. Append learner turn to messages
        messages.append({
            "sender": "learner",
            "text": user_turn_text,
            "options": [],
            "quiz": None,
            "step_id": current_step,
            "timestamp": None,
        })

        # 3. Deduplicate completed_steps and determine next step
        completed_steps = list(dict.fromkeys(completed_steps))
        quiz_hist_len = len(collected_profile.get("quiz_history", []))
        next_step = _determine_next_step(session, completed_steps, pending_quiz is not None, quiz_hist_len)

        # Map next_step to assessment_state and progress completeness
        if next_step == STEP_GOAL:
            next_state = "COLLECTING_GOALS"
            completeness = 20
        elif next_step == STEP_LEVEL:
            next_state = "COLLECTING_PROGRESS"
            completeness = 40
        elif next_step == STEP_AVAILABILITY:
            next_state = "ASSESSING_AVAILABILITY"
            completeness = 60
        elif next_step == STEP_SKILLS:
            next_state = "ASSESSING_SKILLS"
            completeness = 80
        else:
            next_state = "READY_FOR_GENERATION"
            completeness = 100

        # 4. Craft Prompt & Call AI (with Deterministic Next Step Guidance)
        system_prompt = (
            "You are EduNova's AI Learning Advisor managing an interactive assessment conversation.\n"
            "Rules:\n"
            "1. Ask ONE concise question at a time.\n"
            "2. If the user answered a quiz, acknowledge their result with encouragement and clear explanation.\n"
            "3. If the user corrected a previous answer, acknowledge the correction.\n"
            f"4. Next required step is: '{next_step}'. You MUST address this next required step.\n"
            "5. If state is 'READY_FOR_GENERATION', conclude warmly and encourage generating the personalized roadmap.\n"
            "6. You must return ONLY valid JSON in this exact schema:\n"
            "{\n"
            '  "mentor_message": "Response text to learner + the next question.",\n'
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
            f'  "assessment_state": "{next_state}",\n'
            f'  "completeness_percentage": {completeness}\n'
            "}"
        )

        history_payload = []
        for m in messages[-8:]:
            role = "assistant" if m["sender"] == "mentor" else "user"
            history_payload.append({"role": role, "content": m["text"]})

        if quiz_feedback_note:
            history_payload.append({"role": "system", "content": quiz_feedback_note})

        if correction_acknowledged_field:
            history_payload.append({"role": "system", "content": f"User corrected {correction_acknowledged_field}."})

        user_context_info = f"Current Session Facts: Goal={session.goal}, Level={session.target_level}, WeeklyHours={session.weekly_hours}, NextStep={next_step}."
        history_payload.append({"role": "user", "content": f"User replied: '{user_turn_text}'. Intent={intent}. Context: {user_context_info}. Produce the next advisor response."})

        # 5. Build Dynamic Deterministic Fallback (NEVER repeat the same study-hours question!)
        if next_step == STEP_GOAL:
            fallback_mentor_text = "What is your primary learning or career goal?"
            fallback_options = ["Master Python Backend", "Android & Kotlin AI Apps", "Full-Stack Development", "Cloud Architecture"]
            fallback_quiz = None
        elif next_step == STEP_LEVEL:
            fallback_mentor_text = f"Got it! What is your current experience level with {session.goal or 'this area'}?"
            fallback_options = ["Beginner (Starting fresh)", "Intermediate (Know fundamentals)", "Advanced (Built projects)"]
            fallback_quiz = None
        elif next_step == STEP_AVAILABILITY:
            fallback_mentor_text = "How many hours per week can you dedicate to studying and practice?"
            fallback_options = ["2-4 hours/week", "5-10 hours/week", "15+ hours/week"]
            fallback_quiz = None
        elif next_step == STEP_SKILLS:
            diag = _get_fallback_diagnostic_quiz(session.goal)
            fallback_mentor_text = f"Awesome! Let's do a quick diagnostic check on your {session.goal or 'technical'} foundation:"
            fallback_options = []
            fallback_quiz = diag
        else:
            # READY_FOR_GENERATION
            fallback_mentor_text = f"Excellent! I've gathered all your details ({session.goal}, {session.target_level} level, {session.weekly_hours or 5.0} hrs/week). We are ready to build your tailored learning roadmap!"
            fallback_options = ["Generate My Personalized Roadmap"]
            fallback_quiz = None

        if correction_acknowledged_field:
            fallback_mentor_text = f"Got it, I've updated your {correction_acknowledged_field}! " + fallback_mentor_text

        next_mentor_text = fallback_mentor_text
        next_options = fallback_options
        new_pending_quiz = fallback_quiz

        try:
            raw_resp = await openrouter_service.generate_response(
                history=history_payload,
                explanation_mode="general",
                memory_context=system_prompt,
            )
            parsed = _extract_json_from_response(raw_resp)
            if parsed and "mentor_message" in parsed:
                next_mentor_text = parsed["mentor_message"]
                if "quick_options" in parsed and isinstance(parsed["quick_options"], list):
                    next_options = parsed["quick_options"]
                if parsed.get("assessment_state") in ("COLLECTING_GOALS", "COLLECTING_PROGRESS", "ASSESSING_AVAILABILITY", "ASSESSING_SKILLS", "READY_FOR_GENERATION"):
                    next_state = parsed["assessment_state"]
                if "completeness_percentage" in parsed and isinstance(parsed["completeness_percentage"], (int, float)):
                    completeness = int(parsed["completeness_percentage"])

                extracted = parsed.get("extracted_data", {})
                if extracted.get("goal") and not session.goal:
                    session.goal = extracted["goal"]
                if extracted.get("target_level") and not session.target_level:
                    session.target_level = extracted["target_level"]
                if extracted.get("weekly_hours") and not session.weekly_hours:
                    try:
                        session.weekly_hours = float(extracted["weekly_hours"])
                    except (ValueError, TypeError):
                        pass
                if extracted.get("strengths"):
                    collected_profile.setdefault("strengths", []).extend(extracted["strengths"])
                if extracted.get("skill_gaps"):
                    collected_profile.setdefault("skill_gaps", []).extend(extracted["skill_gaps"])

                diag_quiz = parsed.get("diagnostic_quiz")
                if diag_quiz and isinstance(diag_quiz, dict) and "question" in diag_quiz and "options" in diag_quiz:
                    new_pending_quiz = {
                        "question": diag_quiz["question"],
                        "options": diag_quiz["options"],
                        "correct_index": diag_quiz.get("correct_index", 0),
                        "explanation": diag_quiz.get("explanation", ""),
                        "skill_tested": diag_quiz.get("skill_tested", "Concept check"),
                    }
        except Exception as e:
            logger.warning(f"Error calling OpenRouter for assessment turn: {e}")

        # Ensure unique items in strengths and gaps
        if "strengths" in collected_profile:
            collected_profile["strengths"] = list(dict.fromkeys(collected_profile["strengths"]))
        if "skill_gaps" in collected_profile:
            collected_profile["skill_gaps"] = list(dict.fromkeys(collected_profile["skill_gaps"]))

        # Enforce deterministic readiness: if all 3 core dimensions are satisfied and quiz evaluated or reached ready
        if next_step == STEP_READY:
            next_state = "READY_FOR_GENERATION"
            completeness = 100
            new_pending_quiz = None

        # Build public quiz object for client message (WITHOUT answer key!)
        public_quiz_dto = None
        if new_pending_quiz:
            public_quiz_dto = {
                "question": new_pending_quiz["question"],
                "options": new_pending_quiz["options"],
                "skill_tested": new_pending_quiz.get("skill_tested"),
            }

        # Append mentor response
        messages.append({
            "sender": "mentor",
            "text": next_mentor_text,
            "options": next_options,
            "quiz": public_quiz_dto,
            "step_id": next_step,
            "timestamp": None,
        })

        # Update tracking fields in collected_profile
        collected_profile["current_step_id"] = next_step
        collected_profile["completed_step_ids"] = completed_steps

        session = await self.assessment_repo.update_session(
            session=session,
            state=next_state,
            goal=session.goal,
            target_level=session.target_level,
            weekly_hours=session.weekly_hours,
            collected_profile=collected_profile,
            messages=messages,
            pending_quiz=new_pending_quiz,
        )
        await self.db.commit()

        # Structured non-sensitive logging
        logger.info(
            "ASSESSMENT_TRANSITION: session_id=%s prev_step=%s current_step=%s intent=%s next_step=%s state=%s completeness=%s",
            session.id,
            prev_step,
            current_step,
            intent,
            next_step,
            next_state,
            completeness,
        )

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

    async def save_or_sync_personalized_roadmap(
        self, user_id: str, payload: SavePersonalizedRoadmapRequest
    ) -> PersonalizedRoadmapDetailResponse:
        """Saves or synchronizes a personalized roadmap created locally or generated offline."""
        roadmap = None
        if payload.id:
            roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(payload.id, user_id)

        structure_dict = payload.structure.model_dump(by_alias=True)
        items = []
        for phase in structure_dict.get("phases", []):
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

        if roadmap:
            # Update existing roadmap idempotently
            roadmap = await self.roadmap_repo.update_personalized_roadmap(
                roadmap=roadmap,
                title=payload.title,
                category=payload.category or "General",
                description=payload.goal,
                level=payload.level or "Beginner",
                duration=payload.duration or "8 weeks",
                icon=payload.icon or "school",
                accent_theme=payload.accent_theme or "primary",
                structure=structure_dict,
                items=items,
            )
            await self.db.commit()
        else:
            roadmap_id = payload.id or str(uuid.uuid4())
            slug = f"personalized-{uuid.uuid4().hex[:8]}"
            roadmap = await self.roadmap_repo.create_personalized_roadmap(
                user_id=user_id,
                title=payload.title,
                slug=slug,
                category=payload.category or "General",
                description=payload.goal,
                level=payload.level or "Beginner",
                duration=payload.duration or "8 weeks",
                icon=payload.icon or "school",
                accent_theme=payload.accent_theme or "primary",
                structure=structure_dict,
                items=items,
                id=roadmap_id,
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

    async def get_roadmap_milestones(
        self, roadmap_id: str, user_id: str
    ) -> List[str]:
        roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(roadmap_id, user_id)
        if not roadmap or not roadmap.structure:
            return []
        return list(roadmap.structure.get("completedMilestones", []))

    async def sync_roadmap_milestones(
        self,
        roadmap_id: str,
        user_id: str,
        milestone_key: Optional[str] = None,
        is_completed: Optional[bool] = None,
        completed_milestones: Optional[List[str]] = None,
    ) -> List[str]:
        roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(roadmap_id, user_id)
        if not roadmap or not roadmap.structure:
            raise ValueError("Personalized roadmap not found or unauthorized")

        current_structure = dict(roadmap.structure)
        current_completed = set(current_structure.get("completedMilestones", []))

        if completed_milestones is not None:
            current_completed.update(completed_milestones)

        if milestone_key is not None:
            if is_completed is True:
                current_completed.add(milestone_key)
            elif is_completed is False:
                current_completed.discard(milestone_key)

        updated_list = sorted(list(current_completed))
        current_structure["completedMilestones"] = updated_list
        roadmap.structure = current_structure
        await self.db.commit()
        return updated_list

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
                step_id=m.get("step_id"),
                timestamp=m.get("timestamp"),
            )
            for m in messages
        ]

        collected = session.collected_profile or {}
        current_step = collected.get("current_step_id", STEP_GOAL)
        completed_steps = collected.get("completed_step_ids", [])

        return AssessmentSessionResponse(
            id=session.id,
            state=session.state,
            current_step_id=current_step,
            completed_steps=completed_steps,
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
