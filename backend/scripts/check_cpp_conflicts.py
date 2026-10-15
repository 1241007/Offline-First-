import asyncio
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import text
from app.core.database import get_session_factory

async def main():
    factory = get_session_factory()
    async with factory() as conn:
        cid = "41235d86-71c3-58c9-91bd-c2162a7b5fb8"
        c_by_id = await conn.execute(text("SELECT id, name, slug FROM courses WHERE id = :cid"), {"cid": cid})
        rows_id = c_by_id.fetchall()
        print("Course by ID:", rows_id)

        c_by_slug = await conn.execute(text("SELECT id, name, slug FROM courses WHERE slug = 'cpp-programming'"))
        rows_slug = c_by_slug.fetchall()
        print("Course by Slug:", rows_slug)

        cat = await conn.execute(text("SELECT id, name FROM course_categories WHERE name = 'Programming'"))
        print("Programming category:", cat.fetchall())

        # Check existing lesson resource URLs count
        res_cnt = await conn.execute(text("""
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'lessons' AND column_name = 'resource_url'
        """))
        print("lessons.resource_url column exists count:", res_cnt.scalar())

if __name__ == "__main__":
    asyncio.run(main())
