package com.offline_First.data

import android.content.Context
import com.offline_First.data.local.EncryptedTokenStorage
import com.offline_First.data.local.LocalAIRepository
import com.offline_First.data.local.TokenStorage
import com.offline_First.data.remote.AuthApiClient
import com.offline_First.data.remote.AuthenticatedApiClient
import com.offline_First.data.remote.OnlineAIRepository
import com.offline_First.data.remote.OnlineCourseRepository
import com.offline_First.data.remote.OnlineRoadmapRepository
import com.offline_First.data.repository.AuthRepository
import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.data.repository.ModeAwareAIRepository
import com.offline_First.data.repository.RemoteAuthRepository

object AppContainer {
    private val defaultRepositories = BackendNotConfiguredRepositories()
    private var aiRepoOverride: com.offline_First.data.repository.AIRepository? = null

    private var _tokenStorage: TokenStorage? = null
    private var _sessionManager: SessionManager? = null
    private var _authApiClient: AuthApiClient? = null
    private var _authenticatedApiClient: AuthenticatedApiClient? = null
    private var _authRepository: AuthRepository? = null
    private var _profileRepository: com.offline_First.data.repository.ProfileRepository? = null
    private var _learningRepository: com.offline_First.data.repository.LearningRepository? = null

    fun initialize(context: Context) {
        val appContext = context.applicationContext

        val tokenStorage = EncryptedTokenStorage(appContext)
        val sessionManager = SessionManager(tokenStorage)
        val authApiClient = AuthApiClient()
        val authenticatedApiClient = AuthenticatedApiClient(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            publicClient = { authApiClient.okHttpClient }
        )
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
            authenticatedApiClient = authenticatedApiClient
        )

        _tokenStorage = tokenStorage
        _sessionManager = sessionManager
        _authApiClient = authApiClient
        _authenticatedApiClient = authenticatedApiClient
        _authRepository = remoteAuthRepo
        _profileRepository = remoteProfileRepo
        _learningRepository = remoteLearningRepo

        val localRepository = LocalAIRepository(appContext)
        val onlineRepository = OnlineAIRepository(appContext, localRepository)
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
    val courseRepository = OnlineCourseRepository()
    val learningRepository: com.offline_First.data.repository.LearningRepository
        get() = _learningRepository ?: defaultRepositories
    val profileRepository: com.offline_First.data.repository.ProfileRepository
        get() = _profileRepository ?: defaultRepositories
    val roadmapRepository = OnlineRoadmapRepository()
}
