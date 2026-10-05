package com.offline_First

import com.offline_First.data.remote.AppDns
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDnsTest {

    @Test
    fun testResolvesBackendHostname() {
        val addresses = AppDns.lookup("edunova-backend-9waj.onrender.com")
        assertNotNull("Resolved addresses should not be null", addresses)
        assertFalse("Resolved addresses should not be empty", addresses.isEmpty())
        assertTrue("Resolved IP should be valid", addresses.any { it.hostAddress != null })
    }

    @Test
    fun testResolvesHuggingFaceHostname() {
        val addresses = AppDns.lookup("huggingface.co")
        assertNotNull("Resolved addresses should not be null", addresses)
        assertFalse("Resolved addresses should not be empty", addresses.isEmpty())
        assertTrue("Resolved IP should be valid", addresses.any { it.hostAddress != null })
    }

    @Test
    fun testBootstrapFallbackForKnownHost() {
        val addresses = AppDns.lookup("edunova-backend-9waj.onrender.com")
        assertTrue("Should contain IP address", addresses.isNotEmpty())
    }
}
