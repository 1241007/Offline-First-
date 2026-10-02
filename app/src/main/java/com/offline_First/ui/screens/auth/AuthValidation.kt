package com.offline_First.ui.screens.auth

private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

internal fun validateEmail(value: String): String? = when {
    value.isBlank() -> "Please enter your email address."
    !EmailPattern.matches(value.trim()) -> "Please enter a valid email address."
    else -> null
}

internal fun validateMobile(value: String): String? {
    val trimmed = value.trim()
    val digits = trimmed.filter(Char::isDigit)
    return when {
        trimmed.isBlank() -> "Please enter your mobile number."
        !trimmed.matches(Regex("[+()\\d\\s-]+")) || digits.length !in 7..15 ->
            "Please enter a valid mobile number."
        else -> null
    }
}

internal fun validateLoginContact(value: String): String? {
    val trimmed = value.trim()
    return when {
        trimmed.isBlank() -> "Please enter your email or mobile number."
        trimmed.contains("@") -> validateEmail(trimmed)
        else -> validateMobile(trimmed)?.replace("your mobile number", "a valid mobile number")
    }
}