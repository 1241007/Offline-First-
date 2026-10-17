"""Roadmap service for business logic"""

from typing import List, Optional
from fastapi import HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.repositories.roadmap_repository import RoadmapRepository
from app.schemas.roadmap import (
    RoadmapListResponse,
    RoadmapDetailResponse,
    RoadmapItemResponse,
    SavedRoadmapActionResponse,
    SavedRoadmapResponse,
)


class RoadmapService:
    """Service for roadmap-related business logic"""
    
    def __init__(self, db: AsyncSession):
        self.repo = RoadmapRepository(db)
    
    async def get_all_roadmaps(
        self,
        category: Optional[str] = None,
        limit: Optional[int] = None,
        offset: int = 0
    ) -> List[RoadmapListResponse]:
        """Get system roadmaps with optional category filter and pagination"""
        roadmaps = await self.repo.get_all_system_roadmaps(
            category=category,
            limit=limit,
            offset=offset
        )
        
        result = []
        for roadmap in roadmaps:
            # Calculate stages count dynamically
            stages_count = await self.repo.get_roadmap_items_count(roadmap.id)
            
            # Aggregate skills from items (if needed, or leave empty)
            skills = []
            
            result.append(RoadmapListResponse(
                id=roadmap.id,
                title=roadmap.title,
                category=roadmap.category,
                description=roadmap.description,
                skills=skills,
                level=roadmap.level,
                duration=roadmap.duration,
                stages=stages_count,
                icon=roadmap.icon,
                accent_theme=roadmap.accent_theme
            ))
        
        return result
    
    async def get_roadmap_detail(self, roadmap_id: str) -> Optional[RoadmapDetailResponse]:
        """Get roadmap with items"""
        roadmap = await self.repo.get_roadmap_by_id(roadmap_id)
        
        if not roadmap:
            return None
        
        # Calculate stages count
        stages_count = len(roadmap.items)
        
        items = [
            RoadmapItemResponse(
                id=item.id,
                title=item.title,
                description=item.description,
                course_id=item.course_id,
                skills=item.skills if item.skills else [],
                duration=item.duration
            )
            for item in roadmap.items
        ]
        
        return RoadmapDetailResponse(
            id=roadmap.id,
            title=roadmap.title,
            category=roadmap.category,
            description=roadmap.description,
            level=roadmap.level,
            duration=roadmap.duration,
            stages=stages_count,
            icon=roadmap.icon,
            accent_theme=roadmap.accent_theme,
            items=items
        )
    
    async def get_categories(self) -> List[str]:
        """Get all roadmap categories"""
        return await self.repo.get_all_categories()

    async def get_saved_roadmaps(self, user_id: str) -> List[SavedRoadmapResponse]:
        """Get all saved roadmaps for the authenticated user"""
        records = await self.repo.get_saved_roadmaps_for_user(user_id)
        results = []
        for rec in records:
            r = rec.roadmap
            if not r:
                continue
            stages_count = len(r.items) if r.items else 0
            if stages_count == 0 and r.structure and isinstance(r.structure, dict):
                stages_count = len(r.structure.get("phases", []))

            skills = []
            if r.items:
                for it in r.items:
                    if it.skills and isinstance(it.skills, list):
                        skills.extend(it.skills)

            results.append(
                SavedRoadmapResponse(
                    id=rec.id,
                    user_id=rec.user_id,
                    roadmap_id=rec.roadmap_id,
                    roadmap_type=rec.roadmap_type,
                    saved_at=rec.created_at,
                    title=r.title,
                    category=r.category,
                    description=r.description,
                    skills=list(dict.fromkeys(skills))[:10],
                    level=r.level,
                    duration=r.duration,
                    stages=stages_count,
                    icon=r.icon,
                    accent_theme=r.accent_theme,
                )
            )
        return results

    async def save_roadmap(self, user_id: str, roadmap_id: str) -> SavedRoadmapResponse:
        """Save a roadmap for authenticated user (system or user-owned personalized)"""
        roadmap = await self.repo.get_roadmap_by_id(roadmap_id)
        if not roadmap:
            raise HTTPException(status_code=404, detail="Roadmap not found")

        # Access control: If personalized, only the owner can save
        if not roadmap.is_system and roadmap.user_id != user_id:
            raise HTTPException(
                status_code=403,
                detail="Access denied: Cannot save another user's personalized roadmap"
            )

        rec = await self.repo.save_roadmap(user_id, roadmap)
        stages_count = len(roadmap.items) if roadmap.items else 0
        if stages_count == 0 and roadmap.structure and isinstance(roadmap.structure, dict):
            stages_count = len(roadmap.structure.get("phases", []))

        skills = []
        if roadmap.items:
            for it in roadmap.items:
                if it.skills and isinstance(it.skills, list):
                    skills.extend(it.skills)

        return SavedRoadmapResponse(
            id=rec.id,
            user_id=rec.user_id,
            roadmap_id=rec.roadmap_id,
            roadmap_type=rec.roadmap_type,
            saved_at=rec.created_at,
            title=roadmap.title,
            category=roadmap.category,
            description=roadmap.description,
            skills=list(dict.fromkeys(skills))[:10],
            level=roadmap.level,
            duration=roadmap.duration,
            stages=stages_count,
            icon=roadmap.icon,
            accent_theme=roadmap.accent_theme,
        )

    async def unsave_roadmap(self, user_id: str, roadmap_id: str) -> SavedRoadmapActionResponse:
        """Unsave a roadmap for authenticated user"""
        roadmap = await self.repo.get_roadmap_by_id(roadmap_id)
        if not roadmap:
            raise HTTPException(status_code=404, detail="Roadmap not found")

        # Access control: If personalized, only the owner can unsave
        if not roadmap.is_system and roadmap.user_id != user_id:
            raise HTTPException(
                status_code=403,
                detail="Access denied: Cannot unsave another user's personalized roadmap"
            )

        await self.repo.unsave_roadmap(user_id, roadmap.id)
        return SavedRoadmapActionResponse(
            success=True,
            message="Roadmap unsaved successfully",
            roadmap_id=roadmap.id,
            is_saved=False,
        )

