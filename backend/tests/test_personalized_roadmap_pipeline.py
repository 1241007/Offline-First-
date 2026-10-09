"""Comprehensive regression tests for EduNova 2.0 Roadmap Planning Pipeline and Quality Validation"""

import json
import pytest
from unittest.mock import patch, AsyncMock
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.models import User, UserProfile, CourseCategory, Course, CourseModule, Lesson, UserCourseProgress, UserLessonProgress, UserMemory, RoadmapAssessmentSession, Roadmap
from app.schemas.personalized_roadmap import PersonalizedRoadmapSchema, ProfileCorrectionRequest
from app.services.personalized_roadmap_service import PersonalizedRoadmapService


@pytest.fixture
async def setup_learner_data(db_session: AsyncSession):
    """Seed user, profile, courses, and progress for testing context gathering."""
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
    await db_session.commit()


@pytest.mark.asyncio
async def test_learner_profile_normalization_and_integrity(client: AsyncClient, setup_learner_data):
    """Verify that learner level is never silently downgraded and study hours are never parsed as goal."""
    start_resp = await client.post("/api/v1/roadmaps/personalized/start")
    assert start_resp.status_code == 201
    session_id = start_resp.json()["id"]

    # Submit Goal
    resp1 = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "I want to master Asynchronous Backend Systems"},
    )
    assert resp1.status_code == 200
    d1 = resp1.json()
    assert d1["goal"] == "Asynchronous Backend Systems"

    # Submit Advanced Level
    resp2 = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Senior developer with production backend experience (Advanced)"},
    )
    assert resp2.status_code == 200
    d2 = resp2.json()
    assert d2["targetLevel"] == "Advanced", "Must preserve explicitly declared Advanced level"

    # Submit Hours
    resp3 = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "8 hours/week"},
    )
    assert resp3.status_code == 200
    d3 = resp3.json()
    assert d3["weeklyHours"] == 8.0
    assert d3["goal"] == "Asynchronous Backend Systems", "Hours must not overwrite goal"


@pytest.mark.asyncio
async def test_in_place_profile_correction_endpoint(client: AsyncClient, setup_learner_data):
    """Test the POST /correct-field endpoint for Stage E in-place adjustments without session restart."""
    start_resp = await client.post("/api/v1/roadmaps/personalized/start")
    session_id = start_resp.json()["id"]

    # Fill goal, level, hours
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Android Jetpack Compose"},
    )
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Beginner"},
    )
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "5 hours/week"},
    )

    # In-place correct level to Advanced
    corr_resp = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/correct-field",
        json={"field": "target_level", "value": "Advanced"},
    )
    assert corr_resp.status_code == 200
    corr_data = corr_resp.json()
    assert corr_data["targetLevel"] == "Advanced"
    assert corr_data["goal"] == "Android Jetpack Compose"
    assert corr_data["weeklyHours"] == 5.0

    # In-place correct weekly hours to 12
    corr_resp2 = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/correct-field",
        json={"field": "weekly_hours", "value": 12.0},
    )
    assert corr_resp2.status_code == 200
    assert corr_resp2.json()["weeklyHours"] == 12.0


