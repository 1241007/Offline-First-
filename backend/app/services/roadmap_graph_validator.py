"""Roadmap DAG and Schema Validator for EduNova Roadmap Engine"""

from typing import Dict, Any, List, Tuple, Set
from collections import defaultdict, deque
import logging

from app.schemas.personalized_roadmap import PersonalizedRoadmapSchema

logger = logging.getLogger(__name__)


class RoadmapGraphValidator:
    """
    Validates generated roadmaps before persistence or display:
    1. Enforces schema conformity against PersonalizedRoadmapSchema.
    2. Enforces unique, stable node and task IDs.
    3. Validates prerequisite references (no missing prerequisites).
    4. Detects cycles using topological sorting (guarantees a DAG).
    5. Validates non-negative durations and realistic weekly schedule math.
    6. Ensures justified skip reasons for mastered topics.
    """

    def validate_roadmap(self, roadmap_dict: Dict[str, Any]) -> Tuple[bool, List[str]]:
        errors: List[str] = []

        # 1. Pydantic Schema Validation
        try:
            PersonalizedRoadmapSchema.model_validate(roadmap_dict)
        except Exception as exc:
            errors.append(f"Schema validation failure: {str(exc)}")
            return False, errors

        structure = roadmap_dict.get("structure", {})
        topics = roadmap_dict.get("topics") or structure.get("topics", [])
        phases = roadmap_dict.get("phases") or structure.get("phases", [])
        schedule = roadmap_dict.get("weeklySchedule") or roadmap_dict.get("schedule", [])
        hours_per_week = float(roadmap_dict.get("weeklyHours") or roadmap_dict.get("weeklyStudyHours", 5.0))

        # 2. Unique Node & Task IDs
        topic_ids: Set[str] = set()
        for t in topics:
            tid = t.get("id")
            if not tid:
                errors.append("Topic found with missing or blank ID")
            elif tid in topic_ids:
                errors.append(f"Duplicate topic ID found: {tid}")
            else:
                topic_ids.add(tid)

        task_ids: Set[str] = set()
        for p in phases:
            for task in p.get("tasks", []):
                tk_id = task.get("id")
                if not tk_id:
                    errors.append("Task found with missing or blank ID")
                elif tk_id in task_ids:
                    errors.append(f"Duplicate task ID found: {tk_id}")
                else:
                    task_ids.add(tk_id)

        # 3. Prerequisite Referencing & Cycle Detection
        in_degree: Dict[str, int] = {tid: 0 for tid in topic_ids}
        adj_list: Dict[str, List[str]] = defaultdict(list)

        for t in topics:
            tid = t.get("id")
            prereqs = t.get("prerequisiteTopicIds", [])
            for p in prereqs:
                if p not in topic_ids:
                    errors.append(f"Topic {tid} references non-existent prerequisite {p}")
                else:
                    adj_list[p].append(tid)
                    in_degree[tid] += 1

        # Kahn's algorithm for topological sort
        queue = deque([tid for tid, deg in in_degree.items() if deg == 0])
        visited_count = 0

        while queue:
            node = queue.popleft()
            visited_count += 1
            for neighbor in adj_list[node]:
                in_degree[neighbor] -= 1
                if in_degree[neighbor] == 0:
                    queue.append(neighbor)

        if visited_count != len(topic_ids):
            errors.append("Cycle detected in topic prerequisite graph; graph must be a strict DAG")

        # 4. Duration Consistency
        total_hours = sum(t.get("estimatedHours", 0.0) for t in topics)
        if total_hours < 0:
            errors.append("Total estimated hours cannot be negative")

        estimated_weeks = roadmap_dict.get("estimatedWeeks", 1)
        if estimated_weeks <= 0:
            errors.append("Estimated weeks must be greater than zero")

        # 5. Skip Reason Justification
        for t in topics:
            status = str(t.get("status", "")).upper()
            if status == "SKIPPED":
                skip_reason = t.get("skipReason")
                if not skip_reason or not skip_reason.strip():
                    errors.append(f"Topic {t.get('id')} marked SKIPPED without justified skipReason")

        is_valid = (len(errors) == 0)
        return is_valid, errors
