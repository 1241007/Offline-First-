"""
EduNova Production Corpus Re-Embedding Tool.

Re-embeds all existing syllabus chunks in Supabase using the verified
quantized ONNX multilingual-e5-small model, replacing hash-derived fallback vectors
with genuine 384-dimensional semantic embeddings.

Guarantees:
- Strictly verifies SHA-256 of model_quantized.onnx and tokenizer.json before execution.
- Preserves all genuine text, metadata, document IDs, chunk IDs, source references, and content hashes.
- Never deletes or recreates records.
- Applies 'passage: ' prefix, token_type_ids, attention-mask mean pooling, and L2 normalization.
- Validates 384 finite dimensions and unit norm before database UPDATE.
- Resumable and idempotent: tracks embedding_version='1.0-onnx-quantized'.
- Performs read-only post-ingestion verification of dimensions, nulls, and retrieval quality.
"""

import argparse
import asyncio
import logging
import math
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Tuple

# Ensure backend root is on sys.path
_BACKEND_ROOT = Path(__file__).resolve().parents[1]
if str(_BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(_BACKEND_ROOT))

from sqlalchemy import select, text
from app.core.database import get_session_factory
from app.services.local_e5_service import (
    CACHE_DIR,
    EMBEDDING_DIM,
    ONNX_QUANTIZED_SHA256,
    TOKENIZER_SHA256,
    local_e5_service,
    verify_file_sha256,
)

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("reembed_corpus")

TARGET_EMBEDDING_VERSION = "1.0-onnx-quantized"
TARGET_MODEL_NAME = "intfloat/multilingual-e5-small"


def verify_prerequisites() -> bool:
    """Verifies that the verified model and tokenizer exist with correct checksums."""
    model_file = CACHE_DIR / "model_quantized.onnx"
    tok_file = CACHE_DIR / "tokenizer.json"

    if not model_file.exists() or not tok_file.exists():
        logger.error(f"Model artifacts missing from {CACHE_DIR}. Run download_model_artifacts.py first.")
        return False

    if not verify_file_sha256(model_file, ONNX_QUANTIZED_SHA256):
        logger.error("model_quantized.onnx SHA-256 mismatch!")
        return False

    if not verify_file_sha256(tok_file, TOKENIZER_SHA256):
        logger.error("tokenizer.json SHA-256 mismatch!")
        return False

    logger.info("Prerequisites verified: Exact pinned model and tokenizer are present with valid SHA-256 hashes.")
    return True


async def reembed_all_chunks(batch_size: int = 15, dry_run: bool = False) -> Dict[str, Any]:
    """
    Fetches chunks from Supabase, generates genuine ONNX passage embeddings,
    and updates the database in safe, resumable batches.
    """
    if not verify_prerequisites():
        raise RuntimeError("Model prerequisites check failed. Aborting re-embedding.")

    session_factory = get_session_factory()
    stats = {
        "total_chunks_scanned": 0,
        "already_verified": 0,
        "reembedded": 0,
        "failed": 0,
        "errors": [],
    }

    async with session_factory() as db:
        # 1. Warm up connection pool
        logger.info("Warming up Supabase database connection...")
        t0 = time.time()
        await db.execute(text("SELECT 1;"))
        logger.info(f"Connection warmed up in {time.time() - t0:.2f}s")

        # 2. Query all syllabus chunks (ordered by id for deterministic resumption)
        logger.info("Scanning syllabus_chunks in database...")
        rows_res = await db.execute(
            text("""
                SELECT id, textbook_title, chapter, section, content, embedding_version,
                       (embedding IS NULL) AS is_null
                FROM syllabus_chunks
                ORDER BY id;
            """)
        )
        all_chunks = [dict(r._mapping) for r in rows_res.fetchall()]
        stats["total_chunks_scanned"] = len(all_chunks)
        logger.info(f"Found {len(all_chunks)} total chunks in database.")

        # Identify chunks needing re-embedding
        to_process = [c for c in all_chunks if c["embedding_version"] != TARGET_EMBEDDING_VERSION or c["is_null"]]
        stats["already_verified"] = len(all_chunks) - len(to_process)
        logger.info(f"Chunks already on '{TARGET_EMBEDDING_VERSION}': {stats['already_verified']}")
        logger.info(f"Chunks requiring re-embedding: {len(to_process)}")

        if not to_process:
            logger.info("All chunks are already up-to-date with verified ONNX embeddings.")
            return stats

        if dry_run:
            logger.info("[DRY RUN] Would re-embed %d chunks. Exiting without modifying database.", len(to_process))
            return stats

        # 3. Process in batches
        total_batches = math.ceil(len(to_process) / batch_size)
        logger.info(f"Starting re-embedding in {total_batches} batches (batch_size={batch_size})...")

        for batch_idx in range(total_batches):
            batch = to_process[batch_idx * batch_size : (batch_idx + 1) * batch_size]
            b_texts = [c["content"] for c in batch]
            b_ids = [c["id"] for c in batch]

            t_start = time.time()
            try:
                # Generate real passage embeddings with ONNX model
                vectors = await local_e5_service.embed_passages_batch(b_texts)

                # Validate vectors before updating database
                for idx, vec in enumerate(vectors):
                    if len(vec) != EMBEDDING_DIM:
                        raise ValueError(f"Vector for chunk {b_ids[idx]} has length {len(vec)}, expected {EMBEDDING_DIM}")
                    if not all(math.isfinite(x) for x in vec):
                        raise ValueError(f"Vector for chunk {b_ids[idx]} contains NaN or Inf")
                    norm = math.sqrt(sum(x * x for x in vec))
                    if not math.isclose(norm, 1.0, abs_tol=1e-4):
                        raise ValueError(f"Vector for chunk {b_ids[idx]} is not L2 normalized (norm={norm})")

                # Update batch in database
                for chunk_record, vec in zip(batch, vectors):
                    vec_str = "[" + ",".join(f"{x:.8f}" for x in vec) + "]"
                    update_sql = """
                        UPDATE syllabus_chunks
                        SET embedding = CAST(:embedding AS vector),
                            embedding_model = :model_name,
                            embedding_version = :version,
                            updated_at = NOW()
                        WHERE id = :chunk_id;
                    """
                    await db.execute(
                        text(update_sql),
                        {
                            "embedding": vec_str,
                            "model_name": TARGET_MODEL_NAME,
                            "version": TARGET_EMBEDDING_VERSION,
                            "chunk_id": chunk_record["id"],
                        }
                    )

                # Commit batch transaction
                await db.commit()
                stats["reembedded"] += len(batch)
                elapsed = time.time() - t_start
                logger.info(
                    f"Batch {batch_idx + 1}/{total_batches} ({len(batch)} chunks) "
                    f"updated in {elapsed:.2f}s [{stats['reembedded']}/{len(to_process)} completed]"
                )

            except Exception as exc:
                await db.rollback()
                logger.error(f"Error in batch {batch_idx + 1}: {exc}", exc_info=True)
                stats["failed"] += len(batch)
                stats["errors"].append(f"Batch {batch_idx + 1} ({b_ids}): {str(exc)}")
                # Re-raise if persistent failure
                raise

    logger.info(
        f"Re-embedding complete: {stats['reembedded']} re-embedded, "
        f"{stats['already_verified']} previously verified, {stats['failed']} failed."
    )
    return stats


