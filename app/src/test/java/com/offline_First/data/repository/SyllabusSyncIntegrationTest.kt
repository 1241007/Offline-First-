package com.offline_First.data.repository

import com.offline_First.data.local.SyllabusChunkRecord
import com.offline_First.data.local.SyllabusDatabase
import com.offline_First.data.remote.SyllabusChunkDto
import com.offline_First.data.remote.SyllabusChunksResponseDto
import com.offline_First.data.remote.SyllabusManifestDto
import com.offline_First.data.remote.SyllabusPackSummaryDto
import com.offline_First.data.remote.SyllabusSyncClient
import com.offline_First.domain.model.RetrievedChunk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * In-memory test database modeling transactional SQLite behavior,
 * pack staging, transaction rollback, curriculum isolation, and offline search.
 */
class InMemorySyllabusDatabase : SyllabusDatabase(android.content.ContextWrapper(null)) {
    val chunksMap = mutableMapOf<String, SyllabusChunkRecord>()
    val packsMap = mutableMapOf<String, PackMeta>()

    // Transaction state
    private var inTransaction = false
    private var transactionSuccessful = false
    private var chunksBackup = mutableMapOf<String, SyllabusChunkRecord>()
    private var packsBackup = mutableMapOf<String, PackMeta>()

    var throwInTransaction = false

    data class PackMeta(
        val packId: String,
        val board: String,
        val classGrade: String,
        val subject: String,
        val academicYear: String?,
        val corpusVersion: String?,
        val contentHash: String?,
        val chunkCount: Int,
        val lastSyncedAt: Long
    )

    fun beginTransaction() {
        inTransaction = true
        transactionSuccessful = false
        chunksBackup = HashMap(chunksMap)
        packsBackup = HashMap(packsMap)
    }

    fun setTransactionSuccessful() {
        transactionSuccessful = true
    }

    fun endTransaction() {
        if (!transactionSuccessful) {
            // Rollback
            chunksMap.clear()
            chunksMap.putAll(chunksBackup)
            packsMap.clear()
            packsMap.putAll(packsBackup)
        }
        inTransaction = false
        transactionSuccessful = false
    }

    override fun replacePackChunks(
        packId: String,
        board: String,
        classGrade: String,
        subject: String,
        academicYear: String?,
        corpusVersion: String?,
        contentHash: String?,
        records: List<SyllabusChunkRecord>
    ) {
        beginTransaction()
        try {
            if (throwInTransaction) {
                throw IllegalStateException("Simulated SQLite write/disk failure during replacePackChunks")
            }

            // 1. Delete previous chunks for this curriculum pack
            val toRemove = chunksMap.filter { (_, rec) ->
                rec.board.equals(board, ignoreCase = true) &&
                rec.classGrade.equals(classGrade, ignoreCase = true) &&
                rec.subject.equals(subject, ignoreCase = true)
            }.keys
            for (k in toRemove) {
                chunksMap.remove(k)
            }

            // 2. Insert new records
            for (rec in records) {
                chunksMap[rec.chunkId] = rec
            }

            // 3. Update pack metadata
            packsMap[packId] = PackMeta(
                packId = packId,
                board = board,
                classGrade = classGrade,
                subject = subject,
                academicYear = academicYear,
                corpusVersion = corpusVersion,
                contentHash = contentHash,
                chunkCount = records.size,
                lastSyncedAt = System.currentTimeMillis()
            )

            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    override fun isPackCurrent(packId: String, remoteContentHash: String?): Boolean {
        if (remoteContentHash.isNullOrBlank()) return false
        val pack = packsMap[packId] ?: return false
        return pack.contentHash.equals(remoteContentHash.trim(), ignoreCase = true)
    }

    override fun getPackContentHash(packId: String): String? {
        return packsMap[packId]?.contentHash
    }

    fun searchOffline(
        query: String,
        board: String? = null,
        classGrade: String? = null,
        subject: String? = null
    ): List<RetrievedChunk> {
        val qTokens = query.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.length >= 3 }
        val matches = mutableListOf<RetrievedChunk>()

        for ((_, rec) in chunksMap) {
            if (!board.isNullOrBlank() && !rec.board.equals(board, ignoreCase = true)) continue
            if (!classGrade.isNullOrBlank() && !rec.classGrade.equals(classGrade, ignoreCase = true)) continue
            if (!subject.isNullOrBlank() && !rec.subject.equals(subject, ignoreCase = true)) continue

            var score = 0.0f
            val cText = rec.content.lowercase()
            for (t in qTokens) {
                if (cText.contains(t)) score += 1.0f
            }
            if (score > 0.0f) {
                matches.add(
                    RetrievedChunk(
                        chunkId = rec.chunkId,
                        content = rec.content,
                        board = rec.board,
                        classGrade = rec.classGrade,
                        subject = rec.subject,
                        chapter = rec.chapterTitle,
                        relevanceScore = score
                    )
                )
            }
        }
        return matches.sortedByDescending { it.relevanceScore ?: 0f }
    }
}

/**
 * Scriptable SyllabusSyncClient for testing multi-step sync flows.
 */
class ScriptedSyllabusSyncClient : SyllabusSyncClient {
    var packs: List<SyllabusPackSummaryDto> = emptyList()
    var manifests = mutableMapOf<String, SyllabusManifestDto>()
    var pagesByPack = mutableMapOf<String, MutableList<SyllabusChunksResponseDto>>()
    var pagesServed = 0
    var throwOnPage = -1

