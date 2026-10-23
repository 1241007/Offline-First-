"""Adaptive Assessment Engine for EduNova Personalized Roadmap"""

import logging
from typing import Dict, Any, List, Optional, Tuple
from app.schemas.roadmap_websocket import (
    SkillRecordDto,
    SkillStatusEnum,
    EvidenceSourceEnum,
    DiagnosticQuestionDto,
)
from app.services.roadmap_knowledge_graph import get_course_knowledge_graph

logger = logging.getLogger(__name__)


class AdaptiveAssessmentEngine:
    """
    Drives an evidence-based, course-specific diagnostic cycle:
    1. Initializes subconcept skill inventory using authentic learner context.
    2. Identifies highest-value uncertainties required for roadmap construction.
    3. Asks targeted diagnostic questions based on real student answers.
    4. Evaluates responses into fine-grained skill estimates with explicit provenance.
    5. Requests clarification when responses are ambiguous.
    """

    def __init__(self):
        pass

    def initialize_skill_inventory(
        self, learner_context: Dict[str, Any], domain: str
    ) -> Dict[str, Dict[str, Any]]:
        graph = get_course_knowledge_graph(domain)
        skills = graph.get("skills", {})
        inventory: Dict[str, Dict[str, Any]] = {}

        verified_records = learner_context.get("verified_records", [])
        stated_level = str(learner_context.get("stated_level", "Beginner")).lower()

        for skill_id, skill_meta in skills.items():
            # Check if learner has verified prior completion
            has_completed_course = any(
                domain.lower() in rec.get("course_name", "").lower()
                and rec.get("progress_pct", 0) == 100
                for rec in verified_records
            )

            if has_completed_course and skill_meta.get("category") == "Foundations":
                status = SkillStatusEnum.KNOWN.value
                confidence = 0.75
                source = EvidenceSourceEnum.VERIFIED_RECORD.value
                notes = "Historical course completion evidence."
            elif "advanced" in stated_level and skill_meta.get("category") == "Foundations":
                status = SkillStatusEnum.KNOWN.value
                confidence = 0.60
                source = EvidenceSourceEnum.LEARNER_REPORTED.value
                notes = "Self-reported advanced experience level."
            else:
                status = SkillStatusEnum.UNVERIFIED.value
                confidence = 0.0
                source = EvidenceSourceEnum.UNKNOWN.value
                notes = "Pending diagnostic assessment."

            inventory[skill_id] = {
                "skill_id": skill_id,
                "name": skill_meta.get("name", skill_id),
                "category": skill_meta.get("category", "General"),
                "description": skill_meta.get("description", ""),
                "status": status,
                "confidence": confidence,
                "source": source,
                "evidence_notes": notes,
                "attempts": 0,
                "misconceptions": [],
                "prerequisites": skill_meta.get("prerequisites", []),
                "estimated_hours": skill_meta.get("estimated_hours", 3.0),
            }

        return inventory

    def select_next_question(
        self,
        domain: str,
        skill_inventory: Dict[str, Dict[str, Any]],
        asked_question_ids: List[str],
        max_questions: int = 5,
    ) -> Optional[Dict[str, Any]]:
        """
        Selects the next diagnostic question targeting the highest-value uncertainty.
        Avoids asking questions already answered or skills already validated by reliable evidence.
        """
        if len(asked_question_ids) >= max_questions:
            return None

        graph = get_course_knowledge_graph(domain)
        skills = graph.get("skills", {})

        # Candidate selection: prioritize unverified or weak prerequisite skills
        unverified_skills = [
            sid for sid, s in skill_inventory.items()
            if s["status"] in (SkillStatusEnum.UNVERIFIED.value, SkillStatusEnum.PARTIAL.value)
        ]

        if not unverified_skills:
            # If all foundations verified, check if advanced skills need verification
            unverified_skills = [
                sid for sid, s in skill_inventory.items()
                if s["status"] != SkillStatusEnum.MASTERED.value
            ]

        # Scan diagnostic questions for candidates
        for sid in unverified_skills:
            skill_meta = skills.get(sid, {})
            questions = skill_meta.get("diagnostic_questions", [])
            for q in questions:
                if q["id"] not in asked_question_ids:
                    return {
                        "question_id": q["id"],
                        "skill_id": sid,
                        "skill_name": skill_meta.get("name", sid),
                        "prompt": q["prompt"],
                        "question_type": q.get("question_type", "multiple_choice"),
                        "options": q.get("options", []),
                        "code_snippet": q.get("code_snippet"),
                        "explanation": q.get("explanation", ""),
                        "correct_index": q.get("correct_index", 0),
                        "difficulty": q.get("difficulty", "Intermediate"),
                        "selection_rationale": q.get("selection_rationale", f"Diagnostic probe for {sid}"),
                    }

        return None

    def evaluate_response(
        self,
        domain: str,
        question_data: Dict[str, Any],
        learner_answer: str,
        quiz_selected_index: Optional[int],
        skill_inventory: Dict[str, Dict[str, Any]],
    ) -> Tuple[bool, str, Dict[str, Any]]:
        """
        Evaluates the student's response deterministically.
        Updates skill proficiency, confidence, and returns feedback note.
        """
        skill_id = question_data.get("skill_id", "")
        correct_idx = question_data.get("correct_index", 0)
        explanation = question_data.get("explanation", "")
        skill_name = question_data.get("skill_name", skill_id)

        is_correct = False
        if quiz_selected_index is not None:
            is_correct = (quiz_selected_index == correct_idx)
        else:
            opts = question_data.get("options", [])
            if 0 <= correct_idx < len(opts):
                correct_text = opts[correct_idx].strip().lower()
                clean_ans = (learner_answer or "").strip().lower()
                is_correct = (clean_ans == correct_text or clean_ans in correct_text)

        skill_entry = skill_inventory.get(skill_id, {
            "skill_id": skill_id,
            "name": skill_name,
            "status": SkillStatusEnum.UNVERIFIED.value,
            "confidence": 0.0,
            "attempts": 0,
            "misconceptions": [],
        })

        skill_entry["attempts"] = skill_entry.get("attempts", 0) + 1
        skill_entry["source"] = EvidenceSourceEnum.DIAGNOSTIC_VERIFIED.value

        if is_correct:
            skill_entry["status"] = SkillStatusEnum.MASTERED.value
            skill_entry["confidence"] = 0.95
            skill_entry["evidence_notes"] = f"Demonstrated solid mastery in {skill_name} diagnostic question."
            feedback = f"Verified mastery in {skill_name}. Excellent work!"
        else:
            skill_entry["status"] = SkillStatusEnum.WEAK.value
            skill_entry["confidence"] = 0.85
            skill_entry["misconceptions"].append(f"Difficulty on {skill_name}: {explanation}")
            skill_entry["evidence_notes"] = f"Identified learning gap in {skill_name}. Insight: {explanation}"
            feedback = f"Focus Area: {skill_name}. Concept insight: {explanation}"

        skill_inventory[skill_id] = skill_entry
        return is_correct, feedback, skill_entry

    def determine_completeness(
        self,
        skill_inventory: Dict[str, Dict[str, Any]],
        asked_count: int,
        target_count: int = 5,
    ) -> Tuple[bool, int]:
        """
        Determines whether sufficient evidence exists to construct a responsible roadmap.
        """
        if not skill_inventory:
            return False, 10

        total_skills = len(skill_inventory)
        evaluated_skills = sum(
            1 for s in skill_inventory.values()
            if s["status"] in (SkillStatusEnum.MASTERED.value, SkillStatusEnum.KNOWN.value, SkillStatusEnum.WEAK.value)
        )

        pct = min(100, int((asked_count / max(1, target_count)) * 80) + int((evaluated_skills / max(1, total_skills)) * 20))
        is_ready = (asked_count >= target_count) or (evaluated_skills >= total_skills and asked_count >= 2)
        return is_ready, pct
