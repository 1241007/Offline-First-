import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes.chat import router as chat_router
from app.api.routes.courses import router as courses_router
from app.api.routes.roadmaps import router as roadmaps_router
from app.api.routes.learning import router as learning_router
from app.api.routes.profile import router as profile_router
from app.core.config import settings

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="EduNova Backend API",
    description="Backend for EduNova — Courses, Roadmaps, Learning Progress, AI Chat",
    version="2.0.0",
)

# CORS: allow Android emulator (10.0.2.2) and localhost for development
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://10.0.2.2:8000",
        "http://localhost:8000",
        "http://127.0.0.1:8000",
    ],
    allow_credentials=False,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["Content-Type", "Accept"],
)


@app.on_event("startup")
async def startup_event():
    """Log startup information"""
    logger.info("EduNova Backend API started")
    logger.info(f"Environment: {settings.environment}")
    if settings.allow_dev_user_id:
        logger.warning("⚠️  DEVELOPMENT MODE: user_id query parameter is ENABLED")
        logger.warning("⚠️  This is INSECURE and should NEVER be used in production!")


@app.get("/api/v1/health")
async def health():
    return {
        "status": "ok",
        "service": "EduNova Backend API",
        "version": "2.0.0",
        "environment": settings.environment
    }


# Register routers
app.include_router(chat_router)
app.include_router(courses_router)
app.include_router(roadmaps_router)
app.include_router(learning_router)
app.include_router(profile_router)
