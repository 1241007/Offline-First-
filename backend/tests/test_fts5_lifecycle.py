"""Tests for SQLite FTS5 external content lifecycle, triggers, and immediate searchability."""

import sqlite3
import pytest


@pytest.fixture
def sqlite_fts_db():
    """Sets up an in-memory SQLite database with syllabus_chunks and FTS5 triggers."""
    conn = sqlite3.connect(":memory:")
    cursor = conn.cursor()

    # 1. Base content table
    cursor.execute("""
        CREATE TABLE syllabus_chunks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            chunk_id TEXT UNIQUE NOT NULL,
            board TEXT NOT NULL,
            class_grade TEXT NOT NULL,
            subject TEXT NOT NULL,
            chapter_title TEXT NOT NULL,
            section_title TEXT,
            page_reference TEXT,
            content TEXT NOT NULL,
            source_id TEXT,
            corpus_version TEXT
        );
    """)

    # 2. FTS5 External Content Virtual Table
    cursor.execute("""
        CREATE VIRTUAL TABLE chunks_fts USING fts5(
            content,
            subject,
            chapter_title,
            content='syllabus_chunks',
            content_rowid='id'
        );
    """)

    # 3. Synchronization Triggers
    cursor.execute("""
        CREATE TRIGGER chunks_ai AFTER INSERT ON syllabus_chunks BEGIN
            INSERT INTO chunks_fts(rowid, content, subject, chapter_title)
            VALUES (new.id, new.content, new.subject, new.chapter_title);
        END;
    """)

    cursor.execute("""
        CREATE TRIGGER chunks_ad AFTER DELETE ON syllabus_chunks BEGIN
            INSERT INTO chunks_fts(chunks_fts, rowid, content, subject, chapter_title)
            VALUES('delete', old.id, old.content, old.subject, old.chapter_title);
        END;
    """)

    cursor.execute("""
        CREATE TRIGGER chunks_au AFTER UPDATE ON syllabus_chunks BEGIN
            INSERT INTO chunks_fts(chunks_fts, rowid, content, subject, chapter_title)
            VALUES('delete', old.id, old.content, old.subject, old.chapter_title);
            INSERT INTO chunks_fts(rowid, content, subject, chapter_title)
            VALUES (new.id, new.content, new.subject, new.chapter_title);
        END;
    """)

    conn.commit()
    yield conn
    conn.close()


def test_fts5_insert_trigger_immediate_search(sqlite_fts_db):
    """Test that inserting a chunk into syllabus_chunks makes it immediately searchable via FTS5."""
    cursor = sqlite_fts_db.cursor()

    cursor.execute("""
        INSERT INTO syllabus_chunks (chunk_id, board, class_grade, subject, chapter_title, content)
        VALUES ('c1', 'CBSE', '10', 'Science', 'Life Processes', 'Photosynthesis converts sunlight into chemical energy.')
    """)
    sqlite_fts_db.commit()

    # Search via FTS5 MATCH
    cursor.execute("SELECT rowid FROM chunks_fts WHERE chunks_fts MATCH 'photosynthesis*'")
    results = cursor.fetchall()
    assert len(results) == 1
    assert results[0][0] == 1


def test_fts5_update_trigger_synchronization(sqlite_fts_db):
    """Test that updating content updates the FTS5 index and removes the old term."""
    cursor = sqlite_fts_db.cursor()

    cursor.execute("""
        INSERT INTO syllabus_chunks (chunk_id, board, class_grade, subject, chapter_title, content)
        VALUES ('c1', 'CBSE', '10', 'Science', 'Respiration', 'Cellular respiration produces ATP in mitochondria.')
    """)
    sqlite_fts_db.commit()

    # Old term matches
    cursor.execute("SELECT rowid FROM chunks_fts WHERE chunks_fts MATCH 'mitochondria*'")
    assert len(cursor.fetchall()) == 1

    # Update content to something completely different
    cursor.execute("""
        UPDATE syllabus_chunks 
        SET content = 'Anaerobic fermentation occurs in yeast cells.'
        WHERE chunk_id = 'c1'
    """)
    sqlite_fts_db.commit()

    # Old term must NOT match anymore
    cursor.execute("SELECT rowid FROM chunks_fts WHERE chunks_fts MATCH 'mitochondria*'")
    assert len(cursor.fetchall()) == 0

    # New term matches immediately
    cursor.execute("SELECT rowid FROM chunks_fts WHERE chunks_fts MATCH 'fermentation*'")
    assert len(cursor.fetchall()) == 1


def test_fts5_delete_trigger_synchronization(sqlite_fts_db):
    """Test that deleting a chunk deletes it from the FTS5 index immediately."""
    cursor = sqlite_fts_db.cursor()

    cursor.execute("""
        INSERT INTO syllabus_chunks (chunk_id, board, class_grade, subject, chapter_title, content)
        VALUES ('c1', 'CBSE', '10', 'Science', 'Metals', 'Reactivity series of metals with acids.')
    """)
    sqlite_fts_db.commit()

    cursor.execute("SELECT rowid FROM chunks_fts WHERE chunks_fts MATCH 'reactivity*'")
    assert len(cursor.fetchall()) == 1

    cursor.execute("DELETE FROM syllabus_chunks WHERE chunk_id = 'c1'")
    sqlite_fts_db.commit()

    cursor.execute("SELECT rowid FROM chunks_fts WHERE chunks_fts MATCH 'reactivity*'")
    assert len(cursor.fetchall()) == 0


def test_fts5_rebuild_command(sqlite_fts_db):
    """Test that rebuild command restores full searchability."""
    cursor = sqlite_fts_db.cursor()

    cursor.execute("""
        INSERT INTO syllabus_chunks (chunk_id, board, class_grade, subject, chapter_title, content)
        VALUES 
            ('c1', 'CBSE', '10', 'Science', 'Acids', 'Hydrochloric acid reacts with zinc to produce hydrogen gas.'),
            ('c2', 'CBSE', '10', 'Science', 'Bases', 'Sodium hydroxide neutralizes acids to form salt and water.')
    """)
    sqlite_fts_db.commit()

    # Rebuild FTS5 index explicitly
    cursor.execute("INSERT INTO chunks_fts(chunks_fts) VALUES('rebuild');")
    sqlite_fts_db.commit()

    cursor.execute("SELECT rowid FROM chunks_fts WHERE chunks_fts MATCH 'hydrochloric*'")
    assert len(cursor.fetchall()) == 1

    cursor.execute("SELECT rowid FROM chunks_fts WHERE chunks_fts MATCH 'neutralizes*'")
    assert len(cursor.fetchall()) == 1
