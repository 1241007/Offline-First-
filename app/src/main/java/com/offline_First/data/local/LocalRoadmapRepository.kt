package com.offline_First.data.local

import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.AssessmentSessionState
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.domain.model.RoadmapAccentTheme
import com.offline_First.domain.model.RoadmapOption
import com.offline_First.domain.model.TopicLearningContent
import com.offline_First.domain.model.TopicReviewResult
import com.offline_First.domain.model.TopicAssessmentResult
import kotlinx.coroutines.delay

class LocalRoadmapRepository : RoadmapRepository {

    private val categories = listOf(
        "All",
        "Development",
        "Data & AI",
        "Mobile",
        "Cloud & DevOps",
        "Security"
    )

    private val roadmaps = listOf(
        RoadmapOption(
            id = "fe-dev",
            title = "Frontend Developer",
            category = "Development",
            description = "Build polished interfaces and production-ready web apps.",
            skills = listOf("HTML", "CSS", "JavaScript", "React"),
            level = "Beginner",
            duration = "8–10 weeks",
            stages = 6,
            icon = "FE",
            accentTheme = RoadmapAccentTheme.PRIMARY
        ),
        RoadmapOption(
            id = "be-dev",
            title = "Backend Developer",
            category = "Development",
            description = "Create APIs, databases, and backend systems students can ship.",
            skills = listOf("Node.js", "APIs", "Databases", "Security"),
            level = "Intermediate",
            duration = "10–12 weeks",
            stages = 7,
            icon = "BE",
            accentTheme = RoadmapAccentTheme.SECONDARY
        ),
        RoadmapOption(
            id = "fs-dev",
            title = "Full Stack Developer",
            category = "Development",
            description = "Move from UI work to complete product development workflows.",
            skills = listOf("React", "Node", "Databases", "DevOps"),
            level = "Intermediate",
            duration = "12–14 weeks",
            stages = 8,
            icon = "FS",
            accentTheme = RoadmapAccentTheme.SUCCESS
        ),
        RoadmapOption(
            id = "ds-sci",
            title = "Data Scientist",
            category = "Data & AI",
            description = "Learn data analysis, modeling, and decision-making with real datasets.",
            skills = listOf("Python", "SQL", "Statistics", "ML"),
            level = "Beginner",
            duration = "10–12 weeks",
            stages = 7,
            icon = "DS",
            accentTheme = RoadmapAccentTheme.SECONDARY
        ),
        RoadmapOption(
            id = "ml-eng",
            title = "Machine Learning Engineer",
            category = "Data & AI",
            description = "Master model training, evaluation, and deployment for AI products.",
            skills = listOf("Python", "ML", "PyTorch", "Math"),
            level = "Intermediate",
            duration = "12–16 weeks",
            stages = 9,
            icon = "ML",
            accentTheme = RoadmapAccentTheme.ACCENT
        ),
        RoadmapOption(
            id = "android-dev",
            title = "Android Developer",
            category = "Mobile",
            description = "Build user-friendly mobile apps with Kotlin and Jetpack Compose.",
            skills = listOf("Kotlin", "Compose", "UI", "Testing"),
            level = "Beginner",
            duration = "8–10 weeks",
            stages = 6,
            icon = "AD",
            accentTheme = RoadmapAccentTheme.SECONDARY
        ),
        RoadmapOption(
            id = "devops-eng",
            title = "DevOps Engineer",
            category = "Cloud & DevOps",
            description = "Learn automation, deployment flow, and cloud-first engineering habits.",
            skills = listOf("Linux", "CI/CD", "Cloud", "Containers"),
            level = "Intermediate",
            duration = "10–12 weeks",
            stages = 7,
            icon = "DO",
            accentTheme = RoadmapAccentTheme.ACCENT
        ),
        RoadmapOption(
            id = "data-analyst",
            title = "Data Analyst",
            category = "Data & AI",
            description = "Turn messy numbers into trends, dashboards, and smart business decisions.",
            skills = listOf("Excel", "SQL", "Tableau", "Insights"),
            level = "Beginner",
            duration = "6–8 weeks",
            stages = 5,
            icon = "DA",
            accentTheme = RoadmapAccentTheme.SECONDARY
        ),
        RoadmapOption(
            id = "cybersec",
            title = "Cybersecurity",
            category = "Security",
            description = "Build the fundamentals of secure systems, networks, and threat awareness.",
            skills = listOf("Networking", "Security", "Ethical Hacking", "Monitoring"),
            level = "Intermediate",
            duration = "10–12 weeks",
            stages = 7,
            icon = "CY",
            accentTheme = RoadmapAccentTheme.PRIMARY
        )
    )

