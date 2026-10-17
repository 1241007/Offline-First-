package com.offline_First.data.repository

import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseCertificate
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.RevisionItem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Repository interface for course catalog, lesson progress, revisions, and completion certificates.
 */
interface CourseRepository {
    suspend fun getCourses(limit: Int? = null, offset: Int = 0): Result<List<Course>>
    suspend fun getFeaturedCourses(): Result<List<Course>>
    suspend fun getCoursesByCategory(categorySlug: String): Result<List<Course>> = getCourses()
    suspend fun getCourseDetail(courseId: String): Result<CourseDetail?>

    // Progress operations
    fun observeProgressUpdates(): Flow<Long> = emptyFlow()
    suspend fun markLessonCompleted(courseId: String, lessonId: String, completed: Boolean): Result<Unit>
    suspend fun isLessonCompleted(courseId: String, lessonId: String): Boolean
    suspend fun getCompletedLessonCount(courseId: String): Int = getCompletedLessonCount(courseId, emptyList())
    suspend fun getCompletedLessonCount(courseId: String, lessonIds: Collection<String>): Int
    suspend fun getCourseProgressPercentage(courseId: String, lessonIds: Collection<String>): Float
    suspend fun isCourseCompleted(courseId: String, lessonIds: Collection<String>): Boolean

    // Certificate operations
    suspend fun getCertificate(courseId: String): CourseCertificate?
    suspend fun getAllCertificates(): List<CourseCertificate>
    suspend fun issueCertificate(courseId: String, courseName: String, learnerName: String, totalLessons: Int): CourseCertificate?

    // Revision operations
    suspend fun addRevisionItem(item: RevisionItem): Result<Unit>
    suspend fun removeRevisionItem(courseId: String, lessonId: String): Result<Unit>
    suspend fun isMarkedForRevision(courseId: String, lessonId: String): Boolean
    suspend fun getRevisionItems(): Result<List<RevisionItem>>

    // Offline persistence operations
    suspend fun saveCourse(courseId: String): Result<Boolean> = Result.success(true)
    suspend fun unsaveCourse(courseId: String): Result<Boolean> = Result.success(true)
    suspend fun isCourseSaved(courseId: String): Boolean = false
    suspend fun isCourseOfflineReady(courseId: String): Boolean = false
    suspend fun getSavedCourses(): Result<List<Course>> = getSavedCourses("")

    suspend fun saveCourseForOffline(userId: String, course: CourseDetail): Result<Unit> = Result.success(Unit)
    suspend fun unsaveCourse(userId: String, courseId: String): Result<Unit> = Result.success(Unit)
    suspend fun isCourseSaved(userId: String, courseId: String): Boolean = false
    suspend fun isCourseOfflineReady(userId: String, courseId: String): Boolean = false
    suspend fun getSavedCourses(userId: String): Result<List<Course>> = Result.success(emptyList())
    suspend fun getOfflineCourseDetail(courseId: String): Result<CourseDetail?> = Result.success(null)
}

