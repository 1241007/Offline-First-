"""Course-related Pydantic schemas"""

from datetime import datetime
from typing import List, Optional
from urllib.parse import urlparse
from pydantic import BaseModel, ConfigDict, Field, computed_field, field_validator


def to_camel(string: str) -> str:
    """Convert snake_case to camelCase"""
    components = string.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])


class LessonResponse(BaseModel):
    """Lesson in module detail response"""
    id: str
    title: str
    description: Optional[str] = None
    content_type: str = Field("video", alias="contentType")
    duration_minutes: Optional[int] = Field(None, alias="durationMinutes")
    display_order: int = Field(0, alias="displayOrder")
    resource_url: Optional[str] = Field(None, alias="resourceUrl")
    resource_provider: Optional[str] = Field(None, alias="resourceProvider")
    video_url: Optional[str] = Field(None, alias="videoUrl")
    video_provider: Optional[str] = Field(None, alias="videoProvider")

    @field_validator("resource_url", mode="before")
    @classmethod
    def validate_resource_url(cls, v: Optional[str]) -> Optional[str]:
        if not v or not isinstance(v, str):
            return None
        v = v.strip()
        if not v:
            return None
        parsed = urlparse(v)
        if parsed.scheme in ("http", "https") and parsed.netloc:
            return v
        return None

    @field_validator("video_url", mode="before")
    @classmethod
    def validate_video_url(cls, v: Optional[str]) -> Optional[str]:
        if not v or not isinstance(v, str):
            return None
        v = v.strip()
        if not v:
            return None
        parsed = urlparse(v)
        if parsed.scheme in ("http", "https") and parsed.netloc:
            host = parsed.netloc.lower()
            if any(host == domain or host.endswith("." + domain) for domain in ("youtube.com", "youtu.be")):
                return v
        return None

    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class ModuleResponse(BaseModel):
    """Module in course detail response"""
    id: str
    title: str
    description: Optional[str] = None
    display_order: int = Field(0, alias="displayOrder")
    lessons: List[LessonResponse] = Field(default_factory=list)

    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class CourseListResponse(BaseModel):
    """Course in list view"""
    id: str
    name: str
    slug: Optional[str] = None
    description: str
    icon: str
    accent_color: str = Field(alias="accentColor")
    category: str = Field("", description="Category name")
    category_slug: Optional[str] = Field(None, alias="categorySlug")
    level: str = "Beginner"
    duration_hours: Optional[int] = Field(None, alias="durationHours")
    is_featured: bool = Field(False, alias="isFeatured")
    total_lessons: int = Field(0, alias="totalLessons")
    modules_count: int = Field(0, alias="modulesCount")

    @computed_field(alias="accent")
    @property
    def accent(self) -> str:
        return self.accent_color

    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class CourseDetailResponse(BaseModel):
    """Course with full details including modules and lessons"""
    id: str
    name: str
    slug: Optional[str] = None
    description: str
    icon: str
    accent_color: str = Field(alias="accentColor")
    category: str = Field("", description="Category name")
    category_slug: Optional[str] = Field(None, alias="categorySlug")
    level: str = "Beginner"
    duration_hours: Optional[int] = Field(None, alias="durationHours")
    is_featured: bool = Field(False, alias="isFeatured")
    total_lessons: int = Field(0, alias="totalLessons")
    modules_count: int = Field(0, alias="modulesCount")
    prerequisites: List[str] = Field(default_factory=list)
    modules: List[ModuleResponse] = Field(default_factory=list)

    @computed_field(alias="accent")
    @property
    def accent(self) -> str:
        return self.accent_color

    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class SavedCourseActionResponse(BaseModel):
    """Response after saving or unsaving a course"""
    success: bool = True
    message: str
    course_id: str = Field(alias="courseId")
    is_saved: bool = Field(True, alias="isSaved")

    model_config = ConfigDict(populate_by_name=True)


class SavedCourseResponse(BaseModel):
    """Saved course item with course metadata for offline identification and download"""
    id: str
    user_id: str = Field(alias="userId")
    course_id: str = Field(alias="courseId")
    saved_at: datetime = Field(alias="savedAt")
    name: str
    slug: Optional[str] = None
    description: str
    icon: str
    accent_color: str = Field(alias="accentColor")
    category: str = ""
    category_slug: Optional[str] = Field(None, alias="categorySlug")
    level: str = "Beginner"
    duration_hours: Optional[int] = Field(None, alias="durationHours")
    is_featured: bool = Field(False, alias="isFeatured")
    total_lessons: int = Field(0, alias="totalLessons")
    modules_count: int = Field(0, alias="modulesCount")
    course: Optional[CourseListResponse] = None

    @computed_field(alias="accent")
    @property
    def accent(self) -> str:
        return self.accent_color

    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