async def verify_corpus_state() -> Dict[str, Any]:
    """Runs read-only verification queries against Supabase to prove corpus integrity."""
    session_factory = get_session_factory()
    async with session_factory() as db:
        logger.info("\n" + "=" * 60)
        logger.info("VERIFYING POST-INGESTION CORPUS INTEGRITY")
        logger.info("=" * 60)

        # 1. Total chunk and document counts
        total_chunks = await db.scalar(text("SELECT count(*) FROM syllabus_chunks;"))
        total_docs = await db.scalar(text("SELECT count(*) FROM syllabus_documents;"))
        null_embeddings = await db.scalar(text("SELECT count(*) FROM syllabus_chunks WHERE embedding IS NULL;"))

        # 2. Distinct embedding dimensions
        dim_rows = await db.execute(text("SELECT DISTINCT vector_dims(embedding) FROM syllabus_chunks;"))
        dims = [r[0] for r in dim_rows.fetchall()]

        # 3. Model and version distribution
        dist_rows = await db.execute(
            text("""
                SELECT embedding_model, embedding_version, count(*) as count
                FROM syllabus_chunks
                GROUP BY embedding_model, embedding_version
                ORDER BY count DESC;
            """)
        )
        distribution = [dict(r._mapping) for r in dist_rows.fetchall()]

        # 4. Content length and metadata sanity
        len_stats = await db.execute(
            text("""
                SELECT min(length(content)) as min_len,
                       avg(length(content))::int as avg_len,
                       max(length(content)) as max_len
                FROM syllabus_chunks;
            """)
        )
        lens = dict(len_stats.fetchone()._mapping)

        # 5. Semantic retrieval check on a known query
        query_text = "What happens when magnesium ribbon burns in oxygen?"
        q_vec = await local_e5_service.embed_query(query_text)
        q_vec_str = "[" + ",".join(f"{x:.8f}" for x in q_vec) + "]"

        top_search = await db.execute(
            text("""
                SELECT id, textbook_title, chapter, section,
                       (embedding <=> CAST(:embedding AS vector)) as cosine_distance,
                       substring(content, 1, 80) as preview
                FROM syllabus_chunks
                ORDER BY embedding <=> CAST(:embedding AS vector) ASC
                LIMIT 3;
            """),
            {"embedding": q_vec_str}
        )
        semantic_top = [dict(r._mapping) for r in top_search.fetchall()]

        results = {
            "total_documents": total_docs,
            "total_chunks": total_chunks,
            "null_embeddings": null_embeddings,
            "embedding_dimensions": dims,
            "version_distribution": distribution,
            "content_lengths": lens,
            "sample_semantic_retrieval": {
                "query": query_text,
                "top_chunks": semantic_top,
            },
        }

        logger.info(f"Documents:          {total_docs}")
        logger.info(f"Chunks:             {total_chunks}")
        logger.info(f"Null Embeddings:    {null_embeddings}")
        logger.info(f"Vector Dimensions:  {dims}")
        logger.info(f"Version Dist:       {distribution}")
        logger.info(f"Sample Query Top 1: Dist={semantic_top[0]['cosine_distance']:.4f} "
                    f"Chapter='{semantic_top[0]['chapter']}' Preview='{semantic_top[0]['preview']}'")
        logger.info("=" * 60 + "\n")
        return results


async def main_async(args):
    logger.info("Starting EduNova production corpus re-embedding...")
    reembed_stats = await reembed_all_chunks(batch_size=args.batch_size, dry_run=args.dry_run)
    if not args.dry_run:
        await verify_corpus_state()


def main():
    parser = argparse.ArgumentParser(description="Re-embed Supabase syllabus chunks with verified ONNX weights")
    parser.add_argument("--batch-size", type=int, default=15, help="Batch size for ONNX inference and DB commits")
    parser.add_argument("--dry-run", action="store_true", help="Scan and simulate without writing to database")
    parser.add_argument("--verify-only", action="store_true", help="Run read-only verification queries only")
    args = parser.parse_args()

    if args.verify_only:
        asyncio.run(verify_corpus_state())
        return

    asyncio.run(main_async(args))


if __name__ == "__main__":
    main()
