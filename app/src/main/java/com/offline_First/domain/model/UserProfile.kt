package com.offline_First.domain.model

enum class EducationMode {
    SCHOOL,
    GENERAL
}

data class UserProfile(
    val fullName: String,
    val email: String,
    val mobile: String,
    val interests: String,
    val level: String,
    val educationMode: EducationMode = EducationMode.GENERAL
)