    override suspend fun getRoadmaps(
        limit: Int?,
        offset: Int,
        category: String?
    ): Result<List<RoadmapOption>> {
        val filtered = if (category != null && !category.equals("All", ignoreCase = true)) {
            roadmaps.filter { it.category.equals(category, ignoreCase = true) }
        } else {
            roadmaps
        }
        val paged = if (limit != null) {
            filtered.drop(offset).take(limit)
        } else if (offset > 0) {
            filtered.drop(offset)
        } else {
            filtered
        }
        return Result.success(paged)
    }

    override suspend fun getCategories(): List<String> {
        return categories
    }

    override suspend fun validateSubject(subject: String): Result<Pair<Boolean, String>> =
        Result.success(Pair(true, subject.trim()))

    override suspend fun startPersonalizedAssessment(domain: String): Result<AssessmentSessionState> =
        Result.success(LocalPersonalizedRoadmapEngine.startAssessment(domain))

    override suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?> =
        Result.success(LocalPersonalizedRoadmapEngine.getActiveSession())

    override suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState> =
        LocalPersonalizedRoadmapEngine.getSession(sessionId)?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Session not found"))

    override suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int?
    ): Result<AssessmentSessionState> =
        Result.success(LocalPersonalizedRoadmapEngine.submitAnswer(sessionId, answer, quizSelectedIndex))

    override suspend fun correctProfileField(
        sessionId: String,
        field: String,
        value: String
    ): Result<AssessmentSessionState> =
        Result.success(LocalPersonalizedRoadmapEngine.correctProfileField(sessionId, field, value))

    override suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail> =
        Result.success(LocalPersonalizedRoadmapEngine.generateRoadmap(sessionId))

    override suspend fun getMyPersonalizedRoadmaps(
        limit: Int?,
        offset: Int
    ): Result<List<PersonalizedRoadmapDetail>> =
        Result.success(LocalPersonalizedRoadmapEngine.getSavedRoadmaps())

    override fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail> =
        LocalPersonalizedRoadmapEngine.getSavedRoadmaps()

    override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail> =
        LocalPersonalizedRoadmapEngine.getRoadmap(roadmapId)?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Roadmap not found locally"))

    override suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail> =
        Result.success(LocalPersonalizedRoadmapEngine.saveRoadmap(roadmap))

    override suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit> {
        LocalPersonalizedRoadmapEngine.deleteRoadmap(roadmapId)
        return Result.success(Unit)
    }

    override suspend fun renamePersonalizedRoadmap(roadmapId: String, newTitle: String): Result<PersonalizedRoadmapDetail> =
        LocalPersonalizedRoadmapEngine.renameRoadmap(roadmapId, newTitle)?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Roadmap not found locally"))

    override suspend fun getCompletedMilestones(roadmapId: String): Set<String> =
        emptySet()

    override suspend fun toggleMilestoneProgress(roadmapId: String, milestoneKey: String): Result<Set<String>> =
        Result.success(emptySet())

    override suspend fun syncPendingMilestoneProgress(): Result<Unit> =
        Result.success(Unit)

    override suspend fun syncPendingRoadmaps(): Result<Unit> =
        Result.success(Unit)

    override suspend fun getTopicLearningContent(roadmapId: String, topicId: String): Result<TopicLearningContent> = runCatching {
        LocalPersonalizedRoadmapEngine.getTopicLearningContent(roadmapId, topicId)
    }

    override suspend fun reviewTopic(roadmapId: String, topicId: String): Result<TopicReviewResult> = runCatching {
        LocalPersonalizedRoadmapEngine.reviewTopic(roadmapId, topicId)
    }

    override suspend fun submitTopicAssessment(
        roadmapId: String,
        topicId: String,
        answers: List<Int>,
        hintsUsed: Int,
        solutionRevealed: Boolean
    ): Result<TopicAssessmentResult> = runCatching {
        LocalPersonalizedRoadmapEngine.submitTopicAssessment(roadmapId, topicId, answers, hintsUsed, solutionRevealed)
    }

    override suspend fun completeTopic(roadmapId: String, topicId: String, quizPassed: Boolean): Result<PersonalizedRoadmapDetail> = runCatching {
        LocalPersonalizedRoadmapEngine.completeTopic(roadmapId, topicId, quizPassed)
    }

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> {
        delay(600L) // Simulate quick offline generation
        return Result.success(
            GeneratedRoadmapPreview(
                goal = goal,
                level = level,
                studyTime = studyTime,
                duration = "10–12 weeks",
                stages = listOf(
                    "Foundations & Core Principles",
                    "Essential Tooling & Environment Setup",
                    "Practical Core Skills & Project Work",
                    "Intermediate Concepts & Architecture",
                    "Capstone Project & Portfolio Build"
                )
            )
        )
    }
}
