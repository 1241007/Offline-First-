#!/usr/bin/env python3
"""
EduNova — Non-Destructive Live Supabase Table Snapshot.
Dumps all course, module, lesson, and migration records to a local JSON backup file.
"""

import asyncio
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from sqlalchemy import text
from app.core.database import get_session_factory

async def take_snapshot():
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    backup_path = backend_dir / "scripts" / f"supabase_snapshot_{timestamp}.json"
    print(f"Connecting to live Supabase to create table snapshot: {backup_path.name}")

    factory = get_session_factory()
    snapshot = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "tables": {}
    }

    tables_to_dump = [
        "alembic_version",
        "course_categories",
        "courses",
        "course_modules",
        "lessons"
    ]

    async with factory() as session:
        for tbl in tables_to_dump:
            res = await session.execute(text(f"SELECT * FROM {tbl}"))
            keys = res.keys()
            rows = [dict(zip(keys, [str(v) if v is not None else None for v in row])) for row in res.fetchall()]
            snapshot["tables"][tbl] = {
                "count": len(rows),
                "rows": rows
            }
            print(f"  Captured table '{tbl}': {len(rows)} rows")

    backup_path.write_text(json.dumps(snapshot, indent=2), encoding="utf-8")
    print(f"\n[SUCCESS] Snapshot saved successfully to:\n  {backup_path}")
    print(f"File size: {backup_path.stat().st_size} bytes")

if __name__ == "__main__":
    asyncio.run(take_snapshot())
