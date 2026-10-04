import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes.chat import router as chat_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="EduNova AI Chat API",
    description="Backend for EduNova AI Chat — Gemini + Supabase PostgreSQL",
    version="1.0.0",
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
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "Accept"],
)


@app.get("/api/v1/health")
async def health():
    return {"status": "ok", "service": "EduNova AI Chat API"}


app.include_router(chat_router)

logger.info("EduNova AI Chat API started")
