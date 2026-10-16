package com.offline_First.data.local

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.sqrt

class OrtTest {

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dot = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }
        val denom = sqrt(norm1) * sqrt(norm2)
        return if (denom > 1e-12f) dot / denom else 0.0f
    }

    @Test
    fun testCrossRuntimeParityAgainstPythonReference() = runBlocking {
        val modelDir = File("../backend/models/e5_small_quantized")
        val fixturesFile = File("../backend/evals/parity/parity_fixtures.json")
        assertTrue("Model directory must exist", modelDir.exists())
        assertTrue("Parity fixtures file must exist", fixturesFile.exists())

        val service = E5EmbeddingService(modelDirParam = modelDir)
        assertTrue("Service must be ready with verified files", service.isModelReady())

        val jsonStr = fixturesFile.readText(Charsets.UTF_8)
        val root = Json.parseToJsonElement(jsonStr).jsonObject
        val fixtures = root["fixtures"]!!.jsonArray

        val tolerances = root["metadata"]!!.jsonObject["tolerances"]!!.jsonObject
        val minCosine = tolerances["cosine_min"]!!.jsonPrimitive.float
        val maxAbsDiff = tolerances["max_absolute_difference"]!!.jsonPrimitive.float

        val exportMap = mutableMapOf<String, List<Double>>()

        println("=== CROSS-RUNTIME ONNX INFERENCE PARITY REPORT ===")
        for (elem in fixtures) {
            val item = elem.jsonObject
            val id = item["id"]!!.jsonPrimitive.content
            val queryObj = item["query"]!!.jsonObject
            val rawText = queryObj["raw_text"]!!.jsonPrimitive.content

            val refJsonArr = queryObj["embedding"]!!.jsonArray
            val refVec = FloatArray(refJsonArr.size) { refJsonArr[it].jsonPrimitive.float }

            val t0 = System.currentTimeMillis()
            val androidVec = service.embedQuery(rawText)
            val elapsed = System.currentTimeMillis() - t0

            assertEquals(384, androidVec.size)

            val cosSim = cosineSimilarity(androidVec, refVec)
            var maxDiff = 0.0f
            for (j in androidVec.indices) {
                val diff = Math.abs(androidVec[j] - refVec[j])
                if (diff > maxDiff) maxDiff = diff
            }

            println("[$id] Latency: ${elapsed}ms | Cosine: ${"%.6f".format(cosSim)} | MaxAbsDiff: ${"%.6f".format(maxDiff)}")

            assertTrue("Cosine similarity below tolerance on $id: $cosSim < $minCosine", cosSim >= minCosine)
            assertTrue("Max absolute difference exceeded on $id: $maxDiff > $maxAbsDiff", maxDiff <= maxAbsDiff)

            exportMap[id] = androidVec.map { it.toDouble() }
        }

        // Export android embeddings for Python test_008_android_cross_runtime_status
        val exportFile = File("../backend/evals/parity/android_embeddings.json")
        val sb = StringBuilder("{\n")
        val entries = exportMap.entries.toList()
        for (i in entries.indices) {
            val (k, v) = entries[i]
            sb.append("  \"$k\": [${v.joinToString(",")}]")
            if (i < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("}\n")
        exportFile.writeText(sb.toString(), Charsets.UTF_8)
        println("Exported android_embeddings.json with ${exportMap.size} embeddings to ${exportFile.absolutePath}")

        service.close()
    }
}
