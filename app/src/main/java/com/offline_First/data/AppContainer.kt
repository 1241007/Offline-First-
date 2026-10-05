package com.offline_First.data

import com.offline_First.data.remote.OnlineCourseRepository
import com.offline_First.data.remote.OnlineRoadmapRepository
import com.offline_First.data.repository.BackendNotConfiguredRepositories

object AppContainer {
    private val backendNotConfigured = BackendNotConfiguredRepositories()

    val authRepository get() = backendNotConfigured
    val aiRepository get() = backendNotConfigured
    
    // Real implementations for Phase 1
    val courseRepository = OnlineCourseRepository()
    val roadmapRepository = OnlineRoadmapRepository()
    
    // Still using placeholder for these
    val learningRepository get() = backendNotConfigured
    val profileRepository get() = backendNotConfigured
}