    override suspend fun fetchAvailablePacks(): List<SyllabusPackSummaryDto> = packs

    override suspend fun fetchPackManifest(packId: String): SyllabusManifestDto {
        return manifests[packId] ?: throw NoSuchElementException("Manifest not found: $packId")
    }

    override suspend fun fetchPackChunks(
        packId: String,
        cursor: String?,
        limit: Int
    ): SyllabusChunksResponseDto {
        pagesServed++
        if (throwOnPage > 0 && pagesServed >= throwOnPage) {
            throw IOException("Simulated network disconnection during chunk page $pagesServed")
        }

        val pages = pagesByPack[packId] ?: throw NoSuchElementException("No pages for $packId")
        val page = pages.firstOrNull { it.chunks.any { c -> cursor == null || c.chunkId > cursor } }
            ?: pages.last()
        return page
    }
}

class SyllabusSyncIntegrationTest {

    private fun createDummyVector(): List<Float> = List(384) { 0.05f }

    private fun createPackSummary(
        packId: String,
        board: String,
        classGrade: String,
        subject: String,
        chunkCount: Int = 1,
        embeddingModel: String = "intfloat/multilingual-e5-small"
    ) = SyllabusPackSummaryDto(
        packId = packId,
        board = board,
        classGrade = classGrade,
        subject = subject,
        chunkCount = chunkCount,
        embeddingModel = embeddingModel
    )

    private fun createManifest(
        packId: String,
        board: String,
        classGrade: String,
        subject: String,
        chunkCount: Int = 1,
        corpusVersion: String = "1.0",
        contentHash: String? = "mock-hash",
        embeddingModel: String = "intfloat/multilingual-e5-small",
        embeddingDimension: Int = 384
    ) = SyllabusManifestDto(
        packId = packId,
        board = board,
        classGrade = classGrade,
        subject = subject,
        chunkCount = chunkCount,
        corpusVersion = corpusVersion,
        contentHash = contentHash,
        embeddingModel = embeddingModel,
        embeddingDimension = embeddingDimension
    )