@pytest.mark.asyncio
async def test_roadmap_quality_validator_10_checks(db_session: AsyncSession):
    """Unit test the 10-point quality validator against valid and invalid schemas."""
    service = PersonalizedRoadmapService(db_session)
    profile = {
        "goal": "FastAPI Microservices",
        "starting_level": "Advanced",
        "weekly_hours": 8.0,
        "verified_evidence": [],
        "strengths": ["Python 3.11", "SQLAlchemy AsyncIO"],
        "skill_gaps": ["Distributed Tracing"],
    }

    # 1. Defective payload with wrong goal and downgraded level
    defective_data = {
        "title": "8 hours/week - Roadmap",
        "goal": "Beginner",
        "startingLevel": "Beginner",
        "weeklyHours": 0.0,
        "phases": [],
        "weeklySchedule": [],
        "capstoneProject": "",
        "nextAction": "",
    }
    is_valid, errors = service._validate_roadmap_quality(defective_data, profile)
    assert not is_valid
    assert len(errors) >= 5
    assert any("Check 1" in e for e in errors)
    assert any("Check 2" in e for e in errors)
    assert any("Check 3" in e for e in errors)
    assert any("Check 4" in e for e in errors)

    # 2. Complete valid payload
    valid_data = {
        "title": "FastAPI Microservices - Personalized Roadmap",
        "goal": "FastAPI Microservices",
        "startingLevel": "Advanced",
        "category": "Backend Engineering",
        "estimatedDuration": "8 weeks",
        "weeklyHours": 8.0,
        "assessmentSummary": {
            "strengths": ["Python 3.11", "SQLAlchemy AsyncIO"],
            "skillGaps": ["Distributed Tracing"],
            "verifiedEvidence": [],
            "selfReportedInformation": ["Advanced Level"],
            "unknowns": [],
            "skillGapBreakdown": [
                {
                    "skill": "SQLAlchemy AsyncIO",
                    "status": "demonstrated",
                    "source": "diagnostic_verified",
                    "confidence": 0.95,
                }
            ],
            "curriculumRationale": "Advanced plan focusing on concurrency and distributed tracing.",
        },
        "phases": [
            {
                "title": "Phase 1: High-Throughput Async Architecture",
                "objective": "Build event-driven asynchronous microservices",
                "durationWeeks": 3,
                "topics": ["AsyncIO Event Loop", "Connection Pooling", "OpenTelemetry"],
                "tasks": [
                    {
                        "id": "t1_1",
                        "title": "Profile AsyncIO Bottlenecks",
                        "description": "Benchmark latency under load",
                        "instructions": "Run Locust load testing and trace execution bottlenecks.",
                        "estimatedHours": 3.0,
                        "resources": ["AsyncIO Profiling Guide"],
                        "completionCriteria": "Produce latency report",
                        "isCompleted": False,
                        "dependencies": [],
                    }
                ],
                "activities": ["Profile AsyncIO Bottlenecks"],
                "resources": ["FastAPI Documentation"],
                "milestones": [
                    {
                        "title": "Async Core Checkpoint",
                        "completionCriteria": ["Pass latency benchmarks"],
                        "assessment": "Benchmark check",
                        "passingCriteria": "p99 < 50ms",
                    }
                ],
                "recommendedCourseIds": [],
            },
            {
                "title": "Phase 2: Distributed Tracing & Capstone Delivery",
                "objective": "Deploy production-grade microservice cluster",
                "durationWeeks": 5,
                "topics": ["Jaeger Tracing", "Kafka Event Bus", "CI/CD Deployment"],
                "tasks": [
                    {
                        "id": "t2_1",
                        "title": "Deploy Distributed Cluster",
                        "description": "Deploy services with OpenTelemetry collector",
                        "instructions": "Configure Jaeger and verify end-to-end distributed span propagation.",
                        "estimatedHours": 5.0,
                        "resources": ["OpenTelemetry Architecture Guide"],
                        "completionCriteria": "All distributed transactions visible in Jaeger dashboard",
                        "isCompleted": False,
                        "dependencies": ["t1_1"],
                    }
                ],
                "activities": ["Deploy Distributed Cluster"],
                "resources": ["Microservices Deployment Guide"],
                "milestones": [
                    {
                        "title": "Capstone Microservice Delivery",
                        "completionCriteria": ["Ship multi-service cluster with automated CI"],
                        "assessment": "Capstone evaluation",
                        "passingCriteria": "All integration tests pass",
                    }
                ],
                "recommendedCourseIds": [],
            },
        ],
        "weeklySchedule": [
            {
                "dayOrWeek": "Week 1-3",
                "focusTopic": "High-Throughput Async Architecture",
                "estimatedHours": 8.0,
                "tasks": ["Profile AsyncIO Bottlenecks"],
            },
            {
                "dayOrWeek": "Week 4-8",
                "focusTopic": "Distributed Tracing & Capstone",
                "estimatedHours": 8.0,
                "tasks": ["Deploy Distributed Cluster"],
            },
        ],
        "assumptions": ["Assumes experience with Docker and Python"],
        "capstoneProject": "Production-grade distributed microservice cluster with Jaeger tracing",
        "nextAction": "Start Phase 1: Profile AsyncIO Bottlenecks",
        "completedMilestones": [],
    }
    is_valid_2, errors_2 = service._validate_roadmap_quality(valid_data, profile)
    assert is_valid_2, f"Expected valid data to pass validation, got errors: {errors_2}"
    assert len(errors_2) == 0


@pytest.mark.asyncio
async def test_adaptive_diagnostic_explainability(client: AsyncClient, setup_learner_data):
    """Verify diagnostic quizzes include skillTested, difficulty, and selectionRationale."""
    start_resp = await client.post("/api/v1/roadmaps/personalized/start")
    session_id = start_resp.json()["id"]

    # 1. Goal
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Python Data Analysis with Pandas"},
    )
    # 2. Level
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Intermediate"},
    )
    # 3. Hours -> Triggers diagnostic check
    resp = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "8 hours/week"},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["quiz"] is not None
    quiz = data["quiz"]
    assert "question" in quiz
    assert quiz.get("skillTested") is not None
    assert quiz.get("difficulty") is not None
    assert quiz.get("selectionRationale") is not None
    # Verify security: no correct answer leak
    assert "correct_index" not in quiz
    assert "correctIndex" not in quiz
