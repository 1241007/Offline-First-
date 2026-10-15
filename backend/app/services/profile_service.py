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
        """Get user profile, auto-creating a default one if needed for an active user."""
        profile = await self.repo.get_by_user_id(user_id)
        
        if not profile:
            from app.models.user import User
            from sqlalchemy import select
            user = await self.db.scalar(select(User).where(User.id == user_id))
            if not user:
                return None
            profile = await self.repo.create_profile(
                user_id=user_id,
                full_name=user.email.split("@")[0],
                email=user.email,
                mobile=user.mobile,
                interests=None,
                level="Beginner",
                education_mode="general"
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
    
    async def update_profile(
        self,
        user_id: str,
        data: ProfileUpdateRequest
    ) -> ProfileResponse:
        """Update or create user profile and synchronize user contact info"""
        existing = await self.repo.get_by_user_id(user_id)
        
        if existing:
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
            profile = await self.repo.create_profile(
                user_id=user_id,
                full_name=data.full_name,
                email=data.email,
                mobile=data.mobile,
                interests=data.interests,
                level=data.level,
                education_mode=data.education_mode
            )
        
        # Keep user mobile in sync if changed
        from app.models.user import User
        from sqlalchemy import update
        await self.db.execute(
            update(User).where(User.id == user_id).values(mobile=data.mobile)
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
