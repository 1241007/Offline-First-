package com.offline_First.domain.model

/**
 * Domain model representing a school subject (e.g. Mathematics, Physics, Chemistry, Biology).
 */
data class Subject(
    val id: String,
    val name: String,
    val icon: String,
    val progress: Float? = null,
    val totalTopics: Int = 0,
    val gradeLevel: String = "Class 10 • CBSE",
    val completedChapters: Int = 8,
    val totalChapters: Int = 12
)

/**
 * Domain model representing the active learning item for School mode.
 */
data class ContinueLearningItem(
    val subjectName: String,
    val topicName: String,
    val lessonInfo: String,
    val progress: Float,
    val estimatedMinutes: Int = 10,
    val practiceQuestionsCount: Int = 5
)

/**
 * Domain model representing upcoming school exams.
 */
data class UpcomingExam(
    val id: String,
    val title: String,
    val daysRemaining: Int,
    val className: String
)

enum class ChapterStatus {
    COMPLETED,
    IN_PROGRESS,
    NOT_STARTED
}

data class ChapterItem(
    val number: Int,
    val title: String,
    val status: ChapterStatus,
    val progressPercentage: Int = 0
)

/**
 * Domain model representing the focused next study/practice step for School mode.
 */
data class StudyFocusItem(
    val subjectName: String,
    val actionTitle: String,
    val lessonInfo: String
)
