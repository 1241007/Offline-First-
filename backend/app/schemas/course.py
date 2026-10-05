"""Course-related Pydantic schemas"""

from typing import List, Optional
from pydantic import BaseModel, ConfigDict, Field


def to_camel(string: str) -> str:
    """Convert snake_case to camelCase"""
    components = string.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])


class LessonResponse(BaseModel):
    """Lesson in module detail response"""
    id: str
    title: str
    content_type: str = Field(alias="contentType")
    duration_minutes: Optional[int] = Field(None, alias="durationMinutes")
    
    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class ModuleResponse(BaseModel):
    """Module in course detail response"""
    id: str
    title: str
    description: Optional[str] = None
    lessons: List[LessonResponse] = []
    
    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class CourseListResponse(BaseModel):
    """Course in list view (minimal info)"""
    id: str
    name: str
    description: str
    icon: str
    accent: str = Field(alias="accentColor")
    
    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True,
        alias_generator=to_camel
    )


class CourseDetailResponse(BaseModel):
    """Course with full details including modules and lessons"""
    id: str
    name: str
    description: str
    icon: str
    accent: str = Field(alias="accentColor")
    level: str
    duration_hours: Optional[int] = Field(None, alias="durationHours")
    modules: List[ModuleResponse] = []
    
    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True,
        alias_generator=to_camel
    )
