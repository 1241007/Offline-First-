package com.offline_First.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.offline_First.domain.model.RetrievedChunk
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

private const val TAG = "SyllabusDatabase"
private const val SYLLABUS_DB_NAME = "edunova_syllabus.db"
// Bumped from version 1 to 2 to safely migrate away from unsupported SQLite FTS5 module
// on Android and enable safe LIKE-based search plus 384-dimensional vector embeddings.
private const val SYLLABUS_DB_VERSION = 2

/** Data class for inserting or syncing syllabus chunks with precomputed embeddings. */
data class SyllabusChunkRecord(
    val chunkId: String,
    val board: String,
    val classGrade: String,
    val subject: String,
    val chapterTitle: String,
    val sectionTitle: String? = null,
    val pageReference: String? = null,
    val content: String,
    val sourceId: String? = null,
    val corpusVersion: String? = null,
    val language: String = "en",
    val contentHash: String? = null,
    val embedding: FloatArray? = null,
    val embeddingModel: String? = null,
    val embeddingVersion: String? = null
)

/**
 * Android SQLite database for curriculum syllabus content and offline retrieval.
 *
 * Replaces the unverified FTS5 virtual table with a robust, Android API 24+ compatible
 * LIKE-based keyword search combined with local cosine-similarity vector search over
 * precomputed 384-dimensional multilingual embeddings.
 *
 * Upgrades safely from existing DB v1 instances without deleting existing records.
 */
