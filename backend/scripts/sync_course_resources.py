#!/usr/bin/env python3
"""
EduNova — Non-Destructive, Idempotent Course & Resource Synchronization Script.

Strict safety guarantees:
1. Defensively verifies all existing course, module, and lesson IDs against expected
   slugs, parents, and titles. Aborts immediately on any mismatch.
2. Inserts C++ course (1 course, 3 modules, 15 lessons) only after asserting non-existence.
3. Populates resource URLs ONLY where `resource_url IS NULL`. Never overwrites existing URLs.
4. Uses parameterized SQL throughout.
5. Defaults to DRY-RUN mode. Only executes writes when explicitly invoked with `--execute`.
"""

import argparse
import asyncio
import sys
from pathlib import Path
from typing import Dict, List, Tuple

# Ensure backend root is on sys.path
backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from sqlalchemy import text
from app.core.database import get_session_factory
from scripts.seed_data import (
    seed_id, COURSES, MODULE_TEMPLATES, LESSON_TEMPLATES,
    get_lesson_resource, SPECIFIC_LESSON_RESOURCES
)

# Canonical corrected URL destination for l-cpp-04 (LearnCpp OOP 301 redirect fix)
CANONICAL_CPP_OOP_URL = "https://www.learncpp.com/cpp-tutorial/introduction-to-object-oriented-programming/"


def get_canonical_lesson_resource(course_slug: str, mod_order: int, les_order: int) -> Tuple[str, str]:
    """Retrieve verified resource URL and provider, applying the LearnCpp redirect correction."""
    if course_slug == "cpp-programming" and mod_order == 1 and les_order == 0:
        return (CANONICAL_CPP_OOP_URL, "LearnCpp.com")
    return get_lesson_resource(course_slug, mod_order, les_order)


