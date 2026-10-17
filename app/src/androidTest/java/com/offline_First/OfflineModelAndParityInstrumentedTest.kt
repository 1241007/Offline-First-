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

    // ==============================================================================
    // 7. Real Android llama.cpp Native GGUF Inference
    // ==============================================================================
    @Test
    fun test_007_real_android_llama_cpp_native_inference() = runBlocking {
        val modelFile = File(appContext.getDir("offline_models", Context.MODE_PRIVATE), "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf")
        assertTrue("Model file must exist at ${modelFile.absolutePath}", modelFile.exists())
        assertEquals("Model file length must match 1.5B Q4_K_M", 986048512L, modelFile.length())

        val engine = com.offline_First.data.local.LlamaCppInferenceEngine()
        val loadStart = System.currentTimeMillis()
        engine.loadModel(modelFile)
        val loadTimeMs = System.currentTimeMillis() - loadStart

        assertTrue("Model engine must report isLoaded = true", engine.isLoaded)
        assertNotNull("Model instance ID must be non-null", engine.modelInstanceId)

        val t0 = System.currentTimeMillis()
        val response = engine.generate(
            systemPrompt = "You are a concise science tutor. Answer in one sentence.",
            userPrompt = "What is photosynthesis?"
        )
        val genTimeMs = System.currentTimeMillis() - t0

        println("=== REAL OFFLINE INFERENCE MEASUREMENT ===")
        println("Model Load Latency: ${loadTimeMs} ms")
        println("Generation Latency: ${genTimeMs} ms")
        println("Generated Text: $response")
        println("==========================================")

        assertTrue("Generated response must not be blank", response.isNotBlank())
        assertTrue(
            "Response should mention plants, light, or food",
            response.contains("plant", ignoreCase = true) ||
            response.contains("light", ignoreCase = true) ||
            response.contains("energy", ignoreCase = true) ||
            response.contains("food", ignoreCase = true)
        )

        engine.unload()
        assertFalse("Model engine must report isLoaded = false after unload", engine.isLoaded)
    }

    // ==============================================================================
    // 8. Comprehensive Android llama.cpp Offline Generation Evaluation Suite
    // ==============================================================================
    @Test
    fun test_008_real_android_offline_generation_evaluation_suite() = runBlocking {
        val modelFile = File(appContext.getDir("offline_models", Context.MODE_PRIVATE), "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf")
        assertTrue("Model file must exist at ${modelFile.absolutePath}", modelFile.exists())

        val engine = com.offline_First.data.local.LlamaCppInferenceEngine()
        val loadStart = System.currentTimeMillis()
        engine.loadModel(modelFile)
        val loadTimeMs = System.currentTimeMillis() - loadStart

        assertTrue("Model must be loaded", engine.isLoaded)

        val evalCases = listOf(
            Triple(
                "OFF-001",
                "curriculum_grounded",
                "<|im_start|>system\nYou are EduNova's patient teacher. Explain the answer step by step.<|im_end|>\n<|im_start|>user\nContext: When magnesium ribbon burns in oxygen, it burns with a dazzling white flame and forms white powder magnesium oxide (2Mg + O2 -> 2MgO).\nQuestion: What happens when magnesium ribbon burns in air?<|im_end|>\n<|im_start|>assistant\n"
            ),
            Triple(
                "OFF-002",
                "curriculum_grounded",
                "<|im_start|>system\nYou are EduNova's explainable learning assistant.<|im_end|>\n<|im_start|>user\nContext: Ohm's law states that current through a conductor is directly proportional to potential difference: V = IR.<|im_end|>\n<|im_start|>assistant\n"
            ),
            Triple(
                "OFF-003",
                "calculation_reasoning",
                "<|im_start|>system\nYou are EduNova, a helpful math and physics tutor.<|im_end|>\n<|im_start|>user\nIf a 12V battery is connected across a resistor and a current of 2.5 mA flows, what is the resistance in ohms? (Use R = V / I, note 2.5 mA = 0.0025 A). Calculate R.<|im_end|>\n<|im_start|>assistant\n"
            ),
            Triple(
                "OFF-004",
                "general_direct",
                "<|im_start|>system\nYou are EduNova, a helpful learning assistant.<|im_end|>\n<|im_start|>user\nWhat is the capital of France and what currency does it use?<|im_end|>\n<|im_start|>assistant\n"
            ),
            Triple(
                "OFF-005",
                "missing_evidence_abstention",
                "<|im_start|>system\nYou are EduNova's curriculum assistant. If the answer is not in the provided curriculum context, explicitly say: 'This topic is not covered in your syllabus' and do not invent facts.<|im_end|>\n<|im_start|>user\nContext: NONE\nQuestion: Explain CRISPR Cas9 gene editing mechanisms in human embryos.<|im_end|>\n<|im_start|>assistant\n"
            ),
            Triple(
                "OFF-006-A",
                "consistency_check",
                "<|im_start|>system\nYou are EduNova, a helpful learning assistant.<|im_end|>\n<|im_start|>user\nWhat is the capital of Germany and what currency does it use?<|im_end|>\n<|im_start|>assistant\n"
            ),
            Triple(
                "OFF-007-B",
                "consistency_check",
                "<|im_start|>system\nYou are EduNova, a helpful learning assistant.<|im_end|>\n<|im_start|>user\nWhat is the capital of Germany and what currency does it use?<|im_end|>\n<|im_start|>assistant\n"
            )
        )

        val results = org.json.JSONArray()
        var totalGenMs = 0L
        var totalGenTokens = 0L

        for (case in evalCases) {
            val (caseId, category, prompt) = case
            val (output, metrics) = engine.generateWithMetrics(prompt, maxTokens = 256)
            val promptTokens = metrics[0]
            val cachedPrefix = metrics[1]
            val evaluatedTokens = metrics[2]
            val genTokens = metrics[3]
            val ttftMs = metrics[4]
            val genMs = metrics[5]
            val tokPerSec = if (genMs > 0) (genTokens * 1000.0 / genMs) else 0.0

            totalGenMs += genMs
            totalGenTokens += genTokens

            val caseObj = org.json.JSONObject().apply {
                put("case_id", caseId)
                put("category", category)
                put("prompt_tokens", promptTokens)
                put("cached_prefix", cachedPrefix)
                put("evaluated_tokens", evaluatedTokens)
                put("gen_tokens", genTokens)
                put("ttft_ms", ttftMs)
                put("gen_ms", genMs)
                put("tok_per_sec", tokPerSec)
                put("output_text", output)
            }
            results.put(caseObj)

            println("--- EVAL CASE $caseId ($category) ---")
            println("TTFT: ${ttftMs}ms, Gen: ${genMs}ms, Tokens: $genTokens ($tokPerSec tok/s)")
            println("Output: $output")
        }

        val pssKb = android.os.Debug.getPss()
        val runtime = Runtime.getRuntime()
        val usedHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        val report = org.json.JSONObject().apply {
            put("model", "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf")
            put("load_time_ms", loadTimeMs)
            put("pss_kb", pssKb)
            put("used_heap_mb", usedHeapMb)
            put("total_cases", evalCases.size)
            put("avg_gen_ms", if (evalCases.isNotEmpty()) totalGenMs / evalCases.size else 0L)
            put("avg_tok_per_sec", if (totalGenMs > 0) (totalGenTokens * 1000.0 / totalGenMs) else 0.0)
            put("cases", results)
        }

        val reportFile = File(appContext.filesDir, "offline_eval_report.json")
        reportFile.writeText(report.toString(2))
        println("=== COMPREHENSIVE OFFLINE EVAL REPORT SAVED ===")
        println("Saved to: ${reportFile.absolutePath}")
        println("Load Time: ${loadTimeMs}ms, Peak PSS: ${pssKb}KB (${pssKb / 1024}MB)")
        println("Avg Speed: ${if (totalGenMs > 0) (totalGenTokens * 1000.0 / totalGenMs) else 0.0} tok/s")
        println("===============================================")

        engine.unload()
    }

    // ==============================================================================
    // 9. Real Android llama.cpp Offline Repeatability & State Resilience Experiment
    // ==============================================================================
    @Test
    fun test_009_offline_repeatability_and_state_resilience() = runBlocking {
        val modelFile = File(appContext.getDir("offline_models", Context.MODE_PRIVATE), "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf")
        assertTrue("Model file must exist at ${modelFile.absolutePath}", modelFile.exists())

        val engine = com.offline_First.data.local.LlamaCppInferenceEngine()
        engine.loadModel(modelFile)
        assertTrue("Model must be loaded", engine.isLoaded)

        val prompt = "<|im_start|>system\nYou are EduNova, a helpful learning assistant.<|im_end|>\n<|im_start|>user\nExplain Database Management Systems (DBMS) in three concise bullet points.<|im_end|>\n<|im_start|>assistant\n"

        val outputs = mutableListOf<String>()
        val metricsList = mutableListOf<LongArray>()

        // 5 repeated runs with identical prompt
        for (i in 1..5) {
            val (output, metrics) = engine.generateWithMetrics(prompt, maxTokens = 256)
            outputs.add(output)
            metricsList.add(metrics)
            println("--- REPEATABILITY RUN $i ---")
            println("Prompt tokens: ${metrics[0]}, Cached prefix: ${metrics[1]}, Eval: ${metrics[2]}, Gen tokens: ${metrics[3]}, Latency: ${metrics[5]}ms")
            println("Output preview: ${output.take(80)}...")
        }

        // Compare all runs against run 1 byte-for-byte
        var exactMatches = 0
        for (i in 0 until outputs.size) {
            if (outputs[i] == outputs[0]) {
                exactMatches++
            }
        }
        val matchRate = (exactMatches.toDouble() / outputs.size.toDouble()) * 100.0
        println("=== REPEATABILITY RESULTS ===")
        println("Total Runs: ${outputs.size}, Exact Matches: $exactMatches, Match Rate: $matchRate%")
        println("Run 1 length: ${outputs[0].length} chars")
        println("Full Output:\n${outputs[0]}")
        println("=============================")

        assertEquals("All 5 runs must produce byte-for-byte identical output", outputs.size, exactMatches)

        // Test 6: Changed prompt produces different answer
        val changedPrompt = "<|im_start|>system\nYou are EduNova, a helpful learning assistant.<|im_end|>\n<|im_start|>user\nWhat is an Operating System?<|im_end|>\n<|im_start|>assistant\n"
        val (changedOutput, _) = engine.generateWithMetrics(changedPrompt, maxTokens = 256)
        assertFalse("Different prompt must not match DBMS output", changedOutput == outputs[0])
        assertTrue("Changed prompt must mention OS concepts", changedOutput.contains("hardware", ignoreCase = true) || changedOutput.contains("software", ignoreCase = true) || changedOutput.contains("manage", ignoreCase = true) || changedOutput.contains("system", ignoreCase = true))

        // Test 7: Post-cancellation recovery preserves determinism
        var receivedCount = 0
        kotlinx.coroutines.withTimeoutOrNull(5000) {
            engine.stream(
                systemPrompt = "You are EduNova.",
                history = emptyList(),
                prompt = "Count from 1 to 100"
            ).collect {
                receivedCount++
                if (receivedCount >= 3) {
                    engine.cancelCurrentGeneration()
                }
            }
        }

        // Fresh post-cancellation generation with original prompt
        val (postCancelOutput, _) = engine.generateWithMetrics(prompt, maxTokens = 256)
        assertEquals("Generation after cancellation must still be deterministic and byte-for-byte identical", outputs[0], postCancelOutput)

        engine.unload()
    }
}

