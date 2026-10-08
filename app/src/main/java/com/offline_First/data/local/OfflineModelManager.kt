package com.offline_First.data.local

import android.util.Log
import com.offline_First.domain.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "OfflineModelManager"

private fun logInfo(tag: String, message: String) {
    try {
        Log.i(tag, message)
    } catch (_: Throwable) {
        println("[$tag] $message")
    }
}

/**
 * Default idle shutdown timeout: 3 minutes (180,000 ms).
 * If no offline inference requests arrive within this window, the model is unloaded to free native RAM.
 */
const val OFFLINE_MODEL_IDLE_TIMEOUT_MS: Long = 180_000L

/**
 * Thread-safe lifecycle and performance manager around the local GGUF model and llama.cpp runtime.
 *
 * Guarantees:
 * - Model is loaded ONCE and kept alive across requests (instance ID stays identical across consecutive turns).
 * - Multi-turn KV cache is reused: prompt processing only evaluates new user tokens, not old turns.
 * - Real token streaming: tokens emit to UI incrementally without buffering entire text.
 * - Precise monotonic latency instrumentation:
 *     OFFLINE_LATENCY: send_clicked, repository_received, model_manager_called, model_ready,
 *                      inference_started, prompt_processing_started, prompt_processing_completed,
 *                      first_token, generation_completed, ui_received
 * - Idle shutdown (3 min) safely frees native resources without interrupting active inference.
 * - Switching to Online immediately unloads the offline model.
 */
