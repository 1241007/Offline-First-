import asyncio
import json
from sqlalchemy import text
from app.core.database import get_session_factory

async def check():
    factory = get_session_factory()
    async with factory() as db:
        res = await db.execute(text("""
            SELECT sc.id, sc.chapter, sc.section, sc.page_reference, sc.content, sc.textbook_title
            FROM syllabus_chunks sc
            JOIN syllabus_documents sd ON sc.document_id = sd.id
            WHERE sd.source_url LIKE '%cbseacademic.nic.in%'
              AND sc.board = 'CBSE' AND sc.class_grade = '10' AND sc.subject = 'Science'
            ORDER BY sc.page_reference, sc.id;
        """))
        chunks = []
        for row in res.fetchall():
            chunks.append({
                "id": str(row[0]),
                "chapter": row[1],
                "section": row[2],
                "page_reference": row[3],
                "content": row[4],
                "textbook_title": row[5]
            })
        with open("backend/evals/rag/authorized_cbse_chunks.json", "w", encoding="utf-8") as f:
            json.dump(chunks, f, indent=2, ensure_ascii=False)
        print(f"Exported {len(chunks)} authorized CBSE chunks to backend/evals/rag/authorized_cbse_chunks.json")

if __name__ == "__main__":
    asyncio.run(check())
