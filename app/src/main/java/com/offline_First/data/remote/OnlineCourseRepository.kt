package com.offline_First.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OnlineCourseRepository(
    context: Context? = null
) : CourseRepository {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun mapAccent(color: String): CourseAccent {
        return when (color.lowercase()) {
            "secondary", "orange", "yellow", "green", "cyan", "pink", "gray", "indigo" -> CourseAccent.SECONDARY
            "accent", "red", "purple" -> CourseAccent.ACCENT
            else -> CourseAccent.PRIMARY
        }
    }

    private fun CourseDto.toDomain(): Course {
        val completed = progressStore.getCompletedLessonCount(id)
        val prog = if (totalLessons > 0) (completed.toFloat() / totalLessons.toFloat()).coerceIn(0f, 1f) else 0f
        return Course(
            id = id,
            name = name,
            description = description,
            icon = icon,
            accent = mapAccent(accentColor),
            category = category,
            level = level,
            durationHours = durationHours,
            totalLessons = totalLessons,
            modulesCount = modulesCount,
            completedLessons = completed,
            progress = prog
        )
    }

    private fun CourseDetailDto.toDomain(requestedCourseId: String): CourseDetail {
        val domainId = if (id.isNotBlank()) id else requestedCourseId
        return CourseDetail(
            id = domainId,
            name = name,
            description = description,
            icon = icon,
            accent = mapAccent(accentColor),
            category = category,
            level = level,
            durationHours = durationHours,
            prerequisites = prerequisites,
            modules = modules.sortedBy { it.displayOrder }.map { m ->
                CourseModule(
                    id = m.id,
                    courseId = domainId,
                    title = m.title,
                    description = m.description,
                    displayOrder = m.displayOrder,
                    lessons = m.lessons.sortedBy { it.displayOrder }.map { l ->
                        LessonItem(
                            id = l.id,
                            moduleId = m.id,
                            courseId = domainId,
                            title = l.title,
                            description = l.description,
                            contentType = l.contentType,
                            durationMinutes = l.durationMinutes,
                            displayOrder = l.displayOrder,
                            resourceUrl = l.resourceUrl,
                            resourceProvider = l.resourceProvider,
                            videoUrl = l.videoUrl,
                            videoProvider = l.videoProvider,
                            isCompleted = progressStore.isLessonCompleted(domainId, l.id),
                            isMarkedForRevision = progressStore.isMarkedForRevision(domainId, l.id)
                        )
                    }
                )
            }
        )
    }

    @Volatile
    private var cachedCourses: List<Course> = loadPersistedCourses()

    @Volatile
    private var cachedFeatured: List<Course> = loadPersistedFeatured()

    private fun loadPersistedCourses(): List<Course> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_COURSES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<CourseDto>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun loadPersistedFeatured(): List<Course> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_FEATURED, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<CourseDto>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun savePersistedCourses(dtos: List<CourseDto>) {
        val sp = prefs ?: return
        runCatching {
            sp.edit().putString(KEY_COURSES, json.encodeToString(dtos)).apply()
        }
    }

    private fun savePersistedFeatured(dtos: List<CourseDto>) {
        val sp = prefs ?: return
        runCatching {
            sp.edit().putString(KEY_FEATURED, json.encodeToString(dtos)).apply()
        }
    }

    override suspend fun getCourses(limit: Int?, offset: Int): Result<List<Course>> {
        return ChatApiClient.getCourses(featured = null, limit = limit, offset = offset)
            .map { dtos ->
                val list = dtos.map { it.toDomain() }
                if (offset == 0) {
                    cachedCourses = list
                    savePersistedCourses(dtos)
                } else {
                    cachedCourses = (cachedCourses + list).distinctBy { it.id }
                }
                list
            }
            .recoverCatching { error ->
                if (cachedCourses.isNotEmpty()) {
                    cachedCourses
                } else {
                    throw error
                }
            }
    }

    override suspend fun getFeaturedCourses(): Result<List<Course>> {
        return ChatApiClient.getCourses(featured = true)
            .map { dtos ->
                val list = dtos.map { it.toDomain() }
                cachedFeatured = list
                savePersistedFeatured(dtos)
                list
            }
            .recoverCatching { error ->
                if (cachedFeatured.isNotEmpty()) {
                    cachedFeatured
                } else {
                    throw error
                }
            }
    }

    companion object {
        private const val PREFS_NAME = "edunova_courses_cache"
        private const val KEY_COURSES = "persisted_courses"
        private const val KEY_FEATURED = "persisted_featured"
    }
}
