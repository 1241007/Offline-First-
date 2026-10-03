package com.offline_First.domain.model

data class UserProfile(
    val fullName: String,
    val email: String,
    val mobile: String,
    val interests: String,
    val level: String
)
