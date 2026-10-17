import json
import re
import asyncio
from app.core.database import get_session_factory
from sqlalchemy import text
from app.services.supabase_retrieval_service import CORROBORATION_STOPWORDS

def test_corroboration(query, content):
    if not query or not content:
        return False
    words = re.findall(r"\b[a-zA-Z]{3,}\b", query.lower())
    content_lower = content.lower()
    domain_words = [w for w in words if w not in CORROBORATION_STOPWORDS]
    if not domain_words:
        return True

    matches = [w for w in domain_words if w in content_lower or (len(w) >= 6 and w[:5] in content_lower)]

    if len(domain_words) <= 2:
        return len(matches) >= 1
    if len(domain_words) <= 4:
        return len(matches) >= 2
    return len(matches) >= 3 or (len(matches) / len(domain_words)) >= 0.30

async def run_check():
    with open('backend/evals/rag/results/eval_report_20261016_230929.json', 'r', encoding='utf-8') as f:
        data = json.load(f)

    async with get_session_factory()() as db:
        for case in data['case_diagnostics']:
            cid = case['case_id']
            q = case['question']
            ret_ids = case['retrieved_chunk_ids']
            if not ret_ids:
                print(f"{cid}: No chunks retrieved")
                continue
            top_id = ret_ids[0]
            res = await db.execute(text("SELECT content FROM syllabus_chunks WHERE id = :id"), {"id": top_id})
            content = res.scalar() or ""
            corr = test_corroboration(q, content)
            print(f"{cid} ({case['expected_behavior']}): top_id={top_id[:8]} -> corroboration={corr}")

if __name__ == "__main__":
    asyncio.run(run_check())
