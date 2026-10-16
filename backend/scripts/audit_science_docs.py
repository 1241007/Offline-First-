import asyncio
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine
from app.core.config import settings

async def check():
    engine = create_async_engine(settings.async_database_url, pool_pre_ping=True)
    async with engine.connect() as conn:
        res = await conn.execute(text("""
            SELECT id, title, source, source_url, chunk_count
            FROM syllabus_documents 
            WHERE class_grade = '10' AND subject = 'Science'
            ORDER BY created_at ASC;
        """))
        rows = res.fetchall()
        print(f"Total Class 10 Science documents in Supabase: {len(rows)}")
        for r in rows:
            print(f"ID: {r.id} | Chunks: {r.chunk_count} | Title: {r.title} | Source: {r.source} | URL: {r.source_url}")
    await engine.dispose()

if __name__ == "__main__":
    asyncio.run(check())
