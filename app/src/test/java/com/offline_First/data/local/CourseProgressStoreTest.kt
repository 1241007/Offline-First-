package com.offline_First.data.local

import com.offline_First.domain.model.RevisionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CourseProgressStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun completionPersistsAcrossStoreRecreation() {
        val testFile = File(tempFolder.root, "test_course_storage.json")

        // First store session
        val store1 = CourseProgressStore(testFile)
        assertFalse(store1.isLessonCompleted("c-cpp", "les-cpp-101"))
        store1.markLessonCompleted("c-cpp", "les-cpp-101", true)
        store1.markLessonCompleted("c-dsa", "les-dsa-101", true)
        assertTrue(store1.isLessonCompleted("c-cpp", "les-cpp-101"))
        assertTrue(store1.isLessonCompleted("c-dsa", "les-dsa-101"))

        // Recreate store instance pointing to same file (simulating app restart / process recreation)
        val store2 = CourseProgressStore(testFile)
        assertTrue("Completed lesson must survive store recreation", store2.isLessonCompleted("c-cpp", "les-cpp-101"))
        assertTrue("Completed lesson must survive store recreation", store2.isLessonCompleted("c-dsa", "les-dsa-101"))
        assertFalse("Unmarked lesson should be incomplete", store2.isLessonCompleted("c-cpp", "les-cpp-102"))

        // Unmark and verify persistence
        store2.markLessonCompleted("c-cpp", "les-cpp-101", false)
        assertFalse(store2.isLessonCompleted("c-cpp", "les-cpp-101"))

        val store3 = CourseProgressStore(testFile)
        assertFalse(store3.isLessonCompleted("c-cpp", "les-cpp-101"))
        assertTrue(store3.isLessonCompleted("c-dsa", "les-dsa-101"))
    }

    @Test
    fun revisionItemsPersistAcrossStoreRecreation() {
        val testFile = File(tempFolder.root, "test_revision_storage.json")

        val store1 = CourseProgressStore(testFile)
        assertTrue(store1.getRevisionItems().isEmpty())

        val item1 = RevisionItem(
            courseId = "c-py",
            courseName = "Python Programming",
            lessonId = "les-py-101",
            lessonTitle = "Variables & Data Types",
            moduleTitle = "Python Fundamentals"
        )
        val item2 = RevisionItem(
            courseId = "c-dsa",
            courseName = "Data Structures & Algorithms",
            lessonId = "les-dsa-101",
            lessonTitle = "Arrays & Memory Layout",
            moduleTitle = "Core Data Structures"
        )

        store1.addRevisionItem(item1)
        store1.addRevisionItem(item2)
        // Attempt duplicate addition
        store1.addRevisionItem(item1)

        assertEquals("Duplicates should be prevented", 2, store1.getRevisionItems().size)
        assertTrue(store1.isMarkedForRevision("c-py", "les-py-101"))
        assertTrue(store1.isMarkedForRevision("c-dsa", "les-dsa-101"))

        // Recreate store pointing to same file
        val store2 = CourseProgressStore(testFile)
        val itemsReloaded = store2.getRevisionItems()
        assertEquals(2, itemsReloaded.size)
        assertTrue(store2.isMarkedForRevision("c-py", "les-py-101"))
        assertTrue(store2.isMarkedForRevision("c-dsa", "les-dsa-101"))

        // Remove item and verify persistence
        store2.removeRevisionItem("c-py", "les-py-101")
        assertFalse(store2.isMarkedForRevision("c-py", "les-py-101"))
        assertEquals(1, store2.getRevisionItems().size)

        val store3 = CourseProgressStore(testFile)
        assertFalse(store3.isMarkedForRevision("c-py", "les-py-101"))
        assertTrue(store3.isMarkedForRevision("c-dsa", "les-dsa-101"))
        assertEquals(1, store3.getRevisionItems().size)
        assertEquals("les-dsa-101", store3.getRevisionItems().first().lessonId)
    }

    @Test
    fun repositoryWithCustomFileStorageMaintainsFullPersistence() {
        val testFile = File(tempFolder.root, "repo_storage.json")
        val progressStore = CourseProgressStore(testFile)
        val repository = LocalCourseRepository(progressStore)

        val item = RevisionItem(
            courseId = "c-ml",
            courseName = "Machine Learning",
            lessonId = "les-ml-101",
            lessonTitle = "Supervised Learning",
            moduleTitle = "ML Core"
        )

        kotlinx.coroutines.runBlocking {
            repository.addRevisionItem(item)
            repository.markLessonCompleted("c-ml", "les-ml-101", true)

            // Verify course detail reflects both flags
            val detail = repository.getCourseDetail("c-ml").getOrThrow()
            val lesson = detail?.modules?.flatMap { it.lessons }?.find { it.id == "les-ml-101" }
            assertFalse(lesson == null)
            assertTrue(lesson!!.isCompleted)
            assertTrue(lesson.isMarkedForRevision)

            // Re-instantiate repository with new store pointing to same file
            val newStore = CourseProgressStore(testFile)
            val newRepo = LocalCourseRepository(newStore)

            assertTrue(newRepo.isLessonCompleted("c-ml", "les-ml-101"))
            assertTrue(newRepo.isMarkedForRevision("c-ml", "les-ml-101"))
            val revisionList = newRepo.getRevisionItems().getOrThrow()
            assertEquals(1, revisionList.size)
            assertEquals("les-ml-101", revisionList.first().lessonId)
        }
    }

    @Test
    fun progressCalculationHandlesZeroLessonsWithoutDivisionByZero() {
        val testFile = File(tempFolder.root, "test_zero_lessons.json")
        val store = CourseProgressStore(testFile)

        // Empty lesson list
        val progress = store.getCourseProgressPercentage("c-empty", emptyList())
        assertEquals("Zero lessons must result in 0.0f progress", 0.0f, progress, 0.001f)
        assertFalse("Course with zero lessons cannot be completed", store.isCourseCompleted("c-empty", emptyList()))

        // Certificate issuance must return null for empty courses (totalLessons <= 0)
        val cert = store.issueCertificate("c-empty", "Empty Course", "Alice", 0)
        assertTrue("Empty course must not receive a certificate", cert == null)
    }

    @Test
    fun progressCalculationAndCertificateIssuanceRequireAllLessons() {
        val testFile = File(tempFolder.root, "test_cert_issuance.json")
        val store = CourseProgressStore(testFile)

        val lessons = listOf("les-1", "les-2", "les-3", "les-4")
        assertEquals(0, store.getCompletedLessonCount("c-test", lessons))
        assertEquals(0.0f, store.getCourseProgressPercentage("c-test", lessons), 0.001f)
        assertFalse(store.isCourseCompleted("c-test", lessons))

        // Complete 1 lesson (25%)
        store.markLessonCompleted("c-test", "les-1", true)
        assertEquals(1, store.getCompletedLessonCount("c-test", lessons))
        assertEquals(0.25f, store.getCourseProgressPercentage("c-test", lessons), 0.001f)
        assertFalse(store.isCourseCompleted("c-test", lessons))

        // Complete 3 lessons (75%)
        store.markLessonCompleted("c-test", "les-2", true)
        store.markLessonCompleted("c-test", "les-3", true)
        assertEquals(3, store.getCompletedLessonCount("c-test", lessons))
        assertEquals(0.75f, store.getCourseProgressPercentage("c-test", lessons), 0.001f)
        assertFalse(store.isCourseCompleted("c-test", lessons))

        // Complete 4th lesson (100%)
        store.markLessonCompleted("c-test", "les-4", true)
        assertEquals(4, store.getCompletedLessonCount("c-test", lessons))
        assertEquals(1.0f, store.getCourseProgressPercentage("c-test", lessons), 0.001f)
        assertTrue(store.isCourseCompleted("c-test", lessons))

        // Issue certificate
        val cert = store.issueCertificate("c-test", "Test Course", "Alice", lessons.size)
        assertFalse("100% course receives certificate", cert == null)
        assertEquals("c-test", cert!!.courseId)
        assertEquals("Test Course", cert.courseName)
        assertEquals("Alice", cert.learnerName)
        assertTrue("Certificate ID must follow EDUNOVA-CERT pattern", cert.id.startsWith("EDUNOVA-CERT-"))
        assertEquals("LOCAL_PERSISTED", cert.verificationType)

        // Prevent duplicate certificate issuance
        val cert2 = store.issueCertificate("c-test", "Test Course", "Alice", lessons.size)
        assertFalse(cert2 == null)
        assertEquals("Duplicate certificate must retain exact same certificate ID", cert.id, cert2!!.id)
        assertEquals(cert.issuedAtTimestamp, cert2.issuedAtTimestamp)

        // Persistence test across store recreation
        val storeReloaded = CourseProgressStore(testFile)
        val reloadedCert = storeReloaded.getCertificate("c-test")
        assertFalse("Reloaded certificate should exist", reloadedCert == null)
        assertEquals(cert.id, reloadedCert!!.id)
        assertEquals("Alice", reloadedCert.learnerName)
    }
}
