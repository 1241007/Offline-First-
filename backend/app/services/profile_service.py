"""User profile service for business logic"""

from typing import Optional
from sqlalchemy.ext.asyncio import AsyncSession
from app.repositories.profile_repository import ProfileRepository
from app.schemas.profile import ProfileResponse, ProfileUpdateRequest


class ProfileService:
    """Service for user profile business logic"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
        self.repo = ProfileRepository(db)
    
    async def get_profile(self, user_id: str) -> Optional[ProfileResponse]:
        """Get user profile"""
        profile = await self.repo.get_by_user_id(user_id)
        
        if not profile:
            return None
        
        return ProfileResponse(
            full_name=profile.full_name,
            email=profile.email,
            mobile=profile.mobile,
            interests=profile.interests,
            level=profile.level,
            education_mode=profile.education_mode
        )
    
    async def update_profile(
        self,
        user_id: str,
        data: ProfileUpdateRequest
    ) -> ProfileResponse:
        """Update or create user profile"""
        # Check if profile exists
        existing = await self.repo.get_by_user_id(user_id)
        
        if existing:
            # Update existing
            profile = await self.repo.update_profile(
                user_id=user_id,
                full_name=data.full_name,
                email=data.email,
                mobile=data.mobile,
                interests=data.interests,
                level=data.level,
                education_mode=data.education_mode
            )
        else:
            # Create new
            profile = await self.repo.create_profile(
                user_id=user_id,
                full_name=data.full_name,
                email=data.email,
                mobile=data.mobile,
                interests=data.interests,
                level=data.level,
                education_mode=data.education_mode
            )
        
        await self.db.commit()
        
        return ProfileResponse(
            full_name=profile.full_name,
            email=profile.email,
            mobile=profile.mobile,
            interests=profile.interests,
            level=profile.level,
            education_mode=profile.education_mode
        )
