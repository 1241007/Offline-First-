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
    val educationMode: EducationMode = EducationMode.GENERAL,
    val board: String? = null,
    val classGrade: String? = null,
    val subject: String? = null,
    val preferredLanguage: String = "en"
)
