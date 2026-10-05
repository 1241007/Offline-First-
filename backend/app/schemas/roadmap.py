"""Roadmap-related Pydantic schemas"""

from typing import List, Optional
from pydantic import BaseModel, ConfigDict, Field


class RoadmapItemResponse(BaseModel):
    """Roadmap item in detail response"""
    id: str
    title: str
    description: Optional[str] = None
    course_id: Optional[str] = Field(None, alias="courseId")
    skills: Optional[List[str]] = None
    duration: Optional[str] = None
    
    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class RoadmapListResponse(BaseModel):
    """Roadmap in list view"""
    id: str
    title: str
    category: str
    description: str
    skills: List[str] = Field(default_factory=list)  # Aggregated from items
    level: str
    duration: str
    stages: int  # Calculated dynamically
    icon: str
    accent_theme: str = Field(alias="accentTheme")
    
    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class RoadmapDetailResponse(BaseModel):
    """Roadmap with full details including items"""
    id: str
    title: str
    category: str
    description: str
    level: str
    duration: str
    stages: int  # Calculated dynamically
    icon: str
    accent_theme: str = Field(alias="accentTheme")
    items: List[RoadmapItemResponse] = []
    
    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )
