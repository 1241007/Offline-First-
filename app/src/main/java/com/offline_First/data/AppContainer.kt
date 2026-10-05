package com.offline_First.data

import android.content.Context
import com.offline_First.data.local.LocalAIRepository
import com.offline_First.data.remote.OnlineAIRepository
import com.offline_First.data.remote.OnlineCourseRepository
import com.offline_First.data.remote.OnlineRoadmapRepository
import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.data.repository.ModeAwareAIRepository

object AppContainer {
    private val repositories = BackendNotConfiguredRepositories()
    private var aiRepoOverride: com.offline_First.data.repository.AIRepository? = null

    fun initialize(context: Context) {
        val localRepository = LocalAIRepository(context.applicationContext)
        val onlineRepository = OnlineAIRepository(context.applicationContext, localRepository)
        aiRepoOverride = ModeAwareAIRepository(localRepository, onlineRepository)
    }

    val authRepository get() = repositories
    val aiRepository get() = aiRepoOverride ?: repositories
    val courseRepository = OnlineCourseRepository()
    val learningRepository get() = repositories
    val profileRepository get() = repositories
    val roadmapRepository = OnlineRoadmapRepository()
}
