package com.offline_First.data.local

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCourseRepositoryTest {

    private val repository = LocalCourseRepository()

    @Test
    fun allSevenCourseTracksAreAvailableInCatalog() = runBlocking {
        val coursesResult = repository.getCourses()
        assertTrue(coursesResult.isSuccess)
        val courses = coursesResult.getOrThrow()
        assertEquals(7, courses.size)

        val expectedIds = setOf("c-cpp", "c-dsa", "c-ds", "c-py", "c-ml", "c-fs", "c-sde")
        val actualIds = courses.map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }

    @Test
    fun featuredCoursesIncludeExpectedTracks() = runBlocking {
        val featuredResult = repository.getFeaturedCourses()
        assertTrue(featuredResult.isSuccess)
        val featured = featuredResult.getOrThrow()
        assertEquals(3, featured.size)

        val featuredIds = featured.map { it.id }.toSet()
        assertTrue(featuredIds.contains("c-py"))
        assertTrue(featuredIds.contains("c-dsa"))
        assertTrue(featuredIds.contains("c-fs"))
    }

    @Test
    fun eachCourseHasStructuredModulesAndLessonsWithLegitimateResources() = runBlocking {
        val courseIds = listOf("c-cpp", "c-dsa", "c-ds", "c-py", "c-ml", "c-fs", "c-sde")

        for (id in courseIds) {
            val detailResult = repository.getCourseDetail(id)
            assertTrue("Expected course $id to succeed", detailResult.isSuccess)
            val detail = detailResult.getOrThrow()
            assertNotNull("Course detail for $id should not be null", detail)
            requireNotNull(detail)

            assertTrue("Description must not be empty for $id", detail.description.isNotBlank())
            assertTrue("Category must not be empty for $id", detail.category.isNotBlank())
            assertTrue("Level must not be empty for $id", detail.level.isNotBlank())
            assertTrue("Duration should be positive for $id", (detail.durationHours ?: 0) > 0)
            assertTrue("Modules must not be empty for $id", detail.modules.isNotEmpty())

            var lessonCounter = 0
            for (module in detail.modules) {
                assertTrue("Module title must not be empty in $id", module.title.isNotBlank())
                assertTrue("Lessons must not be empty in module ${module.id}", module.lessons.isNotEmpty())

                for (lesson in module.lessons) {
                    lessonCounter++
                    assertEquals(id, lesson.courseId)
                    assertEquals(module.id, lesson.moduleId)
                    assertTrue("Lesson title must not be blank", lesson.title.isNotBlank())
                    assertTrue("Resource URL must start with https://", lesson.resourceUrl?.startsWith("https://") == true)
                    assertTrue("Resource provider must be declared", !lesson.resourceProvider.isNullOrBlank())
                }
            }
            assertEquals(lessonCounter, detail.totalLessonsCount)
        }
    }

    @Test
    fun sampleQuizzesHaveValidQuestionsAndAnswers() = runBlocking {
        val detail = repository.getCourseDetail("c-cpp").getOrThrow()
        assertNotNull(detail)
        val quizLesson = detail?.modules?.flatMap { it.lessons }?.firstOrNull { it.quiz != null }
        assertNotNull(quizLesson)

        val quiz = quizLesson?.quiz
        assertNotNull(quiz)
        assertTrue(quiz!!.questions.isNotEmpty())

        for (q in quiz.questions) {
            assertTrue(q.question.isNotBlank())
            assertTrue(q.options.size >= 2)
            assertTrue(q.correctIndex in 0 until q.options.size)
            assertTrue(q.explanation.isNotBlank())
        }
    }

    @Test
    fun cppCourseIsVisibleAndResolvesBySlugAndLocalId() = runBlocking {
        // Look up by local ID "c-cpp"
        val detailById = repository.getCourseDetail("c-cpp").getOrThrow()
        assertNotNull(detailById)
        assertEquals("c-cpp", detailById?.id)
        assertEquals("C++ Programming", detailById?.name)

        // Look up by canonical slug "cpp-programming"
        val detailBySlug = repository.getCourseDetail("cpp-programming").getOrThrow()
        assertNotNull(detailBySlug)
        assertEquals("c-cpp", detailBySlug?.id)
        assertEquals("C++ Programming", detailBySlug?.name)
        assertEquals(detailById?.modules?.size, detailBySlug?.modules?.size)
    }

    @Test
    fun courseDetailToCoursePopulatesLessonAndModuleCounts() = runBlocking {
        val detail = repository.getCourseDetail("c-cpp").getOrThrow()!!
        val course = detail.toCourse()

        assertTrue("Course totalLessons must be positive", course.totalLessons > 0)
        assertEquals(detail.totalLessonsCount, course.totalLessons)
        assertTrue("Course modulesCount must be positive", course.modulesCount > 0)
        assertEquals(detail.modules.size, course.modulesCount)
    }

    @Test
    fun getCoursesReflectsCurrentProgress() = runBlocking {
        val store = repository.progressStore
        val lessons = listOf("les-cpp-101")
        store.markLessonCompleted("c-cpp", "les-cpp-101", true)

        val courses = repository.getCourses().getOrThrow()
        val cpp = courses.first { it.id == "c-cpp" }

        assertEquals(1, cpp.completedLessons)
        assertTrue(cpp.progress > 0f)
    }
}
