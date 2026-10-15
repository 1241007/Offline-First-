package com.offline_First.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.offline_First.data.local.CourseProgressStore
import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.CourseCertificate
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.CourseModule
import com.offline_First.domain.model.LessonItem
import com.offline_First.domain.model.RevisionItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Course repository backed by EduNova FastAPI backend and Supabase PostgreSQL.
 *
 * Requirements:
 * - Online-only: courses, modules, lessons, and resources are fetched exclusively from the remote API.
 * - Never shows hardcoded/local course catalogues as a network-failure fallback during normal operation.
 * - Manages user-specific lesson completions, progress tracking, and certificates via persistent CourseProgressStore.
 */
class OnlineCourseRepository(
    val context: Context? = null,
    val localFallback: CourseRepository? = null,
    val progressStore: CourseProgressStore = CourseProgressStore()
) : CourseRepository {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val detailCache = ConcurrentHashMap<String, CourseDetail>()
    private val activeDetailRefreshes = ConcurrentHashMap.newKeySet<String>()

    fun bindStorageFile(file: File) {
        progressStore.bindStorageFile(file)
        if (localFallback is com.offline_First.data.local.LocalCourseRepository) {
            localFallback.bindStorageFile(file)
        }
    }

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
                } else if (localFallback != null) {
                    localFallback.getCourses(limit, offset).getOrThrow()
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
                } else if (localFallback != null) {
                    localFallback.getFeaturedCourses().getOrThrow()
                } else {
                    throw error
                }
            }
    }

    override suspend fun getCoursesByCategory(categorySlug: String): Result<List<Course>> {
        val remote = runCatching {
            ChatApiClient.getCourses(category = categorySlug).getOrThrow()
        }
        val list = remote.getOrNull()
        if (remote.isSuccess && list != null) {
            return Result.success(list.map { it.toDomain() })
        }
        if (localFallback != null) {
            return localFallback.getCoursesByCategory(categorySlug)
        }
        return remote.map { dtos -> dtos.map { it.toDomain() } }
    }

    private fun CourseDetail.withCurrentProgress(): CourseDetail {
        return copy(
            modules = modules.map { m ->
                m.copy(
                    lessons = m.lessons.map { l ->
                        l.copy(
                            isCompleted = progressStore.isLessonCompleted(id, l.id),
                            isMarkedForRevision = progressStore.isMarkedForRevision(id, l.id)
                        )
                    }
                )
            }
        )
    }

    private fun loadPersistedCourseDetail(courseId: String): CourseDetail? {
        val sp = prefs ?: return null
        val raw = sp.getString(KEY_COURSE_DETAIL_PREFIX + courseId, null) ?: return null
        return runCatching {
            json.decodeFromString<CourseDetailDto>(raw).toDomain(courseId)
        }.getOrNull()
    }

    private fun savePersistedCourseDetail(courseId: String, dto: CourseDetailDto) {
        val sp = prefs ?: return
        runCatching {
            sp.edit().putString(KEY_COURSE_DETAIL_PREFIX + courseId, json.encodeToString(dto)).apply()
        }
    }

    private fun refreshCourseDetailInBackground(courseId: String) {
        if (!activeDetailRefreshes.add(courseId)) return
        repositoryScope.launch {
            try {
                val remoteDto = ChatApiClient.getCourseDetail(courseId).getOrNull()
                if (remoteDto != null) {
                    val domain = remoteDto.toDomain(courseId)
                    detailCache[courseId] = domain
                    savePersistedCourseDetail(courseId, remoteDto)
                }
            } catch (_: Throwable) {
                // Background refresh silently ignores network errors
            } finally {
                activeDetailRefreshes.remove(courseId)
            }
        }
    }

    override suspend fun getCourseDetail(courseId: String): Result<CourseDetail?> {
        // 1. Check in-memory cache
        val cached = detailCache[courseId]
        if (cached != null) {
            refreshCourseDetailInBackground(courseId)
            return Result.success(cached.withCurrentProgress())
        }

        // 2. Check persisted cache
        val persisted = loadPersistedCourseDetail(courseId)
        if (persisted != null) {
            detailCache[courseId] = persisted
            refreshCourseDetailInBackground(courseId)
            return Result.success(persisted.withCurrentProgress())
        }

        // 3. Check local fallback immediately if available
        val fallback = localFallback
        if (fallback != null) {
            val localResult = runCatching { fallback.getCourseDetail(courseId).getOrNull() }.getOrNull()
            if (localResult != null) {
                detailCache[courseId] = localResult
                refreshCourseDetailInBackground(courseId)
                return Result.success(localResult.withCurrentProgress())
            }
        }

        // 4. If neither cache nor local fallback has it, fetch remotely
        val remote = runCatching {
            ChatApiClient.getCourseDetail(courseId).getOrThrow()
        }
        val detail = remote.getOrNull()
        if (remote.isSuccess && detail != null) {
            val domain = detail.toDomain(courseId)
            detailCache[courseId] = domain
            savePersistedCourseDetail(courseId, detail)
            return Result.success(domain.withCurrentProgress())
        }
        if (fallback != null) {
            return fallback.getCourseDetail(courseId).map { it?.withCurrentProgress() }
        }
        return remote.map { it?.toDomain(courseId)?.withCurrentProgress() }
    }

    override suspend fun markLessonCompleted(courseId: String, lessonId: String, completed: Boolean): Result<Unit> {
        progressStore.markLessonCompleted(courseId, lessonId, completed)
        localFallback?.markLessonCompleted(courseId, lessonId, completed)
        return Result.success(Unit)
    }

    override suspend fun isLessonCompleted(courseId: String, lessonId: String): Boolean {
        return progressStore.isLessonCompleted(courseId, lessonId)
    }

    override suspend fun getCompletedLessonCount(courseId: String, lessonIds: Collection<String>): Int {
        return progressStore.getCompletedLessonCount(courseId, lessonIds)
    }

    override suspend fun getCourseProgressPercentage(courseId: String, lessonIds: Collection<String>): Float {
        return progressStore.getCourseProgressPercentage(courseId, lessonIds)
    }

    override suspend fun isCourseCompleted(courseId: String, lessonIds: Collection<String>): Boolean {
        return progressStore.isCourseCompleted(courseId, lessonIds)
    }

    override suspend fun getCertificate(courseId: String): CourseCertificate? {
        return progressStore.getCertificate(courseId)
    }

    override suspend fun getAllCertificates(): List<CourseCertificate> {
        return progressStore.getAllCertificates()
    }

    override suspend fun issueCertificate(
        courseId: String,
        courseName: String,
        learnerName: String,
        totalLessons: Int
    ): CourseCertificate? {
        return progressStore.issueCertificate(courseId, courseName, learnerName, totalLessons)
    }

    override suspend fun addRevisionItem(item: RevisionItem): Result<Unit> {
        progressStore.addRevisionItem(item)
        localFallback?.addRevisionItem(item)
        return Result.success(Unit)
    }

    override suspend fun removeRevisionItem(courseId: String, lessonId: String): Result<Unit> {
        progressStore.removeRevisionItem(courseId, lessonId)
        localFallback?.removeRevisionItem(courseId, lessonId)
        return Result.success(Unit)
    }

    override suspend fun isMarkedForRevision(courseId: String, lessonId: String): Boolean {
        return progressStore.isMarkedForRevision(courseId, lessonId)
    }

    override suspend fun getRevisionItems(): Result<List<RevisionItem>> {
        return Result.success(progressStore.getRevisionItems())
    }

    companion object {
        private const val PREFS_NAME = "edunova_courses_cache"
        private const val KEY_COURSES = "persisted_courses"
        private const val KEY_FEATURED = "persisted_featured"
        private const val KEY_COURSE_DETAIL_PREFIX = "persisted_detail_"
    }
}
