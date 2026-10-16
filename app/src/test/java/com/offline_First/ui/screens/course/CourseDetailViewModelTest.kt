package com.offline_First.ui.screens.course

import com.offline_First.data.local.LocalCourseRepository
import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseDetailViewModelTest {

    private val localRepository = LocalCourseRepository()

    @Test
    fun loadsEachOfTheSevenCoursesWithFullMetadataAndOrderedLessons() = runBlocking {
        val courseIds = listOf("c-cpp", "c-dsa", "c-ds", "c-py", "c-ml", "c-fs", "c-sde")

        for (id in courseIds) {
            val viewModel = CourseDetailViewModel(
                courseId = id,
                repository = localRepository,
                coroutineContext = Dispatchers.Unconfined
            )

            val state = viewModel.uiState.value
            assertTrue("Expected Success state for $id", state.courseDetailState is UiState.Success)
            val detail = (state.courseDetailState as UiState.Success).data
            assertEquals(id, detail.id)
            assertTrue("Name must be set for $id", detail.name.isNotBlank())
            assertTrue("Category must be set for $id", detail.category.isNotBlank())
            assertTrue("Level must be set for $id", detail.level.isNotBlank())
            assertTrue("Duration should be positive for $id", (detail.durationHours ?: 0) > 0)
            assertTrue("Modules must exist for $id", detail.modules.isNotEmpty())

            // Check modules order
            val moduleOrders = detail.modules.map { it.displayOrder }
            assertEquals(moduleOrders.sorted(), moduleOrders)

            // Check lessons order within each module
            for (module in detail.modules) {
                assertTrue("Module lessons must exist in ${module.id}", module.lessons.isNotEmpty())
                val lessonOrders = module.lessons.map { it.displayOrder }
                assertEquals(lessonOrders.sorted(), lessonOrders)
            }
        }
    }

    @Test
    fun missingCourseProducesEmptyStateWithoutCrashing() = runBlocking {
        val viewModel = CourseDetailViewModel(
            courseId = "c-nonexistent",
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )

        val state = viewModel.uiState.value
        assertTrue("State should be Empty for non-existent course", state.courseDetailState is UiState.Empty)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun repositoryErrorProducesErrorStateAndSupportsRetry() = runBlocking {
        var shouldFail = true
        val failingRepo = object : CourseRepository {
            override suspend fun getCourses(): Result<List<Course>> = Result.success(emptyList())
            override suspend fun getFeaturedCourses(): Result<List<Course>> = Result.success(emptyList())
            override suspend fun getCourseDetail(courseId: String): Result<CourseDetail?> {
                return if (shouldFail) {
                    Result.failure(RuntimeException("Network timeout"))
                } else {
                    localRepository.getCourseDetail(courseId)
                }
            }
            override suspend fun markLessonCompleted(courseId: String, lessonId: String, completed: Boolean): Result<Unit> =
                Result.success(Unit)
            override suspend fun isLessonCompleted(courseId: String, lessonId: String): Boolean = false
            override suspend fun addRevisionItem(item: com.offline_First.domain.model.RevisionItem): Result<Unit> = Result.success(Unit)
            override suspend fun removeRevisionItem(courseId: String, lessonId: String): Result<Unit> = Result.success(Unit)
            override suspend fun isMarkedForRevision(courseId: String, lessonId: String): Boolean = false
            override suspend fun getRevisionItems(): Result<List<com.offline_First.domain.model.RevisionItem>> = Result.success(emptyList())
            override suspend fun getCompletedLessonCount(courseId: String, lessonIds: Collection<String>): Int = 0
            override suspend fun getCourseProgressPercentage(courseId: String, lessonIds: Collection<String>): Float = 0f
            override suspend fun isCourseCompleted(courseId: String, lessonIds: Collection<String>): Boolean = false
            override suspend fun getCertificate(courseId: String): com.offline_First.domain.model.CourseCertificate? = null
            override suspend fun getAllCertificates(): List<com.offline_First.domain.model.CourseCertificate> = emptyList()
            override suspend fun issueCertificate(courseId: String, courseName: String, learnerName: String, totalLessons: Int): com.offline_First.domain.model.CourseCertificate? = null
        }

        val viewModel = CourseDetailViewModel(
            courseId = "c-py",
            repository = failingRepo,
            coroutineContext = Dispatchers.Unconfined
        )

        val errorState = viewModel.uiState.value
        assertTrue("State should be Error", errorState.courseDetailState is UiState.Error)
        assertEquals("Network timeout", (errorState.courseDetailState as UiState.Error).message)

        // Retry after recovery
        shouldFail = false
        viewModel.loadCourseDetail()

        val recoveredState = viewModel.uiState.value
        assertTrue("State should recover to Success", recoveredState.courseDetailState is UiState.Success)
        assertEquals("Python Programming", (recoveredState.courseDetailState as UiState.Success).data.name)
    }

    @Test
    fun fallbackRepositoryReturnsLocalDetailWhenOnlineFails() = runBlocking {
        val failingOnlineRepo = object : CourseRepository {
            override suspend fun getCourses(): Result<List<Course>> =
                Result.failure(RuntimeException("Remote offline"))
            override suspend fun getFeaturedCourses(): Result<List<Course>> =
                Result.failure(RuntimeException("Remote offline"))
            override suspend fun getCourseDetail(courseId: String): Result<CourseDetail?> =
                Result.failure(RuntimeException("Remote offline"))
            override suspend fun markLessonCompleted(courseId: String, lessonId: String, completed: Boolean): Result<Unit> =
                Result.failure(RuntimeException("Remote offline"))
            override suspend fun isLessonCompleted(courseId: String, lessonId: String): Boolean = false
            override suspend fun addRevisionItem(item: com.offline_First.domain.model.RevisionItem): Result<Unit> =
                Result.failure(RuntimeException("Remote offline"))
            override suspend fun removeRevisionItem(courseId: String, lessonId: String): Result<Unit> =
                Result.failure(RuntimeException("Remote offline"))
            override suspend fun isMarkedForRevision(courseId: String, lessonId: String): Boolean = false
            override suspend fun getRevisionItems(): Result<List<com.offline_First.domain.model.RevisionItem>> =
                Result.failure(RuntimeException("Remote offline"))
            override suspend fun getCompletedLessonCount(courseId: String, lessonIds: Collection<String>): Int = 0
            override suspend fun getCourseProgressPercentage(courseId: String, lessonIds: Collection<String>): Float = 0f
            override suspend fun isCourseCompleted(courseId: String, lessonIds: Collection<String>): Boolean = false
            override suspend fun getCertificate(courseId: String): com.offline_First.domain.model.CourseCertificate? = null
            override suspend fun getAllCertificates(): List<com.offline_First.domain.model.CourseCertificate> = emptyList()
            override suspend fun issueCertificate(courseId: String, courseName: String, learnerName: String, totalLessons: Int): com.offline_First.domain.model.CourseCertificate? = null
        }

        // Simulate FallbackCourseRepository behavior
        val fallbackRepo = object : CourseRepository {
            override suspend fun getCourses(): Result<List<Course>> {
                val remote = runCatching { failingOnlineRepo.getCourses().getOrThrow() }
                return remote.recoverCatching { localRepository.getCourses().getOrThrow() }
            }

            override suspend fun getFeaturedCourses(): Result<List<Course>> {
                val remote = runCatching { failingOnlineRepo.getFeaturedCourses().getOrThrow() }
                return remote.recoverCatching { localRepository.getFeaturedCourses().getOrThrow() }
            }

            override suspend fun getCourseDetail(courseId: String): Result<CourseDetail?> {
                val remote = try {
                    failingOnlineRepo.getCourseDetail(courseId)
                } catch (_: Exception) {
                    null
                }
                val detail = remote?.getOrNull()
                if (remote?.isSuccess == true && detail != null) {
                    return remote
                }
                return localRepository.getCourseDetail(courseId)
            }

            override suspend fun markLessonCompleted(courseId: String, lessonId: String, completed: Boolean): Result<Unit> {
                return localRepository.markLessonCompleted(courseId, lessonId, completed)
            }

            override suspend fun isLessonCompleted(courseId: String, lessonId: String): Boolean {
                return localRepository.isLessonCompleted(courseId, lessonId)
            }

            override suspend fun addRevisionItem(item: com.offline_First.domain.model.RevisionItem): Result<Unit> {
                return localRepository.addRevisionItem(item)
            }

            override suspend fun removeRevisionItem(courseId: String, lessonId: String): Result<Unit> {
                return localRepository.removeRevisionItem(courseId, lessonId)
            }

            override suspend fun isMarkedForRevision(courseId: String, lessonId: String): Boolean {
                return localRepository.isMarkedForRevision(courseId, lessonId)
            }

            override suspend fun getRevisionItems(): Result<List<com.offline_First.domain.model.RevisionItem>> {
                return localRepository.getRevisionItems()
            }

            override suspend fun getCompletedLessonCount(courseId: String, lessonIds: Collection<String>): Int {
                return localRepository.getCompletedLessonCount(courseId, lessonIds)
            }

            override suspend fun getCourseProgressPercentage(courseId: String, lessonIds: Collection<String>): Float {
                return localRepository.getCourseProgressPercentage(courseId, lessonIds)
            }

            override suspend fun isCourseCompleted(courseId: String, lessonIds: Collection<String>): Boolean {
                return localRepository.isCourseCompleted(courseId, lessonIds)
            }

            override suspend fun getCertificate(courseId: String): com.offline_First.domain.model.CourseCertificate? {
                return localRepository.getCertificate(courseId)
            }

            override suspend fun getAllCertificates(): List<com.offline_First.domain.model.CourseCertificate> {
                return localRepository.getAllCertificates()
            }

            override suspend fun issueCertificate(courseId: String, courseName: String, learnerName: String, totalLessons: Int): com.offline_First.domain.model.CourseCertificate? {
                return localRepository.issueCertificate(courseId, courseName, learnerName, totalLessons)
            }
        }


        val viewModel = CourseDetailViewModel(
            courseId = "c-cpp",
            repository = fallbackRepo,
            coroutineContext = Dispatchers.Unconfined
        )

        val state = viewModel.uiState.value
        assertTrue("Should fallback to local course data", state.courseDetailState is UiState.Success)
        val course = (state.courseDetailState as UiState.Success).data
        assertEquals("c-cpp", course.id)
        assertEquals("C++ Programming", course.name)
    }
}
