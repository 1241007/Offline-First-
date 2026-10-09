"""Tests for Personalized Roadmap Assessment and Generation"""

import json
import pytest
from unittest.mock import patch, AsyncMock
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import (
    User,
    UserProfile,
    CourseCategory,
    Course,
    CourseModule,
    Lesson,
    UserCourseProgress,
    UserLessonProgress,
    UserMemory,
    RoadmapAssessmentSession,
    Roadmap,
)
from app.schemas.personalized_roadmap import PersonalizedRoadmapSchema


@pytest.fixture
async def setup_learner_data(db_session: AsyncSession):
    """Seed user, profile, courses, and progress for testing context gathering."""
    from sqlalchemy import select
    existing_user = await db_session.scalar(select(User).where(User.id == "test-user-id"))
    if existing_user:
        return

    user = User(
        id="test-user-id",
        email="roadmap-learner@edunova.app",
        mobile="9123456789",
        hashed_password="hashed_password",
        is_active=True,
    )
    db_session.add(user)

    profile = UserProfile(
        id="profile-1",
        user_id="test-user-id",
        full_name="Alex Rivera",
        email="roadmap-learner@edunova.app",
        level="Intermediate",
        education_mode="general",
        interests="AI, Kotlin, Backend Systems",
    )
    db_session.add(profile)

    category = CourseCategory(
        id="cat-1",
        name="Computer Science",
        description="Core CS",
        display_order=1,
    )
    db_session.add(category)

    course = Course(
        id="course-python-101",
        category_id="cat-1",
        name="Python Fundamentals",
        slug="python-fundamentals",
        description="Master Python syntax and basics",
        icon="code",
        level="Beginner",
        status="active",
    )
    db_session.add(course)

    module = CourseModule(
        id="mod-1",
        course_id="course-python-101",
        title="Introduction",
        display_order=1,
    )
    db_session.add(module)

    lesson1 = Lesson(
        id="lesson-1",
        module_id="mod-1",
        title="Variables and Data Types",
        content_type="article",
        display_order=1,
    )
    db_session.add(lesson1)

    # In-progress course
    progress = UserCourseProgress(
        id="prog-1",
        user_id="test-user-id",
        course_id="course-python-101",
        completion_percentage=60.0,
        status="in_progress",
        enrollment_date=pytest.importorskip("datetime").date.today(),
    )
    db_session.add(progress)

    lesson_progress = UserLessonProgress(
        id="lp-1",
        user_id="test-user-id",
        lesson_id="lesson-1",
        completed=True,
    )
    db_session.add(lesson_progress)

    memory = UserMemory(
        id="mem-1",
        user_id="test-user-id",
        category="goal",
        content="Wants to master AI agent engineering and Android",
        active=True,
    )
    db_session.add(memory)

    await db_session.commit()


@pytest.mark.asyncio
async def test_start_assessment_session(client: AsyncClient, setup_learner_data):
    """Test starting a personalized assessment session."""
    mock_ai_resp = json.dumps({
        "mentor_message": "Hello Alex! I see you've already started Python Fundamentals. What is your primary learning goal?",
        "quick_options": ["AI Engineer", "Android Developer", "Full Stack"],
        "assessment_state": "COLLECTING_GOALS",
        "completeness_percentage": 20,
    })

    with patch("app.services.openrouter_service.openrouter_service.generate_response", new=AsyncMock(return_value=mock_ai_resp)):
        response = await client.post("/api/v1/roadmaps/personalized/start")
        assert response.status_code == 201
        data = response.json()
        assert "id" in data
        assert data["state"] == "COLLECTING_GOALS"
        assert "Python Fundamentals" in data["latestMessage"] or "Alex" in data["latestMessage"]
        assert len(data["options"]) == 3
        assert len(data["messages"]) == 1
        assert data["messages"][0]["sender"] == "mentor"