class OfflineModelManager(
    private val engine: OfflineInferenceEngine?,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val idleTimeoutMs: Long = OFFLINE_MODEL_IDLE_TIMEOUT_MS
) {
    private val mutex = Mutex()

    @Volatile
    private var isModelLoaded: Boolean = false

    @Volatile
    private var generationActive: Boolean = false

    private var idleJob: Job? = null
    private var loadedModelPath: String? = null
    private var currentModelInstanceId: String? = null

    private var _loadCount = 0
    private var _unloadCount = 0
    private val messageCounter = AtomicInteger(0)

    val isLoaded: Boolean
        get() = isModelLoaded

    val isGenerating: Boolean
        get() = generationActive

    val modelInstanceId: String?
        get() = currentModelInstanceId ?: engine?.modelInstanceId

    val loadCount: Int
        get() = if (engine?.loadCount != null && engine.loadCount > 0) engine.loadCount else _loadCount

    val unloadCount: Int
        get() = if (engine?.unloadCount != null && engine.unloadCount > 0) engine.unloadCount else _unloadCount

    /**
     * Ensures the model file is loaded into the inference engine.
     * If already loaded with the same file, reuses the instance and resets the idle timer.
     * Thread-safe via Mutex so concurrent calls will not load duplicate instances.
     */
    suspend fun ensureModelLoaded(modelFile: File): Result<Unit> = mutex.withLock {
        try {
            if (engine == null) {
                return Result.failure(IllegalStateException("The Android llama.cpp runtime is unavailable."))
            }
            if (isModelLoaded && loadedModelPath == modelFile.absolutePath && engine.isLoaded) {
                resetIdleTimerLocked()
                return Result.success(Unit)
            }

            logInfo(TAG, "OFFLINE_MODEL: loading")
            engine.loadModel(modelFile)
            isModelLoaded = true
            loadedModelPath = modelFile.absolutePath
            _loadCount++
            currentModelInstanceId = engine.modelInstanceId ?: "${System.identityHashCode(engine)}_${System.currentTimeMillis()}"
            logInfo(TAG, "OFFLINE_MODEL: loaded, instance_id=$modelInstanceId, loadCount=$loadCount, unloadCount=$unloadCount")

            resetIdleTimerLocked()
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            isModelLoaded = false
            loadedModelPath = null
            currentModelInstanceId = null
            Result.failure(e)
        }
    }

    /**
     * Streams inference tokens in real-time with comprehensive monotonic latency instrumentation.
     */
    fun stream(
        systemPrompt: String,
        history: List<ChatMessage>,
        userPrompt: String,
        modelFile: File,
        sendTimestamp: Long = MonotonicClock.elapsedMillis()
    ): Flow<String> = callbackFlow {
        val msgNum = messageCounter.incrementAndGet()
        val tCalled = MonotonicClock.elapsedMillis()
        logInfo(TAG, "OFFLINE_LATENCY: model_manager_called, +${tCalled - sendTimestamp}ms from send_clicked")
        logInfo(TAG, "OFFLINE_MODEL: instance_id=$modelInstanceId, loadCount=$loadCount, unloadCount=$unloadCount")

        val loadResult = ensureModelLoaded(modelFile)
        if (loadResult.isFailure) {
            close(loadResult.exceptionOrNull()!!)
            return@callbackFlow
        }
        val tReady = MonotonicClock.elapsedMillis()
        val timeToModelReady = tReady - tCalled
        logInfo(TAG, "OFFLINE_LATENCY: model_ready, +${tReady - sendTimestamp}ms (time_to_model_ready=${timeToModelReady}ms)")

        mutex.withLock {
            generationActive = true
            cancelIdleTimerLocked()
        }
        logInfo(TAG, "OFFLINE_MODEL: generation_started")

        val tInferenceStarted = MonotonicClock.elapsedMillis()
        logInfo(TAG, "OFFLINE_LATENCY: inference_started, +${tInferenceStarted - sendTimestamp}ms")
        logInfo(TAG, "OFFLINE_LATENCY: prompt_processing_started")

        var firstTokenReceived = false
        var tFirstToken = 0L
        var tokenCount = 0

        val job = coroutineScope.launch {
            try {
                engine!!.stream(systemPrompt, history, userPrompt).collect { piece ->
                    if (!firstTokenReceived) {
                        firstTokenReceived = true
                        tFirstToken = MonotonicClock.elapsedMillis()
                        val ttft = tFirstToken - tInferenceStarted
                        logInfo(TAG, "OFFLINE_LATENCY: prompt_processing_completed")
                        logInfo(TAG, "OFFLINE_LATENCY: first_token, +${tFirstToken - sendTimestamp}ms (TTFT=${ttft}ms)")
                    }
                    tokenCount++
                    trySend(piece)
                }
                close()
            } catch (cancelled: CancellationException) {
                close(cancelled)
            } catch (e: Throwable) {
                close(e)
            } finally {
                val tCompleted = MonotonicClock.elapsedMillis()
                val totalResponseTime = tCompleted - sendTimestamp
                val tokenGenTime = if (firstTokenReceived) tCompleted - tFirstToken else 0L
                val tokPerSec = if (tokenGenTime > 0) (tokenCount * 1000.0) / tokenGenTime else 0.0
                val promptProcTime = if (firstTokenReceived) tFirstToken - tInferenceStarted else 0L
                val ttft = if (firstTokenReceived) tFirstToken - tInferenceStarted else 0L

                logInfo(TAG, "OFFLINE_LATENCY: generation_completed, +${tCompleted - sendTimestamp}ms")
                logInfo(TAG, """
OFFLINE_LATENCY:
model_ready=+${timeToModelReady}ms
prompt_processing=+${promptProcTime}ms
first_token=+${if (firstTokenReceived) tFirstToken - sendTimestamp else 0}ms
generation_complete=+${totalResponseTime}ms
total=${totalResponseTime}ms
                """.trimIndent())
                logInfo(TAG, "OFFLINE_METRICS: message_number=$msgNum, prompt_chars=${userPrompt.length}, prompt_tokens=${userPrompt.length / 4}, context_tokens=${(userPrompt.length + history.sumOf { it.text.length }) / 4}, generated_tokens=$tokenCount, ttft_ms=$ttft, gen_time_ms=$tokenGenTime, tok_per_sec=%.2f".format(tokPerSec))

                mutex.withLock {
                    generationActive = false
                    logInfo(TAG, "OFFLINE_MODEL: generation_completed")
                    resetIdleTimerLocked()
                }
            }
        }

        awaitClose {
            job.cancel()
            engine?.cancelCurrentGeneration()
        }
    }

    /**
     * Executes synchronous inference generation using the loaded model.
     * Retains the model alive across calls. Cancels idle timer while generation is active.
     */
    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        modelFile: File,
        history: List<ChatMessage> = emptyList()
    ): Result<String> {
        val tStart = MonotonicClock.elapsedMillis()
        val builder = StringBuilder()
        val loadResult = ensureModelLoaded(modelFile)
        if (loadResult.isFailure) {
            return Result.failure(loadResult.exceptionOrNull()!!)
        }

        mutex.withLock {
            generationActive = true
            cancelIdleTimerLocked()
        }
        logInfo(TAG, "OFFLINE_MODEL: generation_started")

        return try {
            val response = if (history.isNotEmpty()) {
                val fullChat = buildChatMlPrompt(systemPrompt, history, userPrompt)
                engine!!.generate(systemPrompt, fullChat)
            } else {
                engine!!.generate(systemPrompt, userPrompt)
            }
            Result.success(response)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            Result.failure(e)
        } finally {
            val tDone = MonotonicClock.elapsedMillis()
            mutex.withLock {
                generationActive = false
                logInfo(TAG, "OFFLINE_MODEL: generation_completed, total=${tDone - tStart}ms")
                resetIdleTimerLocked()
            }
        }
    }

    /**
     * Called when the user enters Offline Mode.
     * Preloads the model if a valid model file is provided, and starts the idle timer.
     */
    suspend fun onOfflineModeEntered(modelFile: File?) {
        if (modelFile != null && modelFile.exists() && modelFile.length() > 0) {
            ensureModelLoaded(modelFile)
        } else {
            mutex.withLock {
                resetIdleTimerLocked()
            }
        }
    }

    /**
     * Called when switching to Online Mode.
     * Immediately releases the offline model to free native RAM.
     */
    suspend fun onOnlineModeEntered() = mutex.withLock {
        unloadLocked()
    }

    /**
     * Cancels active inference generation without destroying the loaded model.
     */
    suspend fun cancelGeneration() {
        engine?.cancelCurrentGeneration()
        mutex.withLock {
            generationActive = false
            resetIdleTimerLocked()
        }
    }

    /**
     * Explicitly unloads the model and frees native resources.
     */
    suspend fun unload() = mutex.withLock {
        unloadLocked()
    }

    /**
     * Resets the idle timer manually (e.g., on user message or activity).
     */
    suspend fun resetIdleTimer() = mutex.withLock {
        resetIdleTimerLocked()
    }

    private fun cancelIdleTimerLocked() {
        idleJob?.cancel()
        idleJob = null
    }

    private fun resetIdleTimerLocked() {
        cancelIdleTimerLocked()
        if (!isModelLoaded || generationActive) return

        idleJob = coroutineScope.launch {
            delay(idleTimeoutMs)
            mutex.withLock {
                if (isModelLoaded && !generationActive) {
                    logInfo(TAG, "OFFLINE_MODEL: idle_timeout")
                    unloadLocked()
                }
            }
        }
    }

    private suspend fun unloadLocked() {
        cancelIdleTimerLocked()
        if (generationActive) {
            // Safety rule: never destroy the model while generation is running
            return
        }
        if (!isModelLoaded && loadedModelPath == null) {
            return
        }
        try {
            engine?.unload()
        } finally {
            _unloadCount++
            isModelLoaded = false
            loadedModelPath = null
            currentModelInstanceId = null
            logInfo(TAG, "OFFLINE_MODEL: released, instance_id=null, loadCount=$loadCount, unloadCount=$unloadCount")
        }
    }
}
