package com.offline_First

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.offline_First.data.local.ChatCacheDatabase
import com.offline_First.data.local.EncryptedTokenStorage
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.CourseModule
import com.offline_First.domain.model.LessonItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Android OS Instrumented Test running on Android runtime / emulator (Pixel 9 AVD),
 * exercising the complete Offline-First architecture on real Android SQLite and EncryptedSharedPreferences.
 */
@RunWith(AndroidJUnit4::class)
class OfflineFirstRealDeviceInstrumentedTest {

    private lateinit var appContext: Context
    private lateinit var database: ChatCacheDatabase
    private lateinit var tokenStorage: EncryptedTokenStorage

    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        database = ChatCacheDatabase(appContext)
        tokenStorage = EncryptedTokenStorage(appContext)

        // Clean any test records
        val db = database.writableDatabase
        db.delete("local_saved_courses", "user_id LIKE ?", arrayOf("device-test-%"))
        db.delete("local_saved_roadmaps", "user_id LIKE ?", arrayOf("device-test-%"))
        db.delete("local_course_details", "course_id LIKE ?", arrayOf("dev-course-%"))
        db.delete("local_course_modules", "course_id LIKE ?", arrayOf("dev-course-%"))
        db.delete("local_lessons", "course_id LIKE ?", arrayOf("dev-course-%"))
        db.delete("local_user_lesson_progress", "user_id LIKE ?", arrayOf("device-test-%"))
        db.delete("local_user_course_progress", "user_id LIKE ?", arrayOf("device-test-%"))
        tokenStorage.clearTokens()
        tokenStorage.clearUserIdentity()
    }

    @After
    fun tearDown() {
        val db = database.writableDatabase
        db.delete("local_saved_courses", "user_id LIKE ?", arrayOf("device-test-%"))
        db.delete("local_saved_roadmaps", "user_id LIKE ?", arrayOf("device-test-%"))
        db.delete("local_course_details", "course_id LIKE ?", arrayOf("dev-course-%"))
        db.delete("local_course_modules", "course_id LIKE ?", arrayOf("dev-course-%"))
        db.delete("local_lessons", "course_id LIKE ?", arrayOf("dev-course-%"))
        db.delete("local_user_lesson_progress", "user_id LIKE ?", arrayOf("device-test-%"))
        db.delete("local_user_course_progress", "user_id LIKE ?", arrayOf("device-test-%"))
        tokenStorage.clearTokens()
        tokenStorage.clearUserIdentity()
    }

    @Test
    fun testRealDeviceSQLiteCourseDetailsAndLessonPersistence() {
        val userId = "device-test-user-course"
        val courseDetail = CourseDetail(
            id = "dev-course-101",
            name = "Android Offline Systems",
            description = "Mastering SQLite and WorkManager on Android",
            icon = "book",
            accent = CourseAccent.PRIMARY,
            category = "Engineering",
            level = "Intermediate",
            durationHours = 4,
            prerequisites = listOf("Kotlin", "Android SDK"),
            modules = listOf(
                CourseModule(
                    id = "dev-mod-1",
                    courseId = "dev-course-101",
                    title = "Persistence Architecture",
                    description = "Module on offline storage",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "dev-les-1",
                            moduleId = "dev-mod-1",
                            courseId = "dev-course-101",
                            title = "SQLite B-Tree Storage",
                            description = "Persisting binary structures.",
                            contentType = "article",
                            durationMinutes = 15,
                            displayOrder = 1,
                            resourceUrl = null,
                            resourceProvider = null,
                            videoUrl = null,
                            videoProvider = null,
                            isCompleted = false
                        ),
                        LessonItem(
                            id = "dev-les-2",
                            moduleId = "dev-mod-1",
                            courseId = "dev-course-101",
                            title = "Encrypted Storage",
                            description = "Keystore integration.",
                            contentType = "article",
                            durationMinutes = 20,
                            displayOrder = 2,
                            resourceUrl = null,
                            resourceProvider = null,
                            videoUrl = null,
                            videoProvider = null,
                            isCompleted = false
                        )
                    )
                )
            )
        )

        // Act: Persist atomically on real Android SQLite
        val saved = database.saveCourseForOffline(userId, courseDetail, downloadStatus = "completed", syncStatus = "synced")
        assertTrue("Course should be saved and marked offline ready", saved)

        // Assert: Verify retrieval from real SQLite
        val loadedCourse = database.getOfflineCourseDetail("dev-course-101", userId)
        assertNotNull(loadedCourse)
        assertEquals("Android Offline Systems", loadedCourse?.name)
        assertEquals(listOf("Kotlin", "Android SDK"), loadedCourse?.prerequisites)
        assertEquals(1, loadedCourse?.modules?.size)
        assertEquals(2, loadedCourse?.modules?.get(0)?.lessons?.size)
        assertEquals("dev-les-1", loadedCourse?.modules?.get(0)?.lessons?.get(0)?.id)
        assertEquals("dev-les-2", loadedCourse?.modules?.get(0)?.lessons?.get(1)?.id)

        assertTrue(database.isCourseSaved(userId, "dev-course-101"))
        assertTrue(database.isCourseOfflineReady(userId, "dev-course-101"))
    }

    @Test
    fun testRealDeviceAccountIsolationAndGuardedDelete() {
        val userA = "device-test-user-a"
        val userB = "device-test-user-b"
        val courseId = "dev-course-iso"

        val courseDetail = CourseDetail(
            id = courseId,
            name = "Device Isolation Course",
            description = "Isolation verification",
            icon = "shield",
            accent = CourseAccent.SECONDARY,
            category = "Security",
            level = "Advanced",
            durationHours = 2,
            prerequisites = emptyList(),
            modules = listOf(
                CourseModule(
                    id = "mod-iso-1",
                    courseId = courseId,
                    title = "Module 1",
                    description = "",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-iso-1",
                            moduleId = "mod-iso-1",
                            courseId = courseId,
                            title = "Lesson 1",
                            description = "Content",
                            contentType = "article",
                            durationMinutes = 10,
                            displayOrder = 1,
                            resourceUrl = null,
                            resourceProvider = null,
                            videoUrl = null,
                            videoProvider = null,
                            isCompleted = false
                        )
                    )
                )
            )
        )

        // Save course for User A with pending sync
        database.saveCourseForOffline(userA, courseDetail, downloadStatus = "completed", syncStatus = "pending_sync")

        // User A should see 1 saved course
        val userACourses = database.getSavedCourses(userA)
        assertEquals(1, userACourses.size)
        assertEquals(courseId, userACourses[0].id)

        // User B should see 0 saved courses (Strict User Isolation)
        val userBCourses = database.getSavedCourses(userB)
        assertEquals(0, userBCourses.size)

        // Guarded deletion test:
        // Attempt deletion with onlyIfPendingUnsave = true while it is pending_sync
        val deletedWhilePendingSync = database.deleteSavedCourse(userA, courseId, onlyIfPendingUnsave = true)
        assertFalse("Should NOT delete when onlyIfPendingUnsave = true but record is pending_sync", deletedWhilePendingSync)
        assertEquals(1, database.getSavedCourses(userA).size)

        // Transition to pending_unsave
        val queued = database.queueUnsaveCourse(userA, courseId)
        assertTrue(queued)
        assertFalse("Once pending_unsave, isCourseSaved returns false", database.isCourseSaved(userA, courseId))

        // Now deletion with onlyIfPendingUnsave = true should succeed
        val deletedWhilePendingUnsave = database.deleteSavedCourse(userA, courseId, onlyIfPendingUnsave = true)
        assertTrue("Should delete successfully when record is pending_unsave", deletedWhilePendingUnsave)
        assertEquals(0, database.getSavedCourses(userA).size)
    }

    @Test
    fun testRealDeviceLessonProgressTracking() {
        val userId = "device-test-progress-user"
        val courseId = "dev-course-progress"
        val lessonId = "dev-les-progress-1"

        val courseDetail = CourseDetail(
            id = courseId,
            name = "Progress Tracking Course",
            description = "Progress test",
            icon = "star",
            accent = CourseAccent.PRIMARY,
            category = "General",
            level = "Beginner",
            durationHours = 1,
            prerequisites = emptyList(),
            modules = listOf(
                CourseModule(
                    id = "mod-prog-1",
                    courseId = courseId,
                    title = "Mod 1",
                    description = "",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = lessonId,
                            moduleId = "mod-prog-1",
                            courseId = courseId,
                            title = "Les 1",
                            description = "Body",
                            contentType = "article",
                            durationMinutes = 5,
                            displayOrder = 1,
                            resourceUrl = null,
                            resourceProvider = null,
                            videoUrl = null,
                            videoProvider = null,
                            isCompleted = false
                        )
                    )
                )
            )
        )

        database.saveCourseForOffline(userId, courseDetail, downloadStatus = "completed", syncStatus = "synced")
        assertEquals(0, database.getUserCompletedLessonCount(userId, courseId))

        // Record progress
        database.recordUserLessonProgress(userId, courseId, lessonId, isCompleted = true, syncStatus = "pending_sync")
        assertEquals(1, database.getUserCompletedLessonCount(userId, courseId))

        // Check course detail reflects completed lesson
        val updatedDetail = database.getOfflineCourseDetail(courseId, userId)
        assertTrue(updatedDetail?.modules?.first()?.lessons?.first()?.isCompleted == true)
    }

    @Test
    fun testRealDeviceEncryptedTokenStorageSessionPersistence() {
        tokenStorage.saveTokens("dev-access-token-999", "dev-refresh-token-999")
        tokenStorage.saveUserIdentity("device-test-user-id", "test@edunova.ai", "Offline Device Tester")

        assertTrue(tokenStorage.hasTokens())
        assertEquals("dev-access-token-999", tokenStorage.getAccessToken())
        assertEquals("dev-refresh-token-999", tokenStorage.getRefreshToken())

        val identity = tokenStorage.getUserIdentity()
        assertNotNull(identity)
        assertEquals("device-test-user-id", identity?.userId)
        assertEquals("test@edunova.ai", identity?.email)
        assertEquals("Offline Device Tester", identity?.fullName)

        // Clear and verify
        tokenStorage.clearTokens()
        tokenStorage.clearUserIdentity()
        assertFalse(tokenStorage.hasTokens())
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getUserIdentity())
    }

    @Test
    fun testRealDevicePendingOperationsPreservedAcrossDatabaseReopen() {
        val userId = "device-test-reopen-user"
        val courseId = "dev-course-reopen"

        val courseDetail = CourseDetail(
            id = courseId,
            name = "Reopen Course",
            description = "Persistence across reopen",
            icon = "book",
            accent = CourseAccent.PRIMARY,
            category = "Engineering",
            level = "Beginner",
            durationHours = 1,
            prerequisites = emptyList(),
            modules = listOf(
                CourseModule(
                    id = "mod-reopen-1",
                    courseId = courseId,
                    title = "Mod Reopen",
                    description = "",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-reopen-1",
                            moduleId = "mod-reopen-1",
                            courseId = courseId,
                            title = "Les Reopen",
                            description = "",
                            contentType = "article",
                            durationMinutes = 5,
                            displayOrder = 1,
                            resourceUrl = null,
                            resourceProvider = null,
                            videoUrl = null,
                            videoProvider = null,
                            isCompleted = false
                        )
                    )
                )
            )
        )

        // Insert pending operation
        database.saveCourseForOffline(userId, courseDetail, downloadStatus = "completed", syncStatus = "pending_sync")

        // Close database and reopen fresh instance (simulating process restart)
        database.close()
        val reopenedDatabase = ChatCacheDatabase(appContext)

        // Pending operation must be completely intact
        val pendingCourses = reopenedDatabase.getPendingSavedCourses(userId)
        assertEquals(1, pendingCourses.size)
        assertEquals(courseId, pendingCourses[0].courseId)
        assertEquals("pending_sync", pendingCourses[0].syncStatus)

        reopenedDatabase.close()
    }
}
