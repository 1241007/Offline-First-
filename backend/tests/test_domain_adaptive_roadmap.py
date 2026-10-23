"""Tests for Domain-Aware Adaptive Assessment, Curriculum Grounding, and Topic Completion"""

import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.models import User, UserProfile
from app.services.personalized_roadmap_service import PersonalizedRoadmapService


@pytest.fixture
async def setup_test_learner(db_session: AsyncSession):
    user = await db_session.scalar(select(User).where(User.id == "domain-test-user"))
    if not user:
        user = User(
            id="domain-test-user",
            email="domain-test@edunova.app",
            mobile="9876543210",
            hashed_password="hashed_pw",
            is_active=True,
        )
        db_session.add(user)
        profile = UserProfile(
            id="domain-test-profile",
            user_id="domain-test-user",
            full_name="Domain Tester",
            email="domain-test@edunova.app",
            level="Intermediate",
            education_mode="general",
            interests="Java, SQL, Python",
        )
        db_session.add(profile)
        await db_session.commit()


@pytest.mark.asyncio
async def test_domain_specific_assessment_initialization(client: AsyncClient, setup_test_learner):
    """Verify that domain is transmitted, stored, and guides adaptive question selection."""
    start_resp = await client.post("/api/v1/roadmaps/personalized/start", json={"domain": "Java"})
    assert start_resp.status_code == 201
    data = start_resp.json()
    session_id = data["id"]
    assert data["domain"] == "Java"
    assert "Java" in data["latestMessage"] or "mentor" in data["latestMessage"].lower()

    # Step 1: Goal
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Enterprise Java Microservices with Spring Boot"},
    )
    # Step 2: Level
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Intermediate"},
    )
    # Step 3: Hours -> triggers diagnostic question
    resp = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "8 hours/week"},
    )
    assert resp.status_code == 200
    quiz_data = resp.json().get("quiz")
    assert quiz_data is not None
    # Must test a Java skill
    assert any(
        kw in quiz_data["skillTested"].lower()
        for kw in ["java", "jvm", "stream", "interface", "thread", "spring", "memory"]
    )


@pytest.mark.asyncio
async def test_validate_subject_endpoint(client: AsyncClient, setup_test_learner):
    """Test standardizing domain inputs."""
    r1 = await client.post(
        "/api/v1/roadmaps/personalized/validate-subject",
        json={"subject": "python data science"},
    )
    assert r1.status_code == 200
    d1 = r1.json()
    assert d1["isValid"] is True
    assert d1["standardizedName"] == "Python"
    assert len(d1["suggestions"]) >= 2

    r2 = await client.post(
        "/api/v1/roadmaps/personalized/validate-subject",
        json={"subject": "spring boot and hibernate"},
    )
    assert r2.status_code == 200
    assert r2.json()["standardizedName"] == "Java"


@pytest.mark.asyncio
async def test_roadmap_generation_and_topic_completion_flow(client: AsyncClient, setup_test_learner):
    """Test full flow: assess domain, generate roadmap, get topic content, and complete topic."""
    # 1. Start SQL Assessment
    start_resp = await client.post("/api/v1/roadmaps/personalized/start", json={"domain": "SQL"})
    assert start_resp.status_code == 201
    session_id = start_resp.json()["id"]

    # Provide goal, level, hours
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Relational Database Performance & Analytics"},
    )
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Intermediate"},
    )
    resp3 = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "6 hours/week"},
    )
    # Answer quiz
    quiz = resp3.json()["quiz"]
    assert quiz is not None
    resp4 = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Submitting diagnostic response", "quizSelectedIndex": 0},
    )
    assert resp4.status_code == 200

    # 2. Generate roadmap
    gen_resp = await client.post(f"/api/v1/roadmaps/personalized/session/{session_id}/generate")
    assert gen_resp.status_code in (200, 201)
    roadmap_data = gen_resp.json()
    roadmap_id = roadmap_data["id"]
    structure = roadmap_data["structure"]
    assert structure["domain"] == "SQL"
    assert len(structure["phases"]) >= 2
    assert len(structure["topics"]) >= 2

    # 3. Retrieve topic learning content
    first_task = structure["phases"][0]["tasks"][0]
    topic_id = first_task["topicId"] or first_task["id"]

    content_resp = await client.get(
        f"/api/v1/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/content"
    )
    assert content_resp.status_code == 200
    content = content_resp.json()
    assert content["topicId"] == topic_id
    assert content["domain"] == "SQL"
    assert len(content["learningObjectives"]) >= 1

    # 4. Complete topic
    comp_resp = await client.post(
        f"/api/v1/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/complete",
        json={"quizPassed": True},
    )
    assert comp_resp.status_code == 200
    comp_data = comp_resp.json()
    assert comp_data["isCompleted"] is True
    assert comp_data["progressPercentage"] > 0
    assert topic_id in comp_data["completedTopicIds"]


