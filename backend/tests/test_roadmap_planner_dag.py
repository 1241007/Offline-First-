"""Unit tests for AdaptiveAssessmentEngine and RoadmapPlanner DAG Generation"""

import pytest
from app.services.adaptive_assessment_engine import AdaptiveAssessmentEngine
from app.services.roadmap_planner import RoadmapPlanner
from app.services.roadmap_graph_validator import RoadmapGraphValidator
from app.schemas.roadmap_websocket import SkillStatusEnum


def test_adaptive_assessment_question_influence():
    engine = AdaptiveAssessmentEngine()
    context = {"name": "Alice", "stated_level": "Beginner", "verified_records": []}
    inv = engine.initialize_skill_inventory(context, domain="Python")

    # Verify initial inventory has unverified skills
    assert len(inv) > 0
    assert any(s["status"] == SkillStatusEnum.UNVERIFIED.value for s in inv.values())

    # Select first question
    q1 = engine.select_next_question(domain="Python", skill_inventory=inv, asked_question_ids=[])
    assert q1 is not None

    # Evaluate wrong answer
    is_correct, feedback, updated = engine.evaluate_response(
        domain="Python",
        question_data=q1,
        learner_answer="wrong answer",
        quiz_selected_index=999,
        skill_inventory=inv,
    )
    assert not is_correct
    assert updated["status"] == SkillStatusEnum.WEAK.value
    assert updated["confidence"] > 0.0

    # Select next question; should not repeat q1
    q2 = engine.select_next_question(
        domain="Python",
        skill_inventory=inv,
        asked_question_ids=[q1["question_id"]],
    )
    assert q2 is not None
    assert q2["question_id"] != q1["question_id"]


def test_roadmap_planner_dag_invariants():
    planner = RoadmapPlanner()
    validator = RoadmapGraphValidator()

    skill_inventory = {
        "py_syntax": {
            "name": "Python Syntax",
            "status": SkillStatusEnum.MASTERED.value,
            "category": "Foundations",
            "estimated_hours": 3.0,
            "prerequisites": [],
        },
        "py_control_flow": {
            "name": "Control Flow",
            "status": SkillStatusEnum.WEAK.value,
            "category": "Foundations",
            "estimated_hours": 3.5,
            "prerequisites": ["py_syntax"],
        },
        "py_oop": {
            "name": "OOP",
            "status": SkillStatusEnum.UNVERIFIED.value,
            "category": "Architecture",
            "estimated_hours": 5.0,
            "prerequisites": ["py_control_flow"],
        },
    }

    roadmap = planner.plan_roadmap(
        domain="Python",
        goal="Build Distributed Async Systems",
        target_level="Intermediate",
        weekly_hours=5.0,
        skill_inventory=skill_inventory,
    )

    # Validate output is a valid DAG
    is_valid, errors = validator.validate_roadmap(roadmap)
    assert is_valid, f"Validation errors: {errors}"

    # Mastered foundational topic should be skipped with explicit reason
    topics = roadmap["structure"]["topics"]
    syntax_topic = next((t for t in topics if t["id"] == "py_syntax"), None)
    assert syntax_topic is not None
    assert syntax_topic["status"] == "SKIPPED"
    assert syntax_topic["skipReason"] is not None and len(syntax_topic["skipReason"]) > 0

    # Weak topic should be unlocked and scheduled
    cf_topic = next((t for t in topics if t["id"] == "py_control_flow"), None)
    assert cf_topic is not None
    assert cf_topic["status"] == "UNLOCKED"
