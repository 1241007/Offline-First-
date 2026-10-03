package com.offline_First.data.repository

import com.offline_First.domain.model.UserProfile

/**
 * Repository interface for profile storage.
 */
interface ProfileRepository {
    suspend fun getUserProfile(): Result<UserProfile>
    suspend fun updateUserProfile(profile: UserProfile): Result<Unit>
}
