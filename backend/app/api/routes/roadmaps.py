"""Roadmap API routes"""

from typing import List
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.services.roadmap_service import RoadmapService
from app.schemas.roadmap import RoadmapListResponse, RoadmapDetailResponse

router = APIRouter(prefix="/api/v1", tags=["roadmaps"])


@router.get("/roadmaps", response_model=List[RoadmapListResponse])
async def get_roadmaps(db: AsyncSession = Depends(get_db)):
    """
    Get all system roadmaps.
    """
    service = RoadmapService(db)
    return await service.get_all_roadmaps()


@router.get("/roadmaps/categories", response_model=List[str])
async def get_roadmap_categories(db: AsyncSession = Depends(get_db)):
    """
    Get all roadmap categories.
    """
    service = RoadmapService(db)
    return await service.get_categories()


@router.get("/roadmaps/{roadmap_id}", response_model=RoadmapDetailResponse)
async def get_roadmap_detail(
    roadmap_id: str,
    db: AsyncSession = Depends(get_db)
):
    """
    Get roadmap details with items.
    """
    service = RoadmapService(db)
    roadmap = await service.get_roadmap_detail(roadmap_id)
    
    if not roadmap:
        raise HTTPException(status_code=404, detail="Roadmap not found")
    
    return roadmap
