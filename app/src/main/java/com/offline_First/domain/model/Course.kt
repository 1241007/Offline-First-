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
    val accent: CourseAccent = CourseAccent.PRIMARY,
    val category: String = "",
    val level: String = "Beginner",
    val durationHours: Int? = null,
    val totalLessons: Int = 0,
    val modulesCount: Int = 0,
    val completedLessons: Int = 0,
    val progress: Float = 0f
)
