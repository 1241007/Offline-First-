import asyncio
from app.core.database import get_session_factory
from sqlalchemy import text
from app.services.supabase_retrieval_service import check_lexical_corroboration, CORROBORATION_STOPWORDS
import re

async def check():
    async with get_session_factory()() as db:
        res = await db.execute(text("SELECT content FROM syllabus_chunks WHERE id = '221632d5-52ef-415a-bb57-9c3ae1d9ba30'"))
        content = res.scalar()
        q = "Calculate the drift velocity of electrons using Fermi-Dirac distribution in gallium arsenide semiconductor diodes."
        words = re.findall(r"\b[a-zA-Z]{3,}\b", q.lower())
        domain_words = [w for w in words if w not in CORROBORATION_STOPWORDS]
        print("Domain words:", domain_words)
        matches = [w for w in domain_words if w in content.lower() or (len(w) >= 6 and w[:5] in content.lower())]
        print("Matches in real chunk content:", matches)
        print("Result:", check_lexical_corroboration(q, content))

if __name__ == "__main__":
    asyncio.run(check())
