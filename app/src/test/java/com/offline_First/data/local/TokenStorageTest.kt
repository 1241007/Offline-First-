package com.offline_First.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenStorageTest {

    private lateinit var tokenStorage: TokenStorage

    @Before
    fun setUp() {
        tokenStorage = InMemoryTokenStorage()
    }

    @Test
    fun saveAndRetrieveTokens() {
        assertFalse(tokenStorage.hasTokens())
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())

        tokenStorage.saveTokens("access_123", "refresh_456")

        assertTrue(tokenStorage.hasTokens())
        assertEquals("access_123", tokenStorage.getAccessToken())
        assertEquals("refresh_456", tokenStorage.getRefreshToken())
    }

    @Test
    fun clearRemovesBothTokens() {
        tokenStorage.saveTokens("access_123", "refresh_456")
        assertTrue(tokenStorage.hasTokens())

        tokenStorage.clearTokens()

        assertFalse(tokenStorage.hasTokens())
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
    }

    @Test
    fun saveAndRetrieveUserIdentity() {
        assertNull(tokenStorage.getUserIdentity())

        tokenStorage.saveUserIdentity("user_42", "offline@edunova.com", "Offline Learner")

        val identity = tokenStorage.getUserIdentity()
        assertNotNull(identity)
        assertEquals("user_42", identity?.userId)
        assertEquals("offline@edunova.com", identity?.email)
        assertEquals("Offline Learner", identity?.fullName)

        tokenStorage.clearUserIdentity()
        assertNull(tokenStorage.getUserIdentity())
    }
}
