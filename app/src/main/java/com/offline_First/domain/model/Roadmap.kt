package com.offline_First.domain.model

enum class RoadmapAccentTheme {
    PRIMARY,
    SECONDARY,
    ACCENT,
    SUCCESS
}

data class RoadmapOption(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val skills: List<String>,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    val accentTheme: RoadmapAccentTheme = RoadmapAccentTheme.PRIMARY,
    val isPersonalized: Boolean = false
)

enum class AssessmentSpeaker {
    MENTOR,
    LEARNER
}

data class DiagnosticQuiz(
    val question: String,
    val options: List<String>,
    val skillTested: String? = null,
    val difficulty: String? = null,
    val selectionRationale: String? = null
)

data class AssessmentMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val speaker: AssessmentSpeaker,
    val text: String,
    val options: List<String> = emptyList(),
    val quiz: DiagnosticQuiz? = null,
    val stepId: String? = null,
    val timestamp: String? = null
)

data class AssessmentSessionState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val state: String = "in_progress",
    val currentStepId: String? = null,
    val completedSteps: List<String> = emptyList(),
    val goal: String? = null,
    val targetLevel: String? = null,
    val targetTimeline: String? = null,
    val weeklyHours: Double? = null,
    val latestMessage: String? = null,
    val options: List<String> = emptyList(),
    val quiz: DiagnosticQuiz? = null,
    val completenessPercentage: Int = 0,
    val summary: Map<String, Any?>? = null,
    val roadmapId: String? = null,
    val messages: List<AssessmentMessage> = emptyList(),

    // Extra fields for adaptive assessment
    val sessionId: String = id,
    val domain: String = "Python",
    val phase: AssessmentPhase = AssessmentPhase.PROFILE_COLLECTION,
    val profileGoal: String = "",
    val profileLevel: String = "",
    val profileStudyHoursPerWeek: Int = 0,
    val profileTimelineWeeks: Int? = null,
    val currentQuestion: AssessmentQuestion? = null,
    val questionNumber: Int = 0,
    val totalQuestions: Int = 0,
    val masteryMap: Map<String, TopicMasteryState> = emptyMap(),
    val isComplete: Boolean = false,
    val generatedRoadmapId: String? = null,
    val lastAnswerCorrect: Boolean? = null,
    val lastAnswerExplanation: String = "",
    val correctionPrompt: CorrectionPrompt? = null,
    val errorMessage: String? = null
)

data class PersonalizedTask(
    val id: String,
    val title: String,
    val description: String = "",
    val instructions: String = "",
    val estimatedHours: Double = 1.0,
    val resources: List<String> = emptyList(),
    val completionCriteria: String = "",
    val isCompleted: Boolean = false,
    val dependencies: List<String> = emptyList(),
    val topicId: String? = null,
    val status: String? = null,
    val subtopics: List<String> = emptyList(),
    val learningObjectives: List<String> = emptyList(),
    val codeExample: String? = null,
    val skipReason: String? = null
)

data class PersonalizedMilestone(
    val title: String,
    val completionCriteria: List<String> = emptyList(),
    val assessment: String = "",
    val passingCriteria: String = "",
    val isCompleted: Boolean = false
)

data class PersonalizedPhase(
    val title: String,
    val objective: String,
    val durationWeeks: Int,
    val topics: List<String> = emptyList(),
    val tasks: List<PersonalizedTask> = emptyList(),
    val activities: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val milestones: List<PersonalizedMilestone> = emptyList(),
    val recommendedCourseIds: List<String> = emptyList()
)

data class WeeklyScheduleItem(
    val dayOrWeek: String,
    val focusTopic: String,
    val estimatedHours: Double,
    val tasks: List<String> = emptyList()
)

data class SkillGapItem(
    val skill: String,
    val status: String = "developing",
    val source: String = "diagnostic_verified",
    val confidence: Double = 1.0,
    val rationale: String? = null
)

data class AssessmentSummary(
    val strengths: List<String> = emptyList(),
    val skillGaps: List<String> = emptyList(),
    val verifiedEvidence: List<String> = emptyList(),
    val selfReportedInformation: List<String> = emptyList(),
    val unknowns: List<String> = emptyList(),
    val skillGapBreakdown: List<SkillGapItem> = emptyList(),
    val curriculumRationale: String? = null
)

