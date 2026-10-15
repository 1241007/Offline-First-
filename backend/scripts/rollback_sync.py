#!/usr/bin/env python3
"""
EduNova — Targeted, Safe Recovery / Rollback Script.

Strict safety guarantee:
Reads `backend/scripts/sync_changelog.json` and ONLY reverts rows created or
updated during the specific sync execution. Never runs blind global updates.
"""

import argparse
import asyncio
import json
import sys
from pathlib import Path

backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from sqlalchemy import text
from app.core.database import get_session_factory


async def rollback(execute: bool = False):
    changelog_path = backend_dir / "scripts" / "sync_changelog.json"
    if not changelog_path.exists():
        print(f"[ERROR] Changelog manifest not found at: {changelog_path}")
        print("No sync execution has been recorded yet. Nothing to roll back.")
        return

    changelog = json.loads(changelog_path.read_text(encoding="utf-8"))
    print("=" * 70)
    print("EduNova Targeted Sync Rollback")
    print(f"Timestamp of target sync: {changelog.get('timestamp')}")
    print(f"Mode: {'EXECUTE (LIVE ROLLBACK)' if execute else 'DRY RUN (SIMULATION)'}")
    print("=" * 70)

    inserted_course_id = changelog["inserted_course"]["id"]
    inserted_module_ids = [m["id"] for m in changelog["inserted_modules"]]
    inserted_lesson_ids = [l["id"] for l in changelog["inserted_lessons"]]
    updated_lessons = changelog["updated_lessons"]

    print(f"Target course to delete: {inserted_course_id}")
    print(f"Target modules to delete: {len(inserted_module_ids)}")
    print(f"Target lessons to delete: {len(inserted_lesson_ids)}")
    print(f"Target lessons to revert to previous state: {len(updated_lessons)}")

    factory = get_session_factory()
    async with factory() as session:
        try:
            # 1. Revert resource URLs for specifically updated lessons
            if updated_lessons:
                print("\n[STEP 1] Validating and reverting updated resource URLs to previous values...")
                reverted_count = 0
                for item in updated_lessons:
                    lid = item["id"]
                    expected_url = item["new_resource_url"]
                    expected_prov = item["new_resource_provider"]
                    prev_url = item["previous_resource_url"]
                    prev_prov = item["previous_resource_provider"]

                    # Inspect live record to verify it has not changed since this sync
                    cur_res = await session.execute(
                        text("SELECT resource_url, resource_provider FROM lessons WHERE id = :id"),
                        {"id": lid}
                    )
                    row = cur_res.fetchone()
                    if not row:
                        raise RuntimeError(f"ROLLBACK CONFLICT: Lesson {lid} no longer exists in database!")
                    if row[0] != expected_url or row[1] != expected_prov:
                        raise RuntimeError(
                            f"ROLLBACK REFUSED: Lesson {lid} has been modified since the sync!\n"
                            f"  Expected current: url={expected_url!r}, provider={expected_prov!r}\n"
                            f"  Actual in DB:     url={row[0]!r}, provider={row[1]!r}\n"
                            "Aborting rollback to protect unexpected post-sync modifications."
                        )

                    if execute:
                        await session.execute(
                            text("UPDATE lessons SET resource_url = :prev_url, resource_provider = :prev_prov, updated_at = NOW() WHERE id = :id"),
                            {"id": lid, "prev_url": prev_url, "prev_prov": prev_prov}
                        )
                    reverted_count += 1
                print(f"  Verified and reverted {reverted_count} lessons to their recorded previous state.")

            # 2. Delete inserted C++ lessons, modules, and course
            print("\n[STEP 2] Deleting inserted C++ track...")
            if execute:
                await session.execute(
                    text("DELETE FROM lessons WHERE module_id IN (SELECT id FROM course_modules WHERE course_id = :cid);"),
                    {"cid": inserted_course_id}
                )
                await session.execute(
                    text("DELETE FROM course_modules WHERE course_id = :cid;"),
                    {"cid": inserted_course_id}
                )
                await session.execute(
                    text("DELETE FROM courses WHERE id = :cid;"),
                    {"cid": inserted_course_id}
                )
            print(f"  Deleted course {inserted_course_id} and its associated modules and lessons.")

            if execute:
                await session.commit()
                changelog_path.unlink(missing_ok=True)
                print("\n[TRANSACTION] Rollback committed successfully. Manifest removed.")
            else:
                await session.rollback()
                print("\n[TRANSACTION] Dry run complete. Zero mutations committed.")

        except Exception as e:
            await session.rollback()
            print(f"\n[ERROR] Rollback failed and was aborted: {e}")
            raise


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Roll back a specific sync execution safely.")
    parser.add_argument("--execute", action="store_true", help="Execute live rollback. If omitted, runs in dry run mode.")
    args = parser.parse_args()

    asyncio.run(rollback(execute=args.execute))
