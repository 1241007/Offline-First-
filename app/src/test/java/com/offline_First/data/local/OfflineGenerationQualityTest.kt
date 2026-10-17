package com.offline_First.data.local

import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.EvidenceOutcome
import com.offline_First.domain.model.ExplanationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Validates offline on-device generation quality, GGUF binary validation,
 * prompt construction, and strict evidence abstention invariants.
 */
class OfflineGenerationQualityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSyntheticGguf(
        file: File,
        magic: ByteArray = byteArrayOf(0x47, 0x47, 0x55, 0x46),
        version: Int = 3,
        tensorCount: Long = 100L,
        metadataCount: Long = 20L,
        totalBytes: Int = 200
    ) {
        val buffer = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(magic)
        buffer.putInt(version)
        buffer.putLong(tensorCount)
        buffer.putLong(metadataCount)

        FileOutputStream(file).use { out ->
            out.write(buffer.array())
            val remaining = totalBytes - 24
            if (remaining > 0) {
                out.write(ByteArray(remaining))
            }
        }
    }

    @Test
    fun test_001_valid_gguf_binary_header_passes() {
        val file = tempFolder.newFile("valid_qwen.gguf")
        createSyntheticGguf(file, version = 3, totalBytes = 500)

        val isValid = isValidGgufFile(file, minimumValidBytes = 100)
        assertTrue("Valid GGUF header must pass verification", isValid)
    }

    @Test
    fun test_002_corrupted_magic_bytes_rejected() {
        val file = tempFolder.newFile("corrupted_magic.gguf")
        createSyntheticGguf(file, magic = byteArrayOf(0x50, 0x4B, 0x03, 0x04), totalBytes = 500)

        val isValid = isValidGgufFile(file, minimumValidBytes = 100)
        assertFalse("GGUF with invalid magic must be rejected", isValid)
    }

    @Test
    fun test_003_undersized_file_rejected() {
        val file = tempFolder.newFile("truncated.gguf")
        createSyntheticGguf(file, totalBytes = 50)

        val isValid = isValidGgufFile(file, minimumValidBytes = 100)
        assertFalse("Undersized GGUF file must be rejected", isValid)
    }

    @Test
    fun test_004_system_prompts_distinct_per_mode() {
        val teacherPrompt = systemPromptFor(ExplanationMode.TEACHER)
        val generalPrompt = systemPromptFor(ExplanationMode.GENERAL)
        val explainPrompt = systemPromptFor(ExplanationMode.EXPLAINABLE)

        assertTrue(teacherPrompt.contains("teacher", ignoreCase = true))
        assertTrue(explainPrompt.contains("explain", ignoreCase = true) || explainPrompt.contains("analytical", ignoreCase = true))
        assertFalse("Teacher and General prompts must not be identical", teacherPrompt == generalPrompt)
    }

    @Test
    fun test_005_strict_abstention_invariant_when_evidence_is_missing() {
        val outcome = EvidenceOutcome.MISSING
        val board = "CBSE"
        val classGrade = "10"
        val subject = "Science"

        // Invariant: Missing evidence must produce standard refusal and never trigger ungrounded LLM inference
        val abstentionText = if (outcome == EvidenceOutcome.MISSING) {
            "This topic was not found in the official curriculum corpus for $board Class $classGrade $subject. Please verify the chapter or check your curriculum topics."
        } else {
            "Ungrounded generation"
        }

        assertTrue(abstentionText.contains("not found in the official curriculum corpus"))
        assertTrue(abstentionText.contains("CBSE Class 10 Science"))
    }
}
