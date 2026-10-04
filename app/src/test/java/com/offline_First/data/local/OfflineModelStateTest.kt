package com.offline_First.data.local

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineModelStateTest {
    @Test
    fun deviceRamThresholdSelectsTheModelWithoutUserInput() {
        assertFalse(useLargeOfflineModel(8L * 1024L * 1024L * 1024L - 1L))
        assertTrue(useLargeOfflineModel(8L * 1024L * 1024L * 1024L))
    }

    @Test
    fun onlyCompletePlausibleGgufHeadersCountAsAValidModel() {
        val file = File.createTempFile("edunova-model", ".gguf")
        try {
            writeHeader(file, magic = "GGUF", version = 3, tensors = 1L, metadata = 1L)
            assertTrue(isValidGgufFile(file, minimumValidBytes = 24L))
            assertFalse(isValidGgufFile(file, minimumValidBytes = 25L))

            writeHeader(file, magic = "NOPE", version = 3, tensors = 1L, metadata = 1L)
            assertFalse(isValidGgufFile(file, minimumValidBytes = 24L))

            writeHeader(file, magic = "GGUF", version = 1, tensors = 1L, metadata = 1L)
            assertFalse(isValidGgufFile(file, minimumValidBytes = 24L))

            writeHeader(file, magic = "GGUF", version = 3, tensors = 0L, metadata = 1L)
            assertFalse(isValidGgufFile(file, minimumValidBytes = 24L))
        } finally {
            file.delete()
        }
    }

    private fun writeHeader(file: File, magic: String, version: Int, tensors: Long, metadata: Long) {
        val bytes = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .put(magic.toByteArray(Charsets.US_ASCII))
            .putInt(version)
            .putLong(tensors)
            .putLong(metadata)
            .array()
        FileOutputStream(file).use { it.write(bytes) }
    }
}
