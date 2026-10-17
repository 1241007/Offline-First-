package com.offline_First.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.offline_First.data.local.ChatCacheDatabase
import com.offline_First.data.local.CourseIdNormalizer
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
import kotlinx.coroutines.flow.Flow
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
    val progressStore: CourseProgressStore = (localFallback as? com.offline_First.data.local.LocalCourseRepository)?.progressStore ?: CourseProgressStore(),
    val cacheDb: ChatCacheDatabase? = context?.let { ChatCacheDatabase(it.applicationContext) },
    private val userIdProvider: () -> String = { "default_user" },
    private val isOnlineProvider: () -> Boolean = { true }
) : CourseRepository {

    val isOnline: Boolean
        get() = isOnlineProvider()

    private val currentUserId: String
        get() = userIdProvider().ifBlank { "default_user" }


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
        CourseIdNormalizer.registerRemoteCourse(id, slug, name)
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
        CourseIdNormalizer.registerRemoteCourse(domainId, slug, name)
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
            val dtos = json.decodeFromString<List<CourseDto>>(raw)
            dtos.forEach { CourseIdNormalizer.registerRemoteCourse(it.id, it.slug, it.name) }
            dtos.map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun loadPersistedFeatured(): List<Course> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_FEATURED, null) ?: return emptyList()
        return runCatching {
            val dtos = json.decodeFromString<List<CourseDto>>(raw)
            dtos.forEach { CourseIdNormalizer.registerRemoteCourse(it.id, it.slug, it.name) }
            dtos.map { it.toDomain() }
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
        val uId = currentUserId
        if (!isOnline) {
            val offlineCourses = cacheDb?.getSavedCourses(uId).orEmpty()
            return Result.success(if (offset == 0) offlineCourses else emptyList())
        }
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
                // When network drops / unreachable: never display online catalogue or sample courses!
                // Show ONLY the current user's locally saved, offline-ready courses.
                val offlineCourses = cacheDb?.getSavedCourses(uId).orEmpty()
                if (offset == 0) offlineCourses else emptyList()
            }
    }

    override suspend fun getFeaturedCourses(): Result<List<Course>> {
        val uId = currentUserId
        if (!isOnline) {
            val offlineFeatured = cacheDb?.getSavedCourses(uId).orEmpty()
            return Result.success(offlineFeatured)
        }
        return ChatApiClient.getCourses(featured = true)
            .map { dtos ->
                val list = dtos.map { it.toDomain() }
                cachedFeatured = list
                savePersistedFeatured(dtos)
                list
            }
            .recoverCatching { error ->
                val offlineFeatured = cacheDb?.getSavedCourses(uId).orEmpty()
                offlineFeatured
            }
    }

    override suspend fun getCoursesByCategory(categorySlug: String): Result<List<Course>> {
        val uId = currentUserId
        if (!isOnline) {
            val offlineCourses = cacheDb?.getSavedCourses(uId).orEmpty().filter {
                it.category.equals(categorySlug, ignoreCase = true)
            }
            return Result.success(offlineCourses)
        }
        val remote = runCatching {
            ChatApiClient.getCourses(category = categorySlug).getOrThrow()
        }
        val list = remote.getOrNull()
        if (remote.isSuccess && list != null) {
            return Result.success(list.map { it.toDomain() })
        }
        val offlineCourses = cacheDb?.getSavedCourses(uId).orEmpty().filter {
            it.category.equals(categorySlug, ignoreCase = true)
        }
        return Result.success(offlineCourses)
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
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        val uId = currentUserId

        if (!isOnline) {
            // Strictly offline: only allow if user explicitly saved it and it is offline-ready!
            val isReady = aliases.any { cacheDb?.isCourseOfflineReady(uId, it) == true }
            if (!isReady) {
                return Result.failure(IllegalStateException("This course isn't available offline. Connect to the internet and save it first."))
            }
            for (alias in aliases) {
                val offlineDetail = cacheDb?.getOfflineCourseDetail(alias, uId)
                if (offlineDetail != null) {
                    return Result.success(offlineDetail.withCurrentProgress())
                }
            }
            for (alias in aliases) {
                val fallbackDetail = localFallback?.getCourseDetail(alias)?.getOrNull()
                if (fallbackDetail != null) {
                    return Result.success(fallbackDetail.withCurrentProgress())
                }
            }
            return Result.failure(IllegalStateException("This course isn't available offline. Connect to the internet and save it first."))
        }

        // When ONLINE:
        // 1. Check in-memory cache
        for (alias in aliases) {
            val cached = detailCache[alias]
            if (cached != null) {
                refreshCourseDetailInBackground(courseId)
                return Result.success(cached.withCurrentProgress())
            }
        }

        // 2. Check persisted cache
        for (alias in aliases) {
            val persisted = loadPersistedCourseDetail(alias)
            if (persisted != null) {
                detailCache[courseId] = persisted
                refreshCourseDetailInBackground(courseId)
                return Result.success(persisted.withCurrentProgress())
            }
        }

        // 3. Fetch remotely
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

        // 4. Remote call failed (e.g. network dropped):
        // Fall back to SQLite IF AND ONLY IF course is explicitly saved and offline-ready for current user
        val isReady = aliases.any { cacheDb?.isCourseOfflineReady(uId, it) == true }
        if (isReady) {
            for (alias in aliases) {
                val offlineDetail = cacheDb?.getOfflineCourseDetail(alias, uId)
                if (offlineDetail != null) {
                    return Result.success(offlineDetail.withCurrentProgress())
                }
            }
            for (alias in aliases) {
                val fallbackDetail = localFallback?.getCourseDetail(alias)?.getOrNull()
                if (fallbackDetail != null) {
                    return Result.success(fallbackDetail.withCurrentProgress())
                }
            }
        }

        // Unsaved course and remote unreachable:
        return Result.failure(IllegalStateException("This course isn't available offline. Connect to the internet and save it first."))
    }

    override fun observeProgressUpdates(): Flow<Long> = progressStore.progressUpdates

    override suspend fun getCompletedLessonCount(courseId: String): Int =
        progressStore.getCompletedLessonCount(courseId)

    override suspend fun markLessonCompleted(courseId: String, lessonId: String, completed: Boolean): Result<Unit> {
        progressStore.markLessonCompleted(courseId, lessonId, completed)
        val uId = currentUserId
        cacheDb?.recordUserLessonProgress(uId, courseId, lessonId, completed, syncStatus = "pending_sync")
        if (completed) {
            runCatching {
                val res = ChatApiClient.completeLesson(lessonId)
                if (res.isSuccess) {
                    cacheDb?.markUserLessonProgressSynced(uId, courseId, lessonId)
                }
            }
        }
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        for (alias in aliases) {
            val cached = detailCache[alias]
            if (cached != null) {
                val updated = cached.copy(
                    modules = cached.modules.map { m ->
                        m.copy(
                            lessons = m.lessons.map { l ->
                                if (l.id == lessonId) l.copy(isCompleted = completed)
                                else l.copy(isCompleted = progressStore.isLessonCompleted(alias, l.id))
                            }
                        )
                    }
                )
                detailCache[alias] = updated
            }
        }
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

    // Offline persistence operations
    override suspend fun saveCourse(courseId: String): Result<Boolean> {
        val uId = currentUserId
        // 1. Send save request to remote API if online
        val remoteResult = if (isOnline) ChatApiClient.saveCourse(courseId) else Result.success(SavedCourseActionResponseDto(courseId = courseId))

        // 2. Fetch full course detail to cache metadata, modules, and lessons locally
        val detailResult = getCourseDetail(courseId)
        val detail = detailResult.getOrNull() ?: localFallback?.getCourseDetail(courseId)?.getOrNull()
        if (detail != null && cacheDb != null && detail.modules.isNotEmpty()) {
            CourseIdNormalizer.registerPair(courseId, detail.id)
            val isReady = cacheDb.saveCourseForOffline(
                userId = uId,
                detail = detail,
                downloadStatus = "completed",
                syncStatus = if (remoteResult.isSuccess && isOnline) "synced" else "pending_sync"
            )
            return Result.success(isReady)
        } else if (cacheDb != null) {
            // Downloading/caching detail failed: do NOT mark offline-ready!
            cacheDb.recordFailedDownload(
                userId = uId,
                courseId = courseId,
                syncStatus = if (remoteResult.isSuccess && isOnline) "synced" else "pending_sync"
            )
            return Result.success(false)
        }

        return Result.success(false)
    }

    override suspend fun unsaveCourse(courseId: String): Result<Boolean> {
        val uId = currentUserId
        val remoteResult = ChatApiClient.unsaveCourse(courseId)
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        if (remoteResult.isSuccess) {
            for (alias in aliases) {
                cacheDb?.deleteSavedCourse(uId, alias)
                cacheDb?.unsaveCourse(uId, alias)
            }
        } else {
            for (alias in aliases) {
                cacheDb?.queueUnsaveCourse(uId, alias)
            }
        }
        return Result.success(true)
    }

    override suspend fun isCourseSaved(courseId: String): Boolean {
        return isCourseSaved(currentUserId, courseId)
    }

    override suspend fun isCourseOfflineReady(courseId: String): Boolean {
        return isCourseOfflineReady(currentUserId, courseId)
    }

    override suspend fun getSavedCourses(): Result<List<Course>> {
        val uId = currentUserId
        // 1. Try remote fetch only if online
        if (isOnline) {
            val remoteResult = ChatApiClient.getSavedCourses()
            if (remoteResult.isSuccess) {
            val dtoList = remoteResult.getOrThrow()
            val domainList = dtoList.map { savedDto ->
                val courseDto = savedDto.course ?: CourseDto(
                    id = savedDto.courseId,
                    name = savedDto.name,
                    description = savedDto.description,
                    icon = savedDto.icon,
                    accentColor = savedDto.accentColor,
                    category = savedDto.category,
                    categorySlug = savedDto.categorySlug,
                    slug = savedDto.slug,
                    level = savedDto.level,
                    durationHours = savedDto.durationHours,
                    isFeatured = savedDto.isFeatured,
                    totalLessons = savedDto.totalLessons,
                    modulesCount = savedDto.modulesCount
                )
                courseDto.toDomain()
            }
                return Result.success(domainList)
            }
        }

        // 2. Offline fallback: load strictly user-saved courses from local SQLite
        val offlineCourses = cacheDb?.getSavedCourses(uId) ?: emptyList()
        return Result.success(offlineCourses)
    }

    override suspend fun saveCourseForOffline(userId: String, course: CourseDetail): Result<Unit> {
        val db = cacheDb ?: return Result.failure(IllegalStateException("Local SQLite database not available"))
        val success = db.saveCourseForOffline(userId, course)
        return if (success) Result.success(Unit) else Result.failure(IllegalStateException("Failed to persist course locally"))
    }

    override suspend fun unsaveCourse(userId: String, courseId: String): Result<Unit> {
        val db = cacheDb ?: return Result.failure(IllegalStateException("Local SQLite database not available"))
        db.unsaveCourse(userId, courseId)
        return Result.success(Unit)
    }

    override suspend fun isCourseSaved(userId: String, courseId: String): Boolean {
        val targetUser = userId.ifBlank { currentUserId }
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        return aliases.any { cacheDb?.isCourseSaved(targetUser, it) == true }
    }

    override suspend fun isCourseOfflineReady(userId: String, courseId: String): Boolean {
        val targetUser = userId.ifBlank { currentUserId }
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        return aliases.any { cacheDb?.isCourseOfflineReady(targetUser, it) == true }
    }

    override suspend fun getSavedCourses(userId: String): Result<List<Course>> {
        val targetUser = userId.ifBlank { currentUserId }
        val db = cacheDb ?: return Result.success(emptyList())
        return Result.success(db.getSavedCourses(targetUser))
    }

    override suspend fun getOfflineCourseDetail(courseId: String): Result<CourseDetail?> {
        val db = cacheDb ?: return Result.success(null)
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        for (alias in aliases) {
            val detail = db.getOfflineCourseDetail(alias, currentUserId)
            if (detail != null) {
                return Result.success(detail.withCurrentProgress())
            }
        }
        return Result.success(null)
    }

    companion object {
        private const val PREFS_NAME = "edunova_courses_cache"
        private const val KEY_COURSES = "persisted_courses"
        private const val KEY_FEATURED = "persisted_featured"
        private const val KEY_COURSE_DETAIL_PREFIX = "persisted_detail_"
    }
}
