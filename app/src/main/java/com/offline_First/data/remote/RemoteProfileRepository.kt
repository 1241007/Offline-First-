package com.offline_First.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.offline_First.data.repository.ProfileRepository
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class RemoteProfileRepository(
    context: Context? = null,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val baseUrl: String = ChatApiConfig.BASE_URL
) : ProfileRepository {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _profileState = MutableStateFlow<UserProfile?>(loadCachedProfile())
    val currentProfile: UserProfile? get() = _profileState.value

    override fun observeUserProfile(): Flow<UserProfile?> = _profileState.asStateFlow()

    private fun loadCachedProfile(): UserProfile? {
        val sp = prefs ?: return null
        val email = sp.getString(KEY_EMAIL, null) ?: return null
        val fullName = sp.getString(KEY_FULL_NAME, "") ?: ""
        val mobile = sp.getString(KEY_MOBILE, "") ?: ""
        val interests = sp.getString(KEY_INTERESTS, "") ?: ""
        val level = sp.getString(KEY_LEVEL, "Beginner") ?: "Beginner"
        val modeStr = sp.getString(KEY_MODE, "general") ?: "general"
        val educationMode = if (modeStr.equals("school", ignoreCase = true)) {
            EducationMode.SCHOOL
        } else {
            EducationMode.GENERAL
        }
        return UserProfile(
            fullName = fullName,
            email = email,
            mobile = mobile,
            interests = interests,
            level = level,
            educationMode = educationMode
        )
    }

    private fun saveCachedProfile(profile: UserProfile) {
        prefs?.edit()
            ?.putString(KEY_FULL_NAME, profile.fullName)
            ?.putString(KEY_EMAIL, profile.email)
            ?.putString(KEY_MOBILE, profile.mobile)
            ?.putString(KEY_INTERESTS, profile.interests)
            ?.putString(KEY_LEVEL, profile.level)
            ?.putString(
                KEY_MODE,
                if (profile.educationMode == EducationMode.SCHOOL) "school" else "general"
            )
            ?.apply()
    }

    fun clearCachedProfile() {
        prefs?.edit()?.clear()?.apply()
        _profileState.value = null
    }

    override suspend fun getUserProfile(): Result<UserProfile?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/profile")
                .get()
                .build()

            authenticatedApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: Failed to fetch user profile")
                }
                val body = response.body?.string() ?: throw IOException("Empty profile response body")
                val dto = json.decodeFromString(ProfileResponseDto.serializer(), body)

                val domainProfile = UserProfile(
                    fullName = dto.fullName,
                    email = dto.email,
                    mobile = dto.mobile.orEmpty(),
                    interests = dto.interests.orEmpty(),
                    level = dto.level,
                    educationMode = if (dto.educationMode.equals("school", ignoreCase = true)) {
                        EducationMode.SCHOOL
                    } else {
                        EducationMode.GENERAL
                    }
                )

                saveCachedProfile(domainProfile)
                _profileState.value = domainProfile
                domainProfile
            }
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dto = ProfileUpdateRequestDto(
                fullName = profile.fullName,
                email = profile.email,
                mobile = profile.mobile.ifBlank { null },
                interests = profile.interests.ifBlank { null },
                level = profile.level,
                educationMode = if (profile.educationMode == EducationMode.SCHOOL) "school" else "general"
            )

            val payload = json.encodeToString(ProfileUpdateRequestDto.serializer(), dto)
            val request = Request.Builder()
                .url("$baseUrl/api/v1/profile")
                .put(payload.toRequestBody("application/json".toMediaType()))
                .build()

            authenticatedApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()
                    throw IOException("HTTP ${response.code}: ${errBody ?: "Failed to update profile"}")
                }
                val body = response.body?.string() ?: ""
                val updatedDto = json.decodeFromString(ProfileResponseDto.serializer(), body)

                val updatedProfile = UserProfile(
                    fullName = updatedDto.fullName,
                    email = updatedDto.email,
                    mobile = updatedDto.mobile.orEmpty(),
                    interests = updatedDto.interests.orEmpty(),
                    level = updatedDto.level,
                    educationMode = if (updatedDto.educationMode.equals("school", ignoreCase = true)) {
                        EducationMode.SCHOOL
                    } else {
                        EducationMode.GENERAL
                    }
                )

                saveCachedProfile(updatedProfile)
                _profileState.value = updatedProfile
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "edunova_user_profile_cache"
        private const val KEY_FULL_NAME = "profile_full_name"
        private const val KEY_EMAIL = "profile_email"
        private const val KEY_MOBILE = "profile_mobile"
        private const val KEY_INTERESTS = "profile_interests"
        private const val KEY_LEVEL = "profile_level"
        private const val KEY_MODE = "profile_education_mode"
    }
}
