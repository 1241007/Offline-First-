import asyncio
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import text
from app.core.database import get_session_factory

async def main():
    factory = get_session_factory()
    async with factory() as s:
        # Test SELECT CAST(:prereq AS jsonb) with asyncpg on Supabase
        res = await s.execute(
            text("SELECT CAST(:prereq AS jsonb) as val"),
            {"prereq": '["Basic computer literacy", "Problem-solving fundamentals"]'}
        )
        val = res.scalar()
        print("Successfully evaluated CAST(:prereq AS jsonb) on Supabase PostgreSQL:", val)

if __name__ == "__main__":
    asyncio.run(main())
