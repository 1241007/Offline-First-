"""WebSocket API endpoint for EduNova Adaptive Personalized Roadmap Agent"""

import json
import logging
from typing import Optional
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_session_factory
from app.core.config import settings
from app.core.security import decode_access_token
from app.schemas.roadmap_websocket import (
    RoadmapClientEventType,
    RoadmapServerEventType,
    RoadmapServerEvent,
)
from app.services.roadmap_session_manager import RoadmapSessionManager

logger = logging.getLogger(__name__)

router = APIRouter(tags=["roadmap-websocket"])


def _extract_user_id(websocket: WebSocket, token: Optional[str], user_id_param: Optional[str]) -> Optional[str]:
    # 1. Check Bearer token from WebSocket handshake headers (secure, avoids URL token exposure)
    auth_header = websocket.headers.get("authorization", "")
    if auth_header.lower().startswith("bearer "):
        raw_token = auth_header[7:].strip()
        payload = decode_access_token(raw_token)
        if payload and payload.get("sub"):
            return payload["sub"]

    # 2. Check query param token for backward-compatible test clients
    if token:
        payload = decode_access_token(token)
        if payload and payload.get("sub"):
            return payload["sub"]

    # 2. Development mode fallback
    if (
        (settings.allow_dev_user_id or settings.environment == "development")
        and user_id_param
    ):
        return user_id_param

    return None


@router.websocket("/api/v1/roadmaps/ws/personalized")
async def personalized_roadmap_websocket(
    websocket: WebSocket,
    token: Optional[str] = Query(None),
    user_id: Optional[str] = Query(None),
):
    await websocket.accept()

    user_id_resolved = _extract_user_id(websocket, token=token, user_id_param=user_id)
    if not user_id_resolved:
        error_event = RoadmapServerEvent(
            event=RoadmapServerEventType.ERROR,
            session_id="",
            payload={"code": "UNAUTHORIZED", "message": "Authentication required for roadmap assessment session."},
        )
        await websocket.send_text(error_event.model_dump_json())
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return

    logger.info("Roadmap WebSocket connected: user_id=%s", user_id_resolved)

    try:
        while True:
            raw_text = await websocket.receive_text()
            try:
                data = json.loads(raw_text)
            except Exception:
                err = RoadmapServerEvent(
                    event=RoadmapServerEventType.ERROR,
                    session_id="",
                    payload={"code": "INVALID_JSON", "message": "Malformed JSON payload."},
                )
                await websocket.send_text(err.model_dump_json())
                continue

            event_type = data.get("event")
            session_id = data.get("session_id", "")
            payload = data.get("payload") or {}

            # Ping heartbeat
            if event_type == RoadmapClientEventType.PING.value or event_type == "ping":
                pong = RoadmapServerEvent(
                    event=RoadmapServerEventType.PONG,
                    session_id=session_id,
                    payload={"timestamp": payload.get("timestamp")},
                )
                await websocket.send_text(pong.model_dump_json())
                continue

            from app.core.database import get_db

            async def _execute_event(db: AsyncSession) -> bool:
                session_mgr = RoadmapSessionManager(db)
                if event_type == RoadmapClientEventType.START_ROADMAP.value:
                    resp = await session_mgr.handle_start_roadmap(
                        user_id=user_id_resolved, payload=payload
                    )
                    await websocket.send_text(resp.model_dump_json())

                elif event_type == RoadmapClientEventType.SUBMIT_ANSWER.value:
                    resp = await session_mgr.handle_submit_answer(
                        user_id=user_id_resolved,
                        session_id=session_id,
                        payload=payload,
                    )
                    await websocket.send_text(resp.model_dump_json())

                elif event_type == RoadmapClientEventType.GENERATE_ROADMAP.value:
                    started = RoadmapServerEvent(
                        event=RoadmapServerEventType.ROADMAP_GENERATION_STARTED,
                        session_id=session_id,
                        payload={"message": "Synthesizing personalized DAG curriculum..."},
                    )
                    await websocket.send_text(started.model_dump_json())

                    resp = await session_mgr.handle_generate_roadmap(
                        user_id=user_id_resolved,
                        session_id=session_id,
                    )
                    await websocket.send_text(resp.model_dump_json())

                elif event_type == RoadmapClientEventType.RESUME_SESSION.value:
                    resp = await session_mgr.handle_resume_session(
                        user_id=user_id_resolved,
                        session_id=session_id,
                    )
                    await websocket.send_text(resp.model_dump_json())

                elif event_type == RoadmapClientEventType.CANCEL_SESSION.value:
                    cancelled = RoadmapServerEvent(
                        event=RoadmapServerEventType.SESSION_COMPLETE,
                        session_id=session_id,
                        payload={"status": "CANCELLED"},
                    )
                    await websocket.send_text(cancelled.model_dump_json())
                    return True

                else:
                    unsupported = RoadmapServerEvent(
                        event=RoadmapServerEventType.ERROR,
                        session_id=session_id,
                        payload={"code": "UNKNOWN_EVENT", "message": f"Unsupported event: {event_type}"},
                    )
                    await websocket.send_text(unsupported.model_dump_json())
                return False

            try:
                if get_db in websocket.app.dependency_overrides:
                    gen = websocket.app.dependency_overrides[get_db]()
                    db = await anext(gen) if hasattr(gen, "__anext__") else next(gen)
                    try:
                        should_break = await _execute_event(db)
                        if should_break:
                            break
                    finally:
                        try:
                            if hasattr(gen, "__anext__"):
                                await anext(gen)
                            else:
                                next(gen)
                        except (StopAsyncIteration, StopIteration):
                            pass
                else:
                    session_factory = get_session_factory()
                    async with session_factory() as db:
                        should_break = await _execute_event(db)
                        if should_break:
                            break
            except Exception as exc:
                logger.error("Error processing event '%s' for session '%s': %s", event_type, session_id, exc, exc_info=True)
                err = RoadmapServerEvent(
                    event=RoadmapServerEventType.ERROR,
                    session_id=session_id,
                    payload={"code": "PROCESSING_ERROR", "message": str(exc)},
                )
                await websocket.send_text(err.model_dump_json())

    except WebSocketDisconnect:
        logger.info("Roadmap WebSocket disconnected cleanly: user_id=%s", user_id_resolved)
    except Exception as exc:
        logger.error("Unexpected error in roadmap WebSocket: %s", exc, exc_info=True)
