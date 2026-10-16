"""
EduNova Syllabus Ingestion Pipeline.

Extracts text from syllabus sources (PDF, TXT, MD), generates retrieval passages
with curriculum metadata, produces 384-dimensional multilingual embeddings,
enforces strict authorization gating (CBSE statutory curriculum frameworks vs gated NCERT textbooks),
deduplicates content by SHA-256 hash, and populates Supabase PostgreSQL.

Usage:
    python -m scripts.ingest_syllabus --source-dir ./syllabus_sources --dry-run
    python -m scripts.ingest_syllabus --source-dir ./syllabus_sources
"""

import os
import sys
import argparse
import asyncio
import hashlib
import json
import logging
import math
import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any

from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker

# Ensure backend root is on sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.core.config import settings
from app.models.syllabus import SyllabusDocument, SyllabusChunk
from app.services.local_e5_service import local_e5_service, EMBEDDING_DIM
from app.services.embedding_service import embedding_service

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("ingest_syllabus")

# Standard exclusion reason for gated NCERT textbooks without redistribution authorization
NCERT_GATED_EXCLUSION_REASON = (
    "Official NCERT textbook chapter gated pending formal institutional license / "
    "redistribution rights (Copyright Act, 1957)"
)


@dataclass
class ExtractedChunk:
    chunk_index: int
    content: str
    content_hash: str
    board: str
    class_grade: str
    subject: str
    chapter: Optional[str]
    section: Optional[str]
    page_reference: Optional[str]
    textbook_title: Optional[str]
    source_id: Optional[str]
    corpus_version: str


def compute_sha256(content: str) -> str:
    """Computes SHA-256 hex digest for chunk content deduplication."""
    return hashlib.sha256(content.strip().encode("utf-8")).hexdigest()


def chunk_text(
    text: str,
    target_chunk_chars: int = 1200,
    overlap_chars: int = 200
) -> List[str]:
    """
    Chunks text into retrieval-appropriate passages, preserving paragraph and sentence boundaries.
    """
    clean_text = text.strip()
    if not clean_text:
        return []

    # First split by double newlines (paragraphs)
    paragraphs = [p.strip() for p in clean_text.split("\n\n") if p.strip()]
    if not paragraphs:
        paragraphs = [clean_text]

    chunks: List[str] = []
    current_chunk: List[str] = []
    current_length = 0

    for para in paragraphs:
        para_len = len(para)
        if current_length + para_len + 2 > target_chunk_chars and current_chunk:
            chunk_str = "\n\n".join(current_chunk)
            chunks.append(chunk_str)

            # Build overlap from the end of current chunk
            overlap_text = chunk_str[-overlap_chars:] if len(chunk_str) > overlap_chars else ""
            current_chunk = [overlap_text, para] if overlap_text else [para]
            current_length = sum(len(p) for p in current_chunk) + len(current_chunk) * 2
        else:
            current_chunk.append(para)
            current_length += para_len + 2

    if current_chunk:
        chunk_str = "\n\n".join(current_chunk).strip()
        if chunk_str and (not chunks or chunk_str != chunks[-1]):
            chunks.append(chunk_str)

    return chunks


def parse_metadata_from_path(
    file_path: Path,
    base_dir: Path,
    override_board: Optional[str] = None,
    override_class: Optional[str] = None,
    override_subject: Optional[str] = None,
) -> Dict[str, str]:
    """
    Parses curriculum metadata from directory structure:
    base_dir/{board}/{class_grade}/{subject}/[Chapter_XX_Title.ext]
    """
    try:
        rel_path = file_path.relative_to(base_dir)
        parts = rel_path.parts
    except ValueError:
        parts = file_path.parts

    board = override_board or (parts[0] if len(parts) > 1 else "CBSE")
    class_grade = override_class or (parts[1] if len(parts) > 2 else "10")
    subject = override_subject or (parts[2] if len(parts) > 3 else "Science")

    stem = file_path.stem
    # Clean chapter title from filename like Chapter_01_Chemical_Reactions
    chapter = stem.replace("_", " ").replace("-", " ")
    if chapter.lower().startswith("chapter"):
        # e.g. "Chapter 01 Chemical Reactions" -> "Chemical Reactions"
        subparts = chapter.split(maxsplit=2)
        if len(subparts) >= 3 and subparts[1].isdigit():
            chapter = subparts[2]

    return {
        "board": board,
        "class_grade": class_grade,
        "subject": subject,
        "chapter": chapter.title(),
        "title": stem.replace("_", " ").title(),
    }


