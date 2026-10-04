package com.offline_First.domain.model

/**
 * Domain model representing a school subject (e.g. Mathematics, Physics, Chemistry, Biology).
 */
data class Subject(
    val id: String,
    val name: String,
    val icon: String,
    val progress: Float? = null,
    val totalTopics: Int = 0
)

/**
 * Domain model representing the active learning item for School mode.
 */
data class ContinueLearningItem(
    val subjectName: String,
    val topicName: String,
    val lessonInfo: String,
    val progress: Float
)

/**
 * Domain model representing the focused next study/practice step for School mode.
 */
data class StudyFocusItem(
    val subjectName: String,
    val actionTitle: String,
    val lessonInfo: String
)
