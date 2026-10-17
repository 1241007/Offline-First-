"""Test migration upgrade and downgrade lifecycle against a local test database"""

import os
import sqlite3
import pytest
from alembic import command
from alembic.config import Config


def test_migration_upgrade_and_downgrade_lifecycle(tmp_path):
    """
    Validates that Alembic can cleanly upgrade to head and downgrade
    the new user_saved_courses and user_saved_roadmaps tables
    without modifying production.
    """
    test_db_path = str(tmp_path / "test_migration_lifecycle.db")
    os.environ["DATABASE_URL"] = f"sqlite:///{test_db_path}"

    alembic_cfg = Config("alembic.ini")
    alembic_cfg.set_main_option("sqlalchemy.url", f"sqlite:///{test_db_path}")

    # 1. Upgrade to head
    command.upgrade(alembic_cfg, "head")

    conn = sqlite3.connect(test_db_path)
    cur = conn.cursor()
    cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = [r[0] for r in cur.fetchall()]

    assert "user_saved_courses" in tables
    assert "user_saved_roadmaps" in tables

    # Check unique index on user_saved_courses
    cur.execute("SELECT name, sql FROM sqlite_master WHERE type='index' AND tbl_name='user_saved_courses'")
    course_indexes = {r[0]: (r[1] or "") for r in cur.fetchall()}
    assert "ix_user_saved_courses_user_course" in course_indexes
    assert "UNIQUE" in course_indexes["ix_user_saved_courses_user_course"].upper()

    # Check unique index on user_saved_roadmaps
    cur.execute("SELECT name, sql FROM sqlite_master WHERE type='index' AND tbl_name='user_saved_roadmaps'")
    roadmap_indexes = {r[0]: (r[1] or "") for r in cur.fetchall()}
    assert "ix_user_saved_roadmaps_user_roadmap" in roadmap_indexes
    assert "UNIQUE" in roadmap_indexes["ix_user_saved_roadmaps_user_roadmap"].upper()

    conn.close()

    # 2. Downgrade from c7d8e9f0a1b2 back to previous revision
    command.downgrade(alembic_cfg, "a7c8e9f1b2d3")

    conn = sqlite3.connect(test_db_path)
    cur = conn.cursor()
    cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables_after_down = [r[0] for r in cur.fetchall()]

    assert "user_saved_courses" not in tables_after_down
    assert "user_saved_roadmaps" not in tables_after_down

    conn.close()

    # 3. Re-upgrade back to head
    command.upgrade(alembic_cfg, "head")

    conn = sqlite3.connect(test_db_path)
    cur = conn.cursor()
    cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables_after_reup = [r[0] for r in cur.fetchall()]

    assert "user_saved_courses" in tables_after_reup
    assert "user_saved_roadmaps" in tables_after_reup

    conn.close()
