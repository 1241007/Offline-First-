"""User profile repository for database operations"""

from typing import Optional
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models import UserProfile


class ProfileRepository:
    """Repository for user profile operations"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def get_by_user_id(self, user_id: str) -> Optional[UserProfile]:
        """Get user profile by user_id"""
        query = select(UserProfile).where(UserProfile.user_id == user_id)
        
        result = await self.db.execute(query)
        return result.scalar_one_or_none()
    
    async def create_profile(
        self,
        user_id: str,
        full_name: str,
        email: str,
        mobile: Optional[str],
        interests: Optional[str],
        level: str,
        education_mode: str
    ) -> UserProfile:
        """Create new user profile"""
        profile = UserProfile(
            user_id=user_id,
            full_name=full_name,
            email=email,
            mobile=mobile,
            interests=interests,
            level=level,
            education_mode=education_mode
        )
        
        self.db.add(profile)
        await self.db.flush()
        return profile
    
    async def update_profile(
        self,
        user_id: str,
        full_name: str,
        email: str,
        mobile: Optional[str],
        interests: Optional[str],
        level: str,
        education_mode: str
    ) -> Optional[UserProfile]:
        """Update existing user profile"""
        profile = await self.get_by_user_id(user_id)
        
        if not profile:
            return None
        
        profile.full_name = full_name
        profile.email = email
        profile.mobile = mobile
        profile.interests = interests
        profile.level = level
        profile.education_mode = education_mode
        
        await self.db.flush()
        return profile
