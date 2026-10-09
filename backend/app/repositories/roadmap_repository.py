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

    async def get_user_roadmaps(
        self,
        user_id: str,
        limit: Optional[int] = None,
        offset: int = 0
    ) -> List[Roadmap]:
        """Get user's personalized roadmaps with items"""
        query = (
            select(Roadmap)
            .where(Roadmap.user_id == user_id, Roadmap.is_system == False)
            .options(selectinload(Roadmap.items))
            .order_by(Roadmap.created_at.desc())
            .offset(offset)
        )
        if limit is not None:
            query = query.limit(limit)
        result = await self.db.execute(query)
        return list(result.scalars().all())

    async def get_roadmap_by_id_and_user(
        self, roadmap_id: str, user_id: str
    ) -> Optional[Roadmap]:
        """Get roadmap by id with authorization check (either system roadmap or owned by user)"""
        query = (
            select(Roadmap)
            .where(
                Roadmap.id == roadmap_id,
                (Roadmap.is_system == True) | (Roadmap.user_id == user_id),
            )
            .options(selectinload(Roadmap.items))
        )
        result = await self.db.execute(query)
        return result.scalar_one_or_none()

    async def create_personalized_roadmap(
        self,
        user_id: str,
        title: str,
        slug: str,
        category: str,
        description: str,
        level: str,
        duration: str,
        icon: str,
        accent_theme: str,
        structure: dict,
        items: list[dict],
    ) -> Roadmap:
        """Create a personalized roadmap with items and structure JSON"""
        roadmap = Roadmap(
            user_id=user_id,
            is_system=False,
            title=title,
            slug=slug,
            category=category,
            description=description,
            level=level,
            duration=duration,
            icon=icon,
            accent_theme=accent_theme,
            structure=structure,
        )
        self.db.add(roadmap)
        await self.db.flush()

        for idx, item_data in enumerate(items):
            item = RoadmapItem(
                roadmap_id=roadmap.id,
                course_id=item_data.get("course_id"),
                title=item_data["title"],
                description=item_data.get("description"),
                skills=item_data.get("skills", []),
                duration=item_data.get("duration"),
                display_order=idx + 1,
            )
            self.db.add(item)

        await self.db.flush()
        return await self.get_roadmap_by_id_and_user(roadmap.id, user_id)

