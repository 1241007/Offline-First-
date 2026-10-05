#!/usr/bin/env python3
"""Verify Phase 1 migration was successful"""

import asyncio
from sqlalchemy import text
from app.core.database import get_db


async def verify_migration():
    async for db in get_db():
        try:
            # Check existing tables are intact
            result = await db.execute(text("SELECT COUNT(*) FROM conversations"))
            conv_count = result.scalar()
            
            result = await db.execute(text("SELECT COUNT(*) FROM messages"))
            msg_count = result.scalar()
            
            print("✅ Existing tables preserved:")
            print(f"   - Conversations: {conv_count} rows")
            print(f"   - Messages: {msg_count} rows")
            
            # Check new tables exist
            new_tables = [
                "course_categories",
                "courses",
                "course_modules",
                "lessons",
                "roadmaps",
                "roadmap_items",
                "user_course_progress",
                "user_lesson_progress",
                "user_profiles"
            ]
            
            print("\n✅ New tables created:")
            for table in new_tables:
                result = await db.execute(text(f"SELECT COUNT(*) FROM {table}"))
                count = result.scalar()
                print(f"   - {table}: {count} rows")
            
            # Check migration version
            result = await db.execute(text("SELECT version_num FROM alembic_version"))
            version = result.scalar()
            print(f"\n✅ Current migration version: {version}")
            
            return True
        except Exception as e:
            print(f"❌ Verification failed: {e}")
            import traceback
            traceback.print_exc()
            return False


if __name__ == "__main__":
    success = asyncio.run(verify_migration())
    exit(0 if success else 1)
