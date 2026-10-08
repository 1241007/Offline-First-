package com.offline_First.data.local

/**
 * Provides monotonic timestamps for latency measurements.
 * Uses android.os.SystemClock.elapsedRealtime() on Android, falling back to System.nanoTime() on JVM.
 * Never uses wall-clock time.
 */
object MonotonicClock {
    fun elapsedMillis(): Long = runCatching {
        android.os.SystemClock.elapsedRealtime()
    }.getOrElse {
        System.nanoTime() / 1_000_000L
    }
}