@pytest.mark.asyncio
async def test_adaptive_message_and_quiz_security(client: AsyncClient, setup_learner_data, db_session: AsyncSession):
    """Test messaging flow and verify quiz security (no correct_index leaked to client)."""
    # 1. Start session
    start_resp = await client.post("/api/v1/roadmaps/personalized/start")
    assert start_resp.status_code == 201
    session_id = start_resp.json()["id"]

    # 2. Submit user goal, AI responds with a diagnostic quiz
    mock_quiz_resp = json.dumps({
        "mentor_message": "Great goal! Let's do a quick check on your current Python & AI foundation.",
        "quick_options": [],
        "diagnostic_quiz": {
            "question": "What is the primary difference between a list and a tuple in Python?",
            "options": ["Lists are immutable, tuples mutable", "Tuples are immutable, lists mutable", "Tuples can only store numbers", "No difference"],
            "correct_index": 1,
            "explanation": "Tuples cannot be modified after creation (immutable).",
            "skill_tested": "Python Data Structures",
        },
        "extracted_data": {
            "goal": "AI Engineer",
            "target_level": "Intermediate",
            "weekly_hours": 10.0,
            "unknowns": ["skill_proficiency"],
        },
        "assessment_state": "ASSESSING_SKILLS",
        "completeness_percentage": 50,
    })

    with patch("app.services.openrouter_service.openrouter_service.generate_response", new=AsyncMock(return_value=mock_quiz_resp)):
        msg_resp = await client.post(
            f"/api/v1/roadmaps/personalized/session/{session_id}/message",
            json={"answer": "I want to become an AI Engineer with 10 hours a week"},
        )
        assert msg_resp.status_code == 200
        msg_data = msg_resp.json()

        assert msg_data["state"] == "ASSESSING_SKILLS"
        assert msg_data["quiz"] is not None
        assert msg_data["quiz"]["question"] == "What is the primary difference between a list and a tuple in Python?"
        assert len(msg_data["quiz"]["options"]) == 4

        # CRITICAL MANDATORY CORRECTION 1: Ensure correct_index and explanation are NOT leaked in API response
        assert "correct_index" not in msg_data["quiz"]
        assert "correctIndex" not in msg_data["quiz"]
        assert "explanation" not in msg_data["quiz"]

    # 3. Learner submits correct quiz answer (index 1)
    mock_eval_resp = json.dumps({
        "mentor_message": "Spot on! Tuples are indeed immutable. Your foundation is solid. Let's design your roadmap!",
        "quick_options": ["Ready to build!"],
        "diagnostic_quiz": None,
        "extracted_data": {
            "goal": "AI Engineer",
            "target_level": "Intermediate",
            "weekly_hours": 10.0,
            "strengths": ["Python Data Structures"],
            "skill_gaps": ["Deep Learning & LLM APIs"],
            "unknowns": [],
        },
        "assessment_state": "READY_FOR_GENERATION",
        "completeness_percentage": 100,
    })

    with patch("app.services.openrouter_service.openrouter_service.generate_response", new=AsyncMock(return_value=mock_eval_resp)):
        quiz_submit_resp = await client.post(
            f"/api/v1/roadmaps/personalized/session/{session_id}/message",
            json={"answer": "Tuples are immutable, lists mutable", "quizSelectedIndex": 1},
        )
        assert quiz_submit_resp.status_code == 200
        eval_data = quiz_submit_resp.json()
        assert eval_data["state"] == "READY_FOR_GENERATION"
        assert eval_data["completenessPercentage"] == 100
        assert eval_data["quiz"] is None


