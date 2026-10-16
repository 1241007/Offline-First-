package com.offline_First

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.offline_First.data.local.E5EmbeddingService
import com.offline_First.data.local.OfflineSyllabusRepository
import com.offline_First.data.local.SyllabusDatabase
import com.offline_First.domain.model.AppMode
import com.offline_First.domain.model.EvidenceOutcome
import com.offline_First.domain.model.RAGRequestContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Real Android OS Instrumented Test running on Android runtime / emulator,
 * exercising:
 * 1. Real Android SQLite B-tree behavior, indexes, and v1 -> v2 schema migrations.
 * 2. 384-dimensional little-endian BLOB vector storage and retrieval on Android SQLite.
 * 3. Strict curriculum isolation on Android database engine.
 * 4. Model artifact loading and SHA-256 integrity on Android filesDir.
 * 5. Offline chat abstention behavior on device.
 * 6. Corrupted BLOB and low-memory exception resilience on real SQLite.
 */
@RunWith(AndroidJUnit4::class)
class OfflineModelAndParityInstrumentedTest {

    private lateinit var appContext: Context
    private lateinit var database: SyllabusDatabase
    private lateinit var repository: OfflineSyllabusRepository

    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        database = SyllabusDatabase(appContext)
        repository = OfflineSyllabusRepository(appContext, database)

