import asyncio
import os
import urllib.parse
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine
from app.core.config import settings

async def audit():
    parsed = urllib.parse.urlsplit(settings.database_url)
    print("=== SUPABASE DATABASE AUDIT ===")
    print("DB Host:", parsed.hostname)
    print("DB Port:", parsed.port)
    print("DB User:", parsed.username)
    print("DB Name:", parsed.path.lstrip("/"))
    if "." in parsed.username:
        proj_ref = parsed.username.split(".", 1)[1]
    else:
        proj_ref = parsed.hostname.split(".")[0]
    print("Supabase Project Ref:", proj_ref)
    print("Supabase Dashboard URL: https://supabase.com/dashboard/project/" + proj_ref)
    
    engine = create_async_engine(settings.async_database_url, pool_pre_ping=True)
    async with engine.connect() as conn:
        res = await conn.execute(text("SELECT current_database(), current_schema(), current_user, version();"))
        db_info = res.fetchone()
        print("Current DB:", db_info[0])
        print("Current Schema:", db_info[1])
        print("Current User:", db_info[2])
        print("Postgres Version:", db_info[3][:60])
        
        # Tables in public schema
        t_res = await conn.execute(text(
            "SELECT table_schema, table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;"
        ))
        tables = t_res.fetchall()
        print("\nTables in public schema:")
        for s, t in tables:
            print(f"  {s}.{t}")
            
        # Count documents & chunks
        d_cnt = (await conn.execute(text("SELECT count(*) FROM syllabus_documents;"))).scalar()
        c_cnt = (await conn.execute(text("SELECT count(*) FROM syllabus_chunks;"))).scalar()
        c_non_null = (await conn.execute(text("SELECT count(embedding) FROM syllabus_chunks WHERE embedding IS NOT NULL;"))).scalar()
        
        print(f"\nsyllabus_documents count: {d_cnt}")
        print(f"syllabus_chunks count: {c_cnt}")
        print(f"syllabus_chunks with non-null embedding: {c_non_null}")

        # Vector dimensions
        dim_res = await conn.execute(text("SELECT vector_dims(embedding), count(*) FROM syllabus_chunks GROUP BY vector_dims(embedding);"))
        print("Vector dimensions distribution:", dim_res.fetchall())

        # Check RLS status
        rls_res = await conn.execute(text(
            "SELECT relname, relrowsecurity FROM pg_class WHERE relname IN ('syllabus_documents', 'syllabus_chunks');"
        ))
        print("RLS status (relrowsecurity):", rls_res.fetchall())

        # Check breakdown by board, class_grade, subject
        print("\nBreakdown of syllabus_documents by class and subject:")
        breakdown_res = await conn.execute(text("""
            SELECT board, class_grade, subject, count(*) as doc_count, sum(chunk_count) as total_chunks,
                   min(source_url) as sample_source
            FROM syllabus_documents
            GROUP BY board, class_grade, subject
            ORDER BY class_grade::int, subject;
        """))
        for row in breakdown_res.fetchall():
            print(f"  Class {row[1]} | {row[2]} | docs: {row[3]} | chunks: {row[4]} | sample: {row[5]}")

    await engine.dispose()

if __name__ == "__main__":
    asyncio.run(audit())
