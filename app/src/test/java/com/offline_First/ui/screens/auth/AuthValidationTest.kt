package com.offline_First.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validEmailReturnsNull() {
        assertNull(validateEmail("student@edunova.org"))
        assertNull(validateEmail("asha.learner@domain.co.in"))
    }

    @Test
    fun blankOrInvalidEmailReturnsError() {
        assertEquals("Please enter your email address.", validateEmail(""))
        assertEquals("Please enter your email address.", validateEmail("   "))
        assertEquals("Please enter a valid email address.", validateEmail("invalid-email"))
        assertEquals("Please enter a valid email address.", validateEmail("user@"))
        assertEquals("Please enter a valid email address.", validateEmail("@domain.com"))
    }

    @Test
    fun validMobileReturnsNull() {
        assertNull(validateMobile("+91 98765 43210"))
        assertNull(validateMobile("9876543210"))
        assertNull(validateMobile("(+1) 555-0199"))
    }

    @Test
    fun blankOrInvalidMobileReturnsError() {
        assertEquals("Please enter your mobile number.", validateMobile(""))
        assertEquals("Please enter a valid mobile number.", validateMobile("123"))
        assertEquals("Please enter a valid mobile number.", validateMobile("phone12345678"))
    }

    @Test
    fun loginRequiresAnEmailAddress() {
        assertEquals("Please enter your email address.", validateLoginEmail(""))
        assertNull(validateLoginEmail("user@example.com"))
        assertEquals("Please enter a valid email address.", validateLoginEmail("9876543210"))
        assertEquals("Please enter a valid email address.", validateLoginEmail("user@incomplete"))
    }
}
