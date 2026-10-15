package com.offline_First.data.local

import android.util.Log
import com.offline_First.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "OfflineInferenceEngine"

fun interface NativeTokenCallback {
    fun onToken(piece: String): Boolean
}

interface OfflineInferenceEngine {
    val isLoaded: Boolean
        get() = false
    val modelInstanceId: String?
        get() = null
    val loadCount: Int
        get() = 0
    val unloadCount: Int
        get() = 0

    suspend fun loadModel(modelFile: File)
    suspend fun generate(systemPrompt: String, userPrompt: String): String
    fun stream(systemPrompt: String, history: List<ChatMessage>, prompt: String): Flow<String> = flow {
        val fullPrompt = buildChatMlPrompt(systemPrompt, history, prompt)
        emit(generate(systemPrompt, fullPrompt))
    }
    suspend fun unload()
    fun cancelCurrentGeneration() {}
    fun clearCache() {}
}

class LlamaCppInferenceEngine : OfflineInferenceEngine {
    private val handle = AtomicLong(0L)
    override val isLoaded: Boolean
        get() = handle.get() != 0L

    private var _loadCount: Int = 0
    override val loadCount: Int
        get() = _loadCount

    private var _unloadCount: Int = 0
    override val unloadCount: Int
        get() = _unloadCount

    private var _instanceId: String? = null
    override val modelInstanceId: String?
        get() = _instanceId

    private var loadedModelPath: String? = null
    private val generationLock = ReentrantLock()
    private val noActiveGeneration = generationLock.newCondition()
    private var activeGenerationCount = 0
    private var unloading = false
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "edunova-offline-inference").apply { isDaemon = true }
    }

    override suspend fun loadModel(modelFile: File) = withContext(Dispatchers.IO) {
        require(modelFile.isFile && modelFile.length() > 0L) {
            "The downloaded offline model is missing or empty."
        }
        if (loadedModelPath == modelFile.absolutePath && handle.get() != 0L) {
            return@withContext
        }
        unloadNative()
        try {
            System.loadLibrary("edunova_llama")
        } catch (error: UnsatisfiedLinkError) {
            throw IllegalStateException("The llama.cpp native inference library is unavailable for this device.", error)
        }
        val loadedHandle = LlamaNativeBridge.nativeLoadModel(modelFile.absolutePath)
        check(loadedHandle != 0L) { "llama.cpp could not load the downloaded model." }
        handle.set(loadedHandle)
        loadedModelPath = modelFile.absolutePath
        _loadCount++
        _instanceId = runCatching { LlamaNativeBridge.nativeGetInstanceId(loadedHandle) }
            .getOrNull() ?: loadedHandle.toString()
        Log.i(TAG, "OFFLINE_MODEL: loaded, instance_id=$_instanceId, loadCount=$_loadCount, unloadCount=$_unloadCount")
    }

    override suspend fun generate(systemPrompt: String, userPrompt: String): String = suspendCancellableCoroutine { continuation ->
        val loadedHandle = handle.get()
        if (loadedHandle == 0L) {
            continuation.resumeWith(Result.failure(IllegalStateException("Offline AI has not loaded a local model.")))
            return@suspendCancellableCoroutine
        }
        val prompt = if (userPrompt.startsWith("<|im_start|>")) {
            userPrompt
        } else {
            buildString {
                append("<|im_start|>system\n")
                append(systemPrompt.trim())
                append("<|im_end|>\n<|im_start|>user\n")
                append(userPrompt.trim())
                append("<|im_end|>\n<|im_start|>assistant\n")
            }
        }
        continuation.invokeOnCancellation { cancelGeneration(loadedHandle) }
        executor.execute {
            val mayRun = generationLock.withLock {
                if (unloading || handle.get() != loadedHandle || !continuation.isActive) {
                    false
                } else {
                    LlamaNativeBridge.nativeResetCancellation(loadedHandle)
                    activeGenerationCount += 1
                    true
                }
            }
            if (!mayRun) return@execute

            val result = runCatching {
                LlamaNativeBridge.nativeGenerate(loadedHandle, prompt, 512)
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: throw IllegalStateException("The local model did not generate a response.")
            }
            generationLock.withLock {
                activeGenerationCount -= 1
                if (activeGenerationCount == 0) noActiveGeneration.signalAll()
            }
            if (continuation.isActive) continuation.resumeWith(result)
        }
    }

    override fun stream(
        systemPrompt: String,
        history: List<ChatMessage>,
        prompt: String
    ): Flow<String> = callbackFlow {
        val loadedHandle = handle.get()
        if (loadedHandle == 0L) {
            close(IllegalStateException("Offline AI has not loaded a local model."))
            return@callbackFlow
        }
        val chatPrompt = buildChatMlPrompt(systemPrompt, history, prompt)
        val metrics = LongArray(6)

        executor.execute {
            val mayRun = generationLock.withLock {
                if (unloading || handle.get() != loadedHandle) {
                    false
                } else {
                    LlamaNativeBridge.nativeResetCancellation(loadedHandle)
                    activeGenerationCount += 1
                    true
                }
            }
            if (!mayRun) {
                close()
                return@execute
            }

            try {
                LlamaNativeBridge.nativeGenerateStream(
                    handle = loadedHandle,
                    prompt = chatPrompt,
                    maxTokens = 512,
                    callback = { tokenPiece ->
                        if (!unloading && handle.get() == loadedHandle) {
                            trySend(tokenPiece)
                            true
                        } else {
                            false
                        }
                    },
                    outMetrics = metrics
                )
                close()
            } catch (e: Throwable) {
                close(e)
            } finally {
                generationLock.withLock {
                    activeGenerationCount -= 1
                    if (activeGenerationCount == 0) noActiveGeneration.signalAll()
                }
            }
        }

        awaitClose {
            cancelGeneration(loadedHandle)
        }
    }

    override suspend fun unload() = withContext(Dispatchers.IO) {
        unloadNative()
    }

    private fun unloadNative() {
        val previousHandle = generationLock.withLock {
            unloading = true
            val loaded = handle.get()
            if (loaded != 0L && activeGenerationCount > 0) {
                LlamaNativeBridge.nativeCancel(loaded)
                while (activeGenerationCount > 0) noActiveGeneration.awaitUninterruptibly()
            }
            handle.set(0L)
            loaded
        }
        if (previousHandle != 0L) {
            _unloadCount++
            LlamaNativeBridge.nativeUnload(previousHandle)
            Log.i(TAG, "OFFLINE_MODEL: released, instance_id=$_instanceId, loadCount=$_loadCount, unloadCount=$_unloadCount")
        }
        _instanceId = null
        loadedModelPath = null
        generationLock.withLock { unloading = false }
    }

    override fun cancelCurrentGeneration() {
        val loaded = handle.get()
        if (loaded != 0L) {
            cancelGeneration(loaded)
        }
    }

    private fun cancelGeneration(loadedHandle: Long) {
        generationLock.withLock {
            if (handle.get() == loadedHandle && activeGenerationCount > 0) {
                LlamaNativeBridge.nativeCancel(loadedHandle)
            }
        }
    }

    override fun clearCache() {
        val loaded = handle.get()
        if (loaded != 0L) {
            runCatching { LlamaNativeBridge.nativeClearCache(loaded) }
        }
    }
}

