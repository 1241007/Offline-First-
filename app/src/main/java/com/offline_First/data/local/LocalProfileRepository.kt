package com.offline_First.data.local

import com.offline_First.data.repository.ProfileRepository
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalProfileRepository : ProfileRepository {

    companion object {
        fun defaultProfile() = UserProfile(
            fullName = "Asha Learner",
            email = "asha@example.com",
            mobile = "+91 98765 43210",
            interests = "Android, UI design",
            level = "Intermediate",
            educationMode = EducationMode.GENERAL
        )

        private val _profileFlow = MutableStateFlow(defaultProfile())

        fun resetToDefault() {
            _profileFlow.value = defaultProfile()
        }
    }

    override suspend fun getUserProfile(): Result<UserProfile> {
        return Result.success(_profileFlow.value)
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        _profileFlow.value = profile
        return Result.success(Unit)
    }

    override fun observeUserProfile(): Flow<UserProfile> {
        return _profileFlow.asStateFlow()
    }
}
