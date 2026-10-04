package com.offline_First.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface OfflineInferenceEngine {
    suspend fun loadModel(modelFile: File)
    suspend fun generate(systemPrompt: String, userPrompt: String): String
    suspend fun unload()
}

class LlamaCppInferenceEngine : OfflineInferenceEngine {
    private val handle = AtomicLong(0L)
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
    }

    override suspend fun generate(systemPrompt: String, userPrompt: String): String = suspendCancellableCoroutine { continuation ->
        val loadedHandle = handle.get()
        if (loadedHandle == 0L) {
            continuation.resumeWith(Result.failure(IllegalStateException("Offline AI has not loaded a local model.")))
            return@suspendCancellableCoroutine
        }
        val prompt = buildString {
            append("<|im_start|>system\n")
            append(systemPrompt.trim())
            append("<|im_end|>\n<|im_start|>user\n")
            append(userPrompt.trim())
            append("<|im_end|>\n<|im_start|>assistant\n")
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
            LlamaNativeBridge.nativeUnload(previousHandle)
        }
        loadedModelPath = null
        generationLock.withLock { unloading = false }
    }

    private fun cancelGeneration(loadedHandle: Long) {
        generationLock.withLock {
            if (handle.get() == loadedHandle && activeGenerationCount > 0) {
                LlamaNativeBridge.nativeCancel(loadedHandle)
            }
        }
    }

}

internal object LlamaNativeBridge {
    init {
        System.loadLibrary("edunova_llama")
    }

    external fun nativeLoadModel(modelPath: String): Long
    external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int): String?
    external fun nativeResetCancellation(handle: Long)
    external fun nativeCancel(handle: Long)
    external fun nativeUnload(handle: Long)
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