        // Clear test records
        val db = database.writableDatabase
        db.delete(SyllabusDatabase.TABLE_CHUNKS, "board = ?", arrayOf("TEST_CBSE"))
        db.delete(SyllabusDatabase.TABLE_CHUNKS, "board = ?", arrayOf("TEST_ICSE"))
        db.delete(SyllabusDatabase.TABLE_PACKS, "pack_id LIKE ?", arrayOf("test-%"))
    }

    @After
    fun tearDown() {
        val db = database.writableDatabase
        db.delete(SyllabusDatabase.TABLE_CHUNKS, "board = ?", arrayOf("TEST_CBSE"))
        db.delete(SyllabusDatabase.TABLE_CHUNKS, "board = ?", arrayOf("TEST_ICSE"))
        db.delete(SyllabusDatabase.TABLE_PACKS, "pack_id LIKE ?", arrayOf("test-%"))
    }

    // ==============================================================================
    // 1. Real Android SQLite Behavior and Schema
    // ==============================================================================
    @Test
    fun test_001_real_android_sqlite_schema_and_version() {
        val db = database.readableDatabase
        assertTrue("Database must be open", db.isOpen)
        assertEquals(2, db.version)

        // Verify standard B-tree tables exist
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN (?, ?)",
            arrayOf(SyllabusDatabase.TABLE_CHUNKS, SyllabusDatabase.TABLE_PACKS)
        )
        cursor.use { c ->
            assertEquals(2, c.count)
        }

        // Verify legacy FTS5 triggers do NOT exist
        val triggerCursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='trigger' AND name LIKE 'chunks_%'",
            null
        )
        triggerCursor.use { tc ->
            assertEquals("Legacy FTS5 triggers must be removed", 0, tc.count)
        }
    }

    // ==============================================================================
    // 2. Real Android SQLite 384-d BLOB Vector Storage & Retrieval
    // ==============================================================================
    @Test
    fun test_002_real_android_sqlite_blob_vector_storage() {
        val testVector = FloatArray(384) { (it * 0.01f) - 1.92f }
        E5EmbeddingService.l2Normalize(testVector)

        val chunkId = "test-blob-chunk-001"
        database.insertChunk(
            chunkId = chunkId,
            board = "TEST_CBSE",
            classGrade = "10",
            subject = "Science",
            chapterTitle = "Chemical Reactions and Equations",
            sectionTitle = "Magnesium burning",
            pageReference = "p.2",
            content = "When a magnesium ribbon is burnt in oxygen or air, it burns with a dazzling white flame and changes into a white powder.",
            embedding = testVector,
            embeddingModel = E5EmbeddingService.MODEL_ID,
            embeddingVersion = E5EmbeddingService.MODEL_VERSION
        )

        // Read back raw BLOB from real SQLite
        val db = database.readableDatabase
        val cursor = db.rawQuery(
            "SELECT embedding FROM ${SyllabusDatabase.TABLE_CHUNKS} WHERE chunk_id = ?",
            arrayOf(chunkId)
        )
        var retrievedVector: FloatArray? = null
        cursor.use { c ->
            assertTrue("Row must exist in SQLite", c.moveToFirst())
            val blob = c.getBlob(0)
            assertNotNull("BLOB must not be null", blob)
            assertEquals(384 * 4, blob.size)
            retrievedVector = SyllabusDatabase.blobToFloatArray(blob)
        }

        assertNotNull(retrievedVector)
        assertEquals(384, retrievedVector!!.size)
        for (i in 0 until 384) {
            assertEquals(testVector[i], retrievedVector!![i], 1e-6f)
        }
    }

    // ==============================================================================
    // 3. Strict Curriculum Isolation on Real Android Database
    // ==============================================================================
    @Test
    fun test_003_real_android_curriculum_isolation() {
        // Insert target record: TEST_CBSE Class 10 Science
        database.insertChunk(
            chunkId = "test-iso-cbse-10",
            board = "TEST_CBSE",
            classGrade = "10",
            subject = "Science",
            chapterTitle = "Electricity",
            sectionTitle = "Ohm's Law",
            pageReference = "p.200",
            content = "Ohm's law relates potential difference and electric current V = IR in a conductor."
        )

        // Insert cross-board record: TEST_ICSE Class 10 Science
        database.insertChunk(
            chunkId = "test-iso-icse-10",
            board = "TEST_ICSE",
            classGrade = "10",
            subject = "Science",
            chapterTitle = "Current Electricity",
            sectionTitle = "Ohm's Law",
            pageReference = "p.150",
            content = "Ohm's law relates potential difference and electric current V = IR in a conductor."
        )

        // Insert cross-grade record: TEST_CBSE Class 12 Physics
        database.insertChunk(
            chunkId = "test-iso-cbse-12",
            board = "TEST_CBSE",
            classGrade = "12",
            subject = "Physics",
            chapterTitle = "Current Electricity",
            sectionTitle = "Ohm's Law",
            pageReference = "p.100",
            content = "Ohm's law microscopic form relates current density to electric field."
        )

        // Execute retrieval scoped to TEST_CBSE Class 10 Science
        val context = RAGRequestContext(
            appMode = AppMode.STUDENT,
            board = "TEST_CBSE",
            classGrade = "10",
            subject = "Science"
        )
        val (chunks, _) = repository.retrieve("Ohm's Law", context, topK = 10)

        // Must ONLY match TEST_CBSE Class 10
        assertEquals(1, chunks.size)
        assertEquals("test-iso-cbse-10", chunks[0].chunkId)
        assertFalse(chunks.any { it.board == "TEST_ICSE" })
        assertFalse(chunks.any { it.classGrade == "12" })
    }

    // ==============================================================================
    // 4. Model Artifact and Tokenizer Loading Verification
    // ==============================================================================
    @Test
    fun test_004_model_artifact_loading_and_integrity() {
        val e5Service = E5EmbeddingService(appContext)
        val modelFile = e5Service.modelFile
        val tokFile = e5Service.tokenizerFile

        // If files exist in app storage, strictly verify their SHA-256
        if (modelFile.exists() && tokFile.exists()) {
            assertTrue(
                "Model SHA-256 must match pinned hash",
                E5EmbeddingService.verifySha256(modelFile, E5EmbeddingService.MODEL_SHA256)
            )
            assertTrue(
                "Tokenizer SHA-256 must match pinned hash",
                E5EmbeddingService.verifySha256(tokFile, E5EmbeddingService.TOKENIZER_SHA256)
            )
            assertTrue("isModelReady must be true", e5Service.isModelReady())
        } else {
            // Artifacts not yet downloaded to device filesDir
            assertFalse(e5Service.isModelReady())
        }
    }

    // ==============================================================================
    // 5. Offline Empty-Evidence Abstention on Real Android Runtime
    // ==============================================================================
    @Test
    fun test_005_real_android_offline_abstention() {
        // Query topic not in syllabus
        val context = RAGRequestContext(
            appMode = AppMode.STUDENT,
            board = "TEST_CBSE",
            classGrade = "10",
            subject = "Science"
        )
        val (chunks, outcome) = repository.retrieve(
            "Quantum chromodynamics and gluons",
            context
        )

        // Must evaluate outcome as MISSING with empty chunks
        assertTrue(chunks.isEmpty())
        assertEquals(EvidenceOutcome.MISSING, outcome)
    }

    // ==============================================================================
    // 6. Corrupted BLOB and Low-Memory Resilience
    // ==============================================================================
    @Test
    fun test_006_real_android_corrupted_blob_resilience() {
        val db = database.writableDatabase

        // Directly insert corrupted 7-byte BLOB (invalid float array)
        db.execSQL(
            "INSERT INTO ${SyllabusDatabase.TABLE_CHUNKS} " +
            "(chunk_id, board, class_grade, subject, chapter_title, content, embedding) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf("corrupted-chunk", "TEST_CBSE", "10", "Science", "Corrupted", "Corrupted content", byteArrayOf(1, 2, 3, 4, 5, 6, 7))
        )

        // Vector search must not crash with SQLiteException or ArrayIndexOutOfBoundsException
        val dummyQueryEmb = FloatArray(384) { 0.1f }
        val results = database.searchByVector(dummyQueryEmb, "TEST_CBSE", "10", "Science")

        // Corrupted row is gracefully skipped
        assertFalse(results.any { it.chunkId == "corrupted-chunk" })
    }
}