internal object LlamaNativeBridge {
    init {
        System.loadLibrary("edunova_llama")
    }

    external fun nativeLoadModel(modelPath: String): Long
    external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int): String?
    external fun nativeGenerateStream(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        callback: NativeTokenCallback?,
        outMetrics: LongArray?
    ): String?
    external fun nativeGetInstanceId(handle: Long): String?
    external fun nativeClearCache(handle: Long)
    external fun nativeResetCancellation(handle: Long)
    external fun nativeCancel(handle: Long)
    external fun nativeUnload(handle: Long)
}

/**
 * Builds standard ChatML prompt matching Qwen training format.
 * Enables 100% byte-for-byte prefix matching for KV cache reuse across multi-turn chat.
 */
fun buildChatMlPrompt(
    systemPrompt: String,
    history: List<ChatMessage>,
    newPrompt: String
): String = buildString {
    append("<|im_start|>system\n")
    append(systemPrompt.trim())
    append("<|im_end|>\n")
    history.takeLast(12).forEach { msg ->
        if (msg.fromUser) {
            append("<|im_start|>user\n")
            append(msg.text.trim())
            append("<|im_end|>\n")
        } else {
            append("<|im_start|>assistant\n")
            append(msg.text.trim())
            append("<|im_end|>\n")
        }
    }
    append("<|im_start|>user\n")
    append(newPrompt.trim())
    append("<|im_end|>\n<|im_start|>assistant\n")
}

internal fun systemPromptFor(mode: com.offline_First.domain.model.ExplanationMode): String =
    when (mode) {
        com.offline_First.domain.model.ExplanationMode.TEACHER ->
            "You are EduNova's patient teacher. Explain the answer step by step in age-appropriate language, use a clear example, and end with one short check-for-understanding question."
        com.offline_First.domain.model.ExplanationMode.GENERAL ->
            "You are EduNova, a helpful and accurate learning assistant. Answer naturally and clearly. If unsure, say so rather than inventing facts."
        com.offline_First.domain.model.ExplanationMode.EXPLAINABLE ->
            "You are EduNova's explainable learning assistant. Give a transparent, structured explanation: state the key idea, explain the supporting steps, and distinguish assumptions from facts. Do not reveal hidden chain-of-thought."
    }
