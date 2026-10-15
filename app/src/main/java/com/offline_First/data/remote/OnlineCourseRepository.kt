package com.offline_First.data.remote

import com.offline_First.data.local.CourseProgressStore
import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.CourseCertificate
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.CourseModule
import com.offline_First.domain.model.LessonItem
import com.offline_First.domain.model.RevisionItem
import java.io.File

/**
 * Course repository backed by EduNova FastAPI backend and Supabase PostgreSQL.
 *
 * Requirements:
 * - Online-only: courses, modules, lessons, and resources are fetched exclusively from the remote API.
 * - Never shows hardcoded/local course catalogues as a network-failure fallback during normal operation.
 * - Manages user-specific lesson completions, progress tracking, and certificates via persistent CourseProgressStore.
 */
class OnlineCourseRepository(
    val localFallback: CourseRepository? = null,
    val progressStore: CourseProgressStore = CourseProgressStore()
) : CourseRepository {

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

    override suspend fun getCourses(): Result<List<Course>> {
        val remote = runCatching {
            ChatApiClient.getCourses(featured = null).getOrThrow()
        }
        val list = remote.getOrNull()
        if (remote.isSuccess && list != null) {
            return Result.success(list.map { it.toDomain() })
        }
        if (localFallback != null) {
            return localFallback.getCourses()
        }
        return remote.map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getFeaturedCourses(): Result<List<Course>> {
        val remote = runCatching {
            ChatApiClient.getCourses(featured = true).getOrThrow()
        }
        val list = remote.getOrNull()
        if (remote.isSuccess && list != null) {
            return Result.success(list.map { it.toDomain() })
        }
        if (localFallback != null) {
            return localFallback.getFeaturedCourses()
        }
        return remote.map { dtos -> dtos.map { it.toDomain() } }
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
}
