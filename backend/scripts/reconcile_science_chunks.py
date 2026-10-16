import asyncio
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine
from app.core.config import settings

async def investigate_chunks():
    engine = create_async_engine(settings.async_database_url, pool_pre_ping=True)
    async with engine.connect() as conn:
        # Check all documents for Class 10 Science
        docs = await conn.execute(text("""
            SELECT id, title, source, source_url, chunk_count, created_at, updated_at
            FROM syllabus_documents
            WHERE class_grade = '10' AND subject = 'Science';
        """))
        for d in docs.fetchall():
            actual_chunks = await conn.scalar(text(f"SELECT count(*) FROM syllabus_chunks WHERE document_id = '{d.id}'"))
            print(f"Doc ID: {d.id} | Title: {d.title} | Source: {d.source} | Stored chunk_count: {d.chunk_count} | Actual chunks in DB: {actual_chunks}")

        # Check if there are any other documents or chunks in the database that mention Class 10 Science or 25 chunks
        print("\nAll documents in Supabase with their chunk counts:")
        all_docs = await conn.execute(text("""
            SELECT id, board, class_grade, subject, title, source, chunk_count, source_url
            FROM syllabus_documents
            WHERE class_grade = '10'
            ORDER BY subject;
        """))
        for row in all_docs.fetchall():
            print(f"Class {row[2]} {row[3]} | Title: {row[4]} | Source: {row[5]} | chunk_count: {row[6]}")

        # Check chunk count for CBSE Class 10 Science in syllabus_sync pack
        pack_sql = """
            SELECT sc.board, sc.class_grade, sc.subject, COUNT(*) as chunk_count
            FROM syllabus_chunks sc
            JOIN syllabus_documents sd ON sc.document_id = sd.id
            WHERE sd.source_url LIKE '%cbseacademic.nic.in%'
              AND sc.class_grade = '10' AND sc.subject = 'Science'
            GROUP BY sc.board, sc.class_grade, sc.subject;
        """
        pack_res = await conn.execute(text(pack_sql))
        print("\nPack chunks for Class 10 Science from cbseacademic.nic.in:")
        for r in pack_res.fetchall():
            print(r)

    await engine.dispose()

if __name__ == "__main__":
    asyncio.run(investigate_chunks())
