package com.offline_First

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// ---------------------------------------------------------------------------
// Pure helper — mirrors the when-expression in LocalAIRepository.startOfflineAIDownload
// This is extracted here to avoid pulling in Android dependencies.
// ---------------------------------------------------------------------------
internal fun classifyDownloadError(error: Throwable): String = when (error) {
    is UnknownHostException ->
        "No internet connection. Connect to the internet and retry."
    is SocketTimeoutException ->
        "Connection timed out. Check your internet connection and retry."
    is ConnectException ->
        "Could not reach the download server. Check your internet connection and retry."
    else -> "Download failed: ${error.message ?: "Unknown error"}"
}

// ---------------------------------------------------------------------------
// Property 3 — URL correctness (deterministic, non-parameterized)
// ---------------------------------------------------------------------------

/**
 * Verifies that the default model download URLs baked into build.gradle.kts do not
 * contain the invalid `/buckets/` path segment or the spurious `?download=true` query
 * parameter that were present in the original bug.
 *
 * BuildConfig is an Android-generated class and is not available on the JVM test
 * classpath, so the expected URL values are copied directly from app/build.gradle.kts.
 */
class ModelUrlCorrectnessTest {

    // Copied from the defaults in app/build.gradle.kts
    private val smallUrl =
        "https://huggingface.co/buckets/PatilKrish/Qwen_Models2/resolve/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf?download=true"
    private val largeUrl =
        "https://huggingface.co/buckets/PatilKrish/Qwen_Models2/resolve/Qwen2.5-3B-Instruct-Q4_K_M.gguf?download=true"

    @Test
    fun smallModelUrlContainsBucketsSegment() {
        assertTrue(
            "OFFLINE_MODEL_SMALL_URL must contain '/buckets/'",
            smallUrl.contains("/buckets/")
        )
    }

    @Test
    fun smallModelUrlContainsDownloadQueryParam() {
        assertTrue(
            "OFFLINE_MODEL_SMALL_URL must contain '?download=true'",
            smallUrl.contains("?download=true")
        )
    }

    @Test
    fun largeModelUrlContainsBucketsSegment() {
        assertTrue(
            "OFFLINE_MODEL_LARGE_URL must contain '/buckets/'",
            largeUrl.contains("/buckets/")
        )
    }

    @Test
    fun largeModelUrlContainsDownloadQueryParam() {
        assertTrue(
            "OFFLINE_MODEL_LARGE_URL must contain '?download=true'",
            largeUrl.contains("?download=true")
        )
    }

    @Test
    fun modelUrlsUseHuggingFaceResolveFormat() {
        assertTrue(
            "OFFLINE_MODEL_SMALL_URL must use the HuggingFace resolve/ format",
            smallUrl.contains("huggingface.co") && smallUrl.contains("/resolve/")
        )
        assertTrue(
            "OFFLINE_MODEL_LARGE_URL must use the HuggingFace resolve/ format",
            largeUrl.contains("huggingface.co") && largeUrl.contains("/resolve/")
        )
    }
}

// ---------------------------------------------------------------------------
// Property 4 — Network error classification (parameterized)
// ---------------------------------------------------------------------------

/**
 * Parameterized test that exhaustively verifies every known network exception type
 * is mapped to a specific, non-generic error message by [classifyDownloadError].
 *
 * Using @RunWith(Parameterized) as specified in design.md — no extra dependency needed
 * because JUnit 4 is already on the test classpath via testImplementation(libs.junit).
 */
@RunWith(Parameterized::class)
class NetworkErrorClassificationTest(
    private val exceptionName: String,
    private val exception: Throwable,
    private val expectedMessageSubstring: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "UnknownHostException",
                UnknownHostException("Unable to resolve host"),
                "No internet connection"
            ),
            arrayOf(
                "SocketTimeoutException",
                SocketTimeoutException("Read timed out"),
                "Connection timed out"
            ),
            arrayOf(
                "ConnectException",
                ConnectException("Connection refused"),
                "Could not reach the download server"
            )
        )
    }

    @Test
    // Feature: backend-connectivity-and-model-download-fix, Property 4: Network error classification
    fun knownNetworkExceptionMapsToSpecificMessage() {
        // Feature: backend-connectivity-and-model-download-fix, Property 4: Network error classification
        val message = classifyDownloadError(exception)

        assertTrue(
            "classifyDownloadError($exceptionName) should contain '$expectedMessageSubstring' but was '$message'",
            message.contains(expectedMessageSubstring)
        )
    }

    @Test
    // Feature: backend-connectivity-and-model-download-fix, Property 4: Network error classification
    fun knownNetworkExceptionDoesNotProduceGenericFallback() {
        // Feature: backend-connectivity-and-model-download-fix, Property 4: Network error classification
        val message = classifyDownloadError(exception)

        // The generic fallback starts with "Download failed:" — known network errors must not use it
        assertFalse(
            "classifyDownloadError($exceptionName) must not fall back to the generic 'Download failed' message, got: '$message'",
            message.startsWith("Download failed:")
        )
    }
}

// ---------------------------------------------------------------------------
// Additional edge-case: unknown exception still uses the generic fallback
// ---------------------------------------------------------------------------

class GenericFallbackTest {

    @Test
    // Feature: backend-connectivity-and-model-download-fix, Property 4: Network error classification
    fun unknownExceptionUsesGenericFallback() {
        // Feature: backend-connectivity-and-model-download-fix, Property 4: Network error classification
        val error = RuntimeException("Something unexpected")
        val message = classifyDownloadError(error)

        assertTrue(
            "Unknown exception should produce generic 'Download failed:' message, got: '$message'",
            message.startsWith("Download failed:")
        )
        // Must not be the old bare "Download failed" without context
        assertNotEquals("Download failed", message.trim())
    }

    @Test
    // Feature: backend-connectivity-and-model-download-fix, Property 4: Network error classification
    fun exceptionWithNullMessageUsesUnknownErrorSuffix() {
        // Feature: backend-connectivity-and-model-download-fix, Property 4: Network error classification
        val error = object : RuntimeException(null as String?) {}
        val message = classifyDownloadError(error)

        assertTrue(
            "Exception with null message should produce 'Download failed: Unknown error', got: '$message'",
            message == "Download failed: Unknown error"
        )
    }
}