def resolve_metadata(
    file_path: Path,
    base_dir: Path,
    manifest_data: Optional[Dict[str, Any]] = None,
    override_board: Optional[str] = None,
    override_class: Optional[str] = None,
    override_subject: Optional[str] = None,
) -> Dict[str, Any]:
    """
    Resolves comprehensive curriculum metadata and authorization status by combining:
    1. Directory structure and filename parsing (fallback baseline).
    2. Sidecar .meta.json files ({filename}.meta.json or {stem}.meta.json).
    3. Directory-level sources.json manifests.
    4. Authoritative curriculum manifest (authoritative_curriculum_manifest.json).
    """
    base_meta = parse_metadata_from_path(
        file_path=file_path,
        base_dir=base_dir,
        override_board=override_board,
        override_class=override_class,
        override_subject=override_subject,
    )

    board = base_meta["board"]
    class_grade = base_meta["class_grade"]
    subject = base_meta["subject"]
    chapter = base_meta["chapter"]
    title = base_meta["title"]
    source_url = None
    document_type = "textbook_chapter"
    academic_year = "2026-27"
    authorization_status = None
    exclusion_reason = None

    # Check sidecar .meta.json
    sidecar_candidates = [
        file_path.with_name(f"{file_path.name}.meta.json"),
        file_path.with_name(f"{file_path.stem}.meta.json"),
    ]
    sidecar_meta: Dict[str, Any] = {}
    for sc in sidecar_candidates:
        if sc.is_file():
            try:
                with open(sc, "r", encoding="utf-8") as f:
                    sidecar_meta = json.load(f)
                break
            except Exception as e:
                logger.warning(f"Error reading sidecar {sc}: {e}")

    # Check directory-level sources.json
    dir_sources = file_path.parent / "sources.json"
    if not sidecar_meta and dir_sources.is_file():
        try:
            with open(dir_sources, "r", encoding="utf-8") as f:
                sdata = json.load(f)
                sidecar_meta = sdata.get(file_path.name, {})
        except Exception as e:
            logger.warning(f"Error reading sources.json in {file_path.parent}: {e}")

    # Extract fields from sidecar metadata if available
    if sidecar_meta:
        board = sidecar_meta.get("board") or board
        class_grade = str(sidecar_meta.get("class_grade") or class_grade)
        raw_subj = sidecar_meta.get("subject") or subject
        subject = raw_subj.replace("_", " ")
        title = sidecar_meta.get("title") or sidecar_meta.get("document_title") or title
        chapter = sidecar_meta.get("chapter") or chapter
        source_url = sidecar_meta.get("source_url")
        document_type = sidecar_meta.get("document_type") or document_type
        academic_year = sidecar_meta.get("academic_year") or academic_year
        authorization_status = sidecar_meta.get("authorization_status")

    # Cross-reference with authoritative_curriculum_manifest.json if available
    if manifest_data:
        classes_data = manifest_data.get("classes", {})
        cls_manifest = classes_data.get(class_grade, {})
        
        # Check authorized curriculum documents
        for auth_doc in cls_manifest.get("authorized_curriculum_documents", []):
            if auth_doc.get("filename") == file_path.name:
                title = auth_doc.get("title") or title
                source_url = auth_doc.get("source_url") or source_url
                document_type = "curriculum_framework"
                authorization_status = auth_doc.get("authorization_status") or "AUTHORIZED_FOR_INGESTION"
                break

        # Check copyrighted textbooks
        if not authorization_status:
            for book in cls_manifest.get("copyrighted_textbooks", []):
                # If subject matches and it's a textbook chapter
                if book.get("subject", "").replace("_", " ").lower() == subject.lower():
                    if "curriculum" not in file_path.name.lower():
                        authorization_status = book.get("authorization_status") or "GATED_PENDING_AUTHORIZATION"
                        exclusion_reason = NCERT_GATED_EXCLUSION_REASON
                        break

    # If still not classified, determine by document type heuristics
    if "curriculum" in file_path.name.lower() or file_path.name.startswith("Curriculum_"):
        document_type = "curriculum_framework"
        if not authorization_status:
            authorization_status = "AUTHORIZED_FOR_INGESTION"
    else:
        if not authorization_status:
            authorization_status = "GATED_PENDING_AUTHORIZATION"
            exclusion_reason = NCERT_GATED_EXCLUSION_REASON

    # Standardize subject string (e.g. Social_Science -> Social Science)
    subject = subject.replace("_", " ")

    is_authorized = bool(
        authorization_status and "AUTHORIZED" in authorization_status.upper() and "GATED" not in authorization_status.upper()
    )

    if not is_authorized and not exclusion_reason:
        exclusion_reason = NCERT_GATED_EXCLUSION_REASON

    return {
        "board": board,
        "class_grade": class_grade,
        "subject": subject,
        "chapter": chapter,
        "title": title,
        "source_url": source_url,
        "document_type": document_type,
        "academic_year": academic_year,
        "authorization_status": authorization_status,
        "is_authorized": is_authorized,
        "exclusion_reason": exclusion_reason,
    }


