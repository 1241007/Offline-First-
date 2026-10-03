package com.offline_First.data.local

import com.offline_First.data.repository.ProfileRepository
import com.offline_First.domain.model.UserProfile

class LocalProfileRepository : ProfileRepository {

    private var currentProfile = UserProfile(
        fullName = "Asha Learner",
        email = "asha@example.com",
        mobile = "+91 98765 43210",
        interests = "Android, UI design",
        level = "Intermediate"
    )

    override suspend fun getUserProfile(): Result<UserProfile> {
        return Result.success(currentProfile)
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        currentProfile = profile
        return Result.success(Unit)
    }
}
