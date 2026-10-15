package com.offline_First.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class E5EmbeddingServiceTest {

    @Test
    fun testQuantizedModelArtifactParityWithBackend() {
        // Must specifically point to model_quantized.onnx, NOT model.onnx (fp32)
        assertTrue(E5EmbeddingService.MODEL_URL.endsWith("model_quantized.onnx"))
        assertTrue(E5EmbeddingService.MODEL_URL.contains("model_quantized"))

        // Exact match with backend local_e5_service.py checksum
        assertEquals(
            "f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193",
            E5EmbeddingService.MODEL_SHA256
        )

        // Exact match with backend tokenizer checksum
        assertTrue(E5EmbeddingService.TOKENIZER_URL.endsWith("tokenizer.json"))
        assertEquals(
            "0b44a9d7b51c3c62626640cda0e2c2f70fdacdc25bbbd68038369d14ebdf4c39",
            E5EmbeddingService.TOKENIZER_SHA256
        )

        // Exact match with backend 384 dimensions
        assertEquals(384, E5EmbeddingService.EMBEDDING_DIMENSION)
    }

    @Test
    fun testL2Normalization() {
        val original = floatArrayOf(3.0f, 4.0f, 0.0f)
        val normalized = E5EmbeddingService.l2Normalize(original)
        var sumSquares = 0.0f
        for (v in normalized) {
            sumSquares += v * v
        }
        assertEquals(1.0f, sumSquares, 1e-4f)
        assertEquals(0.6f, normalized[0], 1e-4f)
        assertEquals(0.8f, normalized[1], 1e-4f)
    }
}
