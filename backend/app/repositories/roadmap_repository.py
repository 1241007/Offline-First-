"""Roadmap repository for database operations"""

from typing import List, Optional
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload
from sqlalchemy.ext.asyncio import AsyncSession
from app.models import Roadmap, RoadmapItem


class RoadmapRepository:
    """Repository for roadmap-related database operations"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def get_all_system_roadmaps(
        self,
        category: Optional[str] = None,
        limit: Optional[int] = None,
        offset: int = 0
    ) -> List[Roadmap]:
        """Get system roadmaps with optional category filter and pagination"""
        query = select(Roadmap).where(Roadmap.is_system == True)
        if category and category.lower() != "all":
            query = query.where(Roadmap.category == category)
        query = query.order_by(Roadmap.created_at).offset(offset)
        if limit is not None:
            query = query.limit(limit)
        
        result = await self.db.execute(query)
        return list(result.scalars().all())
    
    async def get_roadmap_by_id(self, roadmap_id: str) -> Optional[Roadmap]:
        """Get roadmap with items"""
        query = select(Roadmap).where(
            Roadmap.id == roadmap_id
        ).options(
            selectinload(Roadmap.items)
        )
        
        result = await self.db.execute(query)
        return result.scalar_one_or_none()
    
    async def get_roadmap_items_count(self, roadmap_id: str) -> int:
        """Get count of items in a roadmap"""
        query = select(func.count(RoadmapItem.id)).where(RoadmapItem.roadmap_id == roadmap_id)
        
        result = await self.db.execute(query)
        return result.scalar() or 0
    
    async def get_all_categories(self) -> List[str]:
        """Get all unique roadmap categories"""
        query = select(Roadmap.category).distinct().where(Roadmap.is_system == True)
        
        result = await self.db.execute(query)
        return list(result.scalars().all())
