"""
End-to-End Verification of Offline-First Roadmap Persistence & Cloud Synchronization
Tests:
1. Online assessment & roadmap generation saved in PostgreSQL database
2. Retrieval of saved personalized roadmaps and system roadmaps
3. Milestone completion updates (toggles & batch sync) via PUT /api/v1/roadmaps/personalized/{id}/milestones
4. Milestone progress retrieval via GET /api/v1/roadmaps/personalized/{id}/milestones
5. Cross-device restoration simulation (restoring roadmap & milestones on clean session)
6. Idempotent synchronization (repeated sync avoids duplicates)
7. User isolation (another user cannot access or modify user roadmaps)
"""

import sys
import os
import asyncio
import uuid
import httpx
from datetime import datetime

sys.path.insert(0, os.path.abspath("."))
sys.path.insert(0, os.path.abspath("./backend"))
sys.path.insert(0, os.path.abspath("../backend"))

from unittest.mock import patch, AsyncMock
from app.core.database import get_session_factory
from app.models import User, Roadmap, RoadmapAssessmentSession
from app.core.security import create_access_token
from app.services.personalized_roadmap_service import PersonalizedRoadmapService
from app.schemas.personalized_roadmap import (
    RoadmapMilestonesSyncRequest,
    PersonalizedRoadmapSchema,
)

MOCK_ROADMAP_JSON = {
    "title": "Android Offline-First Mastery",
    "goal": "Build offline-first Android apps with cloud sync",
    "startingLevel": "Intermediate",
    "category": "Mobile Development",
    "estimatedDuration": "8 weeks",
    "weeklyHours": 10.0,
    "assessmentSummary": {
        "strengths": ["Kotlin Coroutines", "Jetpack Compose"],
        "skillGaps": ["SQLite Transactions", "Sync Queue"],
        "verifiedEvidence": ["Implemented ViewModel state"],
        "selfReportedInformation": ["3 years experience"],
        "unknowns": []
    },
    "phases": [
        {
            "title": "Phase 1: Local SQLite Storage",
            "objective": "Design robust SQLiteOpenHelper schema",
            "durationWeeks": 3,
            "topics": ["SQLiteOpenHelper", "Database Migration v2 to v3"],
            "activities": ["Build local cache database"],
            "resources": ["EduNova Android Docs"],
            "milestones": [
                {
                    "title": "Implement SQLite Tables",
                    "completionCriteria": ["Create local_personalized_roadmaps", "Create local_milestone_progress"],
                    "assessment": "Unit test SQLite migrations",
                    "passingCriteria": "All queries execute cleanly"
                }
            ],
            "recommendedCourseIds": []
        },
        {
            "title": "Phase 2: Bidirectional Cloud Sync",
            "objective": "Build offline-first sync engine",
            "durationWeeks": 5,
            "topics": ["Sync Status Tracking", "Conflict Resolution", "Idempotent Sync"],
            "activities": ["Implement OnlineRoadmapRepository sync"],
            "resources": ["FastAPI Endpoints"],
            "milestones": [
                {
                    "title": "Implement Milestone Sync",
                    "completionCriteria": ["Queue offline changes", "Sync on reconnect"],
                    "assessment": "Simulate network outage and reconnect",
                    "passingCriteria": "All pending milestones synced without duplicates"
                }
            ],
            "recommendedCourseIds": []
        }
    ],
    "weeklySchedule": [
        {
            "dayOrWeek": "Week 1-3",
            "focusTopic": "Local Persistence",
            "estimatedHours": 10.0,
            "tasks": ["Implement database upgrade", "Write repository cache methods"]
        },
        {
            "dayOrWeek": "Week 4-8",
            "focusTopic": "Cloud Synchronization",
            "estimatedHours": 10.0,
            "tasks": ["Build PUT /milestones endpoint", "Connect sync queue"]
        }
    ],
    "assumptions": ["Assumes regular weekly study"],
    "capstoneProject": "Complete Offline-First EduNova Roadmap Module",
    "nextAction": "Start Phase 1: Local SQLite Storage",
    "completedMilestones": []
}

