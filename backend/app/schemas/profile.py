"""User profile-related Pydantic schemas"""

from typing import Optional
from pydantic import BaseModel, ConfigDict, Field


class ProfileResponse(BaseModel):
    """User profile response"""
    full_name: str = Field(alias="fullName")
    email: str
    mobile: Optional[str] = None
    interests: Optional[str] = None
    level: str
    education_mode: str = Field(alias="educationMode")
    board: Optional[str] = None
    class_grade: Optional[str] = Field(default=None, alias="classGrade")
    subject: Optional[str] = None
    preferred_language: Optional[str] = Field(default="en", alias="preferredLanguage")
    
    model_config = ConfigDict(
        populate_by_name=True,
        from_attributes=True
    )


class ProfileUpdateRequest(BaseModel):
    """User profile update request"""
    full_name: str = Field(alias="fullName")
    email: str
    mobile: Optional[str] = None
    interests: Optional[str] = None
    level: str
    education_mode: str = Field(alias="educationMode")
    board: Optional[str] = None
    class_grade: Optional[str] = Field(default=None, alias="classGrade")
    subject: Optional[str] = None
    preferred_language: Optional[str] = Field(default="en", alias="preferredLanguage")
    
    model_config = ConfigDict(
        populate_by_name=True
    )
