package com.offline_First.ui.screens.course

import com.offline_First.data.local.CourseProgressStore
import com.offline_First.data.local.LocalCourseRepository
import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.RevisionItem
import com.offline_First.domain.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RevisionListViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun emptyRevisionListProducesEmptyState() = runBlocking {
        val testFile = File(tempFolder.root, "empty_revision_store.json")
        val repository = LocalCourseRepository(CourseProgressStore(testFile))

        val viewModel = RevisionListViewModel(
            repository = repository,
            coroutineContext = Dispatchers.Unconfined
        )

        val state = viewModel.uiState.value.state
        assertTrue("Empty revision list must produce UiState.Empty", state is UiState.Empty)
    }

    @Test
    fun populatedRevisionListProducesSuccessStateAndItemRemovalWorks() = runBlocking {
        val testFile = File(tempFolder.root, "populated_revision_store.json")
        val repository = LocalCourseRepository(CourseProgressStore(testFile))

        val item1 = RevisionItem(
            courseId = "c-py",
            courseName = "Python Programming",
            lessonId = "les-py-101",
            lessonTitle = "Variables & Types",
            moduleTitle = "Core Basics"
        )
        val item2 = RevisionItem(
            courseId = "c-cpp",
            courseName = "C++ Programming",
            lessonId = "les-cpp-101",
            lessonTitle = "Basic Concepts",
            moduleTitle = "Fundamentals"
        )

        repository.addRevisionItem(item1)
        repository.addRevisionItem(item2)

        val viewModel = RevisionListViewModel(
            repository = repository,
            coroutineContext = Dispatchers.Unconfined
        )

        val state = viewModel.uiState.value.state
        assertTrue("Populated list must produce UiState.Success", state is UiState.Success)
        val items = (state as UiState.Success).data
        assertEquals(2, items.size)

        // Remove item1
        viewModel.removeRevisionItem("c-py", "les-py-101")
        val stateAfterFirstRemoval = viewModel.uiState.value.state
        assertTrue(stateAfterFirstRemoval is UiState.Success)
        val itemsAfterFirst = (stateAfterFirstRemoval as UiState.Success).data
        assertEquals(1, itemsAfterFirst.size)
        assertEquals("les-cpp-101", itemsAfterFirst.first().lessonId)
        assertFalse(repository.isMarkedForRevision("c-py", "les-py-101"))
        assertTrue(repository.isMarkedForRevision("c-cpp", "les-cpp-101"))

        // Remove item2 -> list becomes empty
        viewModel.removeRevisionItem("c-cpp", "les-cpp-101")
        val stateAfterSecondRemoval = viewModel.uiState.value.state
        assertTrue("When all items removed, state transitions to UiState.Empty", stateAfterSecondRemoval is UiState.Empty)
        assertFalse(repository.isMarkedForRevision("c-cpp", "les-cpp-101"))
    }

    @Test
    fun repositoryFailureProducesErrorState() = runBlocking {
        val failingRepo = object : CourseRepository {
            override suspend fun getCourses(limit: Int?, offset: Int): Result<List<Course>> = Result.success(emptyList())
            override suspend fun getFeaturedCourses(): Result<List<Course>> = Result.success(emptyList())
            override suspend fun getCourseDetail(courseId: String): Result<CourseDetail?> = Result.success(null)
            override suspend fun markLessonCompleted(courseId: String, lessonId: String, completed: Boolean): Result<Unit> = Result.success(Unit)
            override suspend fun isLessonCompleted(courseId: String, lessonId: String): Boolean = false
            override suspend fun addRevisionItem(item: RevisionItem): Result<Unit> = Result.success(Unit)
            override suspend fun removeRevisionItem(courseId: String, lessonId: String): Result<Unit> = Result.success(Unit)
            override suspend fun isMarkedForRevision(courseId: String, lessonId: String): Boolean = false
            override suspend fun getRevisionItems(): Result<List<RevisionItem>> =
                Result.failure(RuntimeException("Database disk I/O error"))
            override suspend fun getCompletedLessonCount(courseId: String, lessonIds: Collection<String>): Int = 0
            override suspend fun getCourseProgressPercentage(courseId: String, lessonIds: Collection<String>): Float = 0f
            override suspend fun isCourseCompleted(courseId: String, lessonIds: Collection<String>): Boolean = false
            override suspend fun getCertificate(courseId: String): com.offline_First.domain.model.CourseCertificate? = null
            override suspend fun getAllCertificates(): List<com.offline_First.domain.model.CourseCertificate> = emptyList()
            override suspend fun issueCertificate(courseId: String, courseName: String, learnerName: String, totalLessons: Int): com.offline_First.domain.model.CourseCertificate? = null
        }

        val viewModel = RevisionListViewModel(
            repository = failingRepo,
            coroutineContext = Dispatchers.Unconfined
        )

        val state = viewModel.uiState.value.state
        assertTrue("Repository error must produce UiState.Error", state is UiState.Error)
        assertEquals("Database disk I/O error", (state as UiState.Error).message)
    }
}
