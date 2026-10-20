"""Roadmap API routes for System and Personalized Roadmaps"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.deps import get_current_user_id
from app.services.roadmap_service import RoadmapService
from app.services.personalized_roadmap_service import PersonalizedRoadmapService
from app.schemas.roadmap import (
    RoadmapListResponse,
    RoadmapDetailResponse,
    SavedRoadmapActionResponse,
    SavedRoadmapResponse,
)
from app.schemas.personalized_roadmap import (
    AssessmentSessionResponse,
    SubmitAssessmentAnswerRequest,
    ProfileCorrectionRequest,
    PersonalizedRoadmapDetailResponse,
    SavePersonalizedRoadmapRequest,
    RoadmapMilestonesSyncRequest,
    RoadmapMilestonesSyncResponse,
    RenamePersonalizedRoadmapRequest,
    StartAssessmentRequest,
    ValidateSubjectRequest,
    ValidateSubjectResponse,
    TopicLearningContentResponse,
    CompleteTopicRequest,
    CompleteTopicResponse,
    SubmitTopicAssessmentRequest,
    SubmitTopicAssessmentResponse,
    ReviewTopicResponse,
)

router = APIRouter(prefix="/api/v1", tags=["roadmaps"])


# --- System Roadmaps ---

@router.get("/roadmaps", response_model=List[RoadmapListResponse])
async def get_roadmaps(
    category: Optional[str] = Query(None, description="Filter by category"),
    limit: Optional[int] = Query(None, ge=1, le=100, description="Page limit"),
    offset: int = Query(0, ge=0, description="Page offset"),
    db: AsyncSession = Depends(get_db)
):
    """
    Get system roadmaps with optional category filter and pagination.
    """
    service = RoadmapService(db)
    return await service.get_all_roadmaps(category=category, limit=limit, offset=offset)


@router.get("/roadmaps/categories", response_model=List[str])
async def get_roadmap_categories(db: AsyncSession = Depends(get_db)):
    """
    Get all roadmap categories.
    """
    service = RoadmapService(db)
    return await service.get_categories()


# --- Personalized Roadmap Assessment & Generation ---

@router.post(
    "/roadmaps/personalized/start",
    response_model=AssessmentSessionResponse,
    status_code=status.HTTP_201_CREATED,
)
async def start_personalized_assessment(
    payload: Optional[StartAssessmentRequest] = None,
    domain: Optional[str] = Query(None),
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Start a stateful personalized roadmap assessment session for the authenticated user.
    Gathers authentic user context and returns the mentor's greeting and first question.
    """
    target_domain = (payload.domain if payload and payload.domain else domain) or "Python"
    service = PersonalizedRoadmapService(db)
    return await service.start_assessment_session(user_id=current_user_id, domain=target_domain)


