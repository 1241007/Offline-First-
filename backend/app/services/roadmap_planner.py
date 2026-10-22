"""Roadmap Planner for Individualized DAG-Based Curriculum Generation"""

import math
from typing import Dict, Any, List, Optional
import uuid

from app.schemas.roadmap_websocket import SkillStatusEnum
from app.services.roadmap_knowledge_graph import get_course_knowledge_graph
from app.services.roadmap_graph_validator import RoadmapGraphValidator


class RoadmapPlanner:
    """
    Transforms learner skill inventory, goal, and weekly availability
    into a personalized, validated Directed Acyclic Graph (DAG) learning roadmap.
    """

    def __init__(self):
        self.validator = RoadmapGraphValidator()

    def plan_roadmap(
        self,
        domain: str,
        goal: str,
        target_level: str,
        weekly_hours: float,
        skill_inventory: Dict[str, Dict[str, Any]],
        catalog_courses: Optional[List[Dict[str, Any]]] = None,
    ) -> Dict[str, Any]:
        graph = get_course_knowledge_graph(domain)
        skills = graph.get("skills", {})

        effective_hours = max(1.0, float(weekly_hours or 5.0))
        effective_goal = (goal or f"Master {domain} Engineering").strip()
        effective_level = (target_level or "Beginner").strip()

        topics: List[Dict[str, Any]] = []
        phase_groups: Dict[str, List[Dict[str, Any]]] = {}

        total_study_hours = 0.0
        skipped_topic_ids: set = set()

        # Build topics from skill inventory and course knowledge structure
        for sid, skill_meta in skills.items():
            learner_skill = skill_inventory.get(sid, {})
            status_val = learner_skill.get("status", SkillStatusEnum.UNVERIFIED.value)
            is_mastered = (status_val == SkillStatusEnum.MASTERED.value)
            
            # Allow skipping foundational topics if verified mastery exists, but keep advanced capstones
            category = skill_meta.get("category", "General")
            can_skip = is_mastered and category == "Foundations"

            if can_skip:
                status_str = "SKIPPED"
                skip_reason = f"Demonstrated verified mastery in {skill_meta.get('name', sid)}."
                skipped_topic_ids.add(sid)
            else:
                skip_reason = None
                if status_val == SkillStatusEnum.WEAK.value:
                    status_str = "UNLOCKED" if not skill_meta.get("prerequisites") else "LOCKED"
                    duration_mult = 1.3  # Allocate more time for identified growth areas
                else:
                    status_str = "UNLOCKED" if not skill_meta.get("prerequisites") else "LOCKED"
                    duration_mult = 1.0

                base_hours = float(skill_meta.get("estimated_hours", 3.0)) * duration_mult
                total_study_hours += base_hours

            prereqs = list(skill_meta.get("prerequisites", []))

            topic_item = {
                "id": sid,
                "title": skill_meta.get("name", sid),
                "description": skill_meta.get("description", ""),
                "estimatedHours": float(skill_meta.get("estimated_hours", 3.0)),
                "prerequisiteTopicIds": prereqs,
                "status": status_str,
                "skipReason": skip_reason,
                "subtopics": skill_meta.get("subtopics", [f"{skill_meta.get('name', sid)} Core Concepts"]),
                "learningObjectives": [
                    f"Understand core principles of {skill_meta.get('name', sid)}",
                    f"Apply {skill_meta.get('name', sid)} in real-world scenarios",
                ],
                "passCriteria": skill_meta.get("pass_criteria", "Pass all module tests and practical exercises"),
                "category": category,
            }
            topics.append(topic_item)
            phase_groups.setdefault(category, []).append(topic_item)

        # Update unlocked status if prerequisites are completed/skipped
        for t in topics:
            if t["status"] == "LOCKED":
                all_prereqs_met = all(p in skipped_topic_ids for p in t["prerequisiteTopicIds"])
                if all_prereqs_met:
                    t["status"] = "UNLOCKED"

        # Organize into dynamic phases
        phases: List[Dict[str, Any]] = []
        phase_idx = 1
        for cat_name, cat_topics in phase_groups.items():
            cat_hours = sum(t["estimatedHours"] for t in cat_topics if t["status"] != "SKIPPED")
            phase_weeks = max(1, math.ceil(cat_hours / effective_hours)) if cat_hours > 0 else 1

            tasks = []
            for t in cat_topics:
                tasks.append({
                    "id": f"task_{t['id']}",
                    "title": t["title"],
                    "description": t["description"],
                    "instructions": f"Hands-on exercises and coding lab for {t['title']}",
                    "estimatedHours": t["estimatedHours"],
                    "resources": [f"EduNova {domain} Documentation and Interactive Labs"],
                    "completionCriteria": t["passCriteria"],
                    "isCompleted": False,
                    "dependencies": t["prerequisiteTopicIds"],
                    "topicId": t["id"],
                    "status": t["status"],
                    "subtopics": t["subtopics"],
                    "learningObjectives": t["learningObjectives"],
                    "skipReason": t["skipReason"],
                })

            phases.append({
                "title": f"Phase {phase_idx}: {cat_name}",
                "objective": f"Master {cat_name.lower()} components for {effective_goal}",
                "durationWeeks": phase_weeks,
                "topics": [t["title"] for t in cat_topics],
                "tasks": tasks,
                "activities": [f"Complete {t['title']} exercises" for t in cat_topics],
                "resources": [f"Official documentation for {domain}"],
                "milestones": [
                    {
                        "title": f"Complete Phase {phase_idx} ({cat_name}) Assessments",
                        "completionCriteria": [f"Finish all active modules in {cat_name}"],
                        "assessment": f"{cat_name} Milestone Diagnostic",
                        "passingCriteria": "Scored >= 80% on phase evaluation",
                    }
                ],
                "recommendedCourseIds": [],
            })
            phase_idx += 1

        total_weeks = sum(p["durationWeeks"] for p in phases)

        # Build realistic weekly schedule
        schedule = []
        curr_week = 1
        for p in phases:
            w_cnt = p["durationWeeks"]
            end_w = curr_week + w_cnt - 1
            lbl = f"Week {curr_week}-{end_w}" if w_cnt > 1 else f"Week {curr_week}"
            schedule.append({
                "dayOrWeek": lbl,
                "focusTopic": p["title"],
                "estimatedHours": float(effective_hours * w_cnt),
                "tasks": [t["title"] for t in p["tasks"] if t["status"] != "SKIPPED"][:4],
            })
            curr_week += w_cnt

        # Summary of strengths & gaps
        strengths = [
            s["name"] for s in skill_inventory.values()
            if s.get("status") in (SkillStatusEnum.MASTERED.value, SkillStatusEnum.KNOWN.value)
        ]
        gaps = [
            s["name"] for s in skill_inventory.values()
            if s.get("status") in (SkillStatusEnum.WEAK.value, SkillStatusEnum.PARTIAL.value)
        ]

        edges = []
        for t in topics:
            for p in t["prerequisiteTopicIds"]:
                edges.append({"from": p, "to": t["id"]})

        roadmap_dict = {
            "title": f"Personalized {domain} Roadmap: {effective_goal}",
            "domain": domain,
            "goal": effective_goal,
            "startingLevel": effective_level,
            "category": domain,
            "estimatedDuration": f"{total_weeks} weeks",
            "weeklyHours": effective_hours,
            "phases": phases,
            "topics": topics,
            "edges": edges,
            "weeklySchedule": schedule,
            "assumptions": [f"Learner can commit {effective_hours} hours per week"],
            "capstoneProject": f"{domain} Portfolio Capstone Implementation",
            "nextAction": f"Begin Phase 1: {phases[0]['title'] if phases else 'Foundations'}",
            "completedMilestones": [],
            "estimatedWeeks": total_weeks,
            "weeklyStudyHours": effective_hours,
            "structure": {
                "phases": phases,
                "topics": topics,
            },
            "schedule": schedule,
            "assessmentSummary": {
                "strengths": strengths,
                "skillGaps": gaps,
                "verifiedEvidence": [f"Skill estimate for {s['name']}: {s.get('evidence_notes', '')}" for s in skill_inventory.values() if s.get("evidence_notes")],
                "selfReportedInformation": [f"Declared Starting Level: {effective_level}", f"Target Goal: {effective_goal}"],
                "unknowns": [],
                "skillGapBreakdown": [
                    {
                        "skill": s["name"],
                        "status": "verified_mastery" if s.get("status") == SkillStatusEnum.MASTERED.value else "needs_learning",
                        "source": s.get("source", "diagnostic_verified"),
                        "confidence": float(s.get("confidence", 0.8)),
                        "rationale": s.get("evidence_notes", "Diagnostic observation"),
                    }
                    for s in skill_inventory.values()
                ],
                "curriculumRationale": f"Tailored curriculum prioritizing growth areas in {domain} based on authentic diagnostic outcomes.",
            },
        }

        # Deterministically validate before returning
        is_valid, errors = self.validator.validate_roadmap(roadmap_dict)
        if not is_valid:
            raise ValueError(f"Generated roadmap failed structural DAG validation: {'; '.join(errors)}")

        return roadmap_dict
