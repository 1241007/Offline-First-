package com.offline_First.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.offline_First.domain.model.RetrievedChunk

private const val SYLLABUS_DB_NAME = "edunova_syllabus.db"
private const val SYLLABUS_DB_VERSION = 1

/**
 * SQLite Database managing syllabus chunks and FTS5 external content full-text search.
 *
 * Implements complete FTS5 external-content lifecycle:
 * - Insert, update, and delete synchronization triggers
 * - Rebuild index API
 * - Immediate searchability on pack downloads
 */
open class SyllabusDatabase(context: Context) : SQLiteOpenHelper(context, SYLLABUS_DB_NAME, null, SYLLABUS_DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Content table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS syllabus_chunks (
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
            )
        """.trimIndent())

        // 2. FTS5 External Content Virtual Table
        db.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS chunks_fts USING fts5(
                content,
                subject,
                chapter_title,
                content='syllabus_chunks',
                content_rowid='id'
            )
        """.trimIndent())

        // 3. FTS5 Synchronization Triggers
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS chunks_ai AFTER INSERT ON syllabus_chunks BEGIN
                INSERT INTO chunks_fts(rowid, content, subject, chapter_title)
                VALUES (new.id, new.content, new.subject, new.chapter_title);
            END;
        """.trimIndent())

        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS chunks_ad AFTER DELETE ON syllabus_chunks BEGIN
                INSERT INTO chunks_fts(chunks_fts, rowid, content, subject, chapter_title)
                VALUES('delete', old.id, old.content, old.subject, old.chapter_title);
            END;
        """.trimIndent())

        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS chunks_au AFTER UPDATE ON syllabus_chunks BEGIN
                INSERT INTO chunks_fts(chunks_fts, rowid, content, subject, chapter_title)
                VALUES('delete', old.id, old.content, old.subject, old.chapter_title);
                INSERT INTO chunks_fts(rowid, content, subject, chapter_title)
                VALUES (new.id, new.content, new.subject, new.chapter_title);
            END;
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS chunks_fts")
        db.execSQL("DROP TABLE IF EXISTS syllabus_chunks")
        onCreate(db)
    }

    /**
     * Rebuilds the entire FTS5 index from the content table.
     */
    fun rebuildIndex() {
        writableDatabase.execSQL("INSERT INTO chunks_fts(chunks_fts) VALUES('rebuild');")
    }

    /**
     * Inserts a single syllabus chunk. Triggers automatically update chunks_fts.
     */
    fun insertChunk(
        chunkId: String,
        board: String,
        classGrade: String,
        subject: String,
        chapterTitle: String,
        sectionTitle: String?,
        pageReference: String?,
        content: String,
        sourceId: String? = null,
        corpusVersion: String? = null
    ): Long {
        val cv = ContentValues().apply {
            put("chunk_id", chunkId)
            put("board", board)
            put("class_grade", classGrade)
            put("subject", subject)
            put("chapter_title", chapterTitle)
            put("section_title", sectionTitle)
            put("page_reference", pageReference)
            put("content", content)
            put("source_id", sourceId)
            put("corpus_version", corpusVersion)
        }
        return writableDatabase.insertWithOnConflict(
            "syllabus_chunks",
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Updates an existing chunk by chunk_id. Triggers automatically update chunks_fts.
     */
    fun updateChunkContent(chunkId: String, newContent: String): Int {
        val cv = ContentValues().apply {
            put("content", newContent)
        }
        return writableDatabase.update(
            "syllabus_chunks",
            cv,
            "chunk_id = ?",
            arrayOf(chunkId)
        )
    }

    /**
     * Deletes a chunk by chunk_id. Triggers automatically delete from chunks_fts.
     */
    fun deleteChunk(chunkId: String): Int {
        return writableDatabase.delete(
            "syllabus_chunks",
            "chunk_id = ?",
            arrayOf(chunkId)
        )
    }

    /**
     * Bulk inserts a list of syllabus chunk maps (e.g. from an offline syllabus pack)
     * and rebuilds the index to ensure immediate searchability.
     */
    fun importSyllabusPack(chunks: List<Map<String, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (item in chunks) {
                val cv = ContentValues().apply {
                    put("chunk_id", item["chunk_id"] ?: java.util.UUID.randomUUID().toString())
                    put("board", item["board"] ?: "CBSE")
                    put("class_grade", item["class_grade"] ?: "10")
                    put("subject", item["subject"] ?: "Science")
                    put("chapter_title", item["chapter_title"] ?: "General")
                    put("section_title", item["section_title"])
                    put("page_reference", item["page_reference"])
                    put("content", item["content"] ?: "")
                    put("source_id", item["source_id"])
                    put("corpus_version", item["corpus_version"])
                }
                db.insertWithOnConflict("syllabus_chunks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        rebuildIndex()
    }

    /**
     * Checks if a syllabus pack is available locally.
     */
    fun hasPack(board: String?, classGrade: String?, subject: String?): Boolean {
        val db = readableDatabase
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (!board.isNullOrBlank()) {
            whereClauses.add("board = ?")
            args.add(board.trim())
        }
        if (!classGrade.isNullOrBlank()) {
            whereClauses.add("class_grade = ?")
            args.add(classGrade.trim())
        }
        if (!subject.isNullOrBlank()) {
            whereClauses.add("subject LIKE ?")
            args.add("%${subject.trim()}%")
        }

        val whereStr = if (whereClauses.isNotEmpty()) "WHERE " + whereClauses.joinToString(" AND ") else ""
        val cursor: Cursor = db.rawQuery("SELECT COUNT(*) FROM syllabus_chunks $whereStr", args.toTypedArray())
        val count = cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        return count > 0
    }

    /**
     * Executes FTS5 search using MATCH against chunks_fts and joins syllabus_chunks.
     */
    fun searchChunks(
        query: String,
        board: String? = null,
        classGrade: String? = null,
        subject: String? = null,
        limit: Int = 5
    ): List<RetrievedChunk> {
        val cleanQuery = query.replace("\"", "").trim()
        if (cleanQuery.isEmpty()) return emptyList()

        val ftsMatchQuery = cleanQuery.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" OR ") { "$it*" }

        val db = readableDatabase
        val sql = """
            SELECT c.chunk_id, c.content, c.board, c.class_grade, c.subject,
                   c.chapter_title, c.section_title, c.page_reference, c.source_id,
                   bm25(chunks_fts) as rank_score
            FROM chunks_fts f
            JOIN syllabus_chunks c ON f.rowid = c.id
            WHERE chunks_fts MATCH ?
              AND (? IS NULL OR c.board = ?)
              AND (? IS NULL OR c.class_grade = ?)
              AND (? IS NULL OR c.subject LIKE ?)
            ORDER BY rank_score ASC
            LIMIT ?
        """.trimIndent()

        val args = arrayOf(
            ftsMatchQuery,
            board, board,
            classGrade, classGrade,
            if (subject != null) "%$subject%" else null, if (subject != null) "%$subject%" else null,
            limit.toString()
        )

        val cursor = db.rawQuery(sql, args)
        val results = mutableListOf<RetrievedChunk>()

        cursor.use { c ->
            var rank = 1
            while (c.moveToNext()) {
                val chunkId = c.getString(0)
                val content = c.getString(1)
                val b = c.getString(2)
                val cg = c.getString(3)
                val s = c.getString(4)
                val chapter = c.getString(5)
                val section = c.getString(6)
                val pageRef = c.getString(7)
                val sourceId = c.getString(8)
                val bm25Score = c.getFloat(9)

                // Approximate normalized score based on rank (rank 1 = 0.85, rank 2 = 0.70, etc.)
                val normalizedScore = (1.0f / (rank + 0.5f)).coerceIn(0.0f, 1.0f)

                results.add(
                    RetrievedChunk(
                        chunkId = chunkId,
                        content = content,
                        board = b,
                        classGrade = cg,
                        subject = s,
                        chapter = chapter,
                        section = section,
                        textbookTitle = "$b Class $cg $s",
                        pageReference = pageRef,
                        sourceId = sourceId,
                        relevanceScore = -bm25Score,
                        normalizedScore = normalizedScore
                    )
                )
                rank++
            }
        }

        return results
    }
}
