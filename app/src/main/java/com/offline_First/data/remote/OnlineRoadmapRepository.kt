package com.offline_First.data.remote

import android.content.Context
import com.offline_First.data.local.ChatCacheDatabase
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.*

class OnlineRoadmapRepository(
    context: Context? = null,
    private val userIdProvider: () -> String = { "default_user" }
) : RoadmapRepository {

    val cacheDb: ChatCacheDatabase? = context?.let { ChatCacheDatabase(it.applicationContext) }

    private val currentUserId: String
        get() = userIdProvider().ifBlank { "default_user" }

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
            skillTested = skillTested
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
        return AssessmentSessionState(
            id = id,
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
            messages = messages.map { it.toDomain() }
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
            unknowns = unknowns
        )
    }

    private fun PersonalizedRoadmapDetailResponseDto.toDomain(): PersonalizedRoadmapDetail {
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
            createdAt = createdAt
        )
    }

    override suspend fun getRoadmaps(
        limit: Int?,
        offset: Int,
        category: String?
    ): Result<List<RoadmapOption>> {
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
                if (cached.isNotEmpty()) {
                    if (category != null) {
                        cached.filter { it.category.equals(category, ignoreCase = true) }
                    } else {
                        cached
                    }
                } else {
                    throw error
                }
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

    // --- Personalized Roadmap Assessment & Generation ---

    override suspend fun startPersonalizedAssessment(): Result<AssessmentSessionState> {
        return ChatApiClient.startPersonalizedAssessment()
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
            unknowns = assessmentSummary.unknowns
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
}


