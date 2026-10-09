"""Deterministic Quality Rubric Evaluation for 4 Representative Learner Fixtures"""

import pytest
from sqlalchemy.ext.asyncio import AsyncSession
from app.services.personalized_roadmap_service import PersonalizedRoadmapService


@pytest.mark.asyncio
async def test_quality_fixture_1_beginner_python_data_analysis(db_session: AsyncSession):
    """Fixture 1: Beginner Python for Data Analysis (Goal: Data Analysis, Level: Beginner, 5 hrs/week)."""
    service = PersonalizedRoadmapService(db_session)
    profile = {
        "goal": "Python for Data Analysis",
        "starting_level": "Beginner",
        "weekly_hours": 5.0,
        "verified_evidence": [],
        "strengths": ["Basic Computer Literacy"],
        "skill_gaps": ["Pandas", "NumPy", "Data Cleaning"],
    }
    gap_model = service._step2_build_skill_gap_model(profile)
    phases = service._step3_design_curriculum_phases(profile, gap_model, catalog=[])

    # Evaluate against rubric
    assert len(phases) >= 2
    assert "Foundations" in phases[0]["title"]
    assert any("Syntax" in t["title"] or "Setup" in t["title"] for t in phases[0]["tasks"])
    assert phases[0]["durationWeeks"] > 0
    assert phases[1]["durationWeeks"] > 0
    # Workload plausibility
    total_task_hours = sum(t["estimatedHours"] for p in phases for t in p["tasks"])
    assert 5.0 <= total_task_hours <= 40.0


@pytest.mark.asyncio
async def test_quality_fixture_2_advanced_python_data_analysis(db_session: AsyncSession):
    """Fixture 2: Advanced Python for Data Analysis (Goal: Data Analysis, Level: Advanced, 8 hrs/week).
    Verifies absence of redundant beginner syntax and focus on performance / architecture."""
    service = PersonalizedRoadmapService(db_session)
    profile = {
        "goal": "Advanced Python Data Engineering",
        "starting_level": "Advanced",
        "weekly_hours": 8.0,
        "verified_evidence": ["Completed Python Fundamentals"],
        "strengths": ["Pandas Vectorization", "OOP"],
        "skill_gaps": ["Out-of-Core Processing", "Polars", "Distributed Query Optimization"],
    }
    gap_model = service._step2_build_skill_gap_model(profile)
    phases = service._step3_design_curriculum_phases(profile, gap_model, catalog=[])

    # Rubric: Advanced plan must NOT contain beginner setup or basic syntax tasks
    p1_tasks_titles = [t["title"].lower() for t in phases[0]["tasks"]]
    assert not any("basic syntax" in t for t in p1_tasks_titles)
    assert not any("install python" in t for t in p1_tasks_titles)
    # Must focus on Architecture, Profiling, or Concurrency
    assert any("profile" in t or "benchmark" in t or "concurrency" in t or "architecture" in t for t in p1_tasks_titles)


@pytest.mark.asyncio
async def test_quality_fixture_3_android_kotlin_compose(db_session: AsyncSession):
    """Fixture 3: Modern Android Development with Kotlin & Compose (Level: Intermediate, 6 hrs/week)."""
    service = PersonalizedRoadmapService(db_session)
    profile = {
        "goal": "Android Jetpack Compose & Offline-First Apps",
        "starting_level": "Intermediate",
        "weekly_hours": 6.0,
        "verified_evidence": [],
        "strengths": ["Kotlin Basics", "OOP"],
        "skill_gaps": ["Room SQLite", "Offline Sync Flow", "LaunchedEffect"],
    }
    gap_model = service._step2_build_skill_gap_model(profile)
    phases = service._step3_design_curriculum_phases(profile, gap_model, catalog=[])

    assert len(phases) == 2
    assert "Persistence" in phases[1]["tasks"][0]["title"] or "Integration" in phases[1]["title"]
    assert phases[1]["milestones"][0]["passingCriteria"] != ""


@pytest.mark.asyncio
async def test_quality_fixture_4_machine_learning_interview(db_session: AsyncSession):
    """Fixture 4: Machine Learning Interview Preparation (Level: Advanced, 10 hrs/week)."""
    service = PersonalizedRoadmapService(db_session)
    profile = {
        "goal": "Machine Learning Engineering Interviews",
        "starting_level": "Advanced",
        "weekly_hours": 10.0,
        "verified_evidence": ["Completed Linear Algebra"],
        "strengths": ["PyTorch Basics", "Loss Functions"],
        "skill_gaps": ["Transformer Attention Optimization", "Distributed Training"],
    }
    gap_model = service._step2_build_skill_gap_model(profile)
    phases = service._step3_design_curriculum_phases(profile, gap_model, catalog=[])

    assert len(phases) >= 2
    # Verify measurable capstone project
    assert phases[1]["milestones"][0]["completionCriteria"] is not None
    assert len(phases[1]["milestones"][0]["completionCriteria"]) >= 1