@router.get(
    "/roadmaps/personalized/active",
    response_model=Optional[AssessmentSessionResponse],
)
async def get_active_assessment_session(
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Retrieve user's active in-progress assessment session if one exists for resumption.
    """
    service = PersonalizedRoadmapService(db)
    return await service.get_active_session(user_id=current_user_id)


@router.get(
    "/roadmaps/personalized/session/{session_id}",
    response_model=AssessmentSessionResponse,
)
async def get_assessment_session_by_id(
    session_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Get assessment session state and history by ID.
    """
    service = PersonalizedRoadmapService(db)
    session_resp = await service.get_session_by_id(session_id=session_id, user_id=current_user_id)
    if not session_resp:
        raise HTTPException(status_code=404, detail="Assessment session not found")
    return session_resp


@router.post(
    "/roadmaps/personalized/session/{session_id}/message",
    response_model=AssessmentSessionResponse,
)
async def submit_assessment_answer(
    session_id: str,
    payload: SubmitAssessmentAnswerRequest,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Submit learner response or quiz choice to the AI advisor.
    Updates skill gaps, scores quizzes securely on server, and asks the next question.
    """
    service = PersonalizedRoadmapService(db)
    try:
        return await service.process_learner_response(
            user_id=current_user_id,
            session_id=session_id,
            answer=payload.answer,
            quiz_selected_index=payload.quiz_selected_index,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post(
    "/roadmaps/personalized/session/{session_id}/correct-field",
    response_model=AssessmentSessionResponse,
)
async def correct_profile_field(
    session_id: str,
    payload: ProfileCorrectionRequest,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    In-place edit of learner profile fields during Stage E review without resetting assessment.
    """
    service = PersonalizedRoadmapService(db)
    try:
        return await service.correct_profile_field(
            user_id=current_user_id,
            session_id=session_id,
            field=payload.field,
            value=payload.value,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post(
    "/roadmaps/personalized/session/{session_id}/generate",
    response_model=PersonalizedRoadmapDetailResponse,
    status_code=status.HTTP_201_CREATED,
)
async def generate_personalized_roadmap(
    session_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Generate a validated, personalized roadmap using Claude Sonnet 4.5 based on gathered assessment evidence.
    """
    service = PersonalizedRoadmapService(db)
    try:
        return await service.generate_roadmap_from_assessment(
            user_id=current_user_id,
            session_id=session_id,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post(
    "/roadmaps/personalized/save",
    response_model=PersonalizedRoadmapDetailResponse,
    status_code=status.HTTP_201_CREATED,
)
@router.post(
    "/roadmaps/personalized",
    response_model=PersonalizedRoadmapDetailResponse,
    status_code=status.HTTP_201_CREATED,
)
async def save_personalized_roadmap(
    payload: SavePersonalizedRoadmapRequest,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Save or synchronize a locally created or generated personalized roadmap to the cloud.
    Ensures authenticated-user ownership, idempotency, and server-side authorization.
    """
    service = PersonalizedRoadmapService(db)
    return await service.save_or_sync_personalized_roadmap(
        user_id=current_user_id, payload=payload
    )


@router.get(
    "/roadmaps/personalized/my-roadmaps",
    response_model=List[PersonalizedRoadmapDetailResponse],
)
async def get_my_personalized_roadmaps(
    limit: Optional[int] = Query(None, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    List all personalized roadmaps created by the authenticated user.
    """
    service = PersonalizedRoadmapService(db)
    return await service.get_user_personalized_roadmaps(
        user_id=current_user_id, limit=limit, offset=offset
    )


@router.get(
    "/roadmaps/personalized/{roadmap_id}",
    response_model=PersonalizedRoadmapDetailResponse,
)
async def get_personalized_roadmap_detail(
    roadmap_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Retrieve full personalized roadmap detail with phases, milestones, weekly schedule, and assessment summary.
    """
    service = PersonalizedRoadmapService(db)
    roadmap = await service.get_personalized_roadmap_detail(
        roadmap_id=roadmap_id, user_id=current_user_id
    )
    if not roadmap:
        raise HTTPException(status_code=404, detail="Personalized roadmap not found")
    return roadmap


@router.get(
    "/roadmaps/personalized/{roadmap_id}/milestones",
    response_model=RoadmapMilestonesSyncResponse,
)
async def get_roadmap_milestones(
    roadmap_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Retrieve current completed milestones for a personalized roadmap.
    """
    service = PersonalizedRoadmapService(db)
    completed = await service.get_roadmap_milestones(
        roadmap_id=roadmap_id, user_id=current_user_id
    )
    return RoadmapMilestonesSyncResponse(
        roadmapId=roadmap_id,
        completedMilestones=completed,
    )


@router.put(
    "/roadmaps/personalized/{roadmap_id}/milestones",
    response_model=RoadmapMilestonesSyncResponse,
)
async def sync_roadmap_milestones(
    roadmap_id: str,
    payload: RoadmapMilestonesSyncRequest,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Update or merge completed milestones for a personalized roadmap.
    Supports atomic single milestone toggle or batch list synchronization.
    """
    service = PersonalizedRoadmapService(db)
    try:
        updated = await service.sync_roadmap_milestones(
            roadmap_id=roadmap_id,
            user_id=current_user_id,
            milestone_key=payload.milestone_key,
            is_completed=payload.is_completed,
            completed_milestones=payload.completed_milestones,
        )
        return RoadmapMilestonesSyncResponse(
            roadmapId=roadmap_id,
            completedMilestones=updated,
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.delete(
    "/roadmaps/personalized/{roadmap_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
async def delete_personalized_roadmap(
    roadmap_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Delete a user's personalized roadmap.
    """
    service = PersonalizedRoadmapService(db)
    success = await service.delete_personalized_roadmap(
        roadmap_id=roadmap_id, user_id=current_user_id
    )
    if not success:
        raise HTTPException(status_code=404, detail="Personalized roadmap not found or unauthorized")
    return None


@router.patch(
    "/roadmaps/personalized/{roadmap_id}/rename",
    response_model=PersonalizedRoadmapDetailResponse,
)
async def rename_personalized_roadmap(
    roadmap_id: str,
    payload: RenamePersonalizedRoadmapRequest,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Rename a user's personalized roadmap.
    """
    service = PersonalizedRoadmapService(db)
    updated = await service.rename_personalized_roadmap(
        roadmap_id=roadmap_id, user_id=current_user_id, new_title=payload.title
    )
    if not updated:
        raise HTTPException(status_code=404, detail="Personalized roadmap not found or unauthorized")
    return updated

@router.post(
    "/roadmaps/personalized/validate-subject",
    response_model=ValidateSubjectResponse,
)
async def validate_personalized_subject(
    payload: ValidateSubjectRequest,
    db: AsyncSession = Depends(get_db),
):
    """
    Validate and standardize user-entered course/domain before assessment.
    """
    service = PersonalizedRoadmapService(db)
    return service.validate_subject(payload.subject)


@router.get(
    "/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/content",
    response_model=TopicLearningContentResponse,
)
async def get_topic_learning_content(
    roadmap_id: str,
    topic_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Get rich structured learning content, objectives, code examples, common mistakes, and quiz for a topic.
    """
    service = PersonalizedRoadmapService(db)
    content = await service.get_topic_learning_content(
        roadmap_id=roadmap_id, topic_id=topic_id, user_id=current_user_id
    )
    if not content:
        raise HTTPException(status_code=404, detail="Topic learning content not found")
    return content


@router.post(
    "/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/complete",
    response_model=CompleteTopicResponse,
)
async def complete_topic(
    roadmap_id: str,
    topic_id: str,
    payload: CompleteTopicRequest,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Mark a topic complete, unlock downstream dependencies, and update progress percentage.
    """
    service = PersonalizedRoadmapService(db)
    try:
        return await service.complete_topic(
            roadmap_id=roadmap_id,
            topic_id=topic_id,
            user_id=current_user_id,
            quiz_passed=payload.quiz_passed,
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post(
    "/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/review",
    response_model=ReviewTopicResponse,
)
async def review_topic(
    roadmap_id: str,
    topic_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Record that learner reviewed study material, updating topic status to REVIEWED without bypassing assessment.
    """
    service = PersonalizedRoadmapService(db)
    try:
        return await service.review_topic(
            roadmap_id=roadmap_id,
            topic_id=topic_id,
            user_id=current_user_id,
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post(
    "/roadmaps/personalized/{roadmap_id}/topics/{topic_id}/submit-assessment",
    response_model=SubmitTopicAssessmentResponse,
)
async def submit_topic_assessment(
    roadmap_id: str,
    topic_id: str,
    payload: SubmitTopicAssessmentRequest,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Evaluate topic assessment answers, determine verified completion or mastery, unlock downstream dependencies, and recalculate progress.
    """
    service = PersonalizedRoadmapService(db)
    try:
        return await service.submit_topic_assessment(
            roadmap_id=roadmap_id,
            topic_id=topic_id,
            user_id=current_user_id,
            answers=payload.answers,
            hints_used=payload.hints_used,
            solution_revealed=payload.solution_revealed,
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))



# --- Saved Roadmaps ---

@router.get("/roadmaps/saved", response_model=List[SavedRoadmapResponse])
async def get_saved_roadmaps(
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    List all saved roadmaps for the authenticated user.
    """
    service = RoadmapService(db)
    return await service.get_saved_roadmaps(user_id=current_user_id)


@router.post("/roadmaps/{roadmap_id}/save", response_model=SavedRoadmapResponse)
async def save_roadmap(
    roadmap_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Save a roadmap for offline access (idempotent).
    Enforces authorization for personalized roadmaps.
    """
    service = RoadmapService(db)
    return await service.save_roadmap(user_id=current_user_id, roadmap_id=roadmap_id)


@router.delete("/roadmaps/{roadmap_id}/save", response_model=SavedRoadmapActionResponse)
async def unsave_roadmap(
    roadmap_id: str,
    current_user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    """
    Unsave a roadmap for the authenticated user (idempotent).
    """
    service = RoadmapService(db)
    return await service.unsave_roadmap(user_id=current_user_id, roadmap_id=roadmap_id)


# --- General Roadmap Detail (System & User) ---

@router.get("/roadmaps/{roadmap_id}", response_model=RoadmapDetailResponse)

async def get_roadmap_detail(
    roadmap_id: str,
    db: AsyncSession = Depends(get_db)
):
    """
    Get standard roadmap details with items.
    """
    service = RoadmapService(db)
    roadmap = await service.get_roadmap_detail(roadmap_id)
    
    if not roadmap:
        raise HTTPException(status_code=404, detail="Roadmap not found")
    
    return roadmap
