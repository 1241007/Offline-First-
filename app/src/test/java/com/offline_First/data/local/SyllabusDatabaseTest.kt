package com.offline_First.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllabusDatabaseTest {

    @Test
    fun testFloatArrayToBlobAndBack() {
        val original = FloatArray(384) { idx -> (idx * 0.01f) - 1.92f }
        val blob = SyllabusDatabase.floatArrayToBlob(original)

        assertNotNull(blob)
        assertEquals(384 * 4, blob!!.size)

        val deserialized = SyllabusDatabase.blobToFloatArray(blob)
        assertNotNull(deserialized)
        assertEquals(original.size, deserialized!!.size)

        for (i in original.indices) {
            assertEquals(original[i], deserialized[i], 1e-6f)
        }
    }

    @Test
    fun testNullAndEmptyBlobs() {
        assertNull(SyllabusDatabase.floatArrayToBlob(null))
        assertNull(SyllabusDatabase.floatArrayToBlob(FloatArray(0)))
        assertNull(SyllabusDatabase.blobToFloatArray(null))
        assertNull(SyllabusDatabase.blobToFloatArray(ByteArray(0)))
        // Invalid size not multiple of 4
        assertNull(SyllabusDatabase.blobToFloatArray(ByteArray(10)))
    }

    @Test
    fun testCosineSimilarityIdenticalVectors() {
        val v1 = FloatArray(384) { 0.5f }
        val v2 = FloatArray(384) { 0.5f }
        val sim = SyllabusDatabase.cosineSimilarity(v1, v2)
        assertEquals(1.0f, sim, 1e-4f)
    }

    @Test
    fun testCosineSimilarityOrthogonalVectors() {
        val v1 = FloatArray(4) { 0.0f }.also { it[0] = 1.0f }
        val v2 = FloatArray(4) { 0.0f }.also { it[1] = 1.0f }
        val sim = SyllabusDatabase.cosineSimilarity(v1, v2)
        assertEquals(0.0f, sim, 1e-4f)
    }

    @Test
    fun testCosineSimilarityOppositeVectors() {
        val v1 = floatArrayOf(1.0f, 2.0f, 3.0f)
        val v2 = floatArrayOf(-1.0f, -2.0f, -3.0f)
        val sim = SyllabusDatabase.cosineSimilarity(v1, v2)
        assertEquals(-1.0f, sim, 1e-4f)
    }

    @Test
    fun testCosineSimilarityNormalizedBounds() {
        val v1 = FloatArray(384) { (Math.random() * 2 - 1).toFloat() }
        val v2 = FloatArray(384) { (Math.random() * 2 - 1).toFloat() }
        val sim = SyllabusDatabase.cosineSimilarity(v1, v2)
        assertTrue(sim in -1.0f..1.0f)
    }
}