@pytest.mark.asyncio
async def test_roadmap_generation_and_persistence(client: AsyncClient, setup_learner_data, db_session: AsyncSession):
    """Test generating a personalized roadmap, schema validation, and persistence."""
    # 1. Start session & progress to READY_FOR_GENERATION
    mock_start = json.dumps({
        "mentor_message": "Welcome!",
        "quick_options": ["AI Engineer"],
        "assessment_state": "COLLECTING_GOALS",
        "completeness_percentage": 20,
    })
    with patch("app.services.openrouter_service.openrouter_service.generate_response", new=AsyncMock(return_value=mock_start)):
        start_resp = await client.post("/api/v1/roadmaps/personalized/start")
    session_id = start_resp.json()["id"]

    mock_roadmap_data = {
        "title": "AI Engineer Career Path",
        "goal": "Become a Production AI Engineer",
        "startingLevel": "Intermediate",
        "category": "AI & Data",
        "estimatedDuration": "10 weeks",
        "weeklyHours": 8.0,
        "assessmentSummary": {
            "strengths": ["Python Data Structures", "Basic OOP"],
            "skillGaps": ["PyTorch", "LLM APIs & Prompting", "RAG Systems"],
            "verifiedEvidence": ["Completed course: Python Fundamentals"],
            "selfReportedInformation": ["10 hours/week available"],
            "unknowns": [],
        },
        "phases": [
            {
                "title": "Phase 1: Advanced Python & PyTorch Foundations",
                "objective": "Build solid mathematical and deep learning foundations",
                "durationWeeks": 3,
                "topics": ["Tensors", "Autograd", "Neural Network Modules"],
                "activities": ["Implement linear regression from scratch in PyTorch"],
                "resources": ["PyTorch Documentation"],
                "milestones": [
                    {
                        "title": "PyTorch Mastery Checkpoint",
                        "completionCriteria": ["Train a multi-class image classifier with >90% accuracy"],
                        "assessment": "Model training evaluation and loss curve analysis",
                        "passingCriteria": "Training loss < 0.2 and test accuracy >= 90%",
                    }
                ],
                "recommendedCourseIds": ["course-python-101"],
            },
            {
                "title": "Phase 2: LLM Application Development & RAG",
                "objective": "Build production-grade retrieval-augmented generation agents",
                "durationWeeks": 7,
                "topics": ["OpenRouter API", "Vector Databases", "Embeddings", "LangChain/LlamaIndex"],
                "activities": ["Develop an offline-first RAG assistant"],
                "resources": ["EduNova AI Documentation"],
                "milestones": [
                    {
                        "title": "RAG Agent Capstone",
                        "completionCriteria": ["Build end-to-end vector search pipeline with streaming"],
                        "assessment": "Evaluation of retrieval precision and response latency",
                        "passingCriteria": "Response latency < 1.5s with verified citations",
                    }
                ],
                "recommendedCourseIds": [],
            },
        ],
        "weeklySchedule": [
            {
                "dayOrWeek": "Week 1-3",
                "focusTopic": "PyTorch & Neural Networks",
                "estimatedHours": 8.0,
                "tasks": ["Study PyTorch tutorials", "Complete coding labs"],
            },
            {
                "dayOrWeek": "Week 4-10",
                "focusTopic": "RAG Systems & LLM Agents",
                "estimatedHours": 8.0,
                "tasks": ["Design vector pipeline", "Build frontend UI", "Integrate SSE streaming"],
            },
        ],
        "assumptions": ["Assumes regular practice of 8 hours weekly"],
        "capstoneProject": "End-to-End Multimodal AI Tutor with Local and Cloud Routing",
        "nextAction": "Begin Phase 1: Advanced Python & PyTorch Foundations",
    }

    # Set session state to READY_FOR_GENERATION in DB
    session_query = select(RoadmapAssessmentSession).where(RoadmapAssessmentSession.id == session_id)
    session_result = await db_session.execute(session_query)
    session = session_result.scalar_one()
    session.state = "READY_FOR_GENERATION"
    session.goal = "Become a Production AI Engineer"
    session.target_level = "Intermediate"
    session.weekly_hours = 8.0
    await db_session.commit()

    with patch("app.services.openrouter_service.openrouter_service.generate_response", new=AsyncMock(return_value=json.dumps(mock_roadmap_data))):
        gen_resp = await client.post(f"/api/v1/roadmaps/personalized/session/{session_id}/generate")
        assert gen_resp.status_code == 201
        roadmap_data = gen_resp.json()

        assert roadmap_data["title"] == "AI Engineer Career Path"
        assert roadmap_data["goal"] == "Become a Production AI Engineer"
        assert roadmap_data["stages"] == 2
        assert len(roadmap_data["structure"]["phases"]) == 2
        assert roadmap_data["structure"]["phases"][0]["milestones"][0]["title"] == "PyTorch Mastery Checkpoint"
        assert roadmap_data["structure"]["phases"][0]["milestones"][0]["passingCriteria"] == "Training loss < 0.2 and test accuracy >= 90%"
        assert roadmap_data["structure"]["capstoneProject"] == "End-to-End Multimodal AI Tutor with Local and Cloud Routing"

        roadmap_id = roadmap_data["id"]

        # 2. Test Idempotency: Repeating generation returns the same roadmap without creating duplicate records
        gen_resp2 = await client.post(f"/api/v1/roadmaps/personalized/session/{session_id}/generate")
        assert gen_resp2.status_code == 201
        assert gen_resp2.json()["id"] == roadmap_id

        # 3. Test listing user's personalized roadmaps
        list_resp = await client.get("/api/v1/roadmaps/personalized/my-roadmaps")
        assert list_resp.status_code == 200
        roadmaps_list = list_resp.json()
        assert len(roadmaps_list) >= 1
        assert any(r["id"] == roadmap_id for r in roadmaps_list)

        # 4. Test retrieving personalized roadmap detail
        detail_resp = await client.get(f"/api/v1/roadmaps/personalized/{roadmap_id}")
        assert detail_resp.status_code == 200
        assert detail_resp.json()["id"] == roadmap_id


