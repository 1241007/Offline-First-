import asyncio
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import text
from app.core.database import get_session_factory

async def main():
    factory = get_session_factory()
    async with factory() as conn:
        print("--- EXTENSIONS ---")
        exts = await conn.execute(text("SELECT extname, extversion FROM pg_extension;"))
        for e in exts.fetchall():
            print(f"  Extension: {e[0]} (v{e[1]})")

        for table in ["syllabus_documents", "syllabus_chunks"]:
            print(f"\n--- COLUMNS FOR {table} ---")
            cols = await conn.execute(text(f"""
                SELECT column_name, data_type, is_nullable, column_default, character_maximum_length, udt_name
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = '{table}'
                ORDER BY ordinal_position;
            """))
            for c in cols.fetchall():
                print(f"  {c[0]}: type={c[1]}, udt={c[5]}, nullable={c[2]}, default={c[3]}, max_len={c[4]}")

            print(f"\n--- CONSTRAINTS FOR {table} ---")
            constrs = await conn.execute(text(f"""
                SELECT conname, contype, pg_get_constraintdef(c.oid)
                FROM pg_constraint c
                JOIN pg_namespace n ON n.oid = c.connamespace
                JOIN pg_class cl ON cl.oid = c.conrelid
                WHERE n.nspname = 'public' AND cl.relname = '{table}';
            """))
            for cn in constrs.fetchall():
                print(f"  Constraint: {cn[0]} ({cn[1]}) -> {cn[2]}")

            print(f"\n--- INDEXES FOR {table} ---")
            idxs = await conn.execute(text(f"""
                SELECT indexname, indexdef
                FROM pg_indexes
                WHERE schemaname = 'public' AND tablename = '{table}';
            """))
            for idx in idxs.fetchall():
                print(f"  Index: {idx[0]} -> {idx[1]}")

if __name__ == "__main__":
    asyncio.run(main())
