"""FastAPI dependencies"""

from fastapi import Query, HTTPException, Depends
from app.core.config import settings


async def get_dev_user_id(user_id: str = Query(..., description="User ID (development only)")) -> str:
    """
    Get user_id from query parameter in development mode.
    
    SECURITY WARNING: This is for development only!
    In production, this should be replaced with proper authentication.
    """
    if not settings.allow_dev_user_id:
        raise HTTPException(
            status_code=400,
            detail="user_id query parameter not allowed in production. Authentication required."
        )
    
    if not user_id:
        raise HTTPException(
            status_code=400,
            detail="user_id required in development mode"
        )
    
    return user_id