def extract_text_from_file(file_path: Path) -> List[Tuple[str, Optional[str]]]:
    """
    Extracts text sections with optional page references from a file.
    Returns list of (text, page_reference) tuples.
    """
    suffix = file_path.suffix.lower()

    if suffix in [".txt", ".md"]:
        with open(file_path, "r", encoding="utf-8", errors="replace") as f:
            content = f.read()
        return [(content, None)]

    if suffix == ".pdf":
        try:
            import pypdf
            import pypdf.filters
            try:
                pypdf.filters.ZLIB_MAX_OUTPUT_LENGTH = 300_000_000
            except Exception:
                pass
            reader = pypdf.PdfReader(str(file_path))
            pages = []
            for page_num, page in enumerate(reader.pages, start=1):
                try:
                    page_text = page.extract_text() or ""
                except Exception as exc:
                    logger.warning(f"Could not extract text from page {page_num} of {file_path.name}: {exc}")
                    page_text = ""
                if page_text.strip():
                    pages.append((page_text, f"Page {page_num}"))
            return pages
        except ImportError:
            try:
                import pypdf2
                reader = pypdf2.PdfReader(str(file_path))
                pages = []
                for page_num, page in enumerate(reader.pages, start=1):
                    try:
                        page_text = page.extract_text() or ""
                    except Exception:
                        page_text = ""
                    if page_text.strip():
                        pages.append((page_text, f"Page {page_num}"))
                return pages
            except ImportError:
                logger.warning(
                    f"Neither pypdf nor pypdf2 is installed. Cannot extract text from PDF: {file_path.name}"
                )
                return []

    logger.warning(f"Unsupported file format '{suffix}' for file: {file_path}")
    return []