    // ==============================================================================
    // 1. First Synchronization (Cold Pack Persistence)
    // ==============================================================================
    @Test
    fun test_001_first_synchronization_cold_pack_persistence() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        client.packs = listOf(
            createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 3)
        )
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 3, contentHash = "hash-v1-initial"
        )
        client.pagesByPack["cbse-10-science"] = mutableListOf(
            SyllabusChunksResponseDto(
                packId = "cbse-10-science",
                chunks = listOf(
                    SyllabusChunkDto("c1", "Chemical reactions and stoichiometry", "CBSE", "10", "Science", chapter = "Chemical Reactions", embedding = createDummyVector()),
                    SyllabusChunkDto("c2", "Acids, bases, and indicator reactions", "CBSE", "10", "Science", chapter = "Acids and Bases", embedding = createDummyVector()),
                    SyllabusChunkDto("c3", "Metals, non-metals, and reactivity series", "CBSE", "10", "Science", chapter = "Metals", embedding = createDummyVector())
                ),
                nextCursor = null,
                hasMore = false,
                totalChunks = 3,
                embeddingModel = "intfloat/multilingual-e5-small",
                embeddingDimension = 384
            )
        )

        val repo = SyllabusSyncRepository(client, db)
        val result = repo.syncCurriculumPack("CBSE", "10", "Science")

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
        assertEquals(3, db.chunksMap.size)
        val pack = db.packsMap["cbse-10-science"]
        assertNotNull(pack)
        assertEquals("hash-v1-initial", pack?.contentHash)
        assertEquals(3, pack?.chunkCount)
    }

    // ==============================================================================
    // 2. Repeated Synchronization (Idempotency - No Duplicate Rows)
    // ==============================================================================
    @Test
    fun test_002_repeated_synchronization_idempotency_no_duplicates() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        client.packs = listOf(createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 2))
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 2, contentHash = "hash-idempotent-test"
        )
        client.pagesByPack["cbse-10-science"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-science", listOf(
                SyllabusChunkDto("c1", "Concept 1", "CBSE", "10", "Science", embedding = createDummyVector()),
                SyllabusChunkDto("c2", "Concept 2", "CBSE", "10", "Science", embedding = createDummyVector())
            ), null, false, 2, "intfloat/multilingual-e5-small", "1.0", 384)
        )

        val repo = SyllabusSyncRepository(client, db)
        repo.syncCurriculumPack("CBSE", "10", "Science")
        assertEquals(2, db.chunksMap.size)

        // Second sync (should detect identical hash and skip download)
        val resync = repo.syncCurriculumPack("CBSE", "10", "Science")
        assertTrue(resync.isSuccess)
        // Must maintain exactly 2 chunks (zero duplicates created)
        assertEquals(2, db.chunksMap.size)
    }

    // ==============================================================================
    // 3. Incremental Synchronization After Corpus Version Changes
    // ==============================================================================
    @Test
    fun test_003_incremental_sync_after_corpus_version_change() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        // Setup v1
        client.packs = listOf(createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 1))
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 1, corpusVersion = "1.0", contentHash = "hash-v1"
        )
        client.pagesByPack["cbse-10-science"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-science", listOf(
                SyllabusChunkDto("c1", "Old v1 content", "CBSE", "10", "Science", embedding = createDummyVector())
            ), null, false, 1, "intfloat/multilingual-e5-small", "1.0", 384)
        )

        val repo = SyllabusSyncRepository(client, db)
        repo.syncCurriculumPack("CBSE", "10", "Science")
        assertEquals("hash-v1", db.packsMap["cbse-10-science"]?.contentHash)
        assertEquals("Old v1 content", db.chunksMap["c1"]?.content)

        // Update to v2
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 2, corpusVersion = "2.0", contentHash = "hash-v2-updated"
        )
        client.pagesByPack["cbse-10-science"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-science", listOf(
                SyllabusChunkDto("c1", "Updated v2 content with clarifications", "CBSE", "10", "Science", embedding = createDummyVector()),
                SyllabusChunkDto("c2-new", "Brand new chunk in v2", "CBSE", "10", "Science", embedding = createDummyVector())
            ), null, false, 2, "intfloat/multilingual-e5-small", "1.0", 384)
        )

        val updateResult = repo.syncCurriculumPack("CBSE", "10", "Science")
        assertTrue(updateResult.isSuccess)
        assertEquals(2, db.chunksMap.size)
        assertEquals("Updated v2 content with clarifications", db.chunksMap["c1"]?.content)
        assertEquals("hash-v2-updated", db.packsMap["cbse-10-science"]?.contentHash)
    }

    // ==============================================================================
    // 4. Pagination Across Multiple Chunk Pages
    // ==============================================================================
    @Test
    fun test_004_pagination_across_multiple_chunk_pages() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        client.packs = listOf(createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 4))
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 4, contentHash = "hash-multi-page"
        )

        val p1 = SyllabusChunksResponseDto("cbse-10-science", listOf(
            SyllabusChunkDto("c1", "P1 Chunk 1", "CBSE", "10", "Science", embedding = createDummyVector()),
            SyllabusChunkDto("c2", "P1 Chunk 2", "CBSE", "10", "Science", embedding = createDummyVector())
        ), nextCursor = "c2", hasMore = true, totalChunks = 4, embeddingModel = "intfloat/multilingual-e5-small", embeddingDimension = 384)

        val p2 = SyllabusChunksResponseDto("cbse-10-science", listOf(
            SyllabusChunkDto("c3", "P2 Chunk 3", "CBSE", "10", "Science", embedding = createDummyVector()),
            SyllabusChunkDto("c4", "P2 Chunk 4", "CBSE", "10", "Science", embedding = createDummyVector())
        ), nextCursor = null, hasMore = false, totalChunks = 4, embeddingModel = "intfloat/multilingual-e5-small", embeddingDimension = 384)

        client.pagesByPack["cbse-10-science"] = mutableListOf(p1, p2)

        val repo = SyllabusSyncRepository(client, db)
        val result = repo.syncCurriculumPack("CBSE", "10", "Science")

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull())
        assertEquals(4, db.chunksMap.size)
        assertTrue(db.chunksMap.containsKey("c1"))
        assertTrue(db.chunksMap.containsKey("c4"))
    }

    // ==============================================================================
    // 5. Interrupted Download and Retry (Staging Prevents Dirty State)
    // ==============================================================================
    @Test
    fun test_005_interrupted_download_staging_prevents_dirty_state() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        client.packs = listOf(createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 4))
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 4, contentHash = "hash-fail"
        )
        client.throwOnPage = 2 // Simulate network dropping on page 2

        val p1 = SyllabusChunksResponseDto("cbse-10-science", listOf(
            SyllabusChunkDto("c1", "P1 Chunk 1", "CBSE", "10", "Science", embedding = createDummyVector())
        ), nextCursor = "c1", hasMore = true, totalChunks = 4, embeddingModel = "intfloat/multilingual-e5-small", embeddingDimension = 384)

        val p2 = SyllabusChunksResponseDto("cbse-10-science", listOf(
            SyllabusChunkDto("c2", "P2 Chunk 2", "CBSE", "10", "Science", embedding = createDummyVector())
        ), nextCursor = null, hasMore = false, totalChunks = 4, embeddingModel = "intfloat/multilingual-e5-small", embeddingDimension = 384)

        client.pagesByPack["cbse-10-science"] = mutableListOf(p1, p2)

        val repo = SyllabusSyncRepository(client, db)
        val result = repo.syncCurriculumPack("CBSE", "10", "Science")

        assertTrue(result.isFailure)
        // Memory staging ensures zero partial chunks were written to database
        assertEquals(0, db.chunksMap.size)
        assertEquals(null, db.packsMap["cbse-10-science"])
    }

    // ==============================================================================
    // 6. Database Transaction Rollback on Exception
    // ==============================================================================
    @Test
    fun test_006_database_transaction_rollback_preserves_valid_local_pack() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        // 1. First successful sync
        client.packs = listOf(createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 1))
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 1, contentHash = "hash-orig"
        )
        client.pagesByPack["cbse-10-science"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-science", listOf(
                SyllabusChunkDto("c-orig", "Original valid content", "CBSE", "10", "Science", embedding = createDummyVector())
            ), null, false, 1, "intfloat/multilingual-e5-small", "1.0", 384)
        )

        val repo = SyllabusSyncRepository(client, db)
        repo.syncCurriculumPack("CBSE", "10", "Science")
        assertEquals(1, db.chunksMap.size)

        // 2. Second sync encounters disk write crash during transaction
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 1, corpusVersion = "2.0", contentHash = "hash-new"
        )
        db.throwInTransaction = true

        val crashResult = repo.syncCurriculumPack("CBSE", "10", "Science")
        assertTrue(crashResult.isFailure)

        // ACID guarantee: original chunk and pack meta remain completely intact
        assertEquals(1, db.chunksMap.size)
        assertEquals("c-orig", db.chunksMap["c-orig"]?.chunkId)
        assertEquals("hash-orig", db.packsMap["cbse-10-science"]?.contentHash)
    }

    // ==============================================================================
    // 7. Update and Deletion Reconciliation
    // ==============================================================================
    @Test
    fun test_007_update_and_deletion_reconciliation() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        // Sync initial pack with chunks c1 and c2
        client.packs = listOf(createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 2))
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 2, contentHash = "hash-step1"
        )
        client.pagesByPack["cbse-10-science"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-science", listOf(
                SyllabusChunkDto("c1", "Chunk 1", "CBSE", "10", "Science", embedding = createDummyVector()),
                SyllabusChunkDto("c2", "Chunk 2 (to be deleted)", "CBSE", "10", "Science", embedding = createDummyVector())
            ), null, false, 2, "intfloat/multilingual-e5-small", "1.0", 384)
        )

        val repo = SyllabusSyncRepository(client, db)
        repo.syncCurriculumPack("CBSE", "10", "Science")
        assertEquals(2, db.chunksMap.size)

        // Remote pack drops c2 and adds c3
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 2, corpusVersion = "1.1", contentHash = "hash-step2"
        )
        client.pagesByPack["cbse-10-science"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-science", listOf(
                SyllabusChunkDto("c1", "Chunk 1 (preserved)", "CBSE", "10", "Science", embedding = createDummyVector()),
                SyllabusChunkDto("c3", "Chunk 3 (new)", "CBSE", "10", "Science", embedding = createDummyVector())
            ), null, false, 2, "intfloat/multilingual-e5-small", "1.0", 384)
        )

        repo.syncCurriculumPack("CBSE", "10", "Science")

        // c2 must be safely deleted; c3 added; c1 updated
        assertEquals(2, db.chunksMap.size)
        assertFalse(db.chunksMap.containsKey("c2"))
        assertTrue(db.chunksMap.containsKey("c1"))
        assertTrue(db.chunksMap.containsKey("c3"))
    }

    // ==============================================================================
    // 8. Model Version and Vector Dimension Compatibility Checks
    // ==============================================================================
    @Test
    fun test_008_rejects_incompatible_model_and_dimensions() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        client.packs = listOf(createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 1, embeddingModel = "openai/text-embedding-3-small"))
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 1, embeddingModel = "openai/text-embedding-3-small", embeddingDimension = 1536, contentHash = "hash-incomp"
        )

        val repo = SyllabusSyncRepository(client, db)
        val res = repo.syncCurriculumPack("CBSE", "10", "Science")

        assertTrue(res.isFailure)
        assertTrue(repo.syncState.value is SyllabusSyncState.IncompatibleModel)
        assertEquals(0, db.chunksMap.size)
    }

    // ==============================================================================
    // 9. Preservation of Valid Local Packs After Sync Failure
    // ==============================================================================
    @Test
    fun test_009_preservation_of_valid_local_packs_after_sync_failure() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        // 1. Successfully sync Math
        client.packs = listOf(
            createPackSummary("cbse-10-mathematics", "CBSE", "10", "Mathematics", chunkCount = 1),
            createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 1)
        )
        client.manifests["cbse-10-mathematics"] = createManifest(
            "cbse-10-mathematics", "CBSE", "10", "Mathematics",
            chunkCount = 1, contentHash = "hash-math"
        )
        client.pagesByPack["cbse-10-mathematics"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-mathematics", listOf(
                SyllabusChunkDto("math-1", "Quadratic equations", "CBSE", "10", "Mathematics", embedding = createDummyVector())
            ), null, false, 1, "intfloat/multilingual-e5-small", "1.0", 384)
        )

        val repo = SyllabusSyncRepository(client, db)
        repo.syncCurriculumPack("CBSE", "10", "Mathematics")
        assertEquals(1, db.chunksMap.size)

        // 2. Science sync fails due to missing manifest
        val failResult = repo.syncCurriculumPack("CBSE", "10", "Science")
        assertTrue(failResult.isFailure)

        // Math pack remains intact and accessible
        assertEquals(1, db.chunksMap.size)
        assertTrue(db.chunksMap.containsKey("math-1"))
        assertEquals("hash-math", db.packsMap["cbse-10-mathematics"]?.contentHash)
    }

    // ==============================================================================
    // 10. Curriculum Isolation After Synchronization
    // ==============================================================================
    @Test
    fun test_010_curriculum_isolation_after_synchronization() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        client.packs = listOf(
            createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 1),
            createPackSummary("cbse-10-mathematics", "CBSE", "10", "Mathematics", chunkCount = 1)
        )
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 1, contentHash = "hash-sci"
        )
        client.manifests["cbse-10-mathematics"] = createManifest(
            "cbse-10-mathematics", "CBSE", "10", "Mathematics",
            chunkCount = 1, contentHash = "hash-math"
        )

        client.pagesByPack["cbse-10-science"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-science", listOf(
                SyllabusChunkDto("sci-1", "Chemical reactions in laboratories", "CBSE", "10", "Science", embedding = createDummyVector())
            ), null, false, 1, "intfloat/multilingual-e5-small", "1.0", 384)
        )
        client.pagesByPack["cbse-10-mathematics"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-mathematics", listOf(
                SyllabusChunkDto("math-1", "Quadratic equations solutions", "CBSE", "10", "Mathematics", embedding = createDummyVector())
            ), null, false, 1, "intfloat/multilingual-e5-small", "1.0", 384)
        )

        val repo = SyllabusSyncRepository(client, db)
        repo.syncCurriculumPack("CBSE", "10", "Science")
        repo.syncCurriculumPack("CBSE", "10", "Mathematics")

        // Search strictly for Science
        val sciResults = db.searchOffline(query = "reactions", board = "CBSE", classGrade = "10", subject = "Science")
        assertEquals(1, sciResults.size)
        assertEquals("sci-1", sciResults[0].chunkId)

        // Math search does not leak into Science
        val mathResults = db.searchOffline(query = "equations", board = "CBSE", classGrade = "10", subject = "Mathematics")
        assertEquals(1, mathResults.size)
        assertEquals("math-1", mathResults[0].chunkId)
    }

    // ==============================================================================
    // 11. Offline Retrieval While Network is Disabled
    // ==============================================================================
    @Test
    fun test_011_offline_retrieval_while_network_disabled() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        client.packs = listOf(createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 1))
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 1, contentHash = "hash-air"
        )
        client.pagesByPack["cbse-10-science"] = mutableListOf(
            SyllabusChunksResponseDto("cbse-10-science", listOf(
                SyllabusChunkDto("sci-air-1", "Photosynthesis in green plants", "CBSE", "10", "Science", chapter = "Life Processes", embedding = createDummyVector())
            ), null, false, 1, "intfloat/multilingual-e5-small", "1.0", 384)
        )

        val repo = SyllabusSyncRepository(client, db)
        repo.syncCurriculumPack("CBSE", "10", "Science")

        // Network is now completely severed (client throws on any call)
        client.throwOnPage = 1

        // Offline query succeeds from local SQLite
        val results = db.searchOffline("photosynthesis plants", board = "CBSE", classGrade = "10", subject = "Science")
        assertFalse(results.isEmpty())
        assertEquals("sci-air-1", results[0].chunkId)
        assertTrue(results[0].content.contains("Photosynthesis"))
    }

    // ==============================================================================
    // 12. Rejection of Incompatible Vector Dimension
    // ==============================================================================
    @Test
    fun test_012_rejects_incompatible_vector_dimensions() = runBlocking {
        val client = ScriptedSyllabusSyncClient()
        val db = InMemorySyllabusDatabase()

        client.packs = listOf(createPackSummary("cbse-10-science", "CBSE", "10", "Science", chunkCount = 1))
        // Remote manifest claims 512 dimensions instead of 384
        client.manifests["cbse-10-science"] = createManifest(
            "cbse-10-science", "CBSE", "10", "Science",
            chunkCount = 1, embeddingDimension = 512, contentHash = "hash-dim-fail"
        )

        val repo = SyllabusSyncRepository(client, db)
        val result = repo.syncCurriculumPack("CBSE", "10", "Science")

        assertTrue(result.isFailure)
        assertTrue(repo.syncState.value is SyllabusSyncState.IncompatibleModel)
        assertEquals(0, db.chunksMap.size)
    }
}
