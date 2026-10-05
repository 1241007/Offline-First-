package com.offline_First.data.remote

import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.RoadmapOption
import com.offline_First.domain.model.RoadmapAccentTheme

class OnlineRoadmapRepository : RoadmapRepository {

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
            accentTheme = mapAccentTheme(accentTheme)
        )
    }

    override suspend fun getRoadmaps(): Result<List<RoadmapOption>> {
        return ChatApiClient.getRoadmaps()
            .map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getCategories(): List<String> {
        return ChatApiClient.getRoadmapCategories().getOrElse { emptyList() }
    }

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> {
        // Not implemented in Phase 1
        return Result.failure(UnsupportedOperationException("Personalized roadmap generation not yet implemented"))
    }
}
