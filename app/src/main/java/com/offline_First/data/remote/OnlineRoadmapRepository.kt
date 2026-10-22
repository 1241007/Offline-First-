package com.offline_First.data.remote

import android.content.Context
import android.util.Log
import com.offline_First.data.local.ChatCacheDatabase
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.*

class OnlineRoadmapRepository(
    context: Context? = null,
    private val userIdProvider: () -> String = { "default_user" },
    val cacheDb: ChatCacheDatabase? = context?.let { ChatCacheDatabase(it.applicationContext) },
    private val tokenProvider: () -> String? = { null },
    private val isOnlineProvider: () -> Boolean = { true }
) : RoadmapRepository {

    val isOnline: Boolean
        get() = isOnlineProvider()

    private val currentUserId: String
        get() = userIdProvider().ifBlank { "default_user" }

    // In-memory cache for fast UI rendering between refreshes
    private val _roadmapCache = mutableListOf<PersonalizedRoadmapDetail>()

    // Local milestone progress store (keyed: "$roadmapId/$milestoneKey")
    // Replaced with backend sync in syncPendingMilestoneProgress()
    private val _localMilestones = mutableMapOf<String, MutableSet<String>>()

    // -----------------------------------------------------------------------
    // Static roadmap catalogue
    // -----------------------------------------------------------------------

    private fun mapAccentTheme(theme: String): RoadmapAccentTheme {
        return when (theme.lowercase()) {
            "secondary", "purple", "teal", "orange" -> RoadmapAccentTheme.SECONDARY
            "accent", "red" -> RoadmapAccentTheme.ACCENT
            "success", "green" -> RoadmapAccentTheme.SUCCESS
            else -> RoadmapAccentTheme.PRIMARY
        }
    }

    private fun RoadmapDto.toDomain(): RoadmapOption {
        return RoadmapOption(
            id = id,
            title = title,
            category = category,
            description = description,
            skills = skills,
            level = level,
            duration = duration,
            stages = stages,
            icon = icon,
            accentTheme = mapAccentTheme(accentTheme),
            isPersonalized = false
        )
    }

    private fun AssessmentQuizPublicDto.toDomain(): DiagnosticQuiz {
        return DiagnosticQuiz(
            question = question,
            options = options,
            skillTested = effectiveSkillTested,
            difficulty = difficulty,
            selectionRationale = effectiveSelectionRationale
        )
    }

    private fun PersonalizedTaskDto.toDomain(): PersonalizedTask {
        return PersonalizedTask(
            id = id,
            title = title,
            description = description ?: "",
            instructions = instructions,
            estimatedHours = estimatedHours,
            resources = resources,
            completionCriteria = completionCriteria,
            isCompleted = isCompleted,
            dependencies = dependencies,
            topicId = topicId,
            status = status,
            subtopics = subtopics,
            learningObjectives = learningObjectives,
            codeExample = codeExample,
            skipReason = skipReason
        )
    }

    private fun SkillGapItemDto.toDomain(): SkillGapItem {
        return SkillGapItem(
            skill = skill,
            status = status,
            source = source,
            confidence = confidence,
            rationale = rationale
        )
    }

    private fun AssessmentMessageItemDto.toDomain(): AssessmentMessage {
        val spk = if (sender.lowercase() == "mentor" || sender.lowercase() == "assistant") {
            AssessmentSpeaker.MENTOR
        } else {
            AssessmentSpeaker.LEARNER
        }
        return AssessmentMessage(
            speaker = spk,
            text = text,
            options = options,
            quiz = quiz?.toDomain(),
            stepId = stepId,
            timestamp = timestamp
        )
    }

    private fun AssessmentSessionResponseDto.toDomain(): AssessmentSessionState {
        val isDone = state.equals("READY_FOR_GENERATION", ignoreCase = true) ||
            currentStepId.equals("READY_FOR_GENERATION", ignoreCase = true) ||
            state.equals("SUMMARY", ignoreCase = true) ||
            completenessPercentage >= 100 ||
            !roadmapId.isNullOrBlank()

        val determinedPhase = when {
            isDone || state.equals("SUMMARY", ignoreCase = true) || currentStepId.equals("STAGE_REVIEW", ignoreCase = true) ->
                AssessmentPhase.SUMMARY
            quiz != null || currentStepId?.contains("SKILL", ignoreCase = true) == true ->
                AssessmentPhase.DIAGNOSTIC
            else ->
                AssessmentPhase.PROFILE_COLLECTION
        }

        val domainName = this.domain.takeIf { it.isNotBlank() }
            ?: goal?.takeIf { it.isNotBlank() }
            ?: "Python"

        val effectivePrompt = quiz?.question
            ?: latestMessage
            ?: messages.lastOrNull { it.sender.lowercase() == "mentor" || it.sender.lowercase() == "assistant" }?.text
            ?: "Please select an option to continue:"

        val effectiveOptions = when {
            quiz != null && quiz.options.isNotEmpty() -> quiz.options
            options.isNotEmpty() -> options
            else -> messages.lastOrNull { it.sender.lowercase() == "mentor" || it.sender.lowercase() == "assistant" }?.options ?: emptyList()
        }

        val question = if (determinedPhase == AssessmentPhase.SUMMARY) {
            null
        } else {
            AssessmentQuestion(
                id = "${currentStepId ?: state}_${completedSteps.size}_${quiz?.effectiveSkillTested ?: effectivePrompt.hashCode()}",
                topicTag = quiz?.effectiveSkillTested ?: currentStepId ?: "general",
                questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
                prompt = effectivePrompt,
                options = effectiveOptions,
                difficultyLevel = when (quiz?.difficulty?.lowercase()) {
                    "advanced" -> 3
                    "intermediate" -> 2
                    else -> 1
                },
                explanation = quiz?.effectiveSelectionRationale ?: ""
            )
        }

        val qNum = (completedSteps.size + 1).coerceAtMost(5)

        return AssessmentSessionState(
            id = id,
            sessionId = id,
            state = state,
            currentStepId = currentStepId,
            completedSteps = completedSteps,
            goal = goal,
            targetLevel = targetLevel,
            targetTimeline = targetTimeline,
            weeklyHours = weeklyHours,
            latestMessage = latestMessage,
            options = options,
            quiz = quiz?.toDomain(),
            completenessPercentage = completenessPercentage,
            roadmapId = roadmapId,
            messages = messages.map { it.toDomain() },
            phase = determinedPhase,
            domain = domainName,
            currentQuestion = question,
            questionNumber = qNum,
            totalQuestions = 5,
            isComplete = isDone
        )
    }

    private fun PersonalizedMilestoneDto.toDomain(): PersonalizedMilestone {
        return PersonalizedMilestone(
            title = title,
            completionCriteria = completionCriteria,
            assessment = assessment,
            passingCriteria = passingCriteria
        )
    }

    private fun PersonalizedPhaseDto.toDomain(): PersonalizedPhase {
        return PersonalizedPhase(
            title = title,
            objective = objective,
            durationWeeks = durationWeeks,
            topics = topics,
            tasks = tasks.map { it.toDomain() },
            activities = activities,
            resources = resources,
            milestones = milestones.map { it.toDomain() },
            recommendedCourseIds = recommendedCourseIds
        )
    }

    private fun WeeklyScheduleItemDto.toDomain(): WeeklyScheduleItem {
        return WeeklyScheduleItem(
            dayOrWeek = dayOrWeek,
            focusTopic = focusTopic,
            estimatedHours = estimatedHours,
            tasks = tasks
        )
    }

    private fun AssessmentSummaryDto.toDomain(): AssessmentSummary {
        return AssessmentSummary(
            strengths = strengths,
            skillGaps = skillGaps,
            verifiedEvidence = verifiedEvidence,
            selfReportedInformation = selfReportedInformation,
            unknowns = unknowns,
            skillGapBreakdown = skillGapBreakdown.map { it.toDomain() },
            curriculumRationale = curriculumRationale
        )
    }

    private fun PersonalizedRoadmapDetailResponseDto.toDomain(): PersonalizedRoadmapDetail {
        val mappedTopics = structure.topics.map { dto ->
            PersonalizedRoadmapTopic(
                id = dto.id,
                topicTag = dto.id,
                title = dto.title,
                shortDescription = dto.description,
                estimatedMinutes = (dto.estimatedHours * 60).toInt().coerceAtLeast(15),
                prerequisiteTopicIds = dto.prerequisiteTopicIds,
                status = when (dto.status.uppercase()) {
                    "COMPLETED" -> PersonalizedTopicStatus.COMPLETED
                    "SKIPPED" -> PersonalizedTopicStatus.SKIPPED
                    "IN_PROGRESS" -> PersonalizedTopicStatus.IN_PROGRESS
                    "UNLOCKED" -> PersonalizedTopicStatus.UNLOCKED
                    else -> PersonalizedTopicStatus.LOCKED
                },
                explanation = dto.skipReason ?: dto.description
            )
        }
        val mappedEdges = structure.edges.map { dto ->
            PersonalizedRoadmapEdge(fromTopicId = dto.fromTopicId, toTopicId = dto.toTopicId)
        }

        return PersonalizedRoadmapDetail(
            id = id,
            title = title,
            goal = goal,
            category = category,
            level = level,
            duration = duration,
            stages = stages,
            icon = icon,
            accentTheme = mapAccentTheme(accentTheme),
            weeklyHours = structure.weeklyHours,
            assessmentSummary = structure.assessmentSummary.toDomain(),
            phases = structure.phases.map { it.toDomain() },
            weeklySchedule = structure.weeklySchedule.map { it.toDomain() },
            assumptions = structure.assumptions,
            capstoneProject = structure.capstoneProject,
            nextAction = structure.nextAction,
            completedMilestones = structure.completedMilestones,
            createdAt = createdAt,
            domain = structure.domain ?: (goal.ifBlank { title }),
            topics = mappedTopics,
            edges = mappedEdges
        ).withSynthesizedTopicsIfEmpty()
    }

    override suspend fun getRoadmaps(
        limit: Int?,
        offset: Int,
        category: String?
    ): Result<List<RoadmapOption>> {
        val uId = currentUserId
        if (!isOnline) {
            val offlineRoadmaps = cacheDb?.getSavedRoadmaps(uId).orEmpty()
            val filtered = if (category != null) {
                offlineRoadmaps.filter { it.category.equals(category, ignoreCase = true) }
            } else {
                offlineRoadmaps
            }
            return Result.success(if (offset == 0) filtered else emptyList())
        }

        val db = cacheDb
        val cached = db?.getSystemRoadmaps(currentUserId, category) ?: emptyList()

        return ChatApiClient.getRoadmaps(category = category, limit = limit, offset = offset)
            .map { dtos ->
                val list = dtos.map { it.toDomain() }
                if (offset == 0 && category == null && db != null) {
                    db.upsertSystemRoadmaps(list, currentUserId)
                }
                list
            }
            .recoverCatching { error ->
                val offlineRoadmaps = db?.getSavedRoadmaps(uId).orEmpty()
                val filtered = if (category != null) {
                    offlineRoadmaps.filter { it.category.equals(category, ignoreCase = true) }
                } else {
                    offlineRoadmaps
                }
                if (offset == 0) filtered else emptyList()
            }
    }

    override suspend fun getCategories(): List<String> {
        val db = cacheDb
        val cached = db?.getRoadmapCategories() ?: emptyList()

        return ChatApiClient.getRoadmapCategories()
            .map { cats ->
                if (cats.isNotEmpty() && db != null) {
                    db.upsertRoadmapCategories(cats)
                }
                cats
            }
            .getOrElse {
                if (cached.isNotEmpty()) cached else emptyList()
            }
    }

    override suspend fun validateSubject(subject: String): Result<Pair<Boolean, String>> {
        return ChatApiClient.validateSubject(subject)
            .map { Pair(it.isValid, it.standardizedName) }
            .recoverCatching { Pair(true, subject.trim()) }
    }

    override suspend fun startPersonalizedAssessment(domain: String): Result<AssessmentSessionState> {
        return ChatApiClient.startPersonalizedAssessment(domain)
            .map { it.toDomain() }
    }

    override suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?> {
        return ChatApiClient.getActiveAssessmentSession()
            .map { it?.toDomain() }
    }

    override suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState> {
        return ChatApiClient.getAssessmentSession(sessionId)
            .map { it.toDomain() }
    }

    override suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int?
    ): Result<AssessmentSessionState> {
        return ChatApiClient.submitAssessmentAnswer(
            sessionId = sessionId,
            answer = answer,
            quizSelectedIndex = quizSelectedIndex
        ).map { it.toDomain() }
    }

    override suspend fun correctProfileField(
        sessionId: String,
        field: String,
        value: String
    ): Result<AssessmentSessionState> {
        return ChatApiClient.correctProfileField(
            sessionId = sessionId,
            field = field,
            value = value
        ).map { it.toDomain() }
    }

    override suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail> {
        return ChatApiClient.generatePersonalizedRoadmap(sessionId)
            .map { dto ->
                val domain = dto.toDomain()
                cacheDb?.upsertPersonalizedRoadmaps(listOf(domain), currentUserId, syncStatus = "synced")
                domain
            }
    }

    override fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail> {
        return cacheDb?.getPersonalizedRoadmaps(currentUserId) ?: emptyList()
    }

    override suspend fun getMyPersonalizedRoadmaps(
        limit: Int?,
        offset: Int
    ): Result<List<PersonalizedRoadmapDetail>> {
        val db = cacheDb
        val cached = db?.getPersonalizedRoadmaps(currentUserId) ?: emptyList()

        // Sync any queued offline changes in background (both pending milestones and pending roadmaps)
        runCatching {
            syncPendingRoadmaps()
            syncPendingMilestoneProgress()
        }

        return ChatApiClient.getMyPersonalizedRoadmaps(limit = limit, offset = offset)
            .map { dtos ->
                val list = dtos.map { it.toDomain() }
                if (db != null) {
                    db.upsertPersonalizedRoadmaps(list, currentUserId, syncStatus = "synced")
                    db.getPersonalizedRoadmaps(currentUserId)
                } else {
                    list
                }
            }
            .recoverCatching { error ->
                if (cached.isNotEmpty()) {
                    cached
                } else {
                    throw error
                }
            }
    }

    override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail> {
        val db = cacheDb
        val cached = db?.getPersonalizedRoadmapById(roadmapId, currentUserId)

        return ChatApiClient.getPersonalizedRoadmapDetail(roadmapId)
            .map { dto ->
                val domain = dto.toDomain()
                db?.upsertPersonalizedRoadmaps(listOf(domain), currentUserId, syncStatus = "synced")
                domain
            }
            .recoverCatching { error ->
                cached ?: throw error
            }
    }

    override suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail> {
        val db = cacheDb
        // 1. Save immediately to local persistent SQLite with pending_sync status
        db?.savePersonalizedRoadmap(roadmap, currentUserId, syncStatus = "pending_sync")

        // 2. Attempt immediate cloud synchronization
        runCatching {
            val req = roadmap.toSaveRequestDto()
            val resp = ChatApiClient.savePersonalizedRoadmap(req).getOrThrow()
            val domain = resp.toDomain()
            db?.savePersonalizedRoadmap(domain, currentUserId, syncStatus = "synced")
            domain
        }.onFailure {
            // Local state remains durably saved with sync_status = 'pending_sync'
        }

        val local = db?.getPersonalizedRoadmapById(roadmap.id, currentUserId) ?: roadmap
        return Result.success(local)
    }

    override suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit> {
        val db = cacheDb
        db?.deletePersonalizedRoadmap(roadmapId, currentUserId)
        runCatching {
            ChatApiClient.deletePersonalizedRoadmap(roadmapId).getOrThrow()
        }
        return Result.success(Unit)
    }

    override suspend fun renamePersonalizedRoadmap(
        roadmapId: String,
        newTitle: String
    ): Result<PersonalizedRoadmapDetail> {
        val db = cacheDb
        val cleanTitle = newTitle.trim().ifEmpty { "Personalized Roadmap" }
        db?.updatePersonalizedRoadmapTitle(roadmapId, cleanTitle, currentUserId)

        runCatching {
            val updatedDto = ChatApiClient.renamePersonalizedRoadmap(roadmapId, cleanTitle).getOrThrow()
            val domain = updatedDto.toDomain()
            db?.savePersonalizedRoadmap(domain, currentUserId, syncStatus = "synced")
            return Result.success(domain)
        }
        val fallback = db?.getPersonalizedRoadmapById(roadmapId, currentUserId)
            ?: PersonalizedRoadmapDetail(id = roadmapId, title = cleanTitle)
        return Result.success(fallback)
    }

    override suspend fun getTopicLearningContent(roadmapId: String, topicId: String): Result<TopicLearningContent> {
        val roadmap = cacheDb?.getPersonalizedRoadmapById(roadmapId, currentUserId)?.withSynthesizedTopicsIfEmpty()
            ?: _roadmapCache.find { it.id == roadmapId }?.withSynthesizedTopicsIfEmpty()
            ?: return Result.failure(IllegalArgumentException("Roadmap $roadmapId not found"))

        val topic = roadmap.topics.find { it.id == topicId }
            ?: return Result.failure(IllegalArgumentException("Topic $topicId not found in roadmap"))

        val associatedTask = roadmap.phases.flatMap { it.tasks }.find { it.id == topicId || it.title == topic.title }
        val associatedPhase = roadmap.phases.find { it.tasks.any { t -> t.id == topicId || t.title == topic.title } }

        val objectives = buildList {
            if (associatedTask?.instructions?.isNotBlank() == true) add(associatedTask.instructions)
            if (associatedTask?.completionCriteria?.isNotBlank() == true) add(associatedTask.completionCriteria)
            if (isEmpty()) {
                add("Master the foundational principles of ${topic.title}")
                add("Apply practical patterns to solve real-world problems")
                add("Verify edge cases, error handling, and performance")
            }
        }

        val explanation = associatedTask?.description?.takeIf { it.isNotBlank() }
            ?: topic.shortDescription.takeIf { it.isNotBlank() }
            ?: "This module provides focused, practical training on ${topic.title} tailored for ${roadmap.level} proficiency."

        val concepts = associatedTask?.instructions?.takeIf { it.isNotBlank() }
            ?: associatedPhase?.objective?.takeIf { it.isNotBlank() }
            ?: "Follow step-by-step guidance and architectural best practices to implement ${topic.title}."

        val codeSnippet = """
            # ${topic.title} - Implementation Lab
            def execute_${topic.topicTag}():
                print("Running verified lab for: ${topic.title}")
                # Core logic implementation
                status = "COMPLETED"
                return {"module": "${topic.title}", "status": status}
            
            result = execute_${topic.topicTag}()
            print("Execution result:", result)
        """.trimIndent()

        val content = TopicLearningContent(
            topicId = topic.id,
            topicTitle = topic.title,
            domain = roadmap.goal.ifBlank { roadmap.title },
            learningObjectives = objectives,
            beginnerExplanation = explanation,
            syntaxAndConcepts = concepts,
            codeExamples = listOf(
                TopicCodeExample(
                    title = "${topic.title} Demonstration",
                    code = codeSnippet,
                    explanation = "A complete, verified demonstration illustrating key workflows for ${topic.title}.",
                    expectedOutput = "Running verified lab for: ${topic.title}\nExecution result: {'module': '${topic.title}', 'status': 'COMPLETED'}"
                )
            ),
            commonMistakes = listOf(
                TopicCommonMistake(
                    mistake = "Skipping prerequisite validation",
                    whyItHappens = "Moving forward before verifying foundational syntax or environment dependencies.",
                    howToFix = "Review prerequisite concepts and run automated checks before deploying."
                )
            ),
            practiceQuiz = TopicPracticeQuiz(
                question = "What is the primary role of ${topic.title} in ${roadmap.goal.ifBlank { roadmap.title }}?",
                options = listOf(
                    "Reinforces essential domain competencies and structured execution",
                    "Disables error monitoring and exception logging",
                    "Bypasses automated validation checks",
                    "None of the above"
                ),
                correctOptionIndex = 0,
                explanation = "Correct! ${topic.title} establishes reliable patterns and verified competency."
            ),
            resources = (associatedTask?.resources ?: emptyList()).map { res ->
                TopicLearningResource(
                    title = res,
                    url = "https://docs.edunova.dev",
                    sourceName = "Curriculum Resource",
                    description = "Official curriculum reference for ${topic.title}."
                )
            }.ifEmpty {
                listOf(
                    TopicLearningResource(
                        title = "${topic.title} Reference Guide",
                        url = "https://docs.edunova.dev",
                        sourceName = "EduNova Curriculum",
                        description = "In-depth guide and reference architecture for ${topic.title}."
                    )
                )
            }
        )
        return Result.success(content)
    }

    override suspend fun completeTopic(
        roadmapId: String,
        topicId: String,
        quizPassed: Boolean
    ): Result<PersonalizedRoadmapDetail> {
        val roadmap = cacheDb?.getPersonalizedRoadmapById(roadmapId, currentUserId)?.withSynthesizedTopicsIfEmpty()
            ?: _roadmapCache.find { it.id == roadmapId }?.withSynthesizedTopicsIfEmpty()
            ?: return Result.failure(IllegalArgumentException("Roadmap $roadmapId not found"))

        val completed = roadmap.completedTopicIds.toMutableSet().apply { add(topicId) }
        val inProgress = roadmap.inProgressTopicIds.toMutableSet().apply { remove(topicId) }

        val updatedRoadmap = roadmap.copy(
            completedTopicIds = completed,
            inProgressTopicIds = inProgress,
            updatedAtMillis = System.currentTimeMillis()
        ).withRecomputedStatuses()

        // Cache locally immediately
        cacheDb?.savePersonalizedRoadmap(updatedRoadmap, currentUserId, syncStatus = "synced")
        cacheDb?.saveMilestoneProgress(roadmapId, currentUserId, milestoneKey = topicId, isCompleted = true, syncStatus = "synced")

        // Sync with backend asynchronously
        runCatching {
            ChatApiClient.syncRoadmapMilestones(roadmapId, milestoneKey = topicId, isCompleted = true)
        }

        return Result.success(updatedRoadmap)
    }


    override suspend fun syncPendingRoadmaps(): Result<Unit> {
        val db = cacheDb ?: return Result.success(Unit)
        val pendingRoadmaps = db.getUnsyncedPersonalizedRoadmaps(currentUserId)
        if (pendingRoadmaps.isEmpty()) return Result.success(Unit)

        for (roadmap in pendingRoadmaps) {
            runCatching {
                val req = roadmap.toSaveRequestDto()
                val resp = ChatApiClient.savePersonalizedRoadmap(req).getOrThrow()
                db.markPersonalizedRoadmapSynced(roadmap.id, currentUserId)
            }
        }
        return Result.success(Unit)
    }

    // --- Offline-First Milestone Progress & Cloud Sync ---

    override suspend fun getCompletedMilestones(roadmapId: String): Set<String> {
        val db = cacheDb ?: return emptySet()
        return db.getCompletedMilestones(roadmapId, currentUserId)
    }

    override suspend fun toggleMilestoneProgress(
        roadmapId: String,
        milestoneKey: String
    ): Result<Set<String>> {
        val db = cacheDb
        val currentSet = db?.getCompletedMilestones(roadmapId, currentUserId) ?: emptySet()
        val isCompleted = !currentSet.contains(milestoneKey)

        // 1. Immediately persist change locally with pending_sync status
        db?.saveMilestoneProgress(
            roadmapId = roadmapId,
            userId = currentUserId,
            milestoneKey = milestoneKey,
            isCompleted = isCompleted,
            syncStatus = "pending_sync"
        )

        val updatedSet = if (isCompleted) currentSet + milestoneKey else currentSet - milestoneKey

        // 2. Attempt immediate cloud synchronization
        runCatching {
            val syncResp = ChatApiClient.syncRoadmapMilestones(
                roadmapId = roadmapId,
                milestoneKey = milestoneKey,
                isCompleted = isCompleted
            ).getOrThrow()
            db?.markMilestonesSynced(roadmapId, currentUserId, listOf(milestoneKey))
            syncResp.completedMilestones.toSet()
        }.onFailure {
            // Remains pending_sync in SQLite and will be synchronized when connectivity returns
        }

        return Result.success(updatedSet)
    }

    override suspend fun syncPendingMilestoneProgress(): Result<Unit> {
        val db = cacheDb ?: return Result.success(Unit)
        val pending = db.getPendingSyncMilestones(currentUserId)
        if (pending.isEmpty()) return Result.success(Unit)

        for ((roadmapId, items) in pending.groupBy { it.roadmapId }) {
            val completedSet = db.getCompletedMilestones(roadmapId, currentUserId)
            runCatching {
                ChatApiClient.syncRoadmapMilestones(
                    roadmapId = roadmapId,
                    completedMilestones = completedSet.toList()
                ).getOrThrow()
                db.markMilestonesSynced(roadmapId, currentUserId, items.map { it.milestoneKey })
            }
        }
        return Result.success(Unit)
    }

    suspend fun syncUnsyncedPersonalizedRoadmaps(): Result<Unit> {
        val db = cacheDb ?: return Result.success(Unit)
        val unsynced = db.getUnsyncedPersonalizedRoadmaps(currentUserId)
        for (roadmap in unsynced) {
            val req = roadmap.toSaveRequestDto()
            val res = ChatApiClient.savePersonalizedRoadmap(req)
            if (res.isSuccess) {
                db.markPersonalizedRoadmapSynced(roadmap.id, currentUserId)
            }
        }
        return Result.success(Unit)
    }

    // -----------------------------------------------------------------------
    // Legacy single-overload (kept for RoadmapBuilderScreen compatibility)
    // -----------------------------------------------------------------------

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> {
        val roadmapId = "pers-offline-${System.currentTimeMillis()}"
        val phases = listOf(
            PersonalizedPhase(
                title = "Phase 1: $interest Core Foundations",
                objective = "Master foundational concepts and tools",
                durationWeeks = 2,
                topics = listOf("$interest Basics", "Environment Setup", "Core Syntax"),
                activities = listOf("Set up development workspace", "Complete initial project"),
                resources = listOf("Official Documentation", "Getting Started Guide"),
                milestones = listOf(
                    PersonalizedMilestone(
                        title = "Complete Foundation Lab",
                        completionCriteria = listOf("Build and run starter application"),
                        assessment = "Self Check",
                        passingCriteria = "Application compiles without errors"
                    )
                )
            ),
            PersonalizedPhase(
                title = "Phase 2: Applied Projects & Architecture",
                objective = "Build scalable, production-ready features",
                durationWeeks = 3,
                topics = listOf("Architecture Patterns", "Data Persistence", "API Integration"),
                activities = listOf("Implement offline storage", "Connect API client"),
                resources = listOf("Best Practices Guide"),
                milestones = listOf(
                    PersonalizedMilestone(
                        title = "Implement Offline Persistence",
                        completionCriteria = listOf("Store state durably offline"),
                        assessment = "Code review",
                        passingCriteria = "Pass all offline tests"
                    )
                )
            ),
            PersonalizedPhase(
                title = "Phase 3: Portfolio & Capstone Delivery",
                objective = "Deliver polished, end-to-end capstone application",
                durationWeeks = 3,
                topics = listOf("Testing", "Optimization", "Deployment"),
                activities = listOf("Write automated unit tests", "Prepare portfolio demo"),
                resources = listOf("Deployment Checklist"),
                milestones = listOf(
                    PersonalizedMilestone(
                        title = "Capstone Project Completion",
                        completionCriteria = listOf("Ship complete project with tests"),
                        assessment = "Capstone Evaluation",
                        passingCriteria = "Complete end-to-end user journey"
                    )
                )
            )
        )

        val detail = PersonalizedRoadmapDetail(
            id = roadmapId,
            title = "$interest Mastery Path",
            goal = goal,
            category = interest,
            level = level,
            duration = "8 weeks",
            stages = phases.size,
            icon = "school",
            accentTheme = RoadmapAccentTheme.PRIMARY,
            weeklyHours = 8.0,
            phases = phases,
            weeklySchedule = listOf(
                WeeklyScheduleItem("Week 1-2", "Core Foundations", 8.0, listOf("Set up environment", "Build starter app")),
                WeeklyScheduleItem("Week 3-5", "Applied Projects", 8.0, listOf("Build persistence layer", "Add network sync")),
                WeeklyScheduleItem("Week 6-8", "Capstone & Polish", 8.0, listOf("Run tests", "Deploy final project"))
            ),
            assumptions = listOf("Basic computer literacy"),
            capstoneProject = "$interest Production Showcase App",
            nextAction = "Start Phase 1: $interest Core Foundations",
            completedMilestones = emptyList()
        )

        // Save locally immediately into SQLite database with pending_sync
        cacheDb?.savePersonalizedRoadmap(detail, currentUserId, syncStatus = "pending_sync")

        return Result.success(
            GeneratedRoadmapPreview(
                goal = goal,
                level = level,
                studyTime = studyTime,
                duration = "8 weeks",
                stages = phases.map { it.title }
            )
        )
    }

    private fun PersonalizedRoadmapDetail.toSaveRequestDto(): SavePersonalizedRoadmapRequestDto {
        val phaseDtos = phases.map { p ->
            PersonalizedPhaseDto(
                title = p.title,
                objective = p.objective,
                durationWeeks = p.durationWeeks,
                topics = p.topics,
                tasks = p.tasks.map { t ->
                    PersonalizedTaskDto(
                        id = t.id,
                        title = t.title,
                        description = t.description,
                        instructions = t.instructions,
                        estimatedHours = t.estimatedHours,
                        resources = t.resources,
                        completionCriteria = t.completionCriteria,
                        isCompleted = t.isCompleted,
                        dependencies = t.dependencies
                    )
                },
                activities = p.activities,
                resources = p.resources,
                milestones = p.milestones.map { m ->
                    PersonalizedMilestoneDto(
                        title = m.title,
                        completionCriteria = m.completionCriteria,
                        assessment = m.assessment,
                        passingCriteria = m.passingCriteria
                    )
                },
                recommendedCourseIds = p.recommendedCourseIds
            )
        }
        val schedDtos = weeklySchedule.map { s ->
            WeeklyScheduleItemDto(
                dayOrWeek = s.dayOrWeek,
                focusTopic = s.focusTopic,
                estimatedHours = s.estimatedHours,
                tasks = s.tasks
            )
        }
        val sumDto = AssessmentSummaryDto(
            strengths = assessmentSummary.strengths,
            skillGaps = assessmentSummary.skillGaps,
            verifiedEvidence = assessmentSummary.verifiedEvidence,
            selfReportedInformation = assessmentSummary.selfReportedInformation,
            unknowns = assessmentSummary.unknowns,
            skillGapBreakdown = assessmentSummary.skillGapBreakdown.map {
                SkillGapItemDto(
                    skill = it.skill,
                    status = it.status,
                    source = it.source,
                    confidence = it.confidence,
                    rationale = it.rationale
                )
            },
            curriculumRationale = assessmentSummary.curriculumRationale
        )
        val structureDto = PersonalizedRoadmapStructureDto(
            title = title,
            goal = goal,
            startingLevel = level,
            category = category,
            estimatedDuration = duration,
            weeklyHours = weeklyHours,
            assessmentSummary = sumDto,
            phases = phaseDtos,
            weeklySchedule = schedDtos,
            assumptions = assumptions,
            capstoneProject = capstoneProject,
            nextAction = nextAction,
            completedMilestones = completedMilestones
        )
        return SavePersonalizedRoadmapRequestDto(
            id = id,
            title = title,
            goal = goal,
            category = category,
            level = level,
            duration = duration,
            icon = icon,
            accentTheme = accentTheme.name.lowercase(),
            structure = structureDto
        )
    }

    // -----------------------------------------------------------------------
    // Saved Roadmaps (System & Personalized)
    // -----------------------------------------------------------------------

    override suspend fun saveRoadmap(roadmapId: String): Result<Boolean> {
        val uId = currentUserId
        val isPersonalized = _roadmapCache.any { it.id == roadmapId } || cacheDb?.getPersonalizedRoadmapById(roadmapId, uId) != null
        val type = if (isPersonalized) "personalized" else "system"

        // Ensure local_system_roadmaps has metadata for system roadmaps
        if (type == "system" && cacheDb != null) {
            val existing = cacheDb.getSystemRoadmaps(uId).find { it.id == roadmapId }
            if (existing == null) {
                val staging = com.offline_First.data.staging.PythonRoadmapStagingData.LEARNING_PATHS.find { it.id == roadmapId }
                val title = staging?.title ?: roadmapId.split("-", "_").joinToString(" ") { part ->
                    part.replaceFirstChar { c -> c.uppercase() }
                }
                val option = RoadmapOption(
                    id = roadmapId,
                    title = title,
                    category = staging?.category ?: "Skill-based",
                    description = staging?.description ?: "$title Learning Path",
                    skills = listOf(title),
                    level = staging?.level ?: "Beginner to Advanced",
                    duration = staging?.duration ?: "8 weeks",
                    stages = staging?.totalModules ?: 6,
                    icon = staging?.iconKey ?: "school",
                    accentTheme = RoadmapAccentTheme.PRIMARY,
                    isPersonalized = false
                )
                cacheDb.upsertSystemRoadmaps(listOf(option), uId)
            }
        }

        // 1. Send remote save request if online
        val remoteResult = if (isOnline) {
            ChatApiClient.saveRoadmap(roadmapId)
        } else {
            Result.failure(java.io.IOException("Offline"))
        }

        // 2. Persist locally in SQLite
        cacheDb?.saveRoadmap(
            userId = uId,
            roadmapId = roadmapId,
            roadmapType = type,
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = if (remoteResult.isSuccess) "synced" else "pending_sync"
        )

        return Result.success(true)
    }

    override suspend fun unsaveRoadmap(roadmapId: String): Result<Boolean> {
        val uId = currentUserId
        val remoteResult = if (isOnline) ChatApiClient.unsaveRoadmap(roadmapId) else Result.failure(java.io.IOException("Offline"))
        if (remoteResult.isSuccess) {
            cacheDb?.deleteSavedRoadmap(uId, roadmapId)
            cacheDb?.unsaveRoadmap(uId, roadmapId)
        } else {
            cacheDb?.queueUnsaveRoadmap(uId, roadmapId)
        }
        return Result.success(true)
    }

    override suspend fun isRoadmapSaved(roadmapId: String): Boolean {
        return cacheDb?.isRoadmapSaved(currentUserId, roadmapId) ?: false
    }

    override suspend fun isRoadmapOfflineReady(roadmapId: String): Boolean {
        return cacheDb?.isRoadmapOfflineReady(currentUserId, roadmapId) ?: false
    }

    override suspend fun getSavedRoadmaps(): Result<List<RoadmapOption>> {
        val uId = currentUserId
        if (!isOnline) {
            val offlineSaved = cacheDb?.getSavedRoadmaps(uId) ?: emptyList()
            return Result.success(offlineSaved)
        }

        // 1. Try remote fetch
        val remoteResult = ChatApiClient.getSavedRoadmaps()
        if (remoteResult.isSuccess) {
            val dtoList = remoteResult.getOrThrow()
            val domainList = dtoList.map { dto ->
                // Cache roadmap saved state in SQLite
                cacheDb?.saveRoadmap(
                    userId = uId,
                    roadmapId = dto.roadmapId,
                    roadmapType = dto.roadmapType,
                    downloadStatus = "completed",
                    isOfflineReady = true,
                    syncStatus = "synced"
                )
                // Cache system roadmap details if type is system
                if (dto.roadmapType == "system" && cacheDb != null) {
                    val sysOption = RoadmapOption(
                        id = dto.roadmapId,
                        title = dto.title,
                        category = dto.category,
                        description = dto.description,
                        skills = dto.skills,
                        level = dto.level,
                        duration = dto.duration,
                        stages = dto.stages,
                        icon = dto.icon,
                        accentTheme = mapAccentTheme(dto.accentTheme),
                        isPersonalized = false
                    )
                    cacheDb.upsertSystemRoadmaps(listOf(sysOption), uId)
                }

                RoadmapOption(
                    id = dto.roadmapId,
                    title = dto.title,
                    category = dto.category,
                    description = dto.description,
                    skills = dto.skills,
                    level = dto.level,
                    duration = dto.duration,
                    stages = dto.stages,
                    icon = dto.icon,
                    accentTheme = mapAccentTheme(dto.accentTheme),
                    isPersonalized = dto.roadmapType.equals("personalized", ignoreCase = true)
                )
            }
            return Result.success(domainList)
        }

        // 2. Offline fallback: load strictly user-saved roadmaps from SQLite
        val offlineSaved = cacheDb?.getSavedRoadmaps(uId) ?: emptyList()
        return Result.success(offlineSaved)
    }

    // -----------------------------------------------------------------------
    // Dedicated WebSocket Methods
    // -----------------------------------------------------------------------

    private val wsClient: RoadmapWebSocketClient by lazy {
        RoadmapWebSocketClient(
            okHttpClient = ChatApiClient.okHttpClient,
            tokenProvider = tokenProvider,
            userIdProvider = { currentUserId }
        )
    }

    override fun isWsConnected(): Boolean = wsClient.isConnected

    override fun observeRoadmapWsEvents(): kotlinx.coroutines.flow.Flow<RoadmapWsEvent> {
        return wsClient.events
    }

    override fun connectRoadmapWs(sessionId: String?) {
        val baseUrl = ChatApiClient.customBaseUrl ?: ChatApiConfig.BASE_URL
        wsClient.connect(baseUrl = baseUrl, sessionId = sessionId)
    }

    override fun sendWsStartRoadmap(domain: String, goal: String?, level: String?, weeklyHours: Float?) {
        wsClient.startRoadmap(domain = domain, goal = goal, level = level, weeklyHours = weeklyHours)
    }

    override fun sendWsSubmitAnswer(sessionId: String, questionId: String?, answerText: String?, quizIndex: Int?) {
        wsClient.submitAnswer(sessionId = sessionId, questionId = questionId, answerText = answerText, quizIndex = quizIndex)
    }

    override fun sendWsGenerateRoadmap(sessionId: String) {
        wsClient.generateRoadmap(sessionId = sessionId)
    }

    override fun disconnectRoadmapWs() {
        wsClient.disconnect()
    }
}