class SyllabusIngestionPipeline:
    """Manages the full end-to-end syllabus ingestion process."""

    def __init__(self, db_session: Optional[AsyncSession] = None):
        self.db = db_session

    async def ingest_directory(
        self,
        source_dir: Path,
        dry_run: bool = False,
        corpus_version: str = "1.0",
        board_override: Optional[str] = None,
        class_override: Optional[str] = None,
        subject_override: Optional[str] = None,
    ) -> Dict[str, Any]:
        """
        Discovers files in source_dir, checks authorization, extracts text,
        chunks text, generates 384-dimensional embeddings, and saves to Supabase.
        """
        if not source_dir.exists():
            return {
                "status": "pending_sources",
                "message": f"Source directory '{source_dir}' does not exist. Actual corpus ingestion remains pending until authorized syllabus sources are available.",
                "documents_processed": 0,
                "chunks_created": 0,
            }

        # Load authoritative curriculum manifest if present
        manifest_data = None
        manifest_file = source_dir / "authoritative_curriculum_manifest.json"
        if manifest_file.is_file():
            try:
                with open(manifest_file, "r", encoding="utf-8") as mf:
                    manifest_data = json.load(mf)
                logger.info(f"Loaded authoritative curriculum manifest from {manifest_file.name}")
            except Exception as e:
                logger.warning(f"Failed to read authoritative curriculum manifest: {e}")

        # Discover supported source files
        supported_extensions = [".pdf", ".txt", ".md"]
        candidate_files = [
            p for p in source_dir.rglob("*")
            if p.is_file() and p.suffix.lower() in supported_extensions and p.name != "README.md"
        ]

        if not candidate_files:
            logger.info(
                f"No authorized syllabus files (.pdf, .txt, .md) found in '{source_dir}'. "
                "Actual corpus ingestion remains pending until authorized syllabus sources are available."
            )
            return {
                "status": "pending_sources",
                "message": "No syllabus source files found. Actual corpus ingestion remains pending until authorized syllabus sources are placed in backend/syllabus_sources.",
                "documents_processed": 0,
                "chunks_created": 0,
            }

        candidate_files.sort(key=lambda p: str(p))
        logger.info(f"Discovered {len(candidate_files)} total syllabus candidate file(s).")

        # Classify files through the authorization gate
        eligible_files: List[Tuple[Path, Dict[str, Any]]] = []
        gated_excluded_files: List[Dict[str, Any]] = []

        for f in candidate_files:
            meta = resolve_metadata(
                file_path=f,
                base_dir=source_dir,
                manifest_data=manifest_data,
                override_board=board_override,
                override_class=class_override,
                override_subject=subject_override,
            )

            # Apply CLI filters if specified
            if board_override and meta["board"].upper() != board_override.upper():
                continue
            if class_override and str(meta["class_grade"]) != str(class_override):
                continue
            if subject_override and meta["subject"].lower() != subject_override.lower():
                continue

            if meta["is_authorized"]:
                eligible_files.append((f, meta))
            else:
                gated_excluded_files.append({
                    "filename": f.name,
                    "rel_path": str(f.relative_to(source_dir)),
                    "board": meta["board"],
                    "class_grade": meta["class_grade"],
                    "subject": meta["subject"],
                    "title": meta["title"],
                    "authorization_status": meta["authorization_status"],
                    "exclusion_reason": meta["exclusion_reason"],
                })

        logger.info(
            f"Authorization Gate Summary: {len(eligible_files)} ELIGIBLE file(s), "
            f"{len(gated_excluded_files)} GATED/EXCLUDED file(s) kept offline."
        )

        total_docs = 0
        total_chunks = 0
        skipped_duplicates = 0
        failed_files = []
        processed_documents_meta = []

        for file_path, meta in eligible_files:
            try:
                sections = extract_text_from_file(file_path)
                if not sections:
                    logger.warning(f"No extractable text found in '{file_path.name}'. Skipping.")
                    failed_files.append({"file": file_path.name, "reason": "No extractable text"})
                    continue

                full_text = "\n\n".join(s[0] for s in sections)
                doc_content_hash = compute_sha256(full_text)

                doc_id = str(uuid.uuid4())
                document_chunks: List[ExtractedChunk] = []
                chunk_idx = 0

                for sec_text, page_ref in sections:
                    raw_chunks = chunk_text(sec_text)
                    for chunk_str in raw_chunks:
                        c_hash = compute_sha256(chunk_str)
                        document_chunks.append(
                            ExtractedChunk(
                                chunk_index=chunk_idx,
                                content=chunk_str,
                                content_hash=c_hash,
                                board=meta["board"],
                                class_grade=meta["class_grade"],
                                subject=meta["subject"],
                                chapter=meta["chapter"],
                                section=None,
                                page_reference=page_ref,
                                textbook_title=meta["title"],
                                source_id=doc_id,
                                corpus_version=corpus_version,
                            )
                        )
                        chunk_idx += 1

                if not document_chunks:
                    logger.warning(f"Zero chunks generated for '{file_path.name}'. Skipping.")
                    continue

                logger.info(
                    f"Processing '{file_path.name}': {len(document_chunks)} chunk(s) for "
                    f"{meta['board']} Class {meta['class_grade']} {meta['subject']} ({meta['title']})"
                )

                if dry_run:
                    total_docs += 1
                    total_chunks += len(document_chunks)
                    processed_documents_meta.append({
                        "file": file_path.name,
                        "title": meta["title"],
                        "class": meta["class_grade"],
                        "subject": meta["subject"],
                        "chunks": len(document_chunks),
                        "status": "dry_run_validated",
                    })
                    continue

                if not self.db:
                    raise RuntimeError("Database session required for non-dry-run ingestion")

                # Upsert SyllabusDocument: look up by (board, class_grade, subject, title) or source
                existing_doc_res = await self.db.execute(
                    select(SyllabusDocument).where(
                        (
                            (SyllabusDocument.title == meta["title"]) |
                            (SyllabusDocument.source == file_path.name)
                        ),
                        SyllabusDocument.board == meta["board"],
                        SyllabusDocument.class_grade == meta["class_grade"],
                        SyllabusDocument.subject == meta["subject"],
                    )
                )
                doc_record = existing_doc_res.scalars().first()

                if not doc_record:
                    doc_record = SyllabusDocument(
                        id=doc_id,
                        source=file_path.name,
                        title=meta["title"],
                        board=meta["board"],
                        class_grade=meta["class_grade"],
                        subject=meta["subject"],
                        chapter=meta["chapter"],
                        source_url=meta["source_url"],
                        content_hash=doc_content_hash,
                        corpus_version=corpus_version,
                        ingestion_status="processing",
                        chunk_count=len(document_chunks),
                    )
                    self.db.add(doc_record)
                    await self.db.flush()
                else:
                    doc_id = doc_record.id
                    # Update metadata & source_url if not already set
                    doc_record.source = file_path.name
                    doc_record.source_url = meta["source_url"] or doc_record.source_url
                    doc_record.content_hash = doc_content_hash
                    doc_record.corpus_version = corpus_version
                    doc_record.chunk_count = len(document_chunks)
                    doc_record.ingestion_status = "processing"
                    await self.db.flush()

                # Separate chunks that need insertion vs chunks that already exist
                chunks_to_insert = []
                for chunk in document_chunks:
                    existing_chunk_res = await self.db.execute(
                        select(SyllabusChunk.id).where(
                            SyllabusChunk.content_hash == chunk.content_hash
                        )
                    )
                    if existing_chunk_res.scalar():
                        skipped_duplicates += 1
                    else:
                        chunks_to_insert.append(chunk)

                if chunks_to_insert:
                    # Batch embed using local ONNX E5 service
                    batch_texts = [c.content for c in chunks_to_insert]
                    vectors = await local_e5_service.embed_passages_batch(batch_texts)

                    # Validate embeddings
                    for idx, vec in enumerate(vectors):
                        if len(vec) != EMBEDDING_DIM:
                            raise ValueError(
                                f"Vector dimension mismatch for chunk {idx}: expected {EMBEDDING_DIM}, got {len(vec)}"
                            )
                        if not all(math.isfinite(x) for x in vec):
                            raise ValueError(f"Vector for chunk {idx} contains non-finite values")

                    # Insert chunks with pgvector cast
                    insert_sql = """
                        INSERT INTO syllabus_chunks (
                            id, document_id, content, chunk_index, board, class_grade,
                            subject, chapter, section, language, textbook_title,
                            page_reference, source_id, corpus_version, content_hash,
                            embedding_model, embedding_version, embedding, created_at, updated_at
                        ) VALUES (
                            :id, :document_id, :content, :chunk_index, :board, :class_grade,
                            :subject, :chapter, :section, 'en', :textbook_title,
                            :page_reference, :source_id, :corpus_version, :content_hash,
                            'intfloat/multilingual-e5-small', :embedding_version,
                            CAST(:embedding AS vector), NOW(), NOW()
                        );
                    """
                    for chunk, vec in zip(chunks_to_insert, vectors):
                        c_id = str(uuid.uuid4())
                        vec_str = "[" + ",".join(f"{x:.8f}" for x in vec) + "]"
                        await self.db.execute(
                            text(insert_sql),
                            {
                                "id": c_id,
                                "document_id": doc_id,
                                "content": chunk.content,
                                "chunk_index": chunk.chunk_index,
                                "board": chunk.board,
                                "class_grade": chunk.class_grade,
                                "subject": chunk.subject,
                                "chapter": chunk.chapter,
                                "section": chunk.section,
                                "textbook_title": chunk.textbook_title,
                                "page_reference": chunk.page_reference,
                                "source_id": doc_id,
                                "corpus_version": chunk.corpus_version,
                                "content_hash": chunk.content_hash,
                                "embedding_version": "1.0-onnx-quantized",
                                "embedding": vec_str,
                            }
                        )
                        total_chunks += 1

                doc_record.ingestion_status = "completed"
                await self.db.commit()
                total_docs += 1
                processed_documents_meta.append({
                    "file": file_path.name,
                    "title": meta["title"],
                    "class": meta["class_grade"],
                    "subject": meta["subject"],
                    "chunks": len(document_chunks),
                    "status": "completed",
                })

            except Exception as exc:
                if self.db:
                    await self.db.rollback()
                logger.error(f"Error processing '{file_path.name}': {exc}", exc_info=True)
                failed_files.append({"file": file_path.name, "reason": str(exc)})

        return {
            "status": "completed" if not dry_run else "dry_run_completed",
            "total_candidate_files": len(candidate_files),
            "eligible_files_count": len(eligible_files),
            "gated_excluded_count": len(gated_excluded_files),
            "documents_processed": total_docs,
            "chunks_created": total_chunks,
            "duplicates_skipped": skipped_duplicates,
            "failed_files": failed_files,
            "gated_excluded_files": gated_excluded_files,
            "processed_documents": processed_documents_meta,
        }


