import asyncio
import json
from app.core.database import get_session_factory
from sqlalchemy import text

async def main():
    session_maker = get_session_factory()
    async with session_maker() as session:
        # 1. Documents
        res = await session.execute(text("""
            SELECT id, title, board, class_grade, subject, source_url, chunk_count, ingestion_status 
            FROM syllabus_documents 
            ORDER BY class_grade, subject, title
        """))
        docs = [dict(r._mapping) for r in res.fetchall()]
        print("TOTAL DOCUMENTS IN SUPABASE:", len(docs))
        print(json.dumps(docs, indent=2, default=str))

        # 2. Check chunks per document
        res_chunks = await session.execute(text("""
            SELECT sd.id, sd.title, sd.source_url, sd.board, sd.class_grade, sd.subject, COUNT(sc.id) as actual_chunks
            FROM syllabus_documents sd
            LEFT JOIN syllabus_chunks sc ON sc.document_id = sd.id
            GROUP BY sd.id, sd.title, sd.source_url, sd.board, sd.class_grade, sd.subject
            ORDER BY sd.class_grade, sd.subject, sd.title
        """))
        doc_chunks = [dict(r._mapping) for r in res_chunks.fetchall()]
        print("\nCHUNKS PER DOCUMENT:")
        print(json.dumps(doc_chunks, indent=2, default=str))

        # 3. Chunks with or without document_id
        res_orphan = await session.execute(text("""
            SELECT COUNT(*) as total_chunks,
                   COUNT(document_id) as chunks_with_doc,
                   SUM(CASE WHEN document_id IS NULL THEN 1 ELSE 0 END) as chunks_without_doc
            FROM syllabus_chunks
        """))
        print("\nCHUNK TOTALS:", [dict(r._mapping) for r in res_orphan.fetchall()])

if __name__ == "__main__":
    asyncio.run(main())
