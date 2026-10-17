package com.offline_First.data.local

data class CachedUserIdentity(
    val userId: String,
    val email: String,
    val fullName: String
)

interface TokenStorage {
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
    fun hasTokens(): Boolean

    fun saveUserIdentity(userId: String, email: String, fullName: String)
    fun getUserIdentity(): CachedUserIdentity?
    fun clearUserIdentity()
}

