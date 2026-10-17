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

    @Synchronized
    override fun saveUserIdentity(userId: String, email: String, fullName: String) {
        store[KEY_USER_ID] = userId
        store[KEY_USER_EMAIL] = email
        store[KEY_USER_FULL_NAME] = fullName
    }

    @Synchronized
    override fun getUserIdentity(): CachedUserIdentity? {
        val uid = store[KEY_USER_ID] ?: return null
        val email = store[KEY_USER_EMAIL] ?: ""
        val fullName = store[KEY_USER_FULL_NAME] ?: ""
        return CachedUserIdentity(userId = uid, email = email, fullName = fullName)
    }

    @Synchronized
    override fun clearUserIdentity() {
        store.remove(KEY_USER_ID)
        store.remove(KEY_USER_EMAIL)
        store.remove(KEY_USER_FULL_NAME)
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "cached_user_id"
        private const val KEY_USER_EMAIL = "cached_user_email"
        private const val KEY_USER_FULL_NAME = "cached_user_full_name"
    }
}
