package com.offline_First.data.repository

import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.RoadmapOption

/**
 * Repository interface for roadmap queries and generation.
 * Decouples presentation from whether data originates from local cache or remote API.
 */
interface RoadmapRepository {
    suspend fun getRoadmaps(): Result<List<RoadmapOption>>
    suspend fun getCategories(): List<String>
    suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview>
}
