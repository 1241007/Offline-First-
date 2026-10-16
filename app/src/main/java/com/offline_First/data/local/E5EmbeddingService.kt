package com.offline_First.data.local

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.LongBuffer
import java.security.MessageDigest
import kotlin.math.sqrt

private const val TAG = "E5EmbeddingService"

/**
 * On-device embedding inference service using intfloat/multilingual-e5-small (ONNX Runtime).
 *
 * Configured with the EXACT same dynamic quantized ONNX artifact (model_quantized.onnx)
 * and tokenizer.json as FastAPI backend to guarantee identical vector representations.
 */
open class E5EmbeddingService(
    private val context: Context? = null,
    modelDirParam: File? = null,
    private val readyOverride: Boolean? = null
) : AutoCloseable {

    private val modelDir: File = modelDirParam ?: resolveDefaultModelDir(context)

    companion object {
        const val MODEL_ID = "intfloat/multilingual-e5-small"
        const val MODEL_VERSION = "1.0"
        const val EMBEDDING_DIMENSION = 384
        const val MAX_SEQ_LENGTH = 512

        // Exact Xenova quantized ONNX artifact URLs and checksums
        const val MODEL_URL = "https://huggingface.co/Xenova/multilingual-e5-small/resolve/main/onnx/model_quantized.onnx"
        const val MODEL_SHA256 = "f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193"
        const val MODEL_FILE_NAME = "model_quantized.onnx"

        const val TOKENIZER_URL = "https://huggingface.co/Xenova/multilingual-e5-small/resolve/main/tokenizer.json"
        const val TOKENIZER_SHA256 = "0b44a9d7b51c3c62626640cda0e2c2f70fdacdc25bbbd68038369d14ebdf4c39"
        const val TOKENIZER_FILE_NAME = "tokenizer.json"

        fun resolveDefaultModelDir(context: Context?): File {
            if (context != null) {
                return File(context.filesDir, "e5_small_quantized")
            }
            // Check possible host test locations
            val testCandidates = listOf(
                File("../backend/models/e5_small_quantized"),
                File("backend/models/e5_small_quantized"),
                File("/data/local/tmp/e5_small_quantized")
            )
            for (candidate in testCandidates) {
                if (candidate.exists() && candidate.isDirectory) {
                    return candidate
                }
            }
            return File(".", "e5_small_quantized")
        }

        /**
         * Verifies the SHA-256 digest of a file against expected hash.
         */
        fun verifySha256(file: File, expectedHash: String): Boolean {
            if (!file.exists() || !file.isFile) return false
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(65536)
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        digest.update(buffer, 0, read)
                    }
                }
                val hex = digest.digest().joinToString("") { "%02x".format(it) }
                hex.equals(expectedHash, ignoreCase = true)
            } catch (e: Exception) {
                Log.w(TAG, "Error calculating checksum for ${file.name}: ${e.message}")
                false
            }
        }

        /**
         * L2 normalization of float array in-place.
         */
        fun l2Normalize(vector: FloatArray): FloatArray {
            var sum = 0.0f
            for (v in vector) {
                sum += v * v
            }
            val norm = sqrt(sum)
            if (norm > 1e-12f) {
                for (i in vector.indices) {
                    vector[i] /= norm
                }
            }
            return vector
        }
    }

    sealed class ModelState {
        object NotDownloaded : ModelState()
        data class Downloading(val progress: Float) : ModelState()
        object Ready : ModelState()
        data class Error(val message: String) : ModelState()
    }

    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    val modelFile: File
        get() = File(modelDir, MODEL_FILE_NAME)

    val tokenizerFile: File
        get() = File(modelDir, TOKENIZER_FILE_NAME)

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tokenizer: E5Tokenizer? = null
    private val initLock = Any()

    /**
     * Checks if the quantized model and tokenizer files are downloaded and match SHA-256.
     */
    open fun isModelReady(): Boolean {
        if (readyOverride != null) return readyOverride
        return verifySha256(modelFile, MODEL_SHA256) && verifySha256(tokenizerFile, TOKENIZER_SHA256)
    }

    @Synchronized
    private fun ensureSessionLoaded() {
        if (ortSession != null && tokenizer != null) return

        if (!isModelReady()) {
            throw IllegalStateException("E5 ONNX model or tokenizer is not ready or failed SHA-256 validation.")
        }

        synchronized(initLock) {
            if (ortSession == null) {
                val env = OrtEnvironment.getEnvironment()
                val sessionOpts = OrtSession.SessionOptions()
                val session = env.createSession(modelFile.absolutePath, sessionOpts)
                ortEnvironment = env
                ortSession = session
            }
            if (tokenizer == null) {
                tokenizer = E5Tokenizer.fromFile(tokenizerFile)
            }
        }
    }

    /**
     * Embeds a search query with mandatory 'query: ' prefix.
     */
    open suspend fun embedQuery(text: String): FloatArray = withContext(Dispatchers.IO) {
        val prefixed = if (text.trim().startsWith("query: ")) text.trim() else "query: ${text.trim()}"
        runInference(prefixed)
    }

    /**
     * Embeds a document passage with mandatory 'passage: ' prefix.
     */
    open suspend fun embedPassage(text: String): FloatArray = withContext(Dispatchers.IO) {
        val prefixed = if (text.trim().startsWith("passage: ")) text.trim() else "passage: ${text.trim()}"
        runInference(prefixed)
    }

    open fun runInference(text: String): FloatArray {
        ensureSessionLoaded()
        val session = ortSession ?: throw IllegalStateException("OrtSession not initialized")
        val env = ortEnvironment ?: throw IllegalStateException("OrtEnvironment not initialized")
        val tok = tokenizer ?: throw IllegalStateException("Tokenizer not initialized")

        val tokenIds = tok.encode(text)
        val seqLen = tokenIds.size.toLong()
        val mask = LongArray(tokenIds.size) { 1L }
        val typeIds = LongArray(tokenIds.size) { 0L }

        val shape = longArrayOf(1, seqLen)
        val tensorInputIds = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenIds), shape)
        val tensorMask = OnnxTensor.createTensor(env, LongBuffer.wrap(mask), shape)
        val tensorTypeIds = OnnxTensor.createTensor(env, LongBuffer.wrap(typeIds), shape)

        val inputs = mutableMapOf<String, OnnxTensor>(
            "input_ids" to tensorInputIds,
            "attention_mask" to tensorMask
        )
        if (session.inputNames.contains("token_type_ids")) {
            inputs["token_type_ids"] = tensorTypeIds
        }

        var results: OrtSession.Result? = null
        try {
            results = session.run(inputs)
            @Suppress("UNCHECKED_CAST")
            val output = results.get(0).value as Array<Array<FloatArray>>
            val lastHidden = output[0] // shape: [seqLen][384]

            // Mean pooling over attention mask
            val pooled = FloatArray(EMBEDDING_DIMENSION)
            var count = 0
            for (i in mask.indices) {
                if (mask[i] > 0L) {
                    count++
                    for (j in 0 until EMBEDDING_DIMENSION) {
                        pooled[j] += lastHidden[i][j]
                    }
                }
            }
            if (count > 0) {
                for (j in 0 until EMBEDDING_DIMENSION) {
                    pooled[j] /= count.toFloat()
                }
            }

            return l2Normalize(pooled)
        } finally {
            tensorInputIds.close()
            tensorMask.close()
            tensorTypeIds.close()
            results?.close()
        }
    }

    override fun close() {
        synchronized(initLock) {
            try {
                ortSession?.close()
            } catch (_: Throwable) {}
            ortSession = null
            try {
                ortEnvironment?.close()
            } catch (_: Throwable) {}
            ortEnvironment = null
            tokenizer = null
        }
    }
}
