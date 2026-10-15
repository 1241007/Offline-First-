package com.offline_First.data.remote

import com.offline_First.BuildConfig

/**
 * EduNova AI Chat backend base URL.
 *
 * CONFIGURATION (via gradle.properties):
 * - Production (Render):  backendBaseUrl=https://edunova-backend-9waj.onrender.com
 * - Android Emulator:     backendBaseUrl=http://10.0.2.2:8000
 * - Physical device WiFi: backendBaseUrl=http://192.168.x.x:8000  (use `ipconfig` to find your LAN IP)
 *
 * The value is injected at compile time via BuildConfig.BACKEND_BASE_URL.
 * Do NOT hardcode URLs directly in source code.
 */
object ChatApiConfig {
    val BASE_URL: String = BuildConfig.BACKEND_BASE_URL

    const val CONNECT_TIMEOUT_MS = 60_000   // Render free-tier cold-start can take ~50 s
    const val READ_TIMEOUT_MS = 90_000
    const val API_PREFIX = "/api/v1"
}
