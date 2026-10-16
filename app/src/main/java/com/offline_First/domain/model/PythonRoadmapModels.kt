package com.offline_First.domain.model

/**
 * Status of a topic within the interactive Python roadmap.
 */
enum class TopicLearningStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

/**
 * Practice question to test understanding of a Python topic.
 */
data class PracticeQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

/**
 * Code demonstration with expected execution output.
 */
data class PythonCodeExample(
    val title: String,
    val code: String,
    val expectedOutput: String,
    val explanation: String
)

/**
 * Subtopic conceptual breakdown.
 */
data class PythonSubtopic(
    val title: String,
    val explanation: String,
    val keyPoints: List<String> = emptyList()
)

/**
 * Individual topic node within a roadmap module.
 */
data class PythonTopic(
    val id: String,
    val moduleId: String,
    val title: String,
    val shortDescription: String,
    val overview: String,
    val learningObjectives: List<String>,
    val subtopics: List<PythonSubtopic>,
    val codeExamples: List<PythonCodeExample>,
    val practiceQuestions: List<PracticeQuestion>,
    val estimatedMinutes: Int = 20
)

/**
 * Major module node representing a milestone on the central learning path.
 */
data class PythonModule(
    val id: String,
    val stageNumber: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val iconKey: String = "python",
    val topics: List<PythonTopic>
)

/**
 * Overall progress state of the Python roadmap.
 */
data class PythonRoadmapProgress(
    val completedTopicIds: Set<String> = emptySet(),
    val inProgressTopicIds: Set<String> = emptySet(),
    val skippedTopicIds: Set<String> = emptySet()
) {
    fun getStatus(topicId: String): TopicLearningStatus = when {
        completedTopicIds.contains(topicId) -> TopicLearningStatus.COMPLETED
        inProgressTopicIds.contains(topicId) -> TopicLearningStatus.IN_PROGRESS
        skippedTopicIds.contains(topicId) -> TopicLearningStatus.SKIPPED
        else -> TopicLearningStatus.NOT_STARTED
    }

    val totalCompleted: Int get() = completedTopicIds.size
    val totalInProgress: Int get() = inProgressTopicIds.size
    val totalSkipped: Int get() = skippedTopicIds.size
}
