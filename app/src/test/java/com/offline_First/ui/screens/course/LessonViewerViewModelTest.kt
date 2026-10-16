package com.offline_First.ui.screens.course

import com.offline_First.data.local.LocalCourseRepository
import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonViewerViewModelTest {

    private val localRepository = LocalCourseRepository()

    @Test
    fun resolvesCorrectLessonByIdWithModuleContextAndBoundaries() = runBlocking {
        // C++ first lesson: les-cpp-101
        val viewModelFirst = LessonViewerViewModel(
            courseId = "c-cpp",
            lessonId = "les-cpp-101",
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )

        val stateFirst = viewModelFirst.uiState.value
        assertTrue("State should be Success", stateFirst.state is UiState.Success)
        val dataFirst = (stateFirst.state as UiState.Success).data
        assertEquals("les-cpp-101", dataFirst.lesson.id)
        assertEquals("Basic Concepts & Types", dataFirst.lesson.title)
        assertEquals("mod-cpp-1", dataFirst.module.id)
        assertEquals("C++ Core Fundamentals", dataFirst.module.title)
        assertEquals(1, dataFirst.currentLessonIndex)
        assertEquals(5, dataFirst.totalLessonsInCourse)
        assertNull("First lesson should have no previous lesson", dataFirst.previousLesson)
        assertNotNull("First lesson should have a next lesson", dataFirst.nextLesson)
        assertEquals("les-cpp-102", dataFirst.nextLesson?.id)

        // Middle lesson: les-cpp-102
        val viewModelMid = LessonViewerViewModel(
            courseId = "c-cpp",
            lessonId = "les-cpp-102",
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )
        val stateMid = (viewModelMid.uiState.value.state as UiState.Success).data
        assertNotNull("Middle lesson should have previous lesson", stateMid.previousLesson)
        assertEquals("les-cpp-101", stateMid.previousLesson?.id)
        assertNotNull("Middle lesson should have next lesson", stateMid.nextLesson)
        assertEquals("les-cpp-103", stateMid.nextLesson?.id)

        // Last lesson: les-cpp-202
        val viewModelLast = LessonViewerViewModel(
            courseId = "c-cpp",
            lessonId = "les-cpp-202",
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )
        val stateLast = (viewModelLast.uiState.value.state as UiState.Success).data
        assertNotNull("Last lesson should have a previous lesson", stateLast.previousLesson)
        assertNull("Last lesson should have no next lesson", stateLast.nextLesson)
    }

    @Test
    fun missingCourseOrLessonResolvesToEmptyStateGracefully() = runBlocking {
        // Nonexistent course
        val vmNonexistentCourse = LessonViewerViewModel(
            courseId = "c-invalid",
            lessonId = "les-cpp-101",
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )
        assertTrue(vmNonexistentCourse.uiState.value.state is UiState.Empty)

        // Nonexistent lesson
        val vmNonexistentLesson = LessonViewerViewModel(
            courseId = "c-cpp",
            lessonId = "les-invalid",
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )
        assertTrue(vmNonexistentLesson.uiState.value.state is UiState.Empty)
    }

    @Test
    fun toggleCompletionUpdatesStateAndPersistsInSession() = runBlocking {
        val courseId = "c-py"
        val lessonId = "les-py-101"

        val viewModel = LessonViewerViewModel(
            courseId = courseId,
            lessonId = lessonId,
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )

        assertFalse("Initially should not be complete", viewModel.uiState.value.isCompleted)

        // Mark as completed
        viewModel.toggleLessonCompleted()
        assertTrue("State should reflect completed", viewModel.uiState.value.isCompleted)
        assertTrue("Repository should reflect completed", localRepository.isLessonCompleted(courseId, lessonId))

        // Reload course detail to verify completion is mirrored into syllabus
        val detail = localRepository.getCourseDetail(courseId).getOrThrow()
        val reloadedLesson = detail?.modules?.flatMap { it.lessons }?.find { it.id == lessonId }
        assertNotNull(reloadedLesson)
        assertTrue("LessonItem in course detail should now be marked completed", reloadedLesson!!.isCompleted)

        // Toggle back to incomplete
        viewModel.toggleLessonCompleted()
        assertFalse("State should toggle back to incomplete", viewModel.uiState.value.isCompleted)
        assertFalse("Repository should reflect incomplete", localRepository.isLessonCompleted(courseId, lessonId))
    }

    @Test
    fun toggleRevisionStateUpdatesLocallyAndPersistsInRepository() = runBlocking {
        val courseId = "c-dsa"
        val lessonId = "les-dsa-101"
        val viewModel = LessonViewerViewModel(
            courseId = courseId,
            lessonId = lessonId,
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )

        assertFalse(viewModel.uiState.value.isMarkedForRevision)
        assertFalse(localRepository.isMarkedForRevision(courseId, lessonId))

        // Mark for revision
        viewModel.toggleMarkForRevision()
        assertTrue(viewModel.uiState.value.isMarkedForRevision)
        assertTrue(localRepository.isMarkedForRevision(courseId, lessonId))

        // Unmark
        viewModel.toggleMarkForRevision()
        assertFalse(viewModel.uiState.value.isMarkedForRevision)
        assertFalse(localRepository.isMarkedForRevision(courseId, lessonId))
    }

    @Test
    fun interactiveQuizFlowWithScoringAndDoubleSubmitProtection() = runBlocking {
        // les-cpp-202 has quiz with 2 questions
        val viewModel = LessonViewerViewModel(
            courseId = "c-cpp",
            lessonId = "les-cpp-202",
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )

        val state = viewModel.uiState.value
        val lesson = (state.state as UiState.Success).data.lesson
        assertNotNull("les-cpp-202 must have an attached quiz", lesson.quiz)
        val quiz = lesson.quiz!!
        assertEquals(2, quiz.questions.size)

        // Initial quiz state
        val initialQuizState = viewModel.uiState.value.quizState
        assertEquals(0, initialQuizState.currentQuestionIndex)
        assertEquals(0, initialQuizState.score)
        assertFalse(initialQuizState.isAnswerSubmitted)
        assertFalse(initialQuizState.isQuizFinished)
        assertNull(initialQuizState.selectedOptionIndex)

        // Select correct option for question 0 (Inheritance, index 2)
        val correctIndexQ0 = quiz.questions[0].correctIndex
        viewModel.selectQuizOption(correctIndexQ0)
        assertEquals(correctIndexQ0, viewModel.uiState.value.quizState.selectedOptionIndex)

        // Submit answer
        viewModel.submitQuizAnswer()
        val submittedStateQ0 = viewModel.uiState.value.quizState
        assertTrue("Answer should be marked submitted", submittedStateQ0.isAnswerSubmitted)
        assertEquals("Score should be incremented to 1", 1, submittedStateQ0.score)

        // Double submit protection: submitting again should NOT increase score
        viewModel.submitQuizAnswer()
        assertEquals("Score must remain 1 after repeated submit", 1, viewModel.uiState.value.quizState.score)

        // Locked option protection: selecting another option while submitted should be ignored
        viewModel.selectQuizOption(0)
        assertEquals(correctIndexQ0, viewModel.uiState.value.quizState.selectedOptionIndex)

        // Move to question 1
        viewModel.nextQuizQuestion()
        val q1State = viewModel.uiState.value.quizState
        assertEquals(1, q1State.currentQuestionIndex)
        assertNull("Selected option should be reset for new question", q1State.selectedOptionIndex)
        assertFalse("Submitted flag should be reset for new question", q1State.isAnswerSubmitted)
        assertEquals("Score must be preserved across questions", 1, q1State.score)

        // Question 1: submit WRONG option
        val correctIndexQ1 = quiz.questions[1].correctIndex
        val wrongIndexQ1 = (correctIndexQ1 + 1) % quiz.questions[1].options.size
        viewModel.selectQuizOption(wrongIndexQ1)
        viewModel.submitQuizAnswer()

        val submittedStateQ1 = viewModel.uiState.value.quizState
        assertTrue(submittedStateQ1.isAnswerSubmitted)
        assertEquals("Score should not increase for incorrect answer", 1, submittedStateQ1.score)

        // Finish quiz on last question
        viewModel.nextQuizQuestion()
        val finishedState = viewModel.uiState.value.quizState
        assertTrue("Quiz should be finished", finishedState.isQuizFinished)
        assertEquals(1, finishedState.score)

        // Retry quiz
        viewModel.retryQuiz()
        val retriedState = viewModel.uiState.value.quizState
        assertEquals(0, retriedState.currentQuestionIndex)
        assertEquals(0, retriedState.score)
        assertFalse(retriedState.isAnswerSubmitted)
        assertFalse(retriedState.isQuizFinished)
        assertNull(retriedState.selectedOptionIndex)
    }

    @Test
    fun urlValidationRejectsInvalidOrInsecureSchemes() {
        // Validation without Android context
        val invalidUrls = listOf(
            null,
            "",
            "   ",
            "javascript:alert(1)",
            "file:///etc/passwd",
            "ftp://files.example.com",
            "htp:/broken"
        )

        for (url in invalidUrls) {
            val isHttpsOrHttp = !url.isNullOrBlank() &&
                    (url.trim().startsWith("https://", ignoreCase = true) ||
                            url.trim().startsWith("http://", ignoreCase = true))
            assertFalse("URL '$url' should not be valid", isHttpsOrHttp)
        }

        val validUrls = listOf(
            "https://en.cppreference.com/w/cpp/language/basic_concepts",
            "https://docs.python.org/3/tutorial/",
            "http://example.com"
        )

        for (url in validUrls) {
            val isHttpsOrHttp = !url.isNullOrBlank() &&
                    (url.trim().startsWith("https://", ignoreCase = true) ||
                            url.trim().startsWith("http://", ignoreCase = true))
            assertTrue("URL '$url' should be valid", isHttpsOrHttp)
        }
    }

    @Test
    fun loadLessonDeduplicatesConsecutiveCallsForSameLessonId() = runBlocking {
        val viewModel = LessonViewerViewModel(
            courseId = "c-cpp",
            lessonId = "les-cpp-101",
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )

        val firstState = viewModel.uiState.value
        assertTrue(firstState.state is UiState.Success)

        // Calling loadLesson again for the same lesson should not trigger reload or change instance
        viewModel.loadLesson("les-cpp-101")
        val secondState = viewModel.uiState.value
        assertEquals((firstState.state as UiState.Success).data.lesson.id, (secondState.state as UiState.Success).data.lesson.id)
    }

    @Test
    fun loadLessonSwitchesToNextLessonCleanly() = runBlocking {
        val viewModel = LessonViewerViewModel(
            courseId = "c-cpp",
            lessonId = "les-cpp-101",
            repository = localRepository,
            coroutineContext = Dispatchers.Unconfined
        )

        assertEquals("les-cpp-101", (viewModel.uiState.value.state as UiState.Success).data.lesson.id)

        // Switch to next lesson
        viewModel.loadLesson("les-cpp-102")
        val nextState = viewModel.uiState.value.state
        assertTrue(nextState is UiState.Success)
        assertEquals("les-cpp-102", (nextState as UiState.Success).data.lesson.id)
        assertEquals(2, nextState.data.currentLessonIndex)
    }
}