@pytest.mark.asyncio
async def test_cross_user_isolation(client: AsyncClient, setup_learner_data, db_session: AsyncSession):
    """Test that assessment sessions and roadmaps are protected from unauthorized cross-user access."""
    existing_another = await db_session.scalar(select(User).where(User.id == "another-user-id"))
    if not existing_another:
        another_user = User(
            id="another-user-id",
            email="another-user@edunova.app",
            mobile="9123456780",
            hashed_password="hashed_password",
            is_active=True,
        )
        db_session.add(another_user)

    existing_session = await db_session.scalar(select(RoadmapAssessmentSession).where(RoadmapAssessmentSession.id == "session-user-1"))
    if not existing_session:
        session = RoadmapAssessmentSession(
            id="session-user-1",
            user_id="another-user-id",
            state="COLLECTING_GOALS",
            goal="Private Goal",
        )
        db_session.add(session)
    await db_session.commit()

    # The client fixture acts as 'test-user-id'
    resp = await client.get("/api/v1/roadmaps/personalized/session/session-user-1")
    assert resp.status_code == 404


@pytest.mark.asyncio
async def test_empty_active_session_returns_null(client: AsyncClient, setup_learner_data, db_session: AsyncSession):
    """Test that retrieving active session when none exists returns 200 OK with null body."""
    # Ensure no active sessions for test-user-id
    sessions = (await db_session.scalars(
        select(RoadmapAssessmentSession).where(RoadmapAssessmentSession.user_id == "test-user-id")
    )).all()
    for s in sessions:
        await db_session.delete(s)
    await db_session.commit()

    resp = await client.get("/api/v1/roadmaps/personalized/active")
    assert resp.status_code == 200
    assert resp.json() is None


@pytest.mark.asyncio
async def test_system_roadmaps_endpoint(client: AsyncClient, setup_learner_data, db_session: AsyncSession):
    """Test that GET /api/v1/roadmaps succeeds and queries roadmaps table with structure column intact."""
    resp = await client.get("/api/v1/roadmaps")
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)


@pytest.mark.asyncio
async def test_milestone_progress_sync_and_retrieval(client: AsyncClient, setup_learner_data, db_session: AsyncSession):
    """Test saving, retrieving, and syncing milestone completion state."""
    # Create test personalized roadmap with structure
    roadmap = Roadmap(
        id="test-sync-roadmap-1",
        user_id="test-user-id",
        title="Offline Sync Roadmap",
        slug="offline-sync-roadmap",
        category="Mobile Development",
        description="Learn Android Offline-First",
        level="Intermediate",
        duration="6 weeks",
        icon="school",
        accent_theme="primary",
        structure={
            "title": "Offline Sync Roadmap",
            "goal": "Learn Android Offline-First",
            "startingLevel": "Intermediate",
            "category": "Mobile Development",
            "estimatedDuration": "6 weeks",
            "weeklyHours": 10.0,
            "assessmentSummary": {
                "strengths": ["Kotlin"],
                "skillGaps": ["SQLite"],
                "verifiedEvidence": [],
                "selfReportedInformation": [],
                "unknowns": [],
            },
            "phases": [
                {
                    "title": "Phase 1",
                    "objective": "Basics",
                    "durationWeeks": 2,
                    "topics": ["SQLite"],
                    "activities": ["Coding"],
                    "resources": ["Docs"],
                    "milestones": [
                        {
                            "title": "M1",
                            "completionCriteria": ["Done"],
                            "assessment": "Quiz",
                            "passingCriteria": "Pass",
                        }
                    ],
                }
            ],
            "weeklySchedule": [],
            "assumptions": [],
            "capstoneProject": "App",
            "nextAction": "Start",
            "completedMilestones": [],
        },
    )
    db_session.add(roadmap)
    await db_session.commit()

    # 1. Check initial completed milestones is empty
    resp = await client.get("/api/v1/roadmaps/personalized/test-sync-roadmap-1/milestones")
    assert resp.status_code == 200
    assert resp.json()["completedMilestones"] == []

    # 2. Toggle single milestone to completed
    resp = await client.put(
        "/api/v1/roadmaps/personalized/test-sync-roadmap-1/milestones",
        json={"milestoneKey": "Phase 1_M1", "isCompleted": True},
    )
    assert resp.status_code == 200
    assert resp.json()["completedMilestones"] == ["Phase 1_M1"]

    # 3. Batch sync milestones
    resp = await client.put(
        "/api/v1/roadmaps/personalized/test-sync-roadmap-1/milestones",
        json={"completedMilestones": ["Phase 1_M1", "Phase 2_M2", "Phase 3_M3"]},
    )
    assert resp.status_code == 200
    assert set(resp.json()["completedMilestones"]) == {"Phase 1_M1", "Phase 2_M2", "Phase 3_M3"}

    # 4. Untoggle single milestone
    resp = await client.put(
        "/api/v1/roadmaps/personalized/test-sync-roadmap-1/milestones",
        json={"milestoneKey": "Phase 2_M2", "isCompleted": False},
    )
    assert resp.status_code == 200
    assert set(resp.json()["completedMilestones"]) == {"Phase 1_M1", "Phase 3_M3"}

    # 5. Verify GET reflects updated structure
    resp = await client.get("/api/v1/roadmaps/personalized/test-sync-roadmap-1/milestones")
    assert resp.status_code == 200
    assert set(resp.json()["completedMilestones"]) == {"Phase 1_M1", "Phase 3_M3"}

    # 6. Verify detail endpoint also has completedMilestones in structure
    detail_resp = await client.get("/api/v1/roadmaps/personalized/test-sync-roadmap-1")
    assert detail_resp.status_code == 200
    assert set(detail_resp.json()["structure"]["completedMilestones"]) == {"Phase 1_M1", "Phase 3_M3"}


