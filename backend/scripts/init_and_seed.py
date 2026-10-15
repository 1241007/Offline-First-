#!/usr/bin/env python3
"""
Initialize database tables and seed course catalogue and roadmaps.
"""
import sys
from pathlib import Path

backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

import asyncio
from app.core.database import Base, get_engine, get_session_factory
from app.models import (
    CourseCategory, Course, CourseModule, Lesson,
    Roadmap, RoadmapItem, UserRevisionItem,
    UserCourseProgress, UserLessonProgress, UserProfile,
    Conversation, Message
)
from scripts.seed_data import seed_database


async def init_and_seed():
    engine = get_engine()
    print("[INIT] Creating database tables if they do not exist...")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    print("[OK] Tables created successfully.")

    print("\n[SEED] Seeding courses and roadmaps...")
    session_factory = get_session_factory()
    async with session_factory() as session:
        await seed_database(session)


if __name__ == "__main__":
    asyncio.run(init_and_seed())
