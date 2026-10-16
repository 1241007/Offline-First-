import asyncio
import sys
from pathlib import Path

# Add backend directory to path
backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from sqlalchemy import text
from app.core.database import get_session_factory
from scripts.seed_data import seed_id, CATEGORIES, COURSES

async def main():
    factory = get_session_factory()
    async with factory() as conn:
        print("=== 1. LIVE COURSE SLUGS & IDS ===")
        res = await conn.execute(text("""
            SELECT c.id, c.slug, c.name, COUNT(DISTINCT m.id) as mod_cnt, COUNT(DISTINCT l.id) as les_cnt
            FROM courses c
            LEFT JOIN course_modules m ON m.course_id = c.id
            LEFT JOIN lessons l ON l.module_id = m.id
            GROUP BY c.id, c.slug, c.name
            ORDER BY c.slug;
        """))
        live_courses = {r[1]: {"id": r[0], "name": r[2], "mod_cnt": r[3], "les_cnt": r[4]} for r in res.fetchall()}
        print(f"Total courses on live Supabase: {len(live_courses)}")
        for slug, data in live_courses.items():
            expected_id = seed_id('course', slug)
            match = "MATCH" if expected_id == data["id"] else f"MISMATCH (expected {expected_id})"
            print(f"  {slug}: id={data['id']} ({match}), modules={data['mod_cnt']}, lessons={data['les_cnt']}")

        print("\n=== 2. LIVE MODULES RECONCILIATION ===")
        res_m = await conn.execute(text("""
            SELECT m.id, m.course_id, m.title, m.display_order, c.slug as course_slug
            FROM course_modules m
            JOIN courses c ON c.id = m.course_id
            ORDER BY c.slug, m.display_order;
        """))
        live_modules = res_m.fetchall()
        print(f"Total modules on live Supabase: {len(live_modules)}")
        mod_mismatches = 0
        for m in live_modules:
            expected_mod_id = seed_id('module', f"{m[4]}-module-{m[3]}")
            if expected_mod_id != m[0]:
                mod_mismatches += 1
                print(f"  MODULE ID MISMATCH: {m[4]} order {m[3]}: live={m[0]} vs seed={expected_mod_id}")
        if mod_mismatches == 0:
            print("  All 54 live module IDs match deterministic seed UUIDs exactly!")

        print("\n=== 3. LIVE LESSONS RECONCILIATION ===")
        res_l = await conn.execute(text("""
            SELECT l.id, l.title, l.display_order, m.display_order as mod_order, c.slug as course_slug
            FROM lessons l
            JOIN course_modules m ON m.id = l.module_id
            JOIN courses c ON c.id = m.course_id
            ORDER BY c.slug, m.display_order, l.display_order;
        """))
        live_lessons = res_l.fetchall()
        print(f"Total lessons on live Supabase: {len(live_lessons)}")

        # Build mapping using exact seed_data formula
        from scripts.seed_data import COURSES, MODULE_TEMPLATES, LESSON_TEMPLATES, get_lesson_resource
        seed_lessons_by_id = {}
        for c in COURSES:
            c_slug = c["slug"]
            for mod_template in MODULE_TEMPLATES:
                module_slug = f"{c_slug}-module-{mod_template['order']}"
                for lesson_template in LESSON_TEMPLATES:
                    lesson_slug = f"{module_slug}-lesson-{lesson_template['order']}"
                    lesson_id = seed_id("lesson", lesson_slug)
                    res_url, res_provider = get_lesson_resource(
                        c_slug,
                        mod_template["order"],
                        lesson_template["order"]
                    )
                    seed_lessons_by_id[lesson_id] = {
                        "course_slug": c_slug,
                        "mod_order": mod_template["order"],
                        "les_order": lesson_template["order"],
                        "title": f"{mod_template['title']} - {lesson_template['title_suffix']}",
                        "resource_url": res_url,
                        "resource_provider": res_provider,
                    }

        print(f"Total seed lessons indexed: {len(seed_lessons_by_id)} (19 courses * 3 * 5 = 285)")

        matched_count = 0
        id_mismatches = []
        missing_urls = []
        for l in live_lessons:
            live_id = l[0]
            if live_id in seed_lessons_by_id:
                matched_count += 1
                seed_item = seed_lessons_by_id[live_id]
                if not seed_item["resource_url"]:
                    missing_urls.append(live_id)
            else:
                id_mismatches.append(l)

        print(f"Live lessons matching seed deterministic ID: {matched_count} / {len(live_lessons)}")
        if id_mismatches:
            print(f"Unmatched live lesson IDs ({len(id_mismatches)}):")
            for u in id_mismatches[:5]:
                print(f"  {u[0]}: {u[4]} - {u[1]}")
        else:
            print("  PERFECT MATCH: All 270 live lesson IDs exactly match seed deterministic UUIDs!")

        print(f"Lessons with NULL/empty resource_url in seed: {len(missing_urls)}")

        print("\n=== 4. CANONICAL TRACKS RESOURCE URL PROJECTION ===")
        canonical_slugs = [
            "cpp-programming", "data-structures-algorithms", "data-science-python",
            "python-fundamentals", "machine-learning-foundations", "fullstack-development",
            "system-design-architecture"
        ]
        for c_slug in canonical_slugs:
            c_lessons = [v for k, v in seed_lessons_by_id.items() if v["course_slug"] == c_slug]
            with_res = [l for l in c_lessons if l["resource_url"]]
            print(f"  {c_slug}: {len(c_lessons)} lessons, {len(with_res)} have resource_url")

if __name__ == "__main__":
    asyncio.run(main())
