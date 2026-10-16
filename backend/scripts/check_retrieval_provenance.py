import asyncio
from app.core.database import get_session_factory
from app.schemas.rag import RAGRequestContext, AppMode, CorpusScope
from app.services.supabase_retrieval_service import SupabaseRetrievalService
from sqlalchemy import text

from app.core.config import settings
from app.services.embedding_service import embedding_service

async def check():
    settings.retrieval_timeout_seconds = 15.0
    await embedding_service.embed_query("warmup")
    factory = get_session_factory()
    for attempt in range(3):
        try:
            async with factory() as db:
                await db.execute(text("SELECT 1;"))
                svc = SupabaseRetrievalService(db=db)
                ctx = RAGRequestContext(
                    execution_mode="ONLINE",
                    app_mode=AppMode.STUDENT,
                    corpus_scope=CorpusScope.CURRICULUM,
                    board="CBSE",
                    class_grade="10",
                    subject="Science"
                )
                resp = await svc.retrieve("Ohm's law resistance current voltage in electric circuits", ctx, top_k=5)
                print(f"Attempt {attempt + 1}: Outcome={resp.evidence_outcome}")
                print("Total retrieved chunks:", len(resp.chunks))
                for i, c in enumerate(resp.chunks):
                    doc_info = (await db.execute(text(f"SELECT title, source, source_url FROM syllabus_documents WHERE id = '{c.source_id}'"))).fetchone()
                    print(f"Chunk {i+1}: doc={c.source_id} | title={doc_info[0]} | source_url={doc_info[2]}")
                if resp.chunks:
                    return
        except Exception as e:
            print(f"Attempt {attempt + 1} failed: {e}")
            await asyncio.sleep(1.0)

if __name__ == "__main__":
    asyncio.run(check())
