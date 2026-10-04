package com.offline_First.data

import com.offline_First.data.repository.BackendNotConfiguredRepositories

object AppContainer {
    private val repositories = BackendNotConfiguredRepositories()

    val authRepository get() = repositories
    val aiRepository get() = repositories
    val courseRepository get() = repositories
    val learningRepository get() = repositories
    val profileRepository get() = repositories
    val roadmapRepository get() = repositories
}