data class PersonalizedRoadmapDetail(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "My Personalized Roadmap",
    val goal: String = "",
    val category: String = "",
    val level: String = "",
    val duration: String = "",
    val stages: Int = 0,
    val icon: String = "",
    val accentTheme: RoadmapAccentTheme = RoadmapAccentTheme.PRIMARY,
    val weeklyHours: Double = 0.0,
    val assessmentSummary: AssessmentSummary = AssessmentSummary(),
    val phases: List<PersonalizedPhase> = emptyList(),
    val weeklySchedule: List<WeeklyScheduleItem> = emptyList(),
    val assumptions: List<String> = emptyList(),
    val capstoneProject: String = "",
    val nextAction: String = "",
    val completedMilestones: List<String> = emptyList(),
    val createdAt: String? = null,

    // Extra fields for topic-based model
    val domain: String = "Python",
    val studyHoursPerWeek: Int = 0,
    val estimatedWeeks: Int = 0,
    val topics: List<PersonalizedRoadmapTopic> = emptyList(),
    val edges: List<PersonalizedRoadmapEdge> = emptyList(),
    val assessmentSessionId: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),

    // Derived progress
    val completedTopicIds: Set<String> = emptySet(),
    val inProgressTopicIds: Set<String> = emptySet()
) {
    val totalTopics: Int get() = if (topics.isNotEmpty()) topics.size else phases.sumOf { it.milestones.size }
    val completedCount: Int get() = if (completedTopicIds.isNotEmpty()) completedTopicIds.size else completedMilestones.size
    val skippedCount: Int get() = topics.count { it.status == PersonalizedTopicStatus.SKIPPED }
    val requiredTotalTopics: Int get() = (totalTopics - skippedCount).coerceAtLeast(1)
    val progressPercentage: Int get() =
        if (totalTopics == 0) 0
        else if (totalTopics == skippedCount) 100
        else ((completedCount.toFloat() / requiredTotalTopics.toFloat()) * 100).toInt().coerceIn(0, 100)

    val recommendedNextTopic: PersonalizedRoadmapTopic?
        get() = topics
            .filter { it.status == PersonalizedTopicStatus.UNLOCKED }
            .maxByOrNull { it.priorityScore }

    fun withRecomputedStatuses(): PersonalizedRoadmapDetail {
        val skippedTopicIds = topics.filter { it.status == PersonalizedTopicStatus.SKIPPED }.map { it.id }.toSet()
        val satisfiedTopicIds = completedTopicIds + skippedTopicIds

        val updatedTopics = topics.map { topic ->
            when {
                topic.status == PersonalizedTopicStatus.SKIPPED ->
                    topic
                completedTopicIds.contains(topic.id) ->
                    topic.copy(status = PersonalizedTopicStatus.COMPLETED)
                inProgressTopicIds.contains(topic.id) ->
                    topic.copy(status = PersonalizedTopicStatus.IN_PROGRESS)
                topic.prerequisiteTopicIds.all { prereqId ->
                    satisfiedTopicIds.contains(prereqId)
                } -> topic.copy(status = PersonalizedTopicStatus.UNLOCKED)
                else -> topic.copy(status = PersonalizedTopicStatus.LOCKED)
            }
        }
        return copy(topics = updatedTopics)
    }

    fun withSynthesizedTopicsIfEmpty(): PersonalizedRoadmapDetail {
        if (topics.isNotEmpty()) {
            return this.withRecomputedStatuses()
        }
        if (phases.isEmpty()) {
            return this
        }

        val synthesizedTopics = mutableListOf<PersonalizedRoadmapTopic>()
        val synthesizedEdges = mutableListOf<PersonalizedRoadmapEdge>()
        var previousTopicId: String? = null

        val demonstratedSkills = assessmentSummary.skillGapBreakdown
            .filter { it.status.equals("demonstrated", ignoreCase = true) }
            .map { it.skill.lowercase() }

        phases.forEachIndexed { pIdx, phase ->
            if (phase.tasks.isNotEmpty()) {
                phase.tasks.forEachIndexed { tIdx, task ->
                    val topicId = task.id.ifBlank { "${id}_p${pIdx}_t${tIdx}" }
                    val isDone = task.isCompleted ||
                        completedMilestones.contains(topicId) ||
                        completedMilestones.contains(task.title) ||
                        completedMilestones.contains("${id}_task_${task.id}")

                    val prereqs = if (task.dependencies.isNotEmpty()) {
                        task.dependencies
                    } else if (previousTopicId != null) {
                        listOf(previousTopicId!!)
                    } else {
                        emptyList()
                    }

                    val isSkipped = task.status.equals("SKIPPED", ignoreCase = true)
                    val isDemonstrated = isSkipped || demonstratedSkills.any { skill ->
                        task.title.lowercase().contains(skill) || skill.contains(task.title.lowercase())
                    }
                    val mastery = if (isDemonstrated) TopicMasteryLevel.STRONG else TopicMasteryLevel.UNKNOWN
                    val accelerated = isDemonstrated

                    val status = when {
                        isSkipped -> PersonalizedTopicStatus.SKIPPED
                        isDone -> PersonalizedTopicStatus.COMPLETED
                        prereqs.isEmpty() -> PersonalizedTopicStatus.UNLOCKED
                        else -> PersonalizedTopicStatus.LOCKED
                    }

                    val desc = task.description.ifBlank { task.instructions.ifBlank { "Core module for ${phase.title}" } }
                    val exp = if (isSkipped) task.skipReason ?: "Demonstrated mastery during assessment."
                    else if (accelerated) "Accelerated pathway: Demonstrated proficiency during assessment."
                    else task.completionCriteria.ifBlank { phase.objective }

                    val minutes = (task.estimatedHours * 60).toInt().coerceIn(15, 240)
                    val priority = (100 - (pIdx * 20 + tIdx * 5)).coerceAtLeast(1)

                    synthesizedTopics.add(
                        PersonalizedRoadmapTopic(
                            id = topicId,
                            topicTag = task.title.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                            title = task.title,
                            shortDescription = desc,
                            estimatedMinutes = minutes,
                            prerequisiteTopicIds = prereqs,
                            status = status,
                            masteryLevel = mastery,
                            priorityScore = priority,
                            accelerated = accelerated,
                            explanation = exp
                        )
                    )

                    prereqs.forEach { p ->
                        synthesizedEdges.add(PersonalizedRoadmapEdge(fromTopicId = p, toTopicId = topicId))
                    }
                    previousTopicId = topicId
                }
            } else if (phase.topics.isNotEmpty()) {
                phase.topics.forEachIndexed { topIdx, topicStr ->
                    val topicId = "${id}_p${pIdx}_top${topIdx}"
                    val isDone = completedMilestones.contains(topicId) || completedMilestones.contains(topicStr)
                    val prereqs = if (previousTopicId != null) listOf(previousTopicId!!) else emptyList()
                    val status = if (isDone) PersonalizedTopicStatus.COMPLETED
                    else if (prereqs.isEmpty()) PersonalizedTopicStatus.UNLOCKED
                    else PersonalizedTopicStatus.LOCKED
                    val priority = (100 - (pIdx * 20 + topIdx * 5)).coerceAtLeast(1)

                    synthesizedTopics.add(
                        PersonalizedRoadmapTopic(
                            id = topicId,
                            topicTag = topicStr.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                            title = topicStr,
                            shortDescription = "Core curriculum module in ${phase.title}",
                            estimatedMinutes = 30,
                            prerequisiteTopicIds = prereqs,
                            status = status,
                            masteryLevel = TopicMasteryLevel.UNKNOWN,
                            priorityScore = priority,
                            accelerated = false,
                            explanation = phase.objective
                        )
                    )

                    prereqs.forEach { p ->
                        synthesizedEdges.add(PersonalizedRoadmapEdge(fromTopicId = p, toTopicId = topicId))
                    }
                    previousTopicId = topicId
                }
            } else if (phase.milestones.isNotEmpty()) {
                phase.milestones.forEachIndexed { mIdx, milestone ->
                    val topicId = "${id}_p${pIdx}_m${mIdx}"
                    val isDone = milestone.isCompleted ||
                        completedMilestones.contains(topicId) ||
                        completedMilestones.contains(milestone.title)
                    val prereqs = if (previousTopicId != null) listOf(previousTopicId!!) else emptyList()
                    val status = if (isDone) PersonalizedTopicStatus.COMPLETED
                    else if (prereqs.isEmpty()) PersonalizedTopicStatus.UNLOCKED
                    else PersonalizedTopicStatus.LOCKED
                    val priority = (100 - (pIdx * 20 + mIdx * 5)).coerceAtLeast(1)

                    synthesizedTopics.add(
                        PersonalizedRoadmapTopic(
                            id = topicId,
                            topicTag = milestone.title.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                            title = milestone.title,
                            shortDescription = milestone.assessment.ifBlank { "Milestone for ${phase.title}" },
                            estimatedMinutes = 45,
                            prerequisiteTopicIds = prereqs,
                            status = status,
                            masteryLevel = TopicMasteryLevel.UNKNOWN,
                            priorityScore = priority,
                            accelerated = false,
                            explanation = milestone.passingCriteria
                        )
                    )

                    prereqs.forEach { p ->
                        synthesizedEdges.add(PersonalizedRoadmapEdge(fromTopicId = p, toTopicId = topicId))
                    }
                    previousTopicId = topicId
                }
            }
        }

        val parsedWeeks = duration.filter { it.isDigit() }.toIntOrNull()
            ?: phases.sumOf { it.durationWeeks }.coerceAtLeast(1)
        val parsedHours = weeklyHours.toInt().coerceAtLeast(1)

        val completedSet = synthesizedTopics
            .filter { it.status == PersonalizedTopicStatus.COMPLETED }
            .map { it.id }
            .toSet()

        return copy(
            studyHoursPerWeek = if (studyHoursPerWeek > 0) studyHoursPerWeek else parsedHours,
            estimatedWeeks = if (estimatedWeeks > 0) estimatedWeeks else parsedWeeks,
            domain = if (domain.isNotBlank() && domain != "Python") domain else (goal.ifBlank { title }),
            topics = synthesizedTopics,
            edges = synthesizedEdges,
            completedTopicIds = completedTopicIds + completedSet
        ).withRecomputedStatuses()
    }
}

// Legacy preview for backward compatibility
data class GeneratedRoadmapPreview(
    val goal: String,
    val level: String,
    val studyTime: String,
    val duration: String,
    val stages: List<String>
)
