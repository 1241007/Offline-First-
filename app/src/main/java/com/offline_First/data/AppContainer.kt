package com.offline_First.data

import android.content.Context
import com.offline_First.data.local.EncryptedTokenStorage
import com.offline_First.data.local.LocalAIRepository
<<<<<<< HEAD
import com.offline_First.data.local.LocalCourseRepository
import com.offline_First.data.local.LocalLearningRepository
import com.offline_First.data.local.LocalProfileRepository
import com.offline_First.data.local.LocalRoadmapRepository
import com.offline_First.data.remote.OnlineAIRepository
import com.offline_First.data.remote.OnlineCourseRepository
import com.offline_First.data.remote.OnlineRoadmapRepository
import com.offline_First.data.repository.AIRepository
=======
import com.offline_First.data.local.TokenStorage
import com.offline_First.data.remote.AuthApiClient
import com.offline_First.data.remote.AuthenticatedApiClient
import com.offline_First.data.remote.ChatApiClient
import com.offline_First.data.remote.OnlineAIRepository
import com.offline_First.data.remote.OnlineCourseRepository
import com.offline_First.data.remote.OnlineRoadmapRepository
import com.offline_First.data.repository.AuthRepository
>>>>>>> origin
import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.data.repository.CourseRepository
import com.offline_First.data.repository.LearningRepository
import com.offline_First.data.repository.ModeAwareAIRepository
<<<<<<< HEAD
import com.offline_First.data.repository.ProfileRepository
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.RevisionItem
import com.offline_First.domain.model.RoadmapOption
import java.io.File


object AppContainer {
    private val backendFallback = BackendNotConfiguredRepositories()
    private var aiRepoOverride: AIRepository? = null

    val localProfileRepository = LocalProfileRepository()
    val localCourseRepository = LocalCourseRepository()
    val localLearningRepository = LocalLearningRepository()
    val localRoadmapRepository = LocalRoadmapRepository()
    val onlineCourseRepository = OnlineCourseRepository()
    val onlineRoadmapRepository = OnlineRoadmapRepository()
=======
import com.offline_First.data.repository.RemoteAuthRepository

object AppContainer {
    private val defaultRepositories = BackendNotConfiguredRepositories()
    private var aiRepoOverride: com.offline_First.data.repository.AIRepository? = null
>>>>>>> origin

    private var _tokenStorage: TokenStorage? = null
    private var _sessionManager: SessionManager? = null
    private var _authApiClient: AuthApiClient? = null
    private var _authenticatedApiClient: AuthenticatedApiClient? = null
    private var _authRepository: AuthRepository? = null
    private var _profileRepository: com.offline_First.data.repository.ProfileRepository? = null
    private var _learningRepository: com.offline_First.data.repository.LearningRepository? = null

    fun initialize(context: Context) {
<<<<<<< HEAD
        val storageFile = File(context.applicationContext.filesDir, "edunova_course_storage.json")
        onlineCourseRepository.bindStorageFile(storageFile)
        localCourseRepository.bindStorageFile(storageFile)

        val localRepository = LocalAIRepository(context.applicationContext)
        val onlineRepository = OnlineAIRepository(context.applicationContext, localRepository)
        aiRepoOverride = ModeAwareAIRepository(localRepository, onlineRepository)
    }

    val authRepository get() = backendFallback
    val aiRepository: AIRepository get() = aiRepoOverride ?: backendFallback
    val courseRepository: CourseRepository = onlineCourseRepository
    val learningRepository: LearningRepository = localLearningRepository
    val profileRepository: ProfileRepository = localProfileRepository
    val roadmapRepository: RoadmapRepository = FallbackRoadmapRepository(onlineRoadmapRepository, localRoadmapRepository)
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
=======
        val appContext = context.applicationContext

        val tokenStorage = EncryptedTokenStorage(appContext)
        val sessionManager = SessionManager(tokenStorage)
        val authApiClient = AuthApiClient()
        val authenticatedApiClient = AuthenticatedApiClient(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            publicClient = { authApiClient.okHttpClient }
        )
        ChatApiClient.initialize(authenticatedApiClient.okHttpClient)
        val remoteAuthRepo = RemoteAuthRepository(
            authApiClient = authApiClient,
            authenticatedApiClient = authenticatedApiClient,
            sessionManager = sessionManager,
            tokenStorage = tokenStorage
        )
        val remoteProfileRepo = com.offline_First.data.remote.RemoteProfileRepository(
            context = appContext,
            authenticatedApiClient = authenticatedApiClient
        )
        val remoteLearningRepo = com.offline_First.data.remote.RemoteLearningRepository(
            context = appContext,
            authenticatedApiClient = authenticatedApiClient
        )
        val onlineCourseRepo = com.offline_First.data.remote.OnlineCourseRepository(
            context = appContext
        )
        val onlineRoadmapRepo = com.offline_First.data.remote.OnlineRoadmapRepository(
            context = appContext,
            userIdProvider = {
                (sessionManager.authState.value as? AuthState.Authenticated)?.userId ?: "default_user"
            }
        )

        _tokenStorage = tokenStorage
        _sessionManager = sessionManager
        _authApiClient = authApiClient
        _authenticatedApiClient = authenticatedApiClient
        _authRepository = remoteAuthRepo
        _profileRepository = remoteProfileRepo
        _learningRepository = remoteLearningRepo
        _courseRepository = onlineCourseRepo
        _roadmapRepository = onlineRoadmapRepo

        val localRepository = LocalAIRepository(appContext)
        val onlineRepository = OnlineAIRepository(
            context = appContext,
            delegate = localRepository,
            userIdProvider = {
                (sessionManager.authState.value as? AuthState.Authenticated)?.userId ?: "default_user"
            }
        )
        aiRepoOverride = ModeAwareAIRepository(localRepository, onlineRepository)
    }

    val tokenStorage: TokenStorage
        get() = _tokenStorage ?: throw IllegalStateException("AppContainer must be initialized before use")

    val sessionManager: SessionManager
        get() = _sessionManager ?: throw IllegalStateException("AppContainer must be initialized before use")

    val authApiClient: AuthApiClient
        get() = _authApiClient ?: throw IllegalStateException("AppContainer must be initialized before use")

    val authenticatedApiClient: AuthenticatedApiClient
        get() = _authenticatedApiClient ?: throw IllegalStateException("AppContainer must be initialized before use")

    val authRepository: AuthRepository
        get() = _authRepository ?: defaultRepositories

    val aiRepository get() = aiRepoOverride ?: defaultRepositories
    private var _courseRepository: com.offline_First.data.repository.CourseRepository? = null
    val courseRepository: com.offline_First.data.repository.CourseRepository
        get() = _courseRepository ?: OnlineCourseRepository()
    val learningRepository: com.offline_First.data.repository.LearningRepository
        get() = _learningRepository ?: defaultRepositories
    val profileRepository: com.offline_First.data.repository.ProfileRepository
        get() = _profileRepository ?: defaultRepositories
    private var _roadmapRepository: com.offline_First.data.repository.RoadmapRepository? = null
    val roadmapRepository: com.offline_First.data.repository.RoadmapRepository
        get() = _roadmapRepository ?: OnlineRoadmapRepository()
>>>>>>> origin
}
