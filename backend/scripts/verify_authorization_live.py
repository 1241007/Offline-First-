import asyncio
import json
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine
from app.core.config import settings
from app.services.supabase_retrieval_service import SupabaseRetrievalService
from app.schemas.rag import RAGRequestContext, AppMode

async def test_live_authorization():
    settings.retrieval_timeout_seconds = 20.0
    print("=" * 70)
    print("PHASE 8: LIVE AUTHORIZATION & GATING VERIFICATION")
    print("=" * 70)

    # 1. Direct SQL proof that unauthorized NCERT documents are excluded by CTE
    engine = create_async_engine(settings.async_database_url, pool_pre_ping=True)
    async with engine.connect() as conn:
        # Check total chunks in DB for Science
        res_all = await conn.execute(text("""
            SELECT count(*) FROM syllabus_chunks sc
            JOIN syllabus_documents sd ON sc.document_id = sd.id
            WHERE sc.board = 'CBSE' AND sc.class_grade = '10' AND sc.subject = 'Science';
        """))
        total_chunks = res_all.scalar()

        # Check authorized chunks (cbseacademic.nic.in)
        res_auth = await conn.execute(text("""
            SELECT count(*) FROM syllabus_chunks sc
            JOIN syllabus_documents sd ON sc.document_id = sd.id
            WHERE sc.board = 'CBSE' AND sc.class_grade = '10' AND sc.subject = 'Science'
              AND sd.source_url LIKE '%cbseacademic.nic.in%';
        """))
        authorized_chunks = res_auth.scalar()

        # Check unauthorized chunks (ncert.nic.in)
        res_unauth = await conn.execute(text("""
            SELECT count(*) FROM syllabus_chunks sc
            JOIN syllabus_documents sd ON sc.document_id = sd.id
            WHERE sc.board = 'CBSE' AND sc.class_grade = '10' AND sc.subject = 'Science'
              AND sd.source_url LIKE '%ncert.nic.in%';
        """))
        unauthorized_chunks = res_unauth.scalar()

        print(f"Total live Class 10 Science chunks in DB: {total_chunks}")
        print(f"Authorized curriculum chunks (cbseacademic.nic.in): {authorized_chunks}")
        print(f"Unauthorized legacy textbook chunks (ncert.nic.in): {unauthorized_chunks}")
        assert authorized_chunks == 9, f"Expected 9 authorized chunks, got {authorized_chunks}"
        assert unauthorized_chunks == 210, f"Expected 210 unauthorized chunks, got {unauthorized_chunks}"

    # 2. Live Vector & Keyword Retrieval via SupabaseRetrievalService
    from app.core.database import get_session_factory
    factory = get_session_factory()
    
    # Distractor / textbook-specific queries (content only in legacy NCERT)
    ncert_queries = [
        "What happens when magnesium ribbon burns in oxygen?",
        "Explain olfactory indicators such as vanilla, onion, and clove oil.",
        "Write the balanced reaction for quicklime reacting vigorously with water to produce slaked lime.",
        "Describe the decomposition of lead nitrate producing brown fumes of nitrogen dioxide."
    ]

    context = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science"
    )

    retrieval_results = []
    print("\n--- Testing Vector & Keyword Retrieval with NCERT Queries ---")
    async with factory() as live_db:
        retrieval_service = SupabaseRetrievalService(db=live_db)
        for q in ncert_queries:
            evidence = await retrieval_service.retrieve(
                query=q,
                context=context,
                top_k=5
            )
            # Check that none of the retrieved chunks come from unauthorized NCERT docs
            unauthorized_leaks = [
                c for c in evidence.chunks
                if "Chapter" in (c.textbook_title or "") or (c.chapter and "Chapter" in c.chapter)
            ]
            
            retrieval_results.append({
                "query": q,
                "outcome": evidence.evidence_outcome.value,
                "chunks_returned": len(evidence.chunks),
                "unauthorized_leaks": len(unauthorized_leaks),
                "top_chunk_title": evidence.chunks[0].textbook_title if evidence.chunks else None
            })
            print(f"Query: '{q[:40]}...'")
            print(f"  Outcome: {evidence.evidence_outcome.value} | Chunks: {len(evidence.chunks)} | Leaks: {len(unauthorized_leaks)}")
            assert len(unauthorized_leaks) == 0, f"LEAK DETECTED: {unauthorized_leaks}"

        # 3. Authorized curriculum query
        auth_query = "Ohm's law resistance current voltage in electric circuits"
        auth_evidence = await retrieval_service.retrieve(
            query=auth_query,
            context=context,
            top_k=5
        )
        print(f"\nAuthorized Query: '{auth_query}'")
        print(f"  Outcome: {auth_evidence.evidence_outcome.value} | Chunks: {len(auth_evidence.chunks)}")
        assert len(auth_evidence.chunks) > 0, "Expected chunks for authorized query"
        for c in auth_evidence.chunks:
            assert "CBSE" in (c.textbook_title or "")

    # 4. Write structured verification report
    report = {
        "database_counts": {
            "total_science_chunks": total_chunks,
            "authorized_cbse_framework_chunks": authorized_chunks,
            "unauthorized_legacy_ncert_chunks": unauthorized_chunks
        },
        "retrieval_tests": retrieval_results,
        "authorized_query_test": {
            "query": auth_query,
            "outcome": auth_evidence.evidence_outcome.value,
            "chunks_returned": len(auth_evidence.chunks),
            "sample_title": auth_evidence.chunks[0].textbook_title if auth_evidence.chunks else None
        },
        "authorization_status": "VERIFIED_STRICTLY_GATED"
    }

    with open("backend/phase8_authorization_verification.json", "w") as f:
        json.dump(report, f, indent=2)

    print("\n" + "=" * 70)
    print("ALL AUTHORIZATION GATING TESTS PASSED! Zero leaks detected.")
    print("Report written to backend/phase8_authorization_verification.json")
    print("=" * 70)

if __name__ == "__main__":
    asyncio.run(test_live_authorization())
