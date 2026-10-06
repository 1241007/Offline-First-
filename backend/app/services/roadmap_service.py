"""Roadmap service for business logic"""

from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from app.repositories.roadmap_repository import RoadmapRepository
from app.schemas.roadmap import RoadmapListResponse, RoadmapDetailResponse, RoadmapItemResponse


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
