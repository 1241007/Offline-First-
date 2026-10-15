#!/usr/bin/env python3
"""Read-only inspection script for Supabase live schema and course resources."""
import asyncio
import sys
from pathlib import Path

# Add backend directory to path
backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from sqlalchemy import text
from app.core.database import get_session_factory
from app.core.config import settings
from urllib.parse import urlsplit


async def inspect():
    parsed = urlsplit(settings.database_url)
    print(f"Target Dialect: {parsed.scheme}")
    print(f"Target Host: {parsed.hostname}")
    print(f"Target Port: {parsed.port}")
    print(f"Target Database: {parsed.path.lstrip('/')}")
    print()

    factory = get_session_factory()
    async with factory() as session:
        # 1. Alembic version
        res = await session.execute(text("SELECT version_num FROM alembic_version"))
        version = res.scalar()
        print(f"Live Alembic Revision: {version}")

        # 2. Table list
        res = await session.execute(
            text("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name")
        )
        tables = [r[0] for r in res.fetchall()]
        print(f"Public Tables Count: {len(tables)}")
        print(f"Public Tables: {tables}")
        print()

        # 3. Check columns in key tables
        for t in ["courses", "lessons", "course_categories", "course_modules", "user_revision_items"]:
            if t in tables:
                res = await session.execute(
                    text(f"SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '{t}' ORDER BY ordinal_position")
                )
                cols = [f"{r[0]} ({r[1]})" for r in res.fetchall()]
                print(f"Columns in {t}: {', '.join(cols)}")
            else:
                print(f"Table '{t}' DOES NOT EXIST on live database")

        print()

        # 4. Row counts
        for t in tables:
            res = await session.execute(text(f'SELECT COUNT(*) FROM "{t}"'))
            count = res.scalar()
            print(f"Row count [{t}]: {count}")

        print()

        # 5. Check courses table data
        if "courses" in tables:
            res = await session.execute(text("SELECT id, name, slug FROM courses ORDER BY name"))
            courses = res.fetchall()
            print(f"Total Courses in Supabase: {len(courses)}")
            for c in courses:
                print(f"  - Course: id={c[0]}, name={c[1]}, slug={c[2]}")

        print()

        # 6. Check lessons with resource_url
        if "lessons" in tables:
            res = await session.execute(text("SELECT COUNT(*) FROM lessons WHERE resource_url IS NOT NULL"))
            lessons_with_url = res.scalar()
            res_total = await session.execute(text("SELECT COUNT(*) FROM lessons"))
            total_lessons = res_total.scalar()
            print(f"Lessons with resource_url: {lessons_with_url} / {total_lessons}")


if __name__ == "__main__":
    asyncio.run(inspect())
