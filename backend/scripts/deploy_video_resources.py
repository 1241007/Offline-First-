#!/usr/bin/env python3
"""
EduNova — Phase 5C: Non-Destructive Video Resource Deployment Script.

Safety guarantees:
1. Dry-run by default. Requires `--execute` for live database writes.
2. Checks preflight integrity: live revision 'e3b8a1c92d54', 19 courses, 57 modules, 285 lessons.
3. Strictly asserts all 285 existing reading resource URLs remain non-null and untouched.
4. Adds nullable columns `video_url` and `video_provider` if not present.
5. Populates the 21 verified YouTube resources using parameterized, idempotent SQL.
6. Refuses to overwrite existing video URLs without explicit conflict notification.
7. Commits all changes in a single transaction.
"""

import argparse
import asyncio
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from sqlalchemy import text
from app.core.database import get_session_factory
from scripts.seed_data import seed_id, SPECIFIC_LESSON_VIDEOS

TARGET_REVISION = "a7c8e9f1b2d3"
PREVIOUS_REVISION = "e3b8a1c92d54"


async def run_deployment(execute: bool = False):
    print("=" * 70)
    print("EduNova Phase 5C: YouTube Video Resources Deployment Pipeline")
    print(f"Mode: {'EXECUTE (LIVE WRITES)' if execute else 'DRY RUN (READ-ONLY SIMULATION)'}")
    print("=" * 70)

    factory = get_session_factory()
    async with factory() as session:
        # ===================================================================
        # STEP 1: PREFLIGHT INTEGRITY ASSERTIONS
        # ===================================================================
        print("\n[STEP 1] Running Preflight Assertions...")

        # 1.1 Alembic revision check
        rev_res = await session.execute(text("SELECT version_num FROM alembic_version"))
        current_rev = rev_res.scalar()
        print(f"  Live Alembic revision: {current_rev}")
        if current_rev not in (PREVIOUS_REVISION, TARGET_REVISION):
            raise RuntimeError(
                f"PREFLIGHT FAILED: Unexpected Alembic revision '{current_rev}'. "
                f"Expected '{PREVIOUS_REVISION}' or '{TARGET_REVISION}'."
            )

        # 1.2 Table row counts check
        c_count = (await session.execute(text("SELECT count(*) FROM courses"))).scalar()
        m_count = (await session.execute(text("SELECT count(*) FROM course_modules"))).scalar()
        l_count = (await session.execute(text("SELECT count(*) FROM lessons"))).scalar()
        reading_res_count = (await session.execute(text("SELECT count(*) FROM lessons WHERE resource_url IS NOT NULL"))).scalar()

        print(f"  Courses count: {c_count} (Expected: 19)")
        print(f"  Modules count: {m_count} (Expected: 57)")
        print(f"  Lessons count: {l_count} (Expected: 285)")
        print(f"  Lessons with reading resource_url: {reading_res_count} (Expected: 285)")

        if c_count != 19 or m_count != 57 or l_count != 285 or reading_res_count != 285:
            raise RuntimeError("PREFLIGHT FAILED: Course, module, lesson, or reading resource count mismatch!")

        # 1.3 Check existing columns in lessons
        col_res = await session.execute(text("""
            SELECT column_name FROM information_schema.columns
            WHERE table_name = 'lessons' AND column_name IN ('video_url', 'video_provider')
        """))
        existing_video_cols = [r[0] for r in col_res.fetchall()]
        print(f"  Existing video columns in 'lessons': {existing_video_cols}")

        # 1.4 Validate all 21 target lessons exist and check for video conflicts
        print(f"\n[STEP 2] Validating 21 Target Lessons & Checking for Conflicts...")
        target_updates = []
        for (c_slug, m_order, l_order), (v_url, v_prov) in SPECIFIC_LESSON_VIDEOS.items():
            mod_slug = f"{c_slug}-module-{m_order}"
            les_slug = f"{mod_slug}-lesson-{l_order}"
            les_id = seed_id("lesson", les_slug)

            # Query lesson
            if "video_url" in existing_video_cols:
                q = text("SELECT id, title, resource_url, resource_provider, video_url, video_provider FROM lessons WHERE id = :id")
            else:
                q = text("SELECT id, title, resource_url, resource_provider, NULL as video_url, NULL as video_provider FROM lessons WHERE id = :id")

            row = (await session.execute(q, {"id": les_id})).fetchone()
            if not row:
                raise RuntimeError(f"PREFLIGHT CONFLICT: Target lesson {les_slug} ({les_id}) does not exist in live DB!")

            cur_vid_url = row[4]
            cur_vid_prov = row[5]

            if cur_vid_url is not None and cur_vid_url != v_url:
                raise RuntimeError(
                    f"PREFLIGHT CONFLICT: Lesson {les_id} already has a conflicting video_url:\n"
                    f"  Current in DB: {cur_vid_url}\n"
                    f"  New planned:   {v_url}\n"
                    "Refusing to overwrite conflicting video URL."
                )

            target_updates.append({
                "id": les_id,
                "course_slug": c_slug,
                "module_order": m_order,
                "lesson_order": l_order,
                "title": row[1],
                "reading_url": row[2],
                "reading_provider": row[3],
                "current_video_url": cur_vid_url,
                "new_video_url": v_url,
                "new_video_provider": v_prov,
            })

        print(f"  All {len(target_updates)} target lessons verified. 0 conflicts found.")

        # ===================================================================
        # STEP 3: MIGRATION & DATA UPDATES
        # ===================================================================
        print("\n[STEP 3] Preparing Execution Plan...")
        if "video_url" not in existing_video_cols:
            print("  Plan: Execute DDL to add nullable `video_url` and `video_provider` columns to `lessons` table.")
            print(f"  Plan: Advance `alembic_version` to '{TARGET_REVISION}'.")
        else:
            print("  Columns `video_url` and `video_provider` already exist. DDL addition skipped.")

        print(f"  Plan: Update {len(target_updates)} lessons with verified YouTube resources.")

        if execute:
            print("\n[TRANSACTION] Executing live atomic transaction...")
            try:
                # 3.1 DDL column additions if needed
                if "video_url" not in existing_video_cols:
                    print("  Executing: ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_url text;")
                    await session.execute(text("ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_url text;"))
                    print("  Executing: ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_provider varchar(100);")
                    await session.execute(text("ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_provider varchar(100);"))
                    print(f"  Executing: UPDATE alembic_version SET version_num = '{TARGET_REVISION}';")
                    await session.execute(text("UPDATE alembic_version SET version_num = :rev"), {"rev": TARGET_REVISION})

                # 3.2 Update video resources for the 21 lessons
                for upd in target_updates:
                    await session.execute(
                        text("""
                            UPDATE lessons
                            SET video_url = :v_url,
                                video_provider = :v_prov,
                                updated_at = NOW()
                            WHERE id = :id AND (video_url IS NULL OR video_url = :v_url);
                        """),
                        {
                            "id": upd["id"],
                            "v_url": upd["new_video_url"],
                            "v_prov": upd["new_video_provider"]
                        }
                    )
                await session.commit()
                print("  [SUCCESS] Transaction committed successfully.")
            except Exception as e:
                await session.rollback()
                print(f"  [ERROR] Execution failed, transaction rolled back: {e}")
                raise

            # 3.3 Write changelog manifest
            manifest = {
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "alembic_revision": TARGET_REVISION,
                "updated_lessons_count": len(target_updates),
                "updated_lessons": target_updates
            }
            manifest_path = backend_dir / "scripts" / "video_sync_changelog.json"
            manifest_path.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
            print(f"  Recorded deployment manifest to:\n    {manifest_path}")
        else:
            print("\n[DRY RUN] Dry run simulation complete. Zero live mutations made (read-only).")

        # ===================================================================
        # STEP 4: POST-RUN VERIFICATIONS (When Executed)
        # ===================================================================
        if execute:
            print("\n[STEP 4] Running Post-Execution Verifications...")
            post_rev = (await session.execute(text("SELECT version_num FROM alembic_version"))).scalar()
            post_c = (await session.execute(text("SELECT count(*) FROM courses"))).scalar()
            post_m = (await session.execute(text("SELECT count(*) FROM course_modules"))).scalar()
            post_l = (await session.execute(text("SELECT count(*) FROM lessons"))).scalar()
            post_reading = (await session.execute(text("SELECT count(*) FROM lessons WHERE resource_url IS NOT NULL"))).scalar()
            post_video = (await session.execute(text("SELECT count(*) FROM lessons WHERE video_url IS NOT NULL"))).scalar()

            print(f"  Final Alembic revision: {post_rev} (Expected: {TARGET_REVISION})")
            print(f"  Final Courses count: {post_c} (Expected: 19)")
            print(f"  Final Modules count: {post_m} (Expected: 57)")
            print(f"  Final Lessons count: {post_l} (Expected: 285)")
            print(f"  Final Lessons with reading resource_url: {post_reading} (Expected: 285)")
            print(f"  Final Lessons with video_url: {post_video} (Expected: 21)")

            assert post_rev == TARGET_REVISION, f"Expected revision {TARGET_REVISION}, found {post_rev}"
            assert post_c == 19, f"Course count altered! Found {post_c}"
            assert post_m == 57, f"Module count altered! Found {post_m}"
            assert post_l == 285, f"Lesson count altered! Found {post_l}"
            assert post_reading == 285, f"Reading links altered! Found {post_reading}"
            assert post_video == 21, f"Expected 21 video URLs, found {post_video}"
            print("\n[ALL POST-RUN ASSERTIONS VERIFIED SUCCESSFULLY]")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Deploy YouTube video resources safely to Supabase.")
    parser.add_argument("--execute", action="store_true", help="Execute live deployment. If omitted, runs in dry-run mode.")
    args = parser.parse_args()

    asyncio.run(run_deployment(execute=args.execute))
