"""
EduNova Syllabus Ingestion Pipeline.

Extracts text from syllabus sources (PDF, TXT, MD), generates retrieval passages
with curriculum metadata, produces 384-dimensional multilingual embeddings,
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
import logging
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
from app.services.embedding_service import embedding_service

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("ingest_syllabus")


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
    rel_path = file_path.relative_to(base_dir)
    parts = rel_path.parts

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
            reader = pypdf.PdfReader(str(file_path))
            pages = []
            for page_num, page in enumerate(reader.pages, start=1):
                page_text = page.extract_text() or ""
                if page_text.strip():
                    pages.append((page_text, f"Page {page_num}"))
            return pages
        except ImportError:
            try:
                import pypdf2
                reader = pypdf2.PdfReader(str(file_path))
                pages = []
                for page_num, page in enumerate(reader.pages, start=1):
                    page_text = page.extract_text() or ""
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
        Discovers files in source_dir, chunks them, generates embeddings,
        and saves them to Supabase.
        """
        if not source_dir.exists():
            return {
                "status": "pending_sources",
                "message": f"Source directory '{source_dir}' does not exist. Actual corpus ingestion remains pending until authorized syllabus sources are available.",
                "documents_processed": 0,
                "chunks_created": 0,
            }

        # Discover supported source files
        supported_extensions = [".pdf", ".txt", ".md"]
        files = [
            p for p in source_dir.rglob("*")
            if p.is_file() and p.suffix.lower() in supported_extensions and p.name != "README.md"
        ]

        if not files:
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

        logger.info(f"Discovered {len(files)} syllabus source file(s) for ingestion.")
        total_docs = 0
        total_chunks = 0
        skipped_duplicates = 0

        for file_path in files:
            meta = parse_metadata_from_path(
                file_path=file_path,
                base_dir=source_dir,
                override_board=board_override,
                override_class=class_override,
                override_subject=subject_override,
            )

            sections = extract_text_from_file(file_path)
            if not sections:
                logger.warning(f"No extractable text found in '{file_path.name}'. Skipping.")
                continue

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
                            textbook_title=f"{meta['board']} Class {meta['class_grade']} {meta['subject']}",
                            source_id=doc_id,
                            corpus_version=corpus_version,
                        )
                    )
                    chunk_idx += 1

            if not document_chunks:
                continue

            logger.info(
                f"Processing '{file_path.name}': {len(document_chunks)} chunk(s) for "
                f"{meta['board']} Class {meta['class_grade']} {meta['subject']} ({meta['chapter']})"
            )

            if dry_run:
                total_docs += 1
                total_chunks += len(document_chunks)
                continue

            if not self.db:
                raise RuntimeError("Database session required for non-dry-run ingestion")

            # Check for existing document by title & board/class/subject
            existing_doc_res = await self.db.execute(
                select(SyllabusDocument).where(
                    SyllabusDocument.title == meta["title"],
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
                    corpus_version=corpus_version,
                    ingestion_status="processing",
                    chunk_count=len(document_chunks),
                )
                self.db.add(doc_record)
                await self.db.flush()
            else:
                doc_id = doc_record.id

            # Insert chunks with deduplication
            for chunk in document_chunks:
                # Check deduplication by content_hash
                existing_chunk_res = await self.db.execute(
                    select(SyllabusChunk.id).where(SyllabusChunk.content_hash == chunk.content_hash)
                )
                if existing_chunk_res.scalar():
                    skipped_duplicates += 1
                    continue

                # Generate 384-dimensional passage embedding
                # Notice: E5 models require "passage: " prefix for document passages!
                prefixed_passage = f"passage: {chunk.content}"
                emb_vector = await embedding_service.embed_query(prefixed_passage)

                chunk_id = str(uuid.uuid4())
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

                await self.db.execute(
                    text(insert_sql),
                    {
                        "id": chunk_id,
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
                        "source_id": chunk.source_id,
                        "corpus_version": chunk.corpus_version,
                        "content_hash": chunk.content_hash,
                        "embedding_version": corpus_version,
                        "embedding": str(emb_vector),
                    }
                )
                total_chunks += 1

            doc_record.ingestion_status = "completed"
            doc_record.chunk_count = len(document_chunks)
            await self.db.commit()
            total_docs += 1

        return {
            "status": "completed" if not dry_run else "dry_run_completed",
            "documents_processed": total_docs,
            "chunks_created": total_chunks,
            "duplicates_skipped": skipped_duplicates,
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

    print(f"Starting EduNova Syllabus Ingestion Pipeline")
    print(f"Source Directory: {source_dir}")
    print(f"Dry Run: {args.dry_run}")

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
        print(f"Result: {result}")
        return

    engine = create_async_engine(settings.async_database_url)
    async_session = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

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
        print(f"Result: {result}")

    await engine.dispose()


if __name__ == "__main__":
    asyncio.run(main())
