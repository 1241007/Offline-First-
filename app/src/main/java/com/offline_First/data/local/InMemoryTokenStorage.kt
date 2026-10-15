package com.offline_First.data.local

class InMemoryTokenStorage : TokenStorage {
    private val store = mutableMapOf<String, String>()

    @Synchronized
    override fun saveTokens(accessToken: String, refreshToken: String) {
        store[KEY_ACCESS_TOKEN] = accessToken
        store[KEY_REFRESH_TOKEN] = refreshToken
    }

    @Synchronized
    override fun getAccessToken(): String? {
        return store[KEY_ACCESS_TOKEN]
    }

    @Synchronized
    override fun getRefreshToken(): String? {
        return store[KEY_REFRESH_TOKEN]
    }

    @Synchronized
    override fun clearTokens() {
        store.remove(KEY_ACCESS_TOKEN)
        store.remove(KEY_REFRESH_TOKEN)
    }

    @Synchronized
    override fun hasTokens(): Boolean {
        return !getAccessToken().isNullOrBlank() && !getRefreshToken().isNullOrBlank()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
