"""Personalized Roadmaps API routes"""

from typing import List, Optional, Dict, Any
from fastapi import APIRouter, Depends, HTTPException, Query, Response, Body
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.services.personalized_roadmap_service import PersonalizedRoadmapService
from app.schemas.personalized_roadmap import (
    PersonalizedRoadmapDetailResponse,
    RenameRoadmapRequest,
    TopicLearningContentResponse,
    CompleteTopicRequest,
    CompleteTopicResponse,
    SubmitTopicAssessmentRequest,
    SubmitTopicAssessmentResponse,
    ReviewTopicResponse,
)

router = APIRouter(prefix="/api/v1/personalized-roadmaps", tags=["personalized-roadmaps"])


@router.get("", response_model=List[PersonalizedRoadmapDetailResponse])
async def get_my_personalized_roadmaps(
    limit: Optional[int] = Query(None),
    offset: int = Query(0),
    user_id: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    return await service.get_roadmaps(limit=limit, offset=offset, user_id=user_id)


@router.get("/{roadmap_id}", response_model=PersonalizedRoadmapDetailResponse)
async def get_personalized_roadmap_detail(
    roadmap_id: str,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    roadmap = await service.get_roadmap(roadmap_id)
    if not roadmap:
        raise HTTPException(status_code=404, detail="Personalized roadmap not found")
    return roadmap


@router.post("", response_model=PersonalizedRoadmapDetailResponse)
async def save_personalized_roadmap(
    body: Dict[str, Any],
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    return await service.save_roadmap(body)


@router.delete("/{roadmap_id}", status_code=204)
async def delete_personalized_roadmap(
    roadmap_id: str,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    deleted = await service.delete_roadmap(roadmap_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Roadmap not found")
    return Response(status_code=204)


@router.patch("/{roadmap_id}/rename", response_model=PersonalizedRoadmapDetailResponse)
async def rename_personalized_roadmap(
    roadmap_id: str,
    body: RenameRoadmapRequest,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    roadmap = await service.rename_roadmap(roadmap_id, body.title)
    if not roadmap:
        raise HTTPException(status_code=404, detail="Roadmap not found")
    return roadmap


@router.get("/{roadmap_id}/topics/{topic_id}/content", response_model=TopicLearningContentResponse)
async def get_topic_learning_content(
    roadmap_id: str,
    topic_id: str,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    content = await service.get_topic_learning_content(roadmap_id, topic_id)
    if not content:
        raise HTTPException(status_code=404, detail=f"Topic {topic_id} not found in roadmap {roadmap_id}")
    return content


@router.post("/{roadmap_id}/topics/{topic_id}/complete", response_model=CompleteTopicResponse)
async def complete_topic(
    roadmap_id: str,
    topic_id: str,
    body: CompleteTopicRequest = Body(default=CompleteTopicRequest()),
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    result = await service.complete_topic(roadmap_id, topic_id, quiz_passed=body.quiz_passed)
    if not result:
        raise HTTPException(status_code=404, detail=f"Roadmap {roadmap_id} or topic {topic_id} not found")
    return result


@router.post("/{roadmap_id}/topics/{topic_id}/review", response_model=ReviewTopicResponse)
async def review_topic(
    roadmap_id: str,
    topic_id: str,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    try:
        return await service.review_topic(roadmap_id, topic_id)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{roadmap_id}/topics/{topic_id}/submit-assessment", response_model=SubmitTopicAssessmentResponse)
async def submit_topic_assessment(
    roadmap_id: str,
    topic_id: str,
    body: SubmitTopicAssessmentRequest,
    db: AsyncSession = Depends(get_db)
):
    service = PersonalizedRoadmapService(db)
    try:
        return await service.submit_topic_assessment(
            roadmap_id=roadmap_id,
            topic_id=topic_id,
            answers=body.answers,
            hints_used=body.hints_used,
            solution_revealed=body.solution_revealed,
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