open class SyllabusDatabase(context: Context) : SQLiteOpenHelper(
    context,
    SYLLABUS_DB_NAME,
    null,
    SYLLABUS_DB_VERSION
) {

    companion object {
        const val TABLE_CHUNKS = "syllabus_chunks"
        const val TABLE_PACKS = "syllabus_packs"
        const val DATABASE_VERSION = SYLLABUS_DB_VERSION
        const val EMBEDDING_DIMENSION = 384

        // Common short stop words that do not add keyword filtering value
        private val STOPWORDS = setOf(
            "a", "about", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to",
            "was", "were", "will", "with"
        )

        /**
         * Serializes a 384-element float array into raw 1536-byte little-endian BLOB.
         */
        fun floatArrayToBlob(vector: FloatArray?): ByteArray? {
            if (vector == null || vector.isEmpty()) return null
            val buffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (f in vector) {
                buffer.putFloat(f)
            }
            return buffer.array()
        }

        /**
         * Deserializes a raw little-endian BLOB into a float array.
         */
        fun blobToFloatArray(blob: ByteArray?): FloatArray? {
            if (blob == null || blob.isEmpty() || blob.size % 4 != 0) return null
            val count = blob.size / 4
            val buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
            val result = FloatArray(count)
            for (i in 0 until count) {
                result[i] = buffer.float
            }
            return result
        }

        /**
         * Calculates cosine similarity between two float vectors.
         */
        fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
            if (v1.size != v2.size || v1.isEmpty()) return 0.0f
            var dot = 0.0f
            var norm1 = 0.0f
            var norm2 = 0.0f
            for (i in v1.indices) {
                val a = v1[i]
                val b = v2[i]
                dot += a * b
                norm1 += a * a
                norm2 += b * b
            }
            val denom = sqrt(norm1) * sqrt(norm2)
            return if (denom > 1e-9f) (dot / denom).coerceIn(-1.0f, 1.0f) else 0.0f
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Core syllabus chunks table with full curriculum metadata and embedding storage
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CHUNKS (
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
                corpus_version TEXT,
                language TEXT DEFAULT 'en',
                content_hash TEXT,
                embedding BLOB,
                embedding_model TEXT,
                embedding_version TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """.trimIndent())

        // 2. Syllabus packs metadata table for version tracking, integrity, and idempotency
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_PACKS (
                pack_id TEXT PRIMARY KEY,
                board TEXT NOT NULL,
                class_grade TEXT NOT NULL,
                subject TEXT NOT NULL,
                academic_year TEXT,
                corpus_version TEXT,
                content_hash TEXT,
                chunk_count INTEGER,
                last_synced_at INTEGER
            )
        """.trimIndent())

        // 3. High-performance B-tree indexes for fast curriculum filtering and keyword lookups
        createIndexes(db)
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sc_board ON $TABLE_CHUNKS(board)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sc_class ON $TABLE_CHUNKS(class_grade)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sc_subject ON $TABLE_CHUNKS(subject)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sc_chapter ON $TABLE_CHUNKS(chapter_title)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sc_chunk_id ON $TABLE_CHUNKS(chunk_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sc_hash ON $TABLE_CHUNKS(content_hash)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sp_pack_id ON $TABLE_PACKS(pack_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "Upgrading database from version $oldVersion to $newVersion")
        if (oldVersion < 2) {
            // Safe upgrade path from v1 (which had FTS5 dependencies) to v2
            // 1. Remove legacy FTS5 triggers safely
            val triggers = listOf("chunks_ai", "chunks_ad", "chunks_au")
            for (trigger in triggers) {
                try {
                    db.execSQL("DROP TRIGGER IF EXISTS $trigger")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not drop legacy trigger $trigger: ${e.message}")
                }
            }

            // 2. Remove legacy FTS5 virtual table if it exists
            try {
                db.execSQL("DROP TABLE IF EXISTS chunks_fts")
            } catch (e: Exception) {
                Log.w(TAG, "Could not drop legacy virtual table chunks_fts: ${e.message}")
            }

            // 3. Inspect existing syllabus_chunks columns and add any missing ones without dropping rows
            try {
                val existingCols = mutableSetOf<String>()
                val cursor = db.rawQuery("PRAGMA table_info($TABLE_CHUNKS)", null)
                cursor.use { c ->
                    val nameIdx = c.getColumnIndex("name")
                    while (c.moveToNext()) {
                        if (nameIdx >= 0) {
                            existingCols.add(c.getString(nameIdx).lowercase())
                        }
                    }
                }

                val columnsToAdd = listOf(
                    "language" to "TEXT DEFAULT 'en'",
                    "content_hash" to "TEXT",
                    "embedding" to "BLOB",
                    "embedding_model" to "TEXT",
                    "embedding_version" to "TEXT",
                    "created_at" to "TEXT",
                    "updated_at" to "TEXT"
                )

                for ((colName, colType) in columnsToAdd) {
                    if (!existingCols.contains(colName.lowercase())) {
                        db.execSQL("ALTER TABLE $TABLE_CHUNKS ADD COLUMN $colName $colType")
                        Log.i(TAG, "Added column $colName to $TABLE_CHUNKS")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to safely migrate columns on $TABLE_CHUNKS: ${e.message}", e)
            }

            // 4. Ensure indexes exist
            createIndexes(db)
        }
    }

    /**
     * Backward-compatible rebuild index stub.
     * With standard B-tree tables, SQLite handles indexing automatically.
     */
    fun rebuildIndex() {
        // No-op for B-tree tables. Safe to call from existing code without exceptions.
    }

    /**
     * Inserts or updates a single syllabus chunk.
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
        corpusVersion: String? = null,
        language: String = "en",
        contentHash: String? = null,
        embedding: FloatArray? = null,
        embeddingModel: String? = if (embedding != null) E5EmbeddingService.MODEL_ID else null,
        embeddingVersion: String? = if (embedding != null) E5EmbeddingService.MODEL_VERSION else null
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
            put("language", language)
            put("content_hash", contentHash)
            if (embedding != null) {
                put("embedding", floatArrayToBlob(embedding))
            }
            put("embedding_model", embeddingModel)
            put("embedding_version", embeddingVersion)
            put("updated_at", System.currentTimeMillis().toString())
        }
        return writableDatabase.insertWithOnConflict(
            TABLE_CHUNKS,
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Updates an existing chunk's content.
     */
    fun updateChunkContent(chunkId: String, newContent: String): Int {
        val cv = ContentValues().apply {
            put("content", newContent)
            put("updated_at", System.currentTimeMillis().toString())
        }
        return writableDatabase.update(
            TABLE_CHUNKS,
            cv,
            "chunk_id = ?",
            arrayOf(chunkId)
        )
    }

    /**
     * Deletes a chunk by chunk_id.
     */
    fun deleteChunk(chunkId: String): Int {
        return writableDatabase.delete(
            TABLE_CHUNKS,
            "chunk_id = ?",
            arrayOf(chunkId)
        )
    }

    /**
     * Bulk imports a syllabus pack from legacy map format.
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
                    put("language", item["language"] ?: "en")
                    put("content_hash", item["content_hash"])
                    put("embedding_model", item["embedding_model"])
                    put("embedding_version", item["embedding_version"])
                }
                db.insertWithOnConflict(TABLE_CHUNKS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Bulk imports syllabus chunk records including precomputed vector embeddings.
     */
    open fun importSyllabusRecords(records: List<SyllabusChunkRecord>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (rec in records) {
                val cv = ContentValues().apply {
                    put("chunk_id", rec.chunkId)
                    put("board", rec.board)
                    put("class_grade", rec.classGrade)
                    put("subject", rec.subject)
                    put("chapter_title", rec.chapterTitle)
                    put("section_title", rec.sectionTitle)
                    put("page_reference", rec.pageReference)
                    put("content", rec.content)
                    put("source_id", rec.sourceId)
                    put("corpus_version", rec.corpusVersion)
                    put("language", rec.language)
                    put("content_hash", rec.contentHash)
                    if (rec.embedding != null) {
                        put("embedding", floatArrayToBlob(rec.embedding))
                    }
                    put("embedding_model", rec.embeddingModel)
                    put("embedding_version", rec.embeddingVersion)
                    put("updated_at", System.currentTimeMillis().toString())
                }
                db.insertWithOnConflict(TABLE_CHUNKS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
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
        val cursor: Cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CHUNKS $whereStr", args.toTypedArray())
        val count = cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        return count > 0
    }

    /**
     * Records successful syllabus pack synchronization with its deterministic content hash.
     */
    open fun recordPackSync(
        packId: String,
        board: String,
        classGrade: String,
        subject: String,
        academicYear: String? = "2026-27",
        corpusVersion: String? = "1.0",
        contentHash: String? = null,
        chunkCount: Int = 0
    ) {
        val cv = ContentValues().apply {
            put("pack_id", packId)
            put("board", board)
            put("class_grade", classGrade)
            put("subject", subject)
            put("academic_year", academicYear)
            put("corpus_version", corpusVersion)
            put("content_hash", contentHash)
            put("chunk_count", chunkCount)
            put("last_synced_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_PACKS,
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Atomically replaces all chunks for a curriculum pack and updates the pack sync record
     * in a single ACID transaction. If any failure occurs during staging or writing, the
     * previous pack chunks and metadata remain completely uncorrupted.
     */
    open fun replacePackChunks(
        packId: String,
        board: String,
        classGrade: String,
        subject: String,
        academicYear: String? = "2026-27",
        corpusVersion: String? = "1.0",
        contentHash: String? = null,
        records: List<SyllabusChunkRecord>
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 1. Delete previous chunks for this specific curriculum pack
            db.delete(
                TABLE_CHUNKS,
                "board = ? AND class_grade = ? AND subject = ?",
                arrayOf(board, classGrade, subject)
            )

            // 2. Insert all new staged records
            for (rec in records) {
                val cv = ContentValues().apply {
                    put("chunk_id", rec.chunkId)
                    put("board", rec.board)
                    put("class_grade", rec.classGrade)
                    put("subject", rec.subject)
                    put("chapter_title", rec.chapterTitle)
                    put("section_title", rec.sectionTitle)
                    put("page_reference", rec.pageReference)
                    put("content", rec.content)
                    put("source_id", rec.sourceId)
                    put("corpus_version", rec.corpusVersion)
                    put("language", rec.language)
                    put("content_hash", rec.contentHash)
                    if (rec.embedding != null) {
                        put("embedding", floatArrayToBlob(rec.embedding))
                    }
                    put("embedding_model", rec.embeddingModel)
                    put("embedding_version", rec.embeddingVersion)
                    put("updated_at", System.currentTimeMillis().toString())
                }
                db.insertWithOnConflict(TABLE_CHUNKS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // 3. Atomically update syllabus_packs table
            val packCv = ContentValues().apply {
                put("pack_id", packId)
                put("board", board)
                put("class_grade", classGrade)
                put("subject", subject)
                put("academic_year", academicYear)
                put("corpus_version", corpusVersion)
                put("content_hash", contentHash)
                put("chunk_count", records.size)
                put("last_synced_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                TABLE_PACKS,
                null,
                packCv,
                SQLiteDatabase.CONFLICT_REPLACE
            )

            db.setTransactionSuccessful()
            Log.i(TAG, "Atomically replaced pack '$packId' with ${records.size} chunks (hash: $contentHash)")
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Retrieves the stored content hash for a locally synchronized pack.
     */
    open fun getPackContentHash(packId: String): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT content_hash FROM $TABLE_PACKS WHERE pack_id = ?", arrayOf(packId))
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    /**
     * Checks if the local pack is current by comparing against remote manifest content hash.
     */
    open fun isPackCurrent(packId: String, remoteContentHash: String?): Boolean {
        if (remoteContentHash.isNullOrBlank()) return false
        val localHash = getPackContentHash(packId)
        return localHash != null && localHash.equals(remoteContentHash.trim(), ignoreCase = true)
    }

    /**
     * Identifies and invalidates any legacy or incompatible embeddings (e.g. SHA-256 pseudo-vectors
     * or older model versions) so they cannot silently coexist with genuine E5 embeddings.
     * Sets their embedding BLOB to null so they safely fall back to keyword search until re-synced.
     *
     * @return Number of invalidated records.
     */
    fun invalidateIncompatibleEmbeddings(
        expectedModel: String = E5EmbeddingService.MODEL_ID,
        expectedVersion: String = E5EmbeddingService.MODEL_VERSION
    ): Int {
        val db = writableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_CHUNKS WHERE embedding IS NOT NULL AND (embedding_model IS NOT NULL AND embedding_model != ? OR embedding_version IS NOT NULL AND embedding_version != ?)",
            arrayOf(expectedModel, expectedVersion)
        )
        val count = cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
        if (count > 0) {
            val cv = ContentValues().apply {
                putNull("embedding")
                putNull("embedding_model")
                putNull("embedding_version")
            }
            db.update(
                TABLE_CHUNKS,
                cv,
                "embedding IS NOT NULL AND (embedding_model IS NOT NULL AND embedding_model != ? OR embedding_version IS NOT NULL AND embedding_version != ?)",
                arrayOf(expectedModel, expectedVersion)
            )
            Log.i(TAG, "Invalidated $count incompatible embedding records")
        }
        return count
    }

    /**
     * Safely rebuilds embeddings for chunks whose embedding is missing or was invalidated,
     * using the real on-device E5EmbeddingService.
     */
    suspend fun rebuildMissingEmbeddings(
        e5Service: E5EmbeddingService,
        batchSize: Int = 50
    ): Int {
        if (!e5Service.isModelReady()) return 0
        val db = writableDatabase
        val cursor = db.rawQuery(
            "SELECT chunk_id, content FROM $TABLE_CHUNKS WHERE embedding IS NULL LIMIT ?",
            arrayOf(batchSize.toString())
        )
        val chunksToEmbed = mutableListOf<Pair<String, String>>()
        cursor.use { c ->
            while (c.moveToNext()) {
                chunksToEmbed.add(Pair(c.getString(0), c.getString(1)))
            }
        }
        var updated = 0
        for ((chunkId, content) in chunksToEmbed) {
            val vector = e5Service.embedPassage(content)
            val cv = ContentValues().apply {
                put("embedding", floatArrayToBlob(vector))
                put("embedding_model", E5EmbeddingService.MODEL_ID)
                put("embedding_version", E5EmbeddingService.MODEL_VERSION)
                put("updated_at", System.currentTimeMillis().toString())
            }
            db.update(TABLE_CHUNKS, cv, "chunk_id = ?", arrayOf(chunkId))
            updated++
        }
        return updated
    }

    /**
     * Executes reliable LIKE-based keyword search on Android SQLite.
     *
     * Extracts keywords, constructs weighted score expressions, filters by curriculum metadata,
     * and normalizes the resulting relevance scores.
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

        val rawTokens = cleanQuery.split("\\s+".toRegex()).map { it.trim().lowercase() }
        val filteredTokens = rawTokens.filter { it.length >= 2 && !STOPWORDS.contains(it) }
        val keywords = if (filteredTokens.isNotEmpty()) filteredTokens else rawTokens.filter { it.isNotBlank() }
        if (keywords.isEmpty()) return emptyList()

        val db = readableDatabase

        // Build SELECT score expression:
        // Match in chapter_title: 4 pts
        // Match in subject: 3 pts
        // Match in section_title: 2 pts
        // Match in content: 1 pt
        // Match full query: 5 bonus pts
        val scoreParts = mutableListOf<String>()
        val scoreArgs = mutableListOf<String>()

        for (kw in keywords) {
            val pattern = "%$kw%"
            scoreParts.add("""
                (CASE WHEN lower(c.chapter_title) LIKE ? THEN 4 ELSE 0 END +
                 CASE WHEN lower(c.subject) LIKE ? THEN 3 ELSE 0 END +
                 CASE WHEN lower(coalesce(c.section_title, '')) LIKE ? THEN 2 ELSE 0 END +
                 CASE WHEN lower(c.content) LIKE ? THEN 1 ELSE 0 END)
            """.trimIndent())
            scoreArgs.add(pattern)
            scoreArgs.add(pattern)
            scoreArgs.add(pattern)
            scoreArgs.add(pattern)
        }

        // Exact full phrase bonus
        val fullPhrasePattern = "%${cleanQuery.lowercase()}%"
        scoreParts.add("""
            (CASE WHEN lower(c.content) LIKE ? THEN 5 ELSE 0 END +
             CASE WHEN lower(c.chapter_title) LIKE ? THEN 5 ELSE 0 END)
        """.trimIndent())
        scoreArgs.add(fullPhrasePattern)
        scoreArgs.add(fullPhrasePattern)

        val scoreExpr = scoreParts.joinToString(" + ")

        // WHERE clauses
        val whereClauses = mutableListOf<String>()
        val filterArgs = mutableListOf<String>()

        // At least one keyword must appear in content, chapter, or subject
        val keywordMatchClauses = keywords.map { kw ->
            val p = "%$kw%"
            filterArgs.add(p)
            filterArgs.add(p)
            filterArgs.add(p)
            "(lower(c.content) LIKE ? OR lower(c.chapter_title) LIKE ? OR lower(c.subject) LIKE ?)"
        }
        whereClauses.add("(${keywordMatchClauses.joinToString(" OR ")})")

        if (!board.isNullOrBlank()) {
            whereClauses.add("c.board = ?")
            filterArgs.add(board.trim())
        }
        if (!classGrade.isNullOrBlank()) {
            whereClauses.add("c.class_grade = ?")
            filterArgs.add(classGrade.trim())
        }
        if (!subject.isNullOrBlank()) {
            whereClauses.add("c.subject LIKE ?")
            filterArgs.add("%${subject.trim()}%")
        }

        val allArgs = ArrayList<String>(scoreArgs.size + filterArgs.size + 1)
        allArgs.addAll(scoreArgs)
        allArgs.addAll(filterArgs)
        allArgs.add(limit.toString())

        val sql = """
            SELECT c.chunk_id, c.content, c.board, c.class_grade, c.subject,
                   c.chapter_title, c.section_title, c.page_reference, c.source_id,
                   ($scoreExpr) AS match_score
            FROM $TABLE_CHUNKS c
            WHERE ${whereClauses.joinToString(" AND ")}
            ORDER BY match_score DESC, c.chunk_id ASC
            LIMIT ?
        """.trimIndent()

        val results = mutableListOf<RetrievedChunk>()
        val maxTheoreticalScore = (keywords.size * 4.0f + 5.0f).coerceAtLeast(1.0f)

        try {
            val cursor = db.rawQuery(sql, allArgs.toTypedArray())
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
                    val rawScore = c.getFloat(9)

                    // Normalized score calibrated to [0.0, 1.0]
                    // Rank 1 with strong matches reaches >= 0.70 (SUFFICIENT)
                    val scoreFactor = (rawScore / maxTheoreticalScore).coerceIn(0.1f, 1.0f)
                    val rankDecay = 1.0f / (rank * 0.35f + 0.65f)
                    val normalizedScore = (scoreFactor * 0.7f + rankDecay * 0.3f).coerceIn(0.0f, 1.0f)

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
                            relevanceScore = rawScore,
                            rawRrfScore = rawScore,
                            normalizedScore = normalizedScore
                        )
                    )
                    rank++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "LIKE search query failed: ${e.message}", e)
        }

        return results
    }

    /**
     * Executes local vector cosine similarity search over stored 384-dimensional embeddings.
     */
    fun searchByVector(
        queryEmbedding: FloatArray,
        board: String? = null,
        classGrade: String? = null,
        subject: String? = null,
        limit: Int = 5
    ): List<RetrievedChunk> {
        if (queryEmbedding.size != EMBEDDING_DIMENSION) {
            Log.w(TAG, "Query embedding dimension ${queryEmbedding.size} does not match expected $EMBEDDING_DIMENSION")
            return emptyList()
        }

        val db = readableDatabase
        val whereClauses = mutableListOf(
            "embedding IS NOT NULL",
            "(embedding_model = ? OR embedding_model IS NULL)",
            "(embedding_version = ? OR embedding_version IS NULL)"
        )
        val args = mutableListOf(E5EmbeddingService.MODEL_ID, E5EmbeddingService.MODEL_VERSION)

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

        val sql = """
            SELECT chunk_id, content, board, class_grade, subject,
                   chapter_title, section_title, page_reference, source_id, embedding
            FROM $TABLE_CHUNKS
            WHERE ${whereClauses.joinToString(" AND ")}
        """.trimIndent()

        data class ScoredItem(val chunk: RetrievedChunk, val similarity: Float)
        val scoredList = mutableListOf<ScoredItem>()

        try {
            val cursor = db.rawQuery(sql, args.toTypedArray())
            cursor.use { c ->
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
                    val blob = c.getBlob(9)

                    val chunkEmbedding = blobToFloatArray(blob) ?: continue
                    val sim = cosineSimilarity(queryEmbedding, chunkEmbedding)

                    val chunk = RetrievedChunk(
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
                        relevanceScore = sim,
                        rawRrfScore = sim,
                        normalizedScore = ((sim + 1.0f) / 2.0f).coerceIn(0.0f, 1.0f),
                        vectorRank = null,
                        sparseRank = null,
                        similarityScore = sim
                    )
                    scoredList.add(ScoredItem(chunk, sim))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vector search failed: ${e.message}", e)
        }

        scoredList.sortWith(compareByDescending<ScoredItem> { it.similarity }.thenBy { it.chunk.chunkId })
        return scoredList.take(limit).mapIndexed { idx, item ->
            item.chunk.copy(vectorRank = idx + 1)
        }
    }

    /**
     * Executes hybrid retrieval combining keyword search and vector search with RRF scoring.
     */
    fun hybridSearch(
        query: String,
        queryEmbedding: FloatArray? = null,
        board: String? = null,
        classGrade: String? = null,
        subject: String? = null,
        limit: Int = 5
    ): List<RetrievedChunk> {
        val keywordResults = searchChunks(query, board, classGrade, subject, limit = limit * 2)

        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return keywordResults.take(limit).mapIndexed { idx, chunk ->
                chunk.copy(sparseRank = idx + 1)
            }
        }

        val vectorResults = searchByVector(queryEmbedding, board, classGrade, subject, limit = limit * 2)
        if (vectorResults.isEmpty()) {
            return keywordResults.take(limit).mapIndexed { idx, chunk ->
                chunk.copy(sparseRank = idx + 1)
            }
        }

        // Reciprocal Rank Fusion (k=60)
        val rrfK = 60
        val rrfMaxScore = (1.0f / (rrfK + 1)) * 2.0f
        val scores = mutableMapOf<String, Float>()
        val chunkMap = mutableMapOf<String, RetrievedChunk>()
        val vectorRanks = mutableMapOf<String, Int>()
        val sparseRanks = mutableMapOf<String, Int>()
        val similarities = mutableMapOf<String, Float>()

        keywordResults.forEachIndexed { idx, chunk ->
            val rank = idx + 1
            scores[chunk.chunkId] = (scores[chunk.chunkId] ?: 0.0f) + (1.0f / (rrfK + rank))
            sparseRanks[chunk.chunkId] = rank
            chunkMap[chunk.chunkId] = chunk
        }

        vectorResults.forEachIndexed { idx, chunk ->
            val rank = idx + 1
            scores[chunk.chunkId] = (scores[chunk.chunkId] ?: 0.0f) + (1.0f / (rrfK + rank))
            vectorRanks[chunk.chunkId] = rank
            chunk.similarityScore?.let { similarities[chunk.chunkId] = it }
                ?: chunk.relevanceScore?.let { similarities[chunk.chunkId] = it }
            chunkMap.putIfAbsent(chunk.chunkId, chunk)
        }

        val sorted = scores.entries.sortedWith(compareByDescending<Map.Entry<String, Float>> { it.value }.thenBy { it.key })
        return sorted.take(limit).mapNotNull { entry ->
            val baseChunk = chunkMap[entry.key] ?: return@mapNotNull null
            val rawRrf = entry.value
            val normalized = (rawRrf / rrfMaxScore).coerceIn(0.0f, 1.0f)
            baseChunk.copy(
                relevanceScore = normalized,
                rawRrfScore = rawRrf,
                normalizedScore = normalized,
                vectorRank = vectorRanks[entry.key],
                sparseRank = sparseRanks[entry.key],
                similarityScore = similarities[entry.key]
            )
        }
    }
}
