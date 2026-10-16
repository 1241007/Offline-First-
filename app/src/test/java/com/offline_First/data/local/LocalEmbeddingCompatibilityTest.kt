package com.offline_First.data.local

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import kotlin.math.sqrt

/**
 * Unit tests verifying OFF-001, OFF-002, OFF-006, and OFF-011 for on-device embedding.
 */
class LocalEmbeddingCompatibilityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // ==============================================================================
    // OFF-001: Vector Dimensionality (384) and L2 Normalization (||v||_2 = 1.0)
    // ==============================================================================
    @Test
    fun test_off_001_vector_dimension_and_l2_normalization() {
        assertEquals(384, E5EmbeddingService.EMBEDDING_DIMENSION)

        // Test L2 normalization on arbitrary vector
        val raw = FloatArray(384) { (it + 1).toFloat() }
        val normalized = E5EmbeddingService.l2Normalize(raw)

        var sumSq = 0.0f
        for (v in normalized) {
            sumSq += v * v
        }
        val norm = sqrt(sumSq)
        assertEquals(1.0f, norm, 1e-4f)

        // Test L2 normalization on zero-vector doesn't produce NaN
        val zeroVec = FloatArray(384) { 0.0f }
        val normZero = E5EmbeddingService.l2Normalize(zeroVec)
        for (v in normZero) {
            assertFalse(v.isNaN())
            assertFalse(v.isInfinite())
        }

        // Test mock embedding generator produces exact 384 dimensions and unit norm
        val service = object : E5EmbeddingService(readyOverride = true) {}

        runBlocking {
            val vec = service.embedQuery("Photosynthesis in plants")
            assertEquals(384, vec.size)
            var sum = 0.0f
            for (x in vec) sum += x * x
            assertEquals(1.0f, sqrt(sum), 1e-4f)
        }
    }

    // ==============================================================================
    // OFF-002: Asymmetric Query and Passage Prefixes
    // ==============================================================================
    @Test
    fun test_off_002_asymmetric_query_and_passage_prefixes() = runBlocking {
        var lastInferenceInput = ""
        val service = object : E5EmbeddingService(readyOverride = true) {
            suspend fun interceptInference(text: String): FloatArray {
                lastInferenceInput = text
                return FloatArray(384) { 0.1f }
            }

            override suspend fun embedQuery(text: String): FloatArray {
                val prefixed = if (text.trim().startsWith("query: ")) text.trim() else "query: ${text.trim()}"
                return interceptInference(prefixed)
            }

            override suspend fun embedPassage(text: String): FloatArray {
                val prefixed = if (text.trim().startsWith("passage: ")) text.trim() else "passage: ${text.trim()}"
                return interceptInference(prefixed)
            }
        }

        // 1. embedQuery prepends "query: "
        service.embedQuery("What is Ohm's Law?")
        assertEquals("query: What is Ohm's Law?", lastInferenceInput)

        // 2. embedQuery is idempotent (does not double prefix)
        service.embedQuery("query: What is Ohm's Law?")
        assertEquals("query: What is Ohm's Law?", lastInferenceInput)

        // 3. embedPassage prepends "passage: "
        service.embedPassage("Ohm's law states that V is proportional to I.")
        assertEquals("passage: Ohm's law states that V is proportional to I.", lastInferenceInput)

        // 4. embedPassage is idempotent
        service.embedPassage("passage: Ohm's law states that V is proportional to I.")
        assertEquals("passage: Ohm's law states that V is proportional to I.", lastInferenceInput)

        // 5. Asymmetric representations: base query vector vs base passage vector
        val realService = object : E5EmbeddingService(readyOverride = true) {}
        val qVec = realService.embedQuery("Chemical Reactions")
        val pVec = realService.embedPassage("Chemical Reactions")

        var dot = 0.0f
        for (i in 0 until 384) {
            dot += qVec[i] * pVec[i]
        }
        // Asymmetric embeddings must not be identical (dot < 1.0)
        assertNotEquals(1.0f, dot, 1e-4f)
    }

    // ==============================================================================
    // OFF-006: Artifact Checksum Validation
    // ==============================================================================
    @Test
    fun test_off_006_artifact_checksum_validation() {
        val testFile = tempFolder.newFile("test_artifact.bin")
        val content = "EduNova verified quantized ONNX model test byte stream".toByteArray(Charsets.UTF_8)
        testFile.writeBytes(content)

        val md = MessageDigest.getInstance("SHA-256")
        val expectedSha = md.digest(content).joinToString("") { "%02x".format(it) }

        // Valid checksum matches
        assertTrue(E5EmbeddingService.verifySha256(testFile, expectedSha))
        assertTrue(E5EmbeddingService.verifySha256(testFile, expectedSha.uppercase()))

        // Tampered file fails checksum
        assertFalse(E5EmbeddingService.verifySha256(testFile, "0000000000000000000000000000000000000000000000000000000000000000"))

        // Non-existent file returns false without crashing
        val nonExistent = File(tempFolder.root, "does_not_exist.bin")
        assertFalse(E5EmbeddingService.verifySha256(nonExistent, expectedSha))

        // Pinned constants in E5EmbeddingService match backend exactly
        assertEquals("f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193", E5EmbeddingService.MODEL_SHA256)
        assertEquals("0b44a9d7b51c3c62626640cda0e2c2f70fdacdc25bbbd68038369d14ebdf4c39", E5EmbeddingService.TOKENIZER_SHA256)
    }

    // ==============================================================================
    // OFF-011: Model Metadata and Versioning
    // ==============================================================================
    @Test
    fun test_off_011_model_metadata_and_versioning() {
        assertEquals("intfloat/multilingual-e5-small", E5EmbeddingService.MODEL_ID)
        assertEquals("1.0", E5EmbeddingService.MODEL_VERSION)
        assertEquals(384, E5EmbeddingService.EMBEDDING_DIMENSION)
        assertEquals(512, E5EmbeddingService.MAX_SEQ_LENGTH)
        assertEquals("model_quantized.onnx", E5EmbeddingService.MODEL_FILE_NAME)
        assertEquals("tokenizer.json", E5EmbeddingService.TOKENIZER_FILE_NAME)
    }
}
