package com.offline_First.data.remote

/**
 * EduNova AI Chat backend base URL.
 *
 * CONFIGURATION:
 * - Android Emulator: Use http://10.0.2.2:8000 (maps to host machine's localhost)
 * - Physical device on same WiFi: Use your machine's LAN IP, e.g. http://192.168.1.x:8000
 *   To find your LAN IP on Windows: run `ipconfig` and look for IPv4 Address
 *
 * Do NOT hardcode production URLs in source code. Change this constant for your environment.
 */
object ChatApiConfig {
    // For Android Emulator - points to host machine's localhost:8000
    const val BASE_URL = "http://10.0.2.2:8000"
    
    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 60_000
    const val API_PREFIX = "/api/v1"
}
