"""User profile API routes"""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.core.deps import get_dev_user_id
from app.services.profile_service import ProfileService
from app.schemas.profile import ProfileResponse, ProfileUpdateRequest

router = APIRouter(prefix="/api/v1", tags=["profile"])


@router.get("/profile", response_model=ProfileResponse)
async def get_profile(
    user_id: str = Depends(get_dev_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Get user profile.
    
    Requires: user_id (query parameter in dev mode)
    """
    service = ProfileService(db)
    profile = await service.get_profile(user_id)
    
    if not profile:
        raise HTTPException(status_code=404, detail="Profile not found")
    
    return profile


@router.put("/profile", response_model=ProfileResponse)
async def update_profile(
    data: ProfileUpdateRequest,
    user_id: str = Depends(get_dev_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Update user profile (creates if doesn't exist).
    
    Requires: user_id (query parameter in dev mode)
    """
    service = ProfileService(db)
    return await service.update_profile(user_id, data)
