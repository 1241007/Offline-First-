import logging
import uuid
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.routes.auth import router as auth_router
from app.api.routes.chat import router as chat_router
from app.api.routes.courses import router as courses_router
from app.api.routes.roadmaps import router as roadmaps_router
from app.api.routes.roadmap_websocket import router as roadmap_websocket_router
from app.api.routes.learning import router as learning_router
from app.api.routes.profile import router as profile_router
from app.api.routes.memory import router as memory_router
from app.api.routes.retrieve import router as retrieve_router
from app.api.routes.syllabus_sync import router as syllabus_sync_router
from app.core.config import settings
from app.core.errors import AuthException
from app.core.rate_limit import limiter
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware
from app.middleware.correlation import CorrelationIdMiddleware

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("EduNova Backend API started")
    logger.info(f"Environment: {settings.environment}")
    if settings.allow_dev_user_id:
        logger.warning("⚠️  DEVELOPMENT MODE: user_id query parameter is ENABLED")
        logger.warning("⚠️  This is INSECURE and should NEVER be used in production!")

    # Startup database column & schema resilience check
    try:
        from sqlalchemy import text
        from app.core.database import get_engine, Base
        import app.models  # ensure models are registered
        engine = get_engine()
        async with engine.begin() as conn:
            # 1. Create any missing tables (e.g. user_saved_courses, user_saved_roadmaps)
            await conn.run_sync(Base.metadata.create_all)

            # 2. Ensure domain column exists in roadmap_assessment_sessions
            if "sqlite" in settings.async_database_url:
                result = await conn.execute(text("PRAGMA table_info(roadmap_assessment_sessions)"))
                cols = [row[1] for row in result.fetchall()]
                if "domain" not in cols:
                    await conn.execute(text("ALTER TABLE roadmap_assessment_sessions ADD COLUMN domain VARCHAR(100) DEFAULT 'Python'"))
                    logger.info("Auto-migrated SQLite: added domain column to roadmap_assessment_sessions")
            else:
                await conn.execute(text("ALTER TABLE roadmap_assessment_sessions ADD COLUMN IF NOT EXISTS domain VARCHAR(100) DEFAULT 'Python'"))
                await conn.execute(text("ALTER TABLE roadmaps ADD COLUMN IF NOT EXISTS structure JSONB"))
                logger.info("Auto-migrated PostgreSQL: ensured domain and structure columns exist")
    except Exception as e:
        logger.error(f"Error during startup database auto-migration check: {e}", exc_info=True)

    yield
    from app.services.openrouter_service import openrouter_service
    await openrouter_service.close()
    logger.info("EduNova Backend API shutdown completed")


app = FastAPI(
    title="EduNova Backend API",
    description="Backend for EduNova — Courses, Roadmaps, Learning Progress, AI Chat",
    version="2.0.0",
    lifespan=lifespan,
)

# Attach rate-limiter to app state and add middleware
app.state.limiter = limiter
app.add_middleware(SlowAPIMiddleware)
app.add_middleware(CorrelationIdMiddleware)


@app.exception_handler(RateLimitExceeded)
async def rate_limit_handler(request: Request, exc: RateLimitExceeded):
    return JSONResponse(
        status_code=429,
        content={"code": "RATE_LIMITED", "message": "Too many requests. Please try again later."},
    )

# CORS — loaded from settings so production origins are configurable via env var
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["Content-Type", "Accept", "Authorization", "X-Correlation-ID"],
)


# ── Exception handlers ────────────────────────────────────────────────────────

@app.exception_handler(AuthException)
async def auth_exception_handler(request: Request, exc: AuthException):
    """Return structured auth errors."""
    return JSONResponse(
        status_code=exc.status_code,
        content={"code": exc.code, "message": exc.auth_message},
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Return structured validation errors without raw Pydantic internals."""
    errors = exc.errors()
    # Produce a single readable message from all field errors
    messages = []
    for err in errors:
        loc = " → ".join(str(p) for p in err.get("loc", []) if p != "body")
        msg = err.get("msg", "Invalid value")
        messages.append(f"{loc}: {msg}" if loc else msg)
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={"code": "VALIDATION_ERROR", "message": "; ".join(messages)},
    )


@app.exception_handler(Exception)
async def global_unhandled_exception_handler(request: Request, exc: Exception):
    """Log server-side traceback with correlation ID and return safe, sanitized error response."""
    # If it is an HTTPException, let FastAPI's default handling or re-wrap safely
    from fastapi import HTTPException
    if isinstance(exc, HTTPException):
        return JSONResponse(
            status_code=exc.status_code,
            content={"code": "HTTP_ERROR", "message": exc.detail},
            headers=exc.headers,
        )

    cid = getattr(request.state, "correlation_id", None) or str(uuid.uuid4())
    logger.error(
        f"Unhandled Server Error [Correlation-ID: {cid}] on {request.method} {request.url.path}: {exc}",
        exc_info=True,
    )
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "code": "INTERNAL_SERVER_ERROR",
            "message": "An unexpected error occurred. Please try again later.",
            "correlationId": cid,
        },
        headers={"X-Correlation-ID": cid},
    )


# ── Endpoints ─────────────────────────────────────────────────────────────────

@app.get("/api/v1/health")
async def health():
    return {
        "status": "ok",
        "service": "EduNova Backend API",
        "version": "2.0.0",
        "environment": settings.environment,
        "ai_provider": "openrouter",
        "openrouter_model": settings.openrouter_model,
        "openrouter_base_url": settings.openrouter_base_url,
        "openrouter_configured": bool(settings.openrouter_api_key),
    }


# ── Routers ───────────────────────────────────────────────────────────────────

app.include_router(auth_router)
app.include_router(chat_router)
app.include_router(courses_router)
app.include_router(roadmaps_router)
app.include_router(roadmap_websocket_router)
app.include_router(learning_router)
app.include_router(profile_router)
app.include_router(memory_router)
app.include_router(retrieve_router)
app.include_router(syllabus_sync_router)

