import asyncio
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import text
from app.core.database import get_session_factory
from app.core.config import settings

async def main():
    factory = get_session_factory()
    async with factory() as conn:
        revs = await conn.execute(text("SELECT version_num FROM alembic_version"))
        print("alembic_version table content:", [r[0] for r in revs.fetchall()])

        tables = await conn.execute(text("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
            ORDER BY table_name;
        """))
        table_names = [t[0] for t in tables.fetchall()]
        print("\nTable Row Counts:")
        for t in table_names:
            try:
                cnt_res = await conn.execute(text(f'SELECT COUNT(*) FROM "{t}"'))
                cnt = cnt_res.scalar()
                print(f"  {t}: {cnt} rows")
            except Exception as e:
                print(f"  {t}: ERROR ({e})")

        # Check key columns for master branch tables
        for test_table in ["users", "roadmaps", "roadmap_assessment_sessions", "conversations", "messages", "syllabus_documents", "syllabus_chunks", "course_categories", "courses", "lessons"]:
            if test_table in table_names:
                cols = await conn.execute(text(f"""
                    SELECT column_name, data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = '{test_table}'
                    ORDER BY ordinal_position;
                """))
                print(f"\nColumns in {test_table}:")
                for c in cols.fetchall():
                    print(f"    {c[0]} ({c[1]})")
            else:
                print(f"\nTable {test_table} DOES NOT EXIST in Supabase")

if __name__ == "__main__":
    asyncio.run(main())
