package com.offline_First.domain.model

import kotlinx.serialization.Serializable


/**
 * Detailed domain model for a Course.
 */
data class CourseDetail(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val accent: CourseAccent = CourseAccent.PRIMARY,
    val category: String,
    val level: String,
    val durationHours: Int?,
    val prerequisites: List<String> = emptyList(),
    val modules: List<CourseModule> = emptyList()
) {
    val totalLessonsCount: Int
        get() = modules.sumOf { it.lessons.size }

    fun toCourse(): Course = Course(
        id = id,
        name = name,
        description = description,
        icon = icon,
        accent = accent,
        category = category,
        level = level,
        durationHours = durationHours
    )
}

/**
 * Module grouping ordered lessons within a course.
 */
data class CourseModule(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String? = null,
    val displayOrder: Int = 0,
    val lessons: List<LessonItem> = emptyList()
)

/**
 * Individual lesson item within a module.
 */
data class LessonItem(
    val id: String,
    val moduleId: String,
    val courseId: String,
    val title: String,
    val description: String? = null,
    val contentType: String = "video", // video, article, quiz, exercise, project
    val durationMinutes: Int? = null,
    val displayOrder: Int = 0,
    val resourceUrl: String? = null,
    val resourceProvider: String? = null,
    val videoUrl: String? = null,
    val videoProvider: String? = null,
    val isCompleted: Boolean = false,
    val isMarkedForRevision: Boolean = false,
    val quiz: CourseQuiz? = null
)

/**
 * Practice quiz attached to a lesson or module.
 */
data class CourseQuiz(
    val id: String,
    val title: String,
    val questions: List<QuizQuestion> = emptyList()
)

/**
 * Canonical Quiz question model across EduNova.
 */
data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

/**
 * Item marked for revision by a student.
 */
@Serializable
data class RevisionItem(
    val courseId: String,

    val courseName: String,
    val lessonId: String,
    val lessonTitle: String,
    val moduleTitle: String,
    val resourceUrl: String? = null,
    val resourceProvider: String? = null,
    val markedAt: Long = System.currentTimeMillis()
)

/**
 * Certificate awarded upon completing all required lessons in a course.
 */
@Serializable
data class CourseCertificate(
    val id: String,
    val courseId: String,
    val courseName: String,
    val learnerName: String,
    val completedDate: String,
    val totalLessons: Int,
    val verificationType: String = "LOCAL_PERSISTED",
    val issuedAtTimestamp: Long = System.currentTimeMillis()
)