@pytest.mark.asyncio
async def test_go_domain_adaptive_assessment_and_roadmap(client: AsyncClient, setup_test_learner):
    """Test start assessment with domain='Go' and verify Go diagnostic questions and curriculum."""
    # 1. Start assessment for Go
    start_resp = await client.post("/api/v1/roadmaps/personalized/start", json={"domain": "Go"})
    assert start_resp.status_code == 201
    data = start_resp.json()
    session_id = data["id"]
    assert data["domain"] == "Go"
    assert "Go" in data["latestMessage"] or "mentor" in data["latestMessage"].lower()

    # Step 1: Goal
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "High-Throughput Concurrent Microservices in Go"},
    )
    # Step 2: Level
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Intermediate"},
    )
    # Step 3: Hours -> triggers Go diagnostic question
    resp = await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "8 hours/week"},
    )
    assert resp.status_code == 200
    quiz_data = resp.json().get("quiz")
    assert quiz_data is not None
    assert any(
        kw in quiz_data["skillTested"].lower()
        for kw in ["go", "channel", "goroutine", "concurrency", "defer", "interface"]
    )

    # Submit quiz answer
    await client.post(
        f"/api/v1/roadmaps/personalized/session/{session_id}/message",
        json={"answer": "Submitting Go quiz answer", "quizSelectedIndex": 0},
    )

    # Generate roadmap
    gen_resp = await client.post(f"/api/v1/roadmaps/personalized/session/{session_id}/generate")
    assert gen_resp.status_code in (200, 201)
    roadmap = gen_resp.json()
    assert roadmap["structure"]["domain"] == "Go"
    assert len(roadmap["structure"]["phases"]) >= 2
    assert "Go" in roadmap["structure"]["phases"][0]["title"]


@pytest.mark.asyncio
async def test_assessment_gated_completion_and_review(client: AsyncClient, setup_test_learner):
    """
    Verify:
    1. Topic content includes independent tasks, hints, and security-sanitized assessment questions.
    2. Review endpoint records REVIEWED state without bypassing assessment.
    3. Failing assessment sets NEEDS_REVISION with targeted revision guidance.
    4. Passing assessment awards VERIFIED_COMPLETE / MASTERED and updates progress.
    """
    # 1. Start assessment and generate roadmap
    start_resp = await client.post("/api/v1/roadmaps/personalized/start", json={"domain": "Python"})
    assert start_resp.status_code == 201
    session_id = start_resp.json()["id"]

    await client.post(f"/api/v1/roadmaps/personalized/session/{session_id}/message", json={"answer": "Backend API Development"})
    await client.post(f"/api/v1/roadmaps/personalized/session/{session_id}/message", json={"answer": "Intermediate"})
    await client.post(f"/api/v1/roadmaps/personalized/session/{session_id}/message", json={"answer": "10 hours/week"})
    await client.post(f"/api/v1/roadmaps/personalized/session/{session_id}/message", json={"answer": "Quiz answer", "quizSelectedIndex": 0})

    gen_resp = await client.post(f"/api/v1/roadmaps/personalized/session/{session_id}/generate")
    assert gen_resp.status_code in (200, 201)
    roadmap = gen_resp.json()
    roadmap_id = roadmap["id"]
    first_task = roadmap["structure"]["phases"][0]["tasks"][0]
    topic_id = first_task.get("topicId") or first_task.get("id")

    # 2. Get topic content
    content_resp = await client.get(f"/api/v1/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/content")
    assert content_resp.status_code == 200
    content = content_resp.json()
    assert content["independentTask"] is not None
    assert content["hints"] is not None
    assert content["hints"]["hint1Concept"] is not None
    assert content["hints"]["hint2Strategy"] is not None
    assert content["hints"]["hint3Scaffold"] is not None
    assert len(content["assessmentQuestions"]) >= 1
    # Security: correctOptionIndex must NOT be exposed to client
    for q in content["assessmentQuestions"]:
        assert q["correctOptionIndex"] is None

    # 3. Learner reviews material
    review_resp = await client.post(f"/api/v1/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/review")
    assert review_resp.status_code == 200
    assert review_resp.json()["status"] == "REVIEWED"

    # 4. Submit failing assessment
    fail_resp = await client.post(
        f"/api/v1/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/submit-assessment",
        json={"answers": [99, 99, 99], "hintsUsed": 3, "solutionRevealed": True}
    )
    assert fail_resp.status_code == 200
    fail_data = fail_resp.json()
    assert fail_data["passed"] is False
    assert fail_data["status"] == "NEEDS_REVISION"
    assert "revision" in fail_data["revisionGuidance"].lower() or "achieved" in fail_data["revisionGuidance"].lower()

    # 5. Fetch server-side cached correct answers from roadmap structure directly or simulate passing answers
    # Inspect feedback to see correct options
    correct_answers = [fb["correctOptionIndex"] for fb in fail_data["feedback"]]

    # Submit passing assessment
    pass_resp = await client.post(
        f"/api/v1/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/submit-assessment",
        json={"answers": correct_answers, "hintsUsed": 0, "solutionRevealed": False}
    )
    assert pass_resp.status_code == 200
    pass_data = pass_resp.json()
    assert pass_data["passed"] is True
    assert pass_data["status"] in ("VERIFIED_COMPLETE", "MASTERED")
    assert pass_data["scorePercentage"] == 100
    assert topic_id in pass_data["completedTopicIds"]
    assert pass_data["progressPercentage"] > 0

