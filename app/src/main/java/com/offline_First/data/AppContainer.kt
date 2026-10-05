package com.offline_First.data

import android.content.Context
import com.offline_First.BuildConfig
import com.offline_First.data.local.LocalAIRepository
import com.offline_First.data.remote.OnlineAIRepository
import com.offline_First.data.remote.OnlineCourseRepository
import com.offline_First.data.remote.OnlineRoadmapRepository
import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.data.repository.ModeAwareAIRepository
import com.offline_First.data.remote.SupabaseAuthRepository

object AppContainer {
    private val repositories = BackendNotConfiguredRepositories()
    private var aiRepoOverride: com.offline_First.data.repository.AIRepository? = null
    private var authRepoOverride: SupabaseAuthRepository? = null

    fun initialize(context: Context) {
        val localRepository = LocalAIRepository(context.applicationContext)
        val onlineRepository = OnlineAIRepository(context.applicationContext, localRepository)
        aiRepoOverride = ModeAwareAIRepository(localRepository, onlineRepository)
        authRepoOverride = SupabaseAuthRepository(
            context.applicationContext,
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_ANON_KEY
        )
    }

    val authRepository get() = authRepoOverride ?: repositories
    val aiRepository get() = aiRepoOverride ?: repositories
    val courseRepository = OnlineCourseRepository()
    val learningRepository get() = repositories
    val profileRepository get() = repositories
    val roadmapRepository = OnlineRoadmapRepository()
}
