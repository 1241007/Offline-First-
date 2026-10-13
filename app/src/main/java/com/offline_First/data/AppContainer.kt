package com.offline_First.data

import android.content.Context
import com.offline_First.data.local.LocalAIRepository
import com.offline_First.data.local.LocalCourseRepository
import com.offline_First.data.local.LocalLearningRepository
import com.offline_First.data.local.LocalProfileRepository
import com.offline_First.data.local.LocalRoadmapRepository
import com.offline_First.data.remote.OnlineAIRepository
import com.offline_First.data.remote.OnlineCourseRepository
import com.offline_First.data.remote.OnlineRoadmapRepository
import com.offline_First.data.repository.AIRepository
import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.data.repository.CourseRepository
import com.offline_First.data.repository.LearningRepository
import com.offline_First.data.repository.ModeAwareAIRepository
import com.offline_First.data.repository.ProfileRepository
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.RoadmapOption

object AppContainer {
    private val backendFallback = BackendNotConfiguredRepositories()
    private var aiRepoOverride: AIRepository? = null

    val localProfileRepository = LocalProfileRepository()
    val localCourseRepository = LocalCourseRepository()
    val localLearningRepository = LocalLearningRepository()
    val localRoadmapRepository = LocalRoadmapRepository()
    val onlineCourseRepository = OnlineCourseRepository()
    val onlineRoadmapRepository = OnlineRoadmapRepository()

    fun initialize(context: Context) {
        val localRepository = LocalAIRepository(context.applicationContext)
        val onlineRepository = OnlineAIRepository(context.applicationContext, localRepository)
        aiRepoOverride = ModeAwareAIRepository(localRepository, onlineRepository)
    }

    val authRepository get() = backendFallback
    val aiRepository: AIRepository get() = aiRepoOverride ?: backendFallback
    val courseRepository: CourseRepository = FallbackCourseRepository(onlineCourseRepository, localCourseRepository)
    val learningRepository: LearningRepository = localLearningRepository
    val profileRepository: ProfileRepository = localProfileRepository
    val roadmapRepository: RoadmapRepository = FallbackRoadmapRepository(onlineRoadmapRepository, localRoadmapRepository)
}

private class FallbackCourseRepository(
    private val onlineRepo: CourseRepository,
    private val localRepo: CourseRepository
) : CourseRepository {
    override suspend fun getCourses(): Result<List<Course>> {
        val remote = try {
            onlineRepo.getCourses()
        } catch (_: Exception) {
            null
        }
        val remoteList = remote?.getOrNull()
        if (remote?.isSuccess == true && !remoteList.isNullOrEmpty()) {
            return remote
        }
        return localRepo.getCourses()
    }

    override suspend fun getFeaturedCourses(): Result<List<Course>> {
        val remote = try {
            onlineRepo.getFeaturedCourses()
        } catch (_: Exception) {
            null
        }
        val remoteList = remote?.getOrNull()
        if (remote?.isSuccess == true && !remoteList.isNullOrEmpty()) {
            return remote
        }
        return localRepo.getFeaturedCourses()
    }
}

private class FallbackRoadmapRepository(
    private val onlineRepo: RoadmapRepository,
    private val localRepo: RoadmapRepository
) : RoadmapRepository {
    override suspend fun getRoadmaps(): Result<List<RoadmapOption>> {
        val remote = try {
            onlineRepo.getRoadmaps()
        } catch (_: Exception) {
            null
        }
        val remoteList = remote?.getOrNull()
        if (remote?.isSuccess == true && !remoteList.isNullOrEmpty()) {
            return remote
        }
        return localRepo.getRoadmaps()
    }

    override suspend fun getCategories(): List<String> {
        val remote = try {
            onlineRepo.getCategories()
        } catch (_: Exception) {
            emptyList()
        }
        if (remote.isNotEmpty()) {
            return remote
        }
        return localRepo.getCategories()
    }

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> {
        val remote = try {
            onlineRepo.generatePersonalizedRoadmap(goal, level, studyTime, interest)
        } catch (_: Exception) {
            null
        }
        if (remote?.isSuccess == true) {
            return remote
        }
        return localRepo.generatePersonalizedRoadmap(goal, level, studyTime, interest)
    }
}