@pytest.mark.asyncio
async def test_save_or_sync_personalized_roadmap(client: AsyncClient, setup_learner_data, db_session: AsyncSession):
    """Test saving offline-created or synchronized personalized roadmaps."""
    payload = {
        "id": "offline-created-roadmap-999",
        "title": "Offline Created Machine Learning",
        "goal": "Build Edge ML Apps",
        "category": "Artificial Intelligence",
        "level": "Intermediate",
        "duration": "10 weeks",
        "icon": "psychology",
        "accentTheme": "secondary",
        "structure": {
            "title": "Offline Created Machine Learning",
            "goal": "Build Edge ML Apps",
            "startingLevel": "Intermediate",
            "category": "Artificial Intelligence",
            "estimatedDuration": "10 weeks",
            "weeklyHours": 8.0,
            "assessmentSummary": {
                "strengths": ["Python", "NumPy"],
                "skillGaps": ["TFLite", "ONNX"],
                "verifiedEvidence": ["Completed Python 101"],
                "selfReportedInformation": [],
                "unknowns": []
            },
            "phases": [
                {
                    "title": "Phase 1: Edge Models",
                    "objective": "Quantize models",
                    "durationWeeks": 4,
                    "topics": ["TFLite", "Quantization"],
                    "activities": ["Export models"],
                    "resources": ["TF Docs"],
                    "milestones": [
                        {
                            "title": "Quantize first model",
                            "completionCriteria": ["Model size < 50MB"],
                            "assessment": "Benchmarking",
                            "passingCriteria": "Speed > 30 FPS"
                        }
                    ],
                    "recommendedCourseIds": []
                }
            ],
            "weeklySchedule": [
                {
                    "dayOrWeek": "Week 1",
                    "focusTopic": "Quantization",
                    "estimatedHours": 8.0,
                    "tasks": ["Read docs", "Run script"]
                }
            ],
            "assumptions": ["GPU available for training"],
            "capstoneProject": "Edge Camera App",
            "nextAction": "Download dataset",
            "completedMilestones": ["Phase 1_M1"]
        }
    }

    # 1. Create roadmap via POST /save
    resp = await client.post("/api/v1/roadmaps/personalized/save", json=payload)
    assert resp.status_code == 201
    data = resp.json()
    assert data["id"] == "offline-created-roadmap-999"
    assert data["title"] == "Offline Created Machine Learning"
    assert data["goal"] == "Build Edge ML Apps"
    assert len(data["structure"]["phases"]) == 1
    assert data["structure"]["phases"][0]["title"] == "Phase 1: Edge Models"

    # 2. Verify roadmap is returned in user's roadmaps list
    list_resp = await client.get("/api/v1/roadmaps/personalized/my-roadmaps")
    assert list_resp.status_code == 200
    my_roadmaps = list_resp.json()
    assert any(r["id"] == "offline-created-roadmap-999" for r in my_roadmaps)

    # 3. Idempotent sync: Repeat save with updated milestone/tasks
    payload["structure"]["completedMilestones"] = ["Phase 1_M1", "Phase 1_M2"]
    resp2 = await client.post("/api/v1/roadmaps/personalized/save", json=payload)
    assert resp2.status_code == 201
    data2 = resp2.json()
    assert data2["id"] == "offline-created-roadmap-999"
    assert set(data2["structure"]["completedMilestones"]) == {"Phase 1_M1", "Phase 1_M2"}

    # 4. Verify no duplicates were created
    list_resp2 = await client.get("/api/v1/roadmaps/personalized/my-roadmaps")
    assert list_resp2.status_code == 200
    matching = [r for r in list_resp2.json() if r["id"] == "offline-created-roadmap-999"]
    assert len(matching) == 1, "Must not create duplicate roadmaps on repeated sync"


