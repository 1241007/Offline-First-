package com.offline_First.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.offline_First.data.local.CourseProgressStore
import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.*
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Course repository backed by EduNova FastAPI backend and Supabase PostgreSQL.
 *
 * Requirements:
 * - Online-only with persistent caching and optional local fallback for offline tests/mode.
 * - Manages user-specific lesson completions, progress tracking, and certificates via persistent CourseProgressStore.
 */
class OnlineCourseRepository(
    val localFallback: CourseRepository? = null,
    val progressStore: CourseProgressStore = CourseProgressStore(),
    context: Context? = null
) : CourseRepository {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        context?.filesDir?.let {
            bindStorageFile(File(it, "course_progress.json"))
        }
    }

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
        val remote = runCatching {
            ChatApiClient.getCourses(featured = null, limit = limit, offset = offset).getOrThrow()
        }
        val list = remote.getOrNull()
        if (remote.isSuccess && list != null) {
            val domainList = list.map { it.toDomain() }
            if (offset == 0) {
                cachedCourses = domainList
                savePersistedCourses(list)
            } else {
                cachedCourses = (cachedCourses + domainList).distinctBy { it.id }
            }
            return Result.success(domainList)
        }
        if (cachedCourses.isNotEmpty()) {
            val paged = cachedCourses.drop(offset).let { if (limit != null && limit > 0) it.take(limit) else it }
            return Result.success(paged)
        }
        if (localFallback != null) {
            return localFallback.getCourses(limit, offset)
        }
        return remote.map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getFeaturedCourses(): Result<List<Course>> {
        val remote = runCatching {
            ChatApiClient.getCourses(featured = true).getOrThrow()
        }
        val list = remote.getOrNull()
        if (remote.isSuccess && list != null) {
            val domainList = list.map { it.toDomain() }
            cachedFeatured = domainList
            savePersistedFeatured(list)
            return Result.success(domainList)
        }
        if (cachedFeatured.isNotEmpty()) {
            return Result.success(cachedFeatured)
        }
        if (localFallback != null) {
            return localFallback.getFeaturedCourses()
        }
        return remote.map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getCoursesByCategory(categorySlug: String): Result<List<Course>> {
        return getCourses().map { courses ->
            courses.filter {
                it.category.equals(categorySlug, ignoreCase = true) ||
                it.id.equals(categorySlug, ignoreCase = true)
            }
        }
    }

    override suspend fun getCourseDetail(courseId: String): Result<CourseDetail?> {
        val remote = runCatching {
            ChatApiClient.getCourseDetail(courseId).getOrThrow()
        }
        val detail = remote.getOrNull()
        if (remote.isSuccess && detail != null) {
            return Result.success(detail.toDomain(courseId))
        }
        if (localFallback != null) {
            return localFallback.getCourseDetail(courseId)
        }
        if (remote.isSuccess && detail == null) {
            return Result.failure(NoSuchElementException("Course not found: $courseId"))
        }
        return remote.map { it?.toDomain(courseId) }
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
    }
}
