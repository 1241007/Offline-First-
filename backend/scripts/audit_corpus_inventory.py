"""
Repeatable, secret-safe database corpus and schema inventory script for EduNova.

Audits:
- Documents in `syllabus_documents`
- Chunks in `syllabus_chunks`
- Embedding dimensions, null counts, and model version distribution
- Content-hash deduplication integrity
- Official source provenance against verified NCERT/CBSE authorities
- pgvector IVFFlat and GIN full-text index presence
"""

import argparse
import asyncio
import json
import logging
import sys
from pathlib import Path
from typing import Any, Dict

# Ensure backend root is on sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy import text
from app.core.config import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("corpus_audit")


async def run_corpus_audit() -> Dict[str, Any]:
    """Queries live Supabase PostgreSQL to perform a non-destructive, read-only audit."""
    engine = create_async_engine(settings.async_database_url)
    audit_data: Dict[str, Any] = {}

    async with engine.connect() as conn:
        # 1. Document table checks
        doc_count = await conn.scalar(text("SELECT count(*) FROM syllabus_documents;"))
        doc_rows = await conn.execute(
            text("""
                SELECT id, title, board, class_grade, subject, chapter, 
                       ingestion_status, chunk_count, source_url, content_hash,
                       corpus_version, created_at, updated_at
                FROM syllabus_documents
                ORDER BY title;
            """)
        )
        documents = [dict(r._mapping) for r in doc_rows.fetchall()]

        # 2. Chunk table checks
        chunk_count = await conn.scalar(text("SELECT count(*) FROM syllabus_chunks;"))
        null_embeddings = await conn.scalar(
            text("SELECT count(*) FROM syllabus_chunks WHERE embedding IS NULL;")
        )

        dim_rows = await conn.execute(
            text("SELECT DISTINCT vector_dims(embedding) FROM syllabus_chunks WHERE embedding IS NOT NULL;")
        )
        embedding_dims = [r[0] for r in dim_rows.fetchall()]

        model_rows = await conn.execute(
            text("""
                SELECT embedding_model, embedding_version, count(*) 
                FROM syllabus_chunks 
                GROUP BY embedding_model, embedding_version;
            """)
        )
        models = [
            {"model": r[0], "version": r[1], "chunk_count": r[2]}
            for r in model_rows.fetchall()
        ]

        # 3. Content hash deduplication check
        dup_hashes_count = await conn.scalar(
            text("""
                SELECT count(*) FROM (
                    SELECT content_hash 
                    FROM syllabus_chunks 
                    WHERE content_hash IS NOT NULL 
                    GROUP BY content_hash 
                    HAVING count(*) > 1
                ) t;
            """)
        )

        # 4. Ingestion status distribution
        status_rows = await conn.execute(
            text("SELECT ingestion_status, count(*) FROM syllabus_documents GROUP BY ingestion_status;")
        )
        status_distribution = {r[0]: r[1] for r in status_rows.fetchall()}

        # 5. Check vector & full-text search indexes
        index_rows = await conn.execute(
            text("""
                SELECT indexname, indexdef 
                FROM pg_indexes 
                WHERE tablename = 'syllabus_chunks';
            """)
        )
        indexes = {r[0]: r[1] for r in index_rows.fetchall()}
        has_ivfflat = any("ivfflat" in idx_def.lower() for idx_def in indexes.values())
        has_gin_fts = any("gin" in idx_def.lower() for idx_def in indexes.values())

        # 6. Provenance domain validation
        verified_domains = {"ncert.nic.in", "cbseacademic.nic.in"}
        provenance_check = []
        for doc in documents:
            url = doc.get("source_url") or ""
            is_official = any(domain in url for domain in verified_domains)
            provenance_check.append({
                "document_title": doc.get("title"),
                "source_url": url,
                "is_official_verified": is_official,
                "chunk_count": doc.get("chunk_count"),
            })

        audit_data = {
            "summary": {
                "total_documents": doc_count,
                "total_chunks": chunk_count,
                "null_embeddings": null_embeddings,
                "embedding_dimensions": embedding_dims,
                "duplicate_chunk_content_hashes": dup_hashes_count,
                "ingestion_statuses": status_distribution,
                "indexes": {
                    "has_ivfflat_vector_index": has_ivfflat,
                    "has_gin_fts_index": has_gin_fts,
                    "all_indexes": list(indexes.keys()),
                },
                "models": models,
            },
            "documents": documents,
            "provenance": provenance_check,
        }

    await engine.dispose()
    return audit_data


def main():
    parser = argparse.ArgumentParser(description="EduNova Database Corpus & Schema Inventory Audit")
    parser.add_argument("--output", type=str, default=None, help="Path to output JSON audit report")
    args = parser.parse_args()

    print("=" * 70)
    print("EduNova Live Database Corpus & Schema Inventory Audit")
    print("=" * 70)

    audit_result = asyncio.run(run_corpus_audit())
    summary = audit_result["summary"]

    print(f"\n[SUMMARY]")
    print(f"  Total Documents        : {summary['total_documents']}")
    print(f"  Total Chunks           : {summary['total_chunks']}")
    print(f"  Null Embeddings        : {summary['null_embeddings']}")
    print(f"  Embedding Dimensions   : {summary['embedding_dimensions']}")
    print(f"  Duplicate Content Hashes: {summary['duplicate_chunk_content_hashes']}")
    print(f"  Ingestion Statuses     : {summary['ingestion_statuses']}")
    print(f"  IVFFlat Index Present  : {summary['indexes']['has_ivfflat_vector_index']}")
    print(f"  GIN FTS Index Present  : {summary['indexes']['has_gin_fts_index']}")
    print(f"  Models & Versions      : {summary['models']}")

    print(f"\n[PROVENANCE VERIFICATION]")
    official_count = sum(1 for p in audit_result["provenance"] if p["is_official_verified"])
    print(f"  Official Source Provenance: {official_count}/{len(audit_result['provenance'])} documents verified")
    for p in audit_result["provenance"]:
        status_tag = "VERIFIED" if p["is_official_verified"] else "UNVERIFIED"
        print(f"  [{status_tag}] {p['document_title']} ({p['chunk_count']} chunks) -> {p['source_url']}")

    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(audit_result, f, indent=2, default=str)
        print(f"\n[OK] Full audit written to {out_path}")


if __name__ == "__main__":
    main()
