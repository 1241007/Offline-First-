package com.offline_First.domain.model

enum class CourseAccent {
    PRIMARY,
    SECONDARY,
    ACCENT
}

data class Course(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val accent: CourseAccent = CourseAccent.PRIMARY
)
