"""Assessment API routes"""

from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query, Body
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.services.personalized_roadmap_service import PersonalizedRoadmapService
from app.schemas.personalized_roadmap import (
    AssessmentSessionResponse,
    SubmitAnswerRequest,
    CorrectProfileFieldRequest,
    PersonalizedRoadmapDetailResponse,
    ValidateSubjectRequest,
    ValidateSubjectResponse,
    StartAssessmentRequest,
)

router = APIRouter(prefix="/api/v1/assessment", tags=["assessment"])


@router.post("/validate-subject", response_model=ValidateSubjectResponse)
async def validate_subject(
    body: ValidateSubjectRequest,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    return await service.validate_subject(body.subject)


@router.post("/start", response_model=AssessmentSessionResponse)
async def start_assessment(
    domain: Optional[str] = Query("Python"),
    user_id: Optional[str] = Query(None),
    body: Optional[StartAssessmentRequest] = Body(None),
    db: AsyncSession = Depends(get_db)
):
    target_domain = (body.domain if body and body.domain else domain) or "Python"
    service = PersonalizedRoadmapService(db)
    return await service.start_assessment(domain=target_domain, user_id=user_id)


@router.get("/active", response_model=Optional[AssessmentSessionResponse])
async def get_active_assessment(
    user_id: Optional[str] = None,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    session = await service.get_active_session(user_id=user_id)
    if not session:
        raise HTTPException(status_code=404, detail="No active assessment session found")
    return session


@router.get("/{session_id}", response_model=AssessmentSessionResponse)
async def get_assessment_session(
    session_id: str,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    session = await service.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Assessment session not found")
    return session


@router.post("/{session_id}/answer", response_model=AssessmentSessionResponse)
async def submit_assessment_answer(
    session_id: str,
    body: SubmitAnswerRequest,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    try:
        return await service.submit_answer(
            session_id=session_id,
            answer=body.answer,
            quiz_selected_index=body.quiz_selected_index
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/{session_id}/correct", response_model=AssessmentSessionResponse)
async def correct_profile_field(
    session_id: str,
    body: CorrectProfileFieldRequest,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    try:
        return await service.correct_profile_field(
            session_id=session_id,
            field=body.field,
            value=body.value
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{session_id}/generate-roadmap", response_model=PersonalizedRoadmapDetailResponse)
async def generate_roadmap_from_assessment(
    session_id: str,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    try:
        return await service.generate_personalized_roadmap(session_id)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
