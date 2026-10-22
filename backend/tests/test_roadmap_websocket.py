"""Tests for Roadmap WebSocket Protocol and Session Management"""

import pytest
import json
from starlette.testclient import TestClient

from app.main import app
from app.core.security import create_access_token


from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models import User, UserProfile


@pytest.fixture
async def setup_ws_learner(db_session: AsyncSession):
    user = await db_session.scalar(select(User).where(User.id == "ws-test-user"))
    if not user:
        user = User(
            id="ws-test-user",
            email="ws-test@edunova.app",
            mobile="9876543211",
            hashed_password="hashed_pw",
            is_active=True,
        )
        db_session.add(user)
        profile = UserProfile(
            id="ws-test-profile",
            user_id="ws-test-user",
            full_name="WS Tester",
            email="ws-test@edunova.app",
            level="Intermediate",
            education_mode="general",
            interests="Python",
        )
        db_session.add(profile)
        await db_session.commit()


@pytest.fixture
def test_user_token():
    return create_access_token(user_id="ws-test-user", email="ws-test@edunova.app")


@pytest.fixture
def ws_client(db_session):
    from app.core.database import get_db
    async def override_get_db():
        yield db_session
    app.dependency_overrides[get_db] = override_get_db
    client = TestClient(app)
    yield client
    app.dependency_overrides.clear()


def test_roadmap_websocket_unauthorized():
    client = TestClient(app)
    with client.websocket_connect("/api/v1/roadmaps/ws/personalized") as ws:
        msg = ws.receive_json()
        assert msg["event"] == "error"
        assert msg["payload"]["code"] == "UNAUTHORIZED"


def test_roadmap_websocket_lifecycle(setup_ws_learner, test_user_token, ws_client):
    with ws_client.websocket_connect(f"/api/v1/roadmaps/ws/personalized?token={test_user_token}") as ws:
        # 1. Ping
        ws.send_json({"event": "ping", "payload": {"timestamp": "2026-10-22T00:00:00Z"}})
        pong = ws.receive_json()
        assert pong["event"] == "pong"

        # 2. Start Roadmap
        ws.send_json({
            "event": "start_roadmap",
            "payload": {
                "domain": "Python",
                "goal": "FastAPI Microservices",
                "target_level": "Intermediate",
                "weekly_hours": 6.0
            }
        })
        ready = ws.receive_json()
        assert ready["event"] == "session_ready"
        session_id = ready["session_id"]
        assert session_id is not None
        assert ready["payload"]["domain"] == "Python"
        assert ready["payload"]["currentQuestion"] is not None

        curr_q = ready["payload"]["currentQuestion"]

        # 3. Submit Answer
        ws.send_json({
            "event": "submit_answer",
            "session_id": session_id,
            "payload": {
                "message_id": "msg_001",
                "question_id": curr_q["question_id"],
                "quiz_selected_index": curr_q.get("correct_index", 0),
                "answer_text": "Correct answer"
            }
        })
        resp = ws.receive_json()
        assert resp["event"] in ("next_question", "session_complete")
        assert "skillInventory" in resp["payload"]

        # 4. Generate Roadmap
        ws.send_json({
            "event": "generate_roadmap",
            "session_id": session_id,
            "payload": {}
        })

        gen_start = ws.receive_json()
        assert gen_start["event"] == "roadmap_generation_started"

        gen_ready = ws.receive_json()
        assert gen_ready["event"] == "roadmap_ready"
        roadmap = gen_ready["payload"]["roadmap"]
        assert roadmap["id"] is not None
        assert roadmap["domain"] == "Python"
        assert len(roadmap["structure"]["topics"]) > 0
        assert len(roadmap["structure"]["phases"]) > 0


def test_roadmap_websocket_handshake_header_auth_and_deduplication(setup_ws_learner, test_user_token, ws_client):
    # Connect using HTTP Upgrade Authorization header (NO token in query string)
    headers = {"Authorization": f"Bearer {test_user_token}"}
    with ws_client.websocket_connect("/api/v1/roadmaps/ws/personalized", headers=headers) as ws:
        # Start Roadmap
        ws.send_json({
            "event": "start_roadmap",
            "payload": {
                "domain": "Python",
                "goal": "FastAPI Master",
                "target_level": "Intermediate",
                "weekly_hours": 8.0
            }
        })
        ready = ws.receive_json()
        assert ready["event"] == "session_ready"
        session_id = ready["session_id"]
        curr_q = ready["payload"]["currentQuestion"]

        # Submit Answer with message_id "msg_100"
        ws.send_json({
            "event": "submit_answer",
            "session_id": session_id,
            "payload": {
                "message_id": "msg_100",
                "question_id": curr_q["question_id"],
                "quiz_selected_index": curr_q.get("correct_index", 0),
                "answer_text": "Correct answer"
            }
        })
        resp1 = ws.receive_json()
        assert resp1["event"] in ("next_question", "session_complete")

        # Submit Duplicate Answer with SAME message_id "msg_100"
        ws.send_json({
            "event": "submit_answer",
            "session_id": session_id,
            "payload": {
                "message_id": "msg_100",
                "question_id": curr_q["question_id"],
                "quiz_selected_index": curr_q.get("correct_index", 0),
                "answer_text": "Correct answer"
            }
        })
        resp2 = ws.receive_json()
        # Should be deduplicated into ASSESSMENT_UPDATE without double scoring
        assert resp2["event"] == "assessment_update"

        # Resume session
        ws.send_json({
            "event": "resume_session",
            "session_id": session_id,
            "payload": {}
        })
        resumed = ws.receive_json()
        assert resumed["event"] == "session_restored"
        assert resumed["session_id"] == session_id


def test_roadmap_websocket_session_ownership_enforcement(setup_ws_learner, test_user_token, ws_client):
    other_token = create_access_token(user_id="other-intruder-user", email="other@edunova.app")

    # 1. ws-test-user creates a session
    with ws_client.websocket_connect("/api/v1/roadmaps/ws/personalized", headers={"Authorization": f"Bearer {test_user_token}"}) as ws_legit:
        ws_legit.send_json({
            "event": "start_roadmap",
            "payload": {"domain": "Python", "goal": "Security", "weekly_hours": 5.0}
        })
        ready = ws_legit.receive_json()
        legit_session_id = ready["session_id"]

    # 2. other-intruder-user attempts to hijack and submit answers for legit_session_id
    with ws_client.websocket_connect("/api/v1/roadmaps/ws/personalized", headers={"Authorization": f"Bearer {other_token}"}) as ws_intruder:
        ws_intruder.send_json({
            "event": "submit_answer",
            "session_id": legit_session_id,
            "payload": {
                "message_id": "hijack_01",
                "question_id": "any",
                "quiz_selected_index": 0
            }
        })
        err = ws_intruder.receive_json()
        assert err["event"] == "error"
        assert "unauthorized" in err["payload"]["message"].lower() or "not found" in err["payload"]["message"].lower()


