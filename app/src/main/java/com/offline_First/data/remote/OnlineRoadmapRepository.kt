package com.offline_First.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.RoadmapOption
import com.offline_First.domain.model.RoadmapAccentTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OnlineRoadmapRepository(
    context: Context? = null
) : RoadmapRepository {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun mapAccentTheme(theme: String): RoadmapAccentTheme {
        return when (theme.lowercase()) {
            "secondary", "purple", "teal", "orange" -> RoadmapAccentTheme.SECONDARY
            "accent", "red" -> RoadmapAccentTheme.ACCENT
            "success", "green" -> RoadmapAccentTheme.SUCCESS
            else -> RoadmapAccentTheme.PRIMARY
        }
    }

    private fun RoadmapDto.toDomain(): RoadmapOption {
        return RoadmapOption(
            id = id,
            title = title,
            category = category,
            description = description,
            skills = skills,
            level = level,
            duration = duration,
            stages = stages,
            icon = icon,
            accentTheme = mapAccentTheme(accentTheme)
        )
    }

    @Volatile
    private var cachedRoadmaps: List<RoadmapOption> = loadPersistedRoadmaps()

    @Volatile
    private var cachedCategories: List<String> = loadPersistedCategories()

    private fun loadPersistedRoadmaps(): List<RoadmapOption> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_ROADMAPS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<RoadmapDto>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun loadPersistedCategories(): List<String> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_CATEGORIES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<String>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun savePersistedRoadmaps(dtos: List<RoadmapDto>) {
        val sp = prefs ?: return
        runCatching {
            sp.edit().putString(KEY_ROADMAPS, json.encodeToString(dtos)).apply()
        }
    }

    private fun savePersistedCategories(cats: List<String>) {
        val sp = prefs ?: return
        runCatching {
            sp.edit().putString(KEY_CATEGORIES, json.encodeToString(cats)).apply()
        }
    }

    override suspend fun getRoadmaps(
        limit: Int?,
        offset: Int,
        category: String?
    ): Result<List<RoadmapOption>> {
        return ChatApiClient.getRoadmaps(category = category, limit = limit, offset = offset)
            .map { dtos ->
                val list = dtos.map { it.toDomain() }
                if (offset == 0 && category == null) {
                    cachedRoadmaps = list
                    savePersistedRoadmaps(dtos)
                }
                list
            }
            .recoverCatching { error ->
                if (cachedRoadmaps.isNotEmpty()) {
                    if (category != null) {
                        cachedRoadmaps.filter { it.category.equals(category, ignoreCase = true) }
                    } else {
                        cachedRoadmaps
                    }
                } else {
                    throw error
                }
            }
    }

    override suspend fun getCategories(): List<String> {
        return ChatApiClient.getRoadmapCategories()
            .map { cats ->
                if (cats.isNotEmpty()) {
                    cachedCategories = cats
                    savePersistedCategories(cats)
                }
                cats
            }
            .getOrElse {
                if (cachedCategories.isNotEmpty()) cachedCategories else emptyList()
            }
    }

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> {
        // Not implemented in Phase 1
        return Result.failure(UnsupportedOperationException("Personalized roadmap generation not yet implemented"))
    }

    companion object {
        private const val PREFS_NAME = "edunova_roadmaps_cache"
        private const val KEY_ROADMAPS = "persisted_roadmaps"
        private const val KEY_CATEGORIES = "persisted_categories"
    }
}
