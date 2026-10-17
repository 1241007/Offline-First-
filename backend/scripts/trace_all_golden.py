import asyncio
import json
from app.core.config import settings
settings.retrieval_timeout_seconds = 15.0
from app.core.database import get_session_factory
from app.schemas.rag import RAGRequestContext, AppMode, CorpusScope
from app.services.supabase_retrieval_service import SupabaseRetrievalService

async def trace_all_golden():
    with open('backend/evals/rag/golden_set.jsonl', 'r', encoding='utf-8') as f:
        cases = [json.loads(line) for line in f if line.strip() and not line.startswith('#')]

    async with get_session_factory()() as db:
        svc = SupabaseRetrievalService(db=db)
        print(f"{'Case ID':<15} | {'Exp':<10} | {'Outcome':<10} | {'Top-1 Chunk ID':<36} | {'Sim':<6} | {'Norm':<6} | {'Corrob'}")
        print("-" * 105)
        for c in cases:
            cid = c['case_id']
            q = c['question']
            cur = c.get('curriculum', {})
            cat = c.get('category', '')
            exp = c.get('expected_behavior', 'SUFFICIENT')
            
            if cat == "unconfigured_general_mode":
                ctx = RAGRequestContext(app_mode=AppMode.GENERAL, corpus_scope=CorpusScope.GENERAL_DOCUMENTS)
            elif cat == "missing_profile_trap" or not cur.get("board"):
                ctx = RAGRequestContext(app_mode=AppMode.STUDENT, corpus_scope=CorpusScope.CURRICULUM, board=None, class_grade=None, subject=cur.get("subject"))
            else:
                ctx = RAGRequestContext(app_mode=AppMode.STUDENT, corpus_scope=CorpusScope.CURRICULUM, board=cur.get("board"), class_grade=cur.get("class_grade"), subject=cur.get("subject"), chapter=cur.get("chapter"))
            
            resp = await svc.retrieve(q, ctx, top_k=5)
            top = resp.chunks[0] if resp.chunks else None
            top_id = top.chunk_id if top else "NONE"
            sim = f"{top.similarity_score:.3f}" if top and top.similarity_score is not None else "N/A"
            norm = f"{top.normalized_score:.3f}" if top and top.normalized_score is not None else "N/A"
            
            from app.services.supabase_retrieval_service import check_lexical_corroboration
            corrob = check_lexical_corroboration(q, top.content) if top else "N/A"
            print(f"{cid:<15} | {exp:<10} | {resp.evidence_outcome.value:<10} | {top_id:<36} | {sim:<6} | {norm:<6} | {corrob}")

if __name__ == "__main__":
    asyncio.run(trace_all_golden())
