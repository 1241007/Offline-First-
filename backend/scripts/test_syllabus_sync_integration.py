"""Integration tests against Supabase for syllabus pack synchronization endpoints."""

import sys
import asyncio
from pathlib import Path

# Add backend to path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.core.database import get_session_factory
from app.api.routes.syllabus_sync import (
    list_available_packs,
    get_pack_manifest,
    get_pack_chunks,
)


async def run_integration_tests():
    print("=" * 70)
    print("SYLLABUS PACK SYNC ENDPOINT INTEGRATION VERIFICATION")
    print("=" * 70)

    session_maker = get_session_factory()
    async with session_maker() as session:
        # 1. Test Available Packs List
        print("\n[1] Testing Available Packs List...")
        packs_resp = await list_available_packs(user_id="test-user-sync", db=session)
        print(f"Total packs returned: {packs_resp.total_count}")
        assert packs_resp.total_count == 24, f"Expected 24 packs, got {packs_resp.total_count}"
        print("  PASS: Exactly 24 packs returned across Classes 9, 10, 11, 12.")

        # 2. Test Authorization Gating for Class 10 Science
        print("\n[2] Testing Authorization Gating on cbse-10-science...")
        sci_pack = next((p for p in packs_resp.packs if p.pack_id == "cbse-10-science"), None)
        assert sci_pack is not None, "cbse-10-science pack missing!"
        print(f"  Class 10 Science Pack Chunk Count: {sci_pack.chunk_count}")
        assert sci_pack.chunk_count == 9, f"Expected 9 authorized chunks, got {sci_pack.chunk_count} (textbooks must be excluded!)"
        print("  PASS: Copyrighted NCERT textbooks are excluded; exactly 9 authorized curriculum chunks exposed.")

        # 3. Test Manifest Endpoint & Deterministic Content Hash
        print("\n[3] Testing Manifest for cbse-10-mathematics...")
        math_manifest = await get_pack_manifest(pack_id="cbse-10-mathematics", user_id="test-user-sync", db=session)
        print(f"  Pack ID: {math_manifest.pack_id}")
        print(f"  Board: {math_manifest.board}")
        print(f"  Class: {math_manifest.class_grade}")
        print(f"  Subject: {math_manifest.subject}")
        print(f"  Academic Year: {math_manifest.academic_year}")
        print(f"  Chunk Count: {math_manifest.chunk_count}")
        print(f"  Embedding Dimension: {math_manifest.embedding_dimension}")
        print(f"  Content Hash: {math_manifest.content_hash}")
        assert math_manifest.chunk_count == 10
        assert math_manifest.embedding_dimension == 384
        assert math_manifest.content_hash is not None and len(math_manifest.content_hash) == 64
        assert math_manifest.academic_year == "2026-27"
        print("  PASS: Manifest contains valid SHA-256 content_hash and 2026-27 academic year.")

        # 4. Test Paginated Chunks Endpoint
        print("\n[4] Testing Paginated Chunks for cbse-10-mathematics...")
        page1 = await get_pack_chunks(pack_id="cbse-10-mathematics", cursor=None, limit=5, user_id="test-user-sync", db=session)
        print(f"  Page 1: returned {len(page1.chunks)} chunks, has_more={page1.has_more}, next_cursor={page1.next_cursor}")
        assert len(page1.chunks) == 5
        assert page1.has_more is True
        assert page1.next_cursor is not None
        assert len(page1.chunks[0].embedding) == 384, f"Expected 384 vector dimensions, got {len(page1.chunks[0].embedding)}"

        page2 = await get_pack_chunks(pack_id="cbse-10-mathematics", cursor=page1.next_cursor, limit=5, user_id="test-user-sync", db=session)
        print(f"  Page 2: returned {len(page2.chunks)} chunks, has_more={page2.has_more}, next_cursor={page2.next_cursor}")
        assert len(page2.chunks) == 5
        assert page2.has_more is False
        assert page2.next_cursor is None

        # Verify no duplicate chunk IDs across pages
        p1_ids = {c.chunk_id for c in page1.chunks}
        p2_ids = {c.chunk_id for c in page2.chunks}
        assert len(p1_ids.intersection(p2_ids)) == 0, "Duplicate chunk IDs returned across paginated pages!"
        print("  PASS: Cursor pagination works perfectly with 0 duplicates across pages.")

        print("\n" + "=" * 70)
        print("ALL SYLLABUS SYNC ENDPOINT TESTS PASSED SUCCESSFULLY!")
        print("=" * 70)


if __name__ == "__main__":
    asyncio.run(run_integration_tests())