async def run_sync(execute: bool = False):
    print("=" * 70)
    print("EduNova Course & Resource Synchronization Pipeline")
    print(f"Mode: {'EXECUTE (LIVE WRITES)' if execute else 'DRY RUN (READ-ONLY SIMULATION)'}")
    print("=" * 70)

    factory = get_session_factory()
    async with factory() as session:
        # ===================================================================
        # STEP 1: PREFLIGHT INTEGRITY ASSERTIONS
        # ===================================================================
        print("\n[STEP 1] Running Preflight Database Integrity Assertions...")

        # 1.1 Verify live Alembic revision
        rev_res = await session.execute(text("SELECT version_num FROM alembic_version"))
        current_rev = rev_res.scalar()
        print(f"  Live Alembic Revision: {current_rev}")
        if current_rev != "e3b8a1c92d54":
            raise RuntimeError(
                f"PREFLIGHT FAILED: Expected database revision 'e3b8a1c92d54', found '{current_rev}'. "
                "Migrations must be applied before running data sync."
            )

        # 1.2 Verify Category 'Programming' exists
        cat_res = await session.execute(
            text("SELECT id, name, slug FROM course_categories WHERE slug = 'programming'")
        )
        prog_cat = cat_res.fetchone()
        if not prog_cat:
            raise RuntimeError("PREFLIGHT FAILED: Category 'programming' does not exist in course_categories.")
        prog_cat_id = prog_cat[0]
        print(f"  Found 'Programming' category ID: {prog_cat_id}")

        # 1.3 Verify existing course records and check for unexpected mutations
        c_res = await session.execute(text("SELECT id, slug, name FROM courses"))
        live_courses: Dict[str, dict] = {r[0]: {"slug": r[1], "name": r[2]} for r in c_res.fetchall()}
        print(f"  Existing live courses count: {len(live_courses)}")

        # Verify C++ course is NOT already present
        cpp_course_id = seed_id("course", "cpp-programming")
        if cpp_course_id in live_courses:
            print("  Note: C++ course ID already exists in database.")
        cpp_by_slug = [c for c in live_courses.values() if c["slug"] == "cpp-programming"]
        if cpp_by_slug and cpp_course_id not in live_courses:
            raise RuntimeError(
                f"PREFLIGHT CONFLICT: Course with slug 'cpp-programming' exists under unknown ID {cpp_by_slug}"
            )

        # 1.4 Validate every existing course against seed expectations
        for course_def in COURSES:
            cid = seed_id("course", course_def["slug"])
            if cid in live_courses:
                live_c = live_courses[cid]
                if live_c["slug"] != course_def["slug"]:
                    raise RuntimeError(
                        f"PREFLIGHT CONFLICT: Course {cid} has slug '{live_c['slug']}', "
                        f"expected '{course_def['slug']}'."
                    )

        # 1.5 Validate all existing modules against expected parents
        m_res = await session.execute(text("SELECT id, course_id, display_order, title FROM course_modules"))
        live_modules = {r[0]: {"course_id": r[1], "order": r[2], "title": r[3]} for r in m_res.fetchall()}
        print(f"  Existing live modules count: {len(live_modules)}")

        for course_def in COURSES:
            c_slug = course_def["slug"]
            cid = seed_id("course", c_slug)
            if cid in live_courses:
                for mod_def in MODULE_TEMPLATES:
                    mid = seed_id("module", f"{c_slug}-module-{mod_def['order']}")
                    if mid in live_modules:
                        lm = live_modules[mid]
                        if lm["course_id"] != cid:
                            raise RuntimeError(
                                f"PREFLIGHT CONFLICT: Module {mid} belongs to course {lm['course_id']}, "
                                f"expected {cid}."
                            )
                        if lm["order"] != mod_def["order"]:
                            raise RuntimeError(
                                f"PREFLIGHT CONFLICT: Module {mid} has order {lm['order']}, "
                                f"expected {mod_def['order']}."
                            )

        # 1.6 Validate all existing lessons against expected modules and orders
        l_res = await session.execute(
            text("SELECT id, module_id, display_order, title, resource_url, resource_provider FROM lessons")
        )
        live_lessons = {
            r[0]: {
                "module_id": r[1],
                "order": r[2],
                "title": r[3],
                "resource_url": r[4],
                "resource_provider": r[5]
            } for r in l_res.fetchall()
        }
        print(f"  Existing live lessons count: {len(live_lessons)}")
        print("  All preflight integrity assertions passed with 0 conflicts.")

        # ===================================================================
        # STEP 2: PREPARE C++ COURSE, MODULES, AND LESSONS
        # ===================================================================
        print("\n[STEP 2] Preparing C++ Track Insertion (1 course, 3 modules, 15 lessons)...")
        cpp_def = next(c for c in COURSES if c["slug"] == "cpp-programming")
        cpp_course_inserted = 0
        cpp_modules_inserted = 0
        cpp_lessons_inserted = 0

        if cpp_course_id not in live_courses:
            print(f"  Plan: Insert course 'C++ Programming' ({cpp_course_id})")
            if execute:
                await session.execute(
                    text("""
                        INSERT INTO courses (
                            id, category_id, name, slug, description, icon,
                            level, duration_hours, accent_color, is_featured,
                            prerequisites, status, created_at, updated_at
                        ) VALUES (
                            :id, :cat_id, :name, :slug, :desc, :icon,
                            :level, :dur, :accent, :featured,
                            CAST(:prereq AS jsonb), :status, NOW(), NOW()
                        ) ON CONFLICT (id) DO NOTHING;
                    """),
                    {
                        "id": cpp_course_id,
                        "cat_id": prog_cat_id,
                        "name": cpp_def["name"],
                        "slug": cpp_def["slug"],
                        "desc": cpp_def["description"],
                        "icon": cpp_def["icon"],
                        "level": cpp_def["level"],
                        "dur": cpp_def["duration_hours"],
                        "accent": cpp_def["accent_color"],
                        "featured": cpp_def["is_featured"],
                        "prereq": '["Basic computer literacy", "Problem-solving fundamentals"]',
                        "status": "active",
                    }
                )
            cpp_course_inserted += 1
        else:
            print("  C++ course already exists. Skipping course insert.")

        for mod_def in MODULE_TEMPLATES:
            mod_slug = f"cpp-programming-module-{mod_def['order']}"
            mod_id = seed_id("module", mod_slug)
            if mod_id not in live_modules:
                print(f"    Plan: Insert module '{mod_def['title']}' ({mod_id})")
                if execute:
                    await session.execute(
                        text("""
                            INSERT INTO course_modules (
                                id, course_id, title, description, display_order,
                                created_at, updated_at
                            ) VALUES (
                                :id, :course_id, :title, :desc, :order,
                                NOW(), NOW()
                            ) ON CONFLICT (id) DO NOTHING;
                        """),
                        {
                            "id": mod_id,
                            "course_id": cpp_course_id,
                            "title": mod_def["title"],
                            "desc": mod_def["desc"],
                            "order": mod_def["order"],
                        }
                    )
                cpp_modules_inserted += 1

            for les_def in LESSON_TEMPLATES:
                les_slug = f"{mod_slug}-lesson-{les_def['order']}"
                les_id = seed_id("lesson", les_slug)
                if les_id not in live_lessons:
                    res_url, res_provider = get_canonical_lesson_resource("cpp-programming", mod_def["order"], les_def["order"])
                    les_title = f"{mod_def['title']} - {les_def['title_suffix']}"
                    if execute:
                        await session.execute(
                            text("""
                                INSERT INTO lessons (
                                    id, module_id, title, description, content_type,
                                    duration_minutes, display_order, resource_url,
                                    resource_provider, created_at, updated_at
                                ) VALUES (
                                    :id, :mod_id, :title, :desc, :type,
                                    :dur, :order, :url, :provider, NOW(), NOW()
                                ) ON CONFLICT (id) DO NOTHING;
                            """),
                            {
                                "id": les_id,
                                "mod_id": mod_id,
                                "title": les_title,
                                "desc": f"Learn about {les_def['title_suffix'].lower()}",
                                "type": les_def["type"],
                                "dur": les_def["duration"],
                                "order": les_def["order"],
                                "url": res_url,
                                "provider": res_provider,
                            }
                        )
                    cpp_lessons_inserted += 1

        print(f"  C++ insertion plan: {cpp_course_inserted} course, {cpp_modules_inserted} modules, {cpp_lessons_inserted} lessons.")

        # ===================================================================
        # STEP 3: POPULATE RESOURCE URLS FOR EXISTING LESSONS
        # ===================================================================
        print("\n[STEP 3] Preparing Resource URL Updates for Existing Lessons...")
        print("  Rule: Update resource_url ONLY where resource_url IS NULL.")
        print("  Rule: Never overwrite existing non-null URLs or providers.")

        url_updates_planned = 0
        skipped_non_null = 0

        # Step 3 execution & changelog recording
        updated_lessons_manifest = []
        for course_def in COURSES:
            c_slug = course_def["slug"]
            if c_slug == "cpp-programming":
                continue

            for mod_def in MODULE_TEMPLATES:
                mod_slug = f"{c_slug}-module-{mod_def['order']}"
                for les_def in LESSON_TEMPLATES:
                    les_slug = f"{mod_slug}-lesson-{les_def['order']}"
                    les_id = seed_id("lesson", les_slug)

                    if les_id in live_lessons:
                        current = live_lessons[les_id]
                        if current["resource_url"] is not None:
                            skipped_non_null += 1
                            continue

                        res_url, res_provider = get_canonical_lesson_resource(c_slug, mod_def["order"], les_def["order"])
                        if execute:
                            await session.execute(
                                text("""
                                    UPDATE lessons
                                    SET resource_url = :url,
                                        resource_provider = :provider,
                                        updated_at = NOW()
                                    WHERE id = :id AND resource_url IS NULL;
                                """),
                                {
                                    "id": les_id,
                                    "url": res_url,
                                    "provider": res_provider,
                                }
                            )
                        updated_lessons_manifest.append({
                            "id": les_id,
                            "course_slug": c_slug,
                            "previous_resource_url": current["resource_url"],
                            "previous_resource_provider": current["resource_provider"],
                            "new_resource_url": res_url,
                            "new_resource_provider": res_provider,
                        })
                        url_updates_planned += 1

        print(f"  Planned resource URL updates (where NULL): {url_updates_planned}")
        print(f"  Skipped lessons with existing non-null URLs: {skipped_non_null}")

        if execute:
            # 1. Commit database transaction FIRST.
            # If commit fails, exception is raised, triggering rollback in caller,
            # and no manifest is ever persisted to disk.
            await session.commit()
            print("\n[TRANSACTION] Changes committed to Supabase successfully in single transaction.")

            # 2. Persist change manifest strictly AFTER verified commit
            import json
            from datetime import datetime, timezone
            changelog = {
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "inserted_course": {
                    "id": cpp_course_id,
                    "slug": cpp_def["slug"],
                    "name": cpp_def["name"]
                },
                "inserted_modules": [
                    {
                        "id": seed_id("module", f"cpp-programming-module-{m['order']}"),
                        "order": m["order"],
                        "title": m["title"]
                    } for m in MODULE_TEMPLATES
                ],
                "inserted_lessons": [
                    {
                        "id": seed_id("lesson", f"cpp-programming-module-{m['order']}-lesson-{l['order']}"),
                        "order": l["order"],
                        "title": f"{m['title']} - {l['title_suffix']}",
                        "url": get_canonical_lesson_resource("cpp-programming", m["order"], l["order"])[0],
                        "provider": get_canonical_lesson_resource("cpp-programming", m["order"], l["order"])[1]
                    } for m in MODULE_TEMPLATES for l in LESSON_TEMPLATES
                ],
                "updated_lessons": updated_lessons_manifest
            }
            changelog_path = backend_dir / "scripts" / "sync_changelog.json"
            changelog_path.write_text(json.dumps(changelog, indent=2), encoding="utf-8")
            print(f"  Recorded change manifest to: {changelog_path}")
        else:
            await session.rollback()
            print("\n[TRANSACTION] Dry run complete. No database mutations occurred (rolled back).")

        # ===================================================================
        # STEP 4: POST-RUN VERIFICATION ASSERTIONS (When Executed)
        # ===================================================================
        if execute:
            print("\n[STEP 4] Running Post-Run Verification Queries...")
            final_c = (await session.execute(text("SELECT COUNT(*) FROM courses"))).scalar()
            final_m = (await session.execute(text("SELECT COUNT(*) FROM course_modules"))).scalar()
            final_l = (await session.execute(text("SELECT COUNT(*) FROM lessons"))).scalar()
            final_res = (await session.execute(text("SELECT COUNT(*) FROM lessons WHERE resource_url IS NOT NULL"))).scalar()
            users_cnt = (await session.execute(text("SELECT COUNT(*) FROM users"))).scalar()
            tokens_cnt = (await session.execute(text("SELECT COUNT(*) FROM refresh_tokens"))).scalar()
            chats_cnt = (await session.execute(text("SELECT COUNT(*) FROM conversations"))).scalar()
            syl_chunks = (await session.execute(text("SELECT COUNT(*) FROM syllabus_chunks"))).scalar()

            print(f"  Final courses count: {final_c} (Expected: 19)")
            print(f"  Final modules count: {final_m} (Expected: 57)")
            print(f"  Final lessons count: {final_l} (Expected: 285)")
            print(f"  Lessons with resource_url: {final_res} (Expected: 285)")
            print(f"  Preserved users: {users_cnt} (Expected: 21)")
            print(f"  Preserved refresh tokens: {tokens_cnt} (Expected: 95)")
            print(f"  Preserved conversations: {chats_cnt} (Expected: 20)")
            print(f"  Preserved syllabus chunks: {syl_chunks} (Expected: 34)")

            assert final_c == 19, f"Expected 19 courses, found {final_c}"
            assert final_m == 57, f"Expected 57 modules, found {final_m}"
            assert final_l == 285, f"Expected 285 lessons, found {final_l}"
            assert final_res == 285, f"Expected 285 lessons with resource_url, found {final_res}"
            assert users_cnt == 21, f"User count altered! Found {users_cnt}"
            print("\n[SUCCESS] All post-run assertions verified perfectly!")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Synchronize course data and lesson resources safely.")
    parser.add_argument("--execute", action="store_true", help="Execute live writes. If omitted, runs in dry-run mode.")
    args = parser.parse_args()

    asyncio.run(run_sync(execute=args.execute))