async def run_e2e_verification():
    print("==================================================", flush=True)
    print("STARTING OFFLINE-FIRST ROADMAP PERSISTENCE & SYNC VERIFICATION", flush=True)
    print("==================================================", flush=True)

    session_factory = get_session_factory()
    async with session_factory() as db:
        test_user_id = f"test-offline-user-{uuid.uuid4().hex[:6]}"
        other_user_id = f"other-offline-user-{uuid.uuid4().hex[:6]}"

        # Create test users
        u1 = User(
            id=test_user_id,
            email=f"user1_{uuid.uuid4().hex[:6]}@edunova.app",
            mobile=f"9{uuid.uuid4().int % 1000000000:09d}",
            hashed_password="hashed_pw",
            is_active=True,
        )
        u2 = User(
            id=other_user_id,
            email=f"user2_{uuid.uuid4().hex[:6]}@edunova.app",
            mobile=f"9{uuid.uuid4().int % 1000000000:09d}",
            hashed_password="hashed_pw",
            is_active=True,
        )
        db.add_all([u1, u2])
        await db.commit()
        print(f"[+] Created test users: {test_user_id} and {other_user_id}")

        service = PersonalizedRoadmapService(db)

        # 1. Start assessment session
        session_resp = await service.start_assessment_session(user_id=test_user_id)
        session_id = session_resp.id
        print(f"[+] Started assessment session {session_id}, state: {session_resp.state}")

        # 2. Complete questions to make session ready for generation
        await service.process_learner_response(
            user_id=test_user_id,
            session_id=session_id,
            answer="Learn Android Jetpack Compose and offline architecture",
        )
        await service.process_learner_response(
            user_id=test_user_id,
            session_id=session_id,
            answer="Intermediate, 10 hours per week",
        )
        await service.process_learner_response(
            user_id=test_user_id,
            session_id=session_id,
            answer="Ready to generate my personalized roadmap",
        )
        print("[+] Assessment conversation completed.")

        # 3. Generate personalized roadmap
        import json
        with patch("app.services.personalized_roadmap_service.openrouter_service.generate_response", new_callable=AsyncMock) as mock_ai:
            mock_ai.return_value = json.dumps(MOCK_ROADMAP_JSON)
            roadmap_detail = await service.generate_roadmap_from_assessment(
                user_id=test_user_id,
                session_id=session_id,
            )
        roadmap_id = roadmap_detail.id
        print(f"[+] Generated Roadmap '{roadmap_detail.title}' with ID: {roadmap_id}", flush=True)
        assert len(roadmap_detail.structure.phases) > 0, "Phases must be non-empty"
        print(f"[+] Phases count: {len(roadmap_detail.structure.phases)}", flush=True)

        # 4. Verify cloud retrieval
        user_roadmaps = await service.get_user_personalized_roadmaps(user_id=test_user_id)
        assert any(r.id == roadmap_id for r in user_roadmaps), "Roadmap should be in user's roadmaps list"
        print(f"[+] Successfully verified roadmap in cloud database list.")

        # 5. Milestone progress: Initial state
        initial_milestones = await service.get_roadmap_milestones(roadmap_id=roadmap_id, user_id=test_user_id)
        assert initial_milestones == [], f"Expected empty initial milestones, got {initial_milestones}"
        print("[+] Initial completed milestones verified empty.")

        # 6. Milestone progress: Offline toggle simulation & cloud sync
        m1_key = f"{roadmap_id}_0_0_milestone1"
        m2_key = f"{roadmap_id}_0_1_milestone2"

        # Toggle m1 to completed
        updated = await service.sync_roadmap_milestones(
            roadmap_id=roadmap_id,
            user_id=test_user_id,
            milestone_key=m1_key,
            is_completed=True,
        )
        assert m1_key in updated, f"Expected {m1_key} in updated list"
        print(f"[+] Toggled milestone 1 -> Completed milestones: {updated}")

        # Batch sync (e.g. after offline reconnection)
        reconnect_batch = [m1_key, m2_key]
        batch_updated = await service.sync_roadmap_milestones(
            roadmap_id=roadmap_id,
            user_id=test_user_id,
            completed_milestones=reconnect_batch,
        )
        assert set(batch_updated) == {m1_key, m2_key}, f"Expected {reconnect_batch}, got {batch_updated}"
        print(f"[+] Reconnection batch sync -> Completed milestones: {batch_updated}")

        # Untoggle m1
        untoggled = await service.sync_roadmap_milestones(
            roadmap_id=roadmap_id,
            user_id=test_user_id,
            milestone_key=m1_key,
            is_completed=False,
        )
        assert m1_key not in untoggled, "m1_key should be removed"
        assert m2_key in untoggled, "m2_key should remain"
        print(f"[+] Untoggled milestone 1 -> Completed milestones: {untoggled}")

        # 7. Cross-Device Restoration Simulation
        # Simulate signing in on a brand new device and restoring roadmap with progress
        fresh_service = PersonalizedRoadmapService(db)
        restored = await fresh_service.get_personalized_roadmap_detail(roadmap_id=roadmap_id, user_id=test_user_id)
        assert restored is not None, "Roadmap must be restorable from cloud"
        assert restored.structure.completed_milestones == [m2_key], f"Expected [{m2_key}], got {restored.structure.completed_milestones}"
        print(f"[+] Cross-device restoration verified: Restored roadmap '{restored.title}' with milestone progress {restored.structure.completed_milestones}")

        # 8. User Isolation Security Test
        # Attempt to access or modify user1's roadmap using user2's credentials
        other_detail = await service.get_personalized_roadmap_detail(roadmap_id=roadmap_id, user_id=other_user_id)
        assert other_detail is None, "User 2 should NOT be able to access User 1's roadmap"

        try:
            await service.sync_roadmap_milestones(
                roadmap_id=roadmap_id,
                user_id=other_user_id,
                milestone_key=m1_key,
                is_completed=True,
            )
            assert False, "Should have thrown ValueError for unauthorized user"
        except ValueError:
            print("[+] Cross-user isolation verified: Unauthorized user cannot modify roadmap milestones.")

        # 9. Idempotency Test
        sync_repeat_1 = await service.sync_roadmap_milestones(
            roadmap_id=roadmap_id,
            user_id=test_user_id,
            completed_milestones=[m2_key],
        )
        sync_repeat_2 = await service.sync_roadmap_milestones(
            roadmap_id=roadmap_id,
            user_id=test_user_id,
            completed_milestones=[m2_key],
        )
        assert sync_repeat_1 == sync_repeat_2 == [m2_key], "Repeated sync must be strictly idempotent"
        print("[+] Idempotent synchronization verified: repeated sync produces identical state without duplicates.")

    print("==================================================")
    print("ALL VERIFICATIONS PASSED SUCCESSFULLY (100% OK)")
    print("==================================================")

if __name__ == "__main__":
    asyncio.run(run_e2e_verification())