async def main():
    parser = argparse.ArgumentParser(description="EduNova Syllabus Ingestion Pipeline")
    parser.add_argument(
        "--source-dir",
        type=str,
        default=str(Path(__file__).resolve().parent.parent / "syllabus_sources"),
        help="Directory containing syllabus files",
    )
    parser.add_argument("--dry-run", action="store_true", help="Simulate without database writes")
    parser.add_argument("--board", type=str, default=None, help="Override curriculum board")
    parser.add_argument("--class-grade", type=str, default=None, help="Override class/grade")
    parser.add_argument("--subject", type=str, default=None, help="Override subject")
    parser.add_argument("--version", type=str, default="1.0", help="Corpus version string")

    args = parser.parse_args()
    source_dir = Path(args.source_dir)

    print("=" * 70)
    print("EduNova Syllabus Ingestion Pipeline")
    print("=" * 70)
    print(f"Source Directory : {source_dir}")
    print(f"Dry Run          : {args.dry_run}")
    print(f"Curriculum Board : {args.board or 'All (CBSE default)'}")
    print(f"Class / Grade    : {args.class_grade or 'All (9-12)'}")
    print(f"Subject          : {args.subject or 'All'}")
    print(f"Corpus Version   : {args.version}")
    print("=" * 70)

    if args.dry_run:
        pipeline = SyllabusIngestionPipeline(db_session=None)
        result = await pipeline.ingest_directory(
            source_dir=source_dir,
            dry_run=True,
            corpus_version=args.version,
            board_override=args.board,
            class_override=args.class_grade,
            subject_override=args.subject,
        )
        print("\n[DRY RUN SUMMARY]")
        print(f"  Total Candidates Discovered : {result.get('total_candidate_files', 0)}")
        print(f"  Eligible Authorized Files   : {result.get('eligible_files_count', 0)}")
        print(f"  Gated Excluded Files        : {result.get('gated_excluded_count', 0)}")
        print(f"  Documents Validated         : {result.get('documents_processed', 0)}")
        print(f"  Chunks Generated            : {result.get('chunks_created', 0)}")
        print(f"  Duplicates Skipped          : {result.get('duplicates_skipped', 0)}")
        print(f"  Failures                    : {len(result.get('failed_files', []))}")
        print("\n[OK] Dry run completed successfully.")
        return

    engine = create_async_engine(
        settings.async_database_url,
        pool_pre_ping=True,
        pool_recycle=300,
    )
    async_session = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    max_retries = 3
    retry_delay = 3.0
    result = None

    for attempt in range(1, max_retries + 1):
        try:
            async with async_session() as session:
                pipeline = SyllabusIngestionPipeline(db_session=session)
                result = await pipeline.ingest_directory(
                    source_dir=source_dir,
                    dry_run=False,
                    corpus_version=args.version,
                    board_override=args.board,
                    class_override=args.class_grade,
                    subject_override=args.subject,
                )
                break
        except Exception as exc:
            logger.warning(f"Database operation failed on attempt {attempt}/{max_retries}: {exc}")
            if attempt < max_retries:
                await asyncio.sleep(retry_delay * attempt)
            else:
                await engine.dispose()
                raise

    await engine.dispose()

    print("\n[INGESTION SUMMARY]")
    print(f"  Total Candidates Discovered : {result.get('total_candidate_files', 0)}")
    print(f"  Eligible Authorized Files   : {result.get('eligible_files_count', 0)}")
    print(f"  Gated Excluded Files        : {result.get('gated_excluded_count', 0)}")
    print(f"  Documents Processed/Updated : {result.get('documents_processed', 0)}")
    print(f"  Chunks Created/Persisted    : {result.get('chunks_created', 0)}")
    print(f"  Duplicates Skipped          : {result.get('duplicates_skipped', 0)}")
    print(f"  Failures                    : {len(result.get('failed_files', []))}")
    print("\n[OK] Ingestion pipeline execution completed.")


if __name__ == "__main__":
    asyncio.run(main())
