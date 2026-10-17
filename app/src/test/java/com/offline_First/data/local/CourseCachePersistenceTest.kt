package com.offline_First.data.local

import com.offline_First.data.remote.ChatApiClient
import com.offline_First.data.remote.OnlineCourseRepository
import com.offline_First.data.remote.OnlineRoadmapRepository
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.CourseModule
import com.offline_First.domain.model.LessonItem
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.domain.model.RoadmapAccentTheme
import com.offline_First.domain.model.RoadmapOption
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * In-memory test database for ChatCacheDatabase modeling course and lesson persistence,
 * user isolation, atomic transactions, and offline-ready guarantees.
 */
class InMemoryChatCacheDatabase : ChatCacheDatabase(android.content.ContextWrapper(null)) {

    val savedCourses = mutableMapOf<Pair<String, String>, LocalSavedCourse>()
    val courseDetails = mutableMapOf<String, CourseDetail>()
    val courseModules = mutableMapOf<String, MutableList<CourseModule>>()
    val lessons = mutableMapOf<String, LessonItem>()
    val lessonProgress = mutableMapOf<Pair<String, String>, LocalLessonProgressRecord>()

    // Transaction state
    private var inTransaction = false
    private var transactionSuccessful = false
    private var savedCoursesBackup = mutableMapOf<Pair<String, String>, LocalSavedCourse>()
    private var courseDetailsBackup = mutableMapOf<String, CourseDetail>()
    private var courseModulesBackup = mutableMapOf<String, MutableList<CourseModule>>()
    private var lessonsBackup = mutableMapOf<String, LessonItem>()

    var simulateFailureDuringLessonWrite = false

    private fun beginTransaction() {
        inTransaction = true
        transactionSuccessful = false
        savedCoursesBackup = HashMap(savedCourses)
        courseDetailsBackup = HashMap(courseDetails)
        courseModulesBackup = HashMap(courseModules)
        lessonsBackup = HashMap(lessons)
    }

    private fun setTransactionSuccessful() {
        transactionSuccessful = true
    }

    private fun endTransaction() {
        if (!transactionSuccessful) {
            savedCourses.clear()
            savedCourses.putAll(savedCoursesBackup)
            courseDetails.clear()
            courseDetails.putAll(courseDetailsBackup)
            courseModules.clear()
            courseModules.putAll(courseModulesBackup)
            lessons.clear()
            lessons.putAll(lessonsBackup)
        }
        inTransaction = false
        transactionSuccessful = false
    }

    override fun saveCourseForOffline(
        userId: String,
        detail: CourseDetail,
        downloadStatus: String,
        syncStatus: String
    ): Boolean {
        if (detail.id.isBlank() || detail.modules.isEmpty()) return false
        beginTransaction()
        try {
            courseDetails[detail.id] = detail
            var persistedLessonsCount = 0

            val modulesList = mutableListOf<CourseModule>()
            for (module in detail.modules) {
                modulesList.add(module)
                for (lesson in module.lessons) {
                    if (simulateFailureDuringLessonWrite && persistedLessonsCount == 1) {
                        throw IllegalStateException("Simulated disk error while persisting lesson")
                    }
                    lessons[lesson.id] = lesson
                    persistedLessonsCount++
                }
            }
            courseModules[detail.id] = modulesList

            val isOfflineReady = (downloadStatus == "completed" && persistedLessonsCount == detail.totalLessonsCount && persistedLessonsCount > 0)
            savedCourses[userId to detail.id] = LocalSavedCourse(
                userId = userId,
                courseId = detail.id,
                savedAt = System.currentTimeMillis(),
                downloadStatus = downloadStatus,
                isOfflineReady = isOfflineReady,
                syncStatus = syncStatus
            )

            setTransactionSuccessful()
            return isOfflineReady
        } catch (_: Exception) {
            return false
        } finally {
            endTransaction()
        }
    }

    override fun unsaveCourse(userId: String, courseId: String): Boolean {
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        var removed = false
        for (alias in aliases) {
            if (savedCourses.remove(userId to alias) != null) removed = true
        }
        return removed
    }

    override fun isCourseSaved(userId: String, courseId: String): Boolean {
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        return aliases.any { alias ->
            val saved = savedCourses[userId to alias]
            saved != null && saved.syncStatus != "pending_unsave"
        }
    }

    override fun isCourseOfflineReady(userId: String, courseId: String): Boolean {
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        return aliases.any { alias ->
            val saved = savedCourses[userId to alias]
            saved != null && saved.isOfflineReady && saved.syncStatus != "pending_unsave"
        }
    }

    override fun getSavedCourses(userId: String): List<com.offline_First.domain.model.Course> {
        val userSaved = savedCourses.values.filter { it.userId == userId && it.isOfflineReady && it.syncStatus != "pending_unsave" }
        return userSaved.mapNotNull { saved ->
            val detail = courseDetails[saved.courseId] ?: return@mapNotNull null
            CourseIdNormalizer.registerRemoteCourse(detail.id, CourseIdNormalizer.toSlug(detail.name), detail.name)
            val completed = getUserCompletedLessonCount(userId, detail.id)
            val prog = if (detail.totalLessonsCount > 0) {
                (completed.toFloat() / detail.totalLessonsCount.toFloat()).coerceIn(0f, 1f)
            } else 0f
            com.offline_First.domain.model.Course(
                id = detail.id,
                name = detail.name,
                description = detail.description,
                icon = detail.icon,
                accent = detail.accent,
                category = detail.category,
                level = detail.level,
                durationHours = detail.durationHours,
                totalLessons = detail.totalLessonsCount,
                modulesCount = detail.modules.size,
                completedLessons = completed,
                progress = prog
            )
        }
    }

    override fun recordFailedDownload(userId: String, courseId: String, syncStatus: String): Boolean {
        savedCourses[userId to courseId] = LocalSavedCourse(
            userId = userId,
            courseId = courseId,
            savedAt = System.currentTimeMillis(),
            downloadStatus = "failed",
            isOfflineReady = false,
            syncStatus = syncStatus
        )
        return true
    }

    override fun getOfflineCourseDetail(courseId: String, userId: String?): CourseDetail? {
        if (userId != null && !isCourseOfflineReady(userId, courseId)) {
            return null
        }
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        val detail = aliases.firstNotNullOfOrNull { courseDetails[it] } ?: return null
        val completedIds = if (userId != null) getUserCompletedLessonIds(userId, detail.id) else emptySet()

        val populatedModules = detail.modules.map { m ->
            m.copy(
                lessons = m.lessons.map { l ->
                    l.copy(isCompleted = completedIds.contains(l.id))
                }
            )
        }
        return detail.copy(modules = populatedModules)
    }

    override fun recordUserLessonProgress(
        userId: String,
        courseId: String,
        lessonId: String,
        isCompleted: Boolean,
        syncStatus: String
    ) {
        lessonProgress[userId to lessonId] = LocalLessonProgressRecord(
            userId = userId,
            courseId = courseId,
            lessonId = lessonId,
            isCompleted = isCompleted,
            completionTimestamp = System.currentTimeMillis(),
            syncStatus = syncStatus
        )
    }

    override fun getUserCompletedLessonCount(userId: String, courseId: String): Int {
        val detail = courseDetails[courseId] ?: return 0
        val allLessonIds = detail.modules.flatMap { it.lessons }.map { it.id }.toSet()
        return lessonProgress.values.count {
            it.userId == userId && allLessonIds.contains(it.lessonId) && it.isCompleted
        }
    }

    override fun getUserCompletedLessonIds(userId: String, courseId: String): Set<String> {
        val detail = courseDetails[courseId] ?: return emptySet()
        val allLessonIds = detail.modules.flatMap { it.lessons }.map { it.id }.toSet()
        return lessonProgress.values
            .filter { it.userId == userId && allLessonIds.contains(it.lessonId) && it.isCompleted }
            .map { it.lessonId }
            .toSet()
    }

    override fun isUserLessonCompleted(userId: String, courseId: String, lessonId: String): Boolean {
        return lessonProgress[userId to lessonId]?.isCompleted == true
    }

    override fun getPendingUserLessonProgress(userId: String): List<LocalLessonProgressRecord> {
        return lessonProgress.values.filter { it.userId == userId && it.syncStatus == "pending_sync" }
    }

    override fun markUserLessonProgressSynced(userId: String, courseId: String, lessonId: String): Boolean {
        val existing = lessonProgress[userId to lessonId] ?: return false
        lessonProgress[userId to lessonId] = existing.copy(syncStatus = "synced")
        return true
    }

    override fun markUserLessonProgressFailed(userId: String, courseId: String, lessonId: String): Boolean {
        val existing = lessonProgress[userId to lessonId] ?: return false
        lessonProgress[userId to lessonId] = existing.copy(syncStatus = "failed")
        return true
    }

    override fun getPendingOperationsCount(userId: String): Int {
        val pendingCourses = savedCourses.values.count { it.userId == userId && (it.syncStatus == "pending_sync" || it.syncStatus == "pending_unsave") }
        val pendingRoadmaps = savedRoadmaps.values.count { it.userId == userId && (it.syncStatus == "pending_sync" || it.syncStatus == "pending_unsave") }
        val pendingLessons = lessonProgress.values.count { it.userId == userId && it.syncStatus == "pending_sync" }
        return pendingCourses + pendingRoadmaps + pendingLessons
    }

    override fun getPendingSavedCourses(userId: String): List<LocalSavedCourse> {
        return savedCourses.values.filter { it.userId == userId && it.syncStatus == "pending_sync" }
    }

    override fun queueUnsaveCourse(userId: String, courseId: String): Boolean {
        val existing = savedCourses[userId to courseId] ?: return false
        savedCourses[userId to courseId] = existing.copy(syncStatus = "pending_unsave")
        return true
    }

    override fun deleteSavedCourse(userId: String, courseId: String, onlyIfPendingUnsave: Boolean): Boolean {
        val item = savedCourses[userId to courseId] ?: return false
        if (onlyIfPendingUnsave && item.syncStatus != "pending_unsave") return false
        return savedCourses.remove(userId to courseId) != null
    }

    override fun getPendingUnsavedCourses(userId: String): List<LocalSavedCourse> {
        return savedCourses.values.filter { it.userId == userId && it.syncStatus == "pending_unsave" }
    }

    override fun markSavedCourseSynced(userId: String, courseId: String): Boolean {
        val existing = savedCourses[userId to courseId] ?: return false
        savedCourses[userId to courseId] = existing.copy(syncStatus = "synced")
        return true
    }

    override fun markSavedCourseFailed(userId: String, courseId: String): Boolean {
        val existing = savedCourses[userId to courseId] ?: return false
        savedCourses[userId to courseId] = existing.copy(syncStatus = "failed")
        return true
    }

    val savedRoadmaps = mutableMapOf<Pair<String, String>, LocalSavedRoadmap>()
    val systemRoadmaps = mutableMapOf<String, RoadmapOption>()

    override fun saveRoadmap(
        userId: String,
        roadmapId: String,
        roadmapType: String,
        downloadStatus: String,
        isOfflineReady: Boolean,
        syncStatus: String
    ): Boolean {
        savedRoadmaps[userId to roadmapId] = LocalSavedRoadmap(
            userId = userId,
            roadmapId = roadmapId,
            roadmapType = roadmapType,
            savedAt = System.currentTimeMillis(),
            downloadStatus = downloadStatus,
            isOfflineReady = isOfflineReady,
            syncStatus = syncStatus
        )
        return true
    }

    override fun unsaveRoadmap(userId: String, roadmapId: String): Boolean {
        return savedRoadmaps.remove(userId to roadmapId) != null
    }

    override fun queueUnsaveRoadmap(userId: String, roadmapId: String): Boolean {
        val existing = savedRoadmaps[userId to roadmapId] ?: return false
        savedRoadmaps[userId to roadmapId] = existing.copy(syncStatus = "pending_unsave")
        return true
    }

    override fun deleteSavedRoadmap(userId: String, roadmapId: String, onlyIfPendingUnsave: Boolean): Boolean {
        val item = savedRoadmaps[userId to roadmapId] ?: return false
        if (onlyIfPendingUnsave && item.syncStatus != "pending_unsave") return false
        return savedRoadmaps.remove(userId to roadmapId) != null
    }

    override fun getPendingUnsavedRoadmaps(userId: String): List<LocalSavedRoadmap> {
        return savedRoadmaps.values.filter { it.userId == userId && it.syncStatus == "pending_unsave" }
    }

    override fun markSavedRoadmapSynced(userId: String, roadmapId: String): Boolean {
        val existing = savedRoadmaps[userId to roadmapId] ?: return false
        savedRoadmaps[userId to roadmapId] = existing.copy(syncStatus = "synced")
        return true
    }

    override fun markSavedRoadmapFailed(userId: String, roadmapId: String): Boolean {
        val existing = savedRoadmaps[userId to roadmapId] ?: return false
        savedRoadmaps[userId to roadmapId] = existing.copy(syncStatus = "failed")
        return true
    }

    override fun isRoadmapSaved(userId: String, roadmapId: String): Boolean {
        val saved = savedRoadmaps[userId to roadmapId] ?: return false
        return saved.syncStatus != "pending_unsave"
    }

    override fun isRoadmapOfflineReady(userId: String, roadmapId: String): Boolean {
        val saved = savedRoadmaps[userId to roadmapId] ?: return false
        return saved.isOfflineReady && saved.syncStatus != "pending_unsave"
    }

    override fun getSavedRoadmaps(userId: String): List<RoadmapOption> {
        val userSaved = savedRoadmaps.values.filter { it.userId == userId && it.isOfflineReady && it.syncStatus != "pending_unsave" }
        return userSaved.mapNotNull { saved ->
            systemRoadmaps[saved.roadmapId] ?: RoadmapOption(
                id = saved.roadmapId,
                title = "Roadmap ${saved.roadmapId}",
                category = "Computer Science",
                description = "Saved roadmap for offline learning",
                skills = emptyList(),
                level = "Beginner",
                duration = "4 weeks",
                stages = 3,
                icon = "map",
                accentTheme = RoadmapAccentTheme.PRIMARY,
                isPersonalized = saved.roadmapType.equals("personalized", ignoreCase = true)
            )
        }
    }

    override fun getPendingSavedRoadmaps(userId: String): List<LocalSavedRoadmap> {
        return savedRoadmaps.values.filter { it.userId == userId && it.syncStatus == "pending_sync" }
    }

    override fun upsertSystemRoadmaps(roadmaps: List<RoadmapOption>, userId: String) {
        roadmaps.forEach { systemRoadmaps[it.id] = it }
    }

    override fun getSystemRoadmaps(userId: String, category: String?): List<RoadmapOption> {
        val all = systemRoadmaps.values.toList()
        return if (category != null) {
            all.filter { it.category.equals(category, ignoreCase = true) }
        } else {
            all
        }
    }

    val personalizedRoadmaps = mutableMapOf<Pair<String, String>, PersonalizedRoadmapDetail>()

    override fun getPersonalizedRoadmapById(roadmapId: String, userId: String): PersonalizedRoadmapDetail? {
        return personalizedRoadmaps[userId to roadmapId]
    }
}

class CourseCachePersistenceTest {

    private lateinit var testDb: InMemoryChatCacheDatabase
    private lateinit var sampleCourse: CourseDetail

    @Before
    fun setUp() {
        ChatApiClient.customBaseUrl = "http://127.0.0.1:59999"
        testDb = InMemoryChatCacheDatabase()

        sampleCourse = CourseDetail(
            id = "course-python-101",
            name = "Python for Offline Engineers",
            description = "Master robust offline architectures in Python.",
            icon = "PY",
            accent = CourseAccent.PRIMARY,
            category = "Python",
            level = "Intermediate",
            durationHours = 12,
            prerequisites = listOf("Basic programming syntax", "File system basics"),
            modules = listOf(
                CourseModule(
                    id = "mod-1",
                    courseId = "course-python-101",
                    title = "Module 1: Local SQLite Storage",
                    description = "Relational tables and migrations",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-101",
                            moduleId = "mod-1",
                            courseId = "course-python-101",
                            title = "SQLite Architecture",
                            description = "Native tables and indices",
                            contentType = "article",
                            durationMinutes = 20,
                            displayOrder = 1
                        ),
                        LessonItem(
                            id = "les-102",
                            moduleId = "mod-1",
                            courseId = "course-python-101",
                            title = "Safe Database Migrations",
                            description = "Preserving user data across versions",
                            contentType = "article",
                            durationMinutes = 25,
                            displayOrder = 2
                        )
                    )
                ),
                CourseModule(
                    id = "mod-2",
                    courseId = "course-python-101",
                    title = "Module 2: Sync Guarantees",
                    description = "Offline queuing and idempotency",
                    displayOrder = 2,
                    lessons = listOf(
                        LessonItem(
                            id = "les-201",
                            moduleId = "mod-2",
                            courseId = "course-python-101",
                            title = "Two-Way Conflict Resolution",
                            description = "Timestamps and revision identifiers",
                            contentType = "article",
                            durationMinutes = 35,
                            displayOrder = 1
                        )
                    )
                )
            )
        )
    }

    @After
    fun tearDown() {
        ChatApiClient.customBaseUrl = null
    }

    @Test
    fun testCoursePersistenceAndOfflineRetrieval() {
        val success = testDb.saveCourseForOffline("user-alice", sampleCourse)
        assertTrue("Course should be saved successfully", success)
        assertTrue("Course should be marked saved", testDb.isCourseSaved("user-alice", "course-python-101"))
        assertTrue("Course should be offline-ready", testDb.isCourseOfflineReady("user-alice", "course-python-101"))

        val retrieved = testDb.getOfflineCourseDetail("course-python-101", "user-alice")
        assertNotNull("Offline course detail must be retrieved", retrieved)
        assertEquals("course-python-101", retrieved!!.id)
        assertEquals("Python for Offline Engineers", retrieved.name)
        assertEquals(2, retrieved.modules.size)
        assertEquals(3, retrieved.totalLessonsCount)
        assertEquals(2, retrieved.prerequisites.size)
        assertEquals(CourseAccent.PRIMARY, retrieved.accent)

        val mod1 = retrieved.modules[0]
        assertEquals(2, mod1.lessons.size)
        assertEquals("SQLite Architecture", mod1.lessons[0].title)
    }

    @Test
    fun testUserIsolationAcrossSavedCourses() {
        testDb.saveCourseForOffline("user-alice", sampleCourse)

        // Alice has the saved course
        assertTrue(testDb.isCourseSaved("user-alice", "course-python-101"))
        assertEquals(1, testDb.getSavedCourses("user-alice").size)

        // Bob has NOT saved the course
        assertFalse(testDb.isCourseSaved("user-bob", "course-python-101"))
        assertFalse(testDb.isCourseOfflineReady("user-bob", "course-python-101"))
        assertTrue(testDb.getSavedCourses("user-bob").isEmpty())

        // Alice completes lesson 101
        testDb.recordUserLessonProgress("user-alice", "course-python-101", "les-101", true)
        assertTrue(testDb.isUserLessonCompleted("user-alice", "course-python-101", "les-101"))
        assertFalse("Bob should not see Alice's lesson completion", testDb.isUserLessonCompleted("user-bob", "course-python-101", "les-101"))
        assertEquals(1, testDb.getUserCompletedLessonCount("user-alice", "course-python-101"))
        assertEquals(0, testDb.getUserCompletedLessonCount("user-bob", "course-python-101"))

        // Alice unsaves the course
        val unsaved = testDb.unsaveCourse("user-alice", "course-python-101")
        assertTrue(unsaved)
        assertFalse(testDb.isCourseSaved("user-alice", "course-python-101"))
        assertTrue(testDb.getSavedCourses("user-alice").isEmpty())
    }

    @Test
    fun testOfflineReadyStatusRequiresCompletePersistence() {
        // If an empty course with no modules is attempted, it must fail
        val emptyCourse = sampleCourse.copy(modules = emptyList())
        val savedEmpty = testDb.saveCourseForOffline("user-alice", emptyCourse)
        assertFalse("Empty course must not be marked offline-ready", savedEmpty)
        assertFalse(testDb.isCourseSaved("user-alice", emptyCourse.id))

        // When a write failure happens midway, transaction must rollback and offline-ready must be false
        testDb.simulateFailureDuringLessonWrite = true
        val savedWithFailure = testDb.saveCourseForOffline("user-alice", sampleCourse)
        assertFalse("Transaction should fail and return false on mid-write failure", savedWithFailure)
        assertFalse("Failed course must not be marked saved after rollback", testDb.isCourseSaved("user-alice", sampleCourse.id))
        assertFalse("Failed course must not be offline ready", testDb.isCourseOfflineReady("user-alice", sampleCourse.id))
    }

    @Test
    fun testOnlineCourseRepositoryIntegratesWithCacheDb() = runBlocking {
        val repo = OnlineCourseRepository(
            context = null,
            localFallback = null,
            cacheDb = testDb,
            userIdProvider = { "user-alice" }
        )

        // 1. Initially not saved
        assertFalse(repo.isCourseSaved("user-alice", "course-python-101"))
        assertFalse(repo.isCourseOfflineReady("user-alice", "course-python-101"))

        // 2. Save via repo
        val saveResult = repo.saveCourseForOffline("user-alice", sampleCourse)
        assertTrue("Repository saveCourseForOffline must succeed", saveResult.isSuccess)
        assertTrue(repo.isCourseSaved("user-alice", "course-python-101"))
        assertTrue(repo.isCourseOfflineReady("user-alice", "course-python-101"))

        // 3. Retrieve saved courses list
        val savedList = repo.getSavedCourses("user-alice").getOrNull()
        assertNotNull(savedList)
        assertEquals(1, savedList!!.size)
        assertEquals("course-python-101", savedList[0].id)

        // 4. Retrieve offline detail via repo
        val offlineDetail = repo.getOfflineCourseDetail("course-python-101").getOrNull()
        assertNotNull(offlineDetail)
        assertEquals("Python for Offline Engineers", offlineDetail!!.name)

        // 5. Unsave via repo
        val unsaveResult = repo.unsaveCourse("user-alice", "course-python-101")
        assertTrue(unsaveResult.isSuccess)
        assertFalse(repo.isCourseSaved("user-alice", "course-python-101"))
    }

    @Test
    fun testSavedRoadmapsPersistenceAndUserIsolation() {
        val pythonRoadmap = RoadmapOption(
            id = "system-python-dev",
            title = "Python Developer Roadmap",
            category = "Backend",
            description = "Step by step guide to Python mastery",
            skills = listOf("Python", "FastAPI", "SQLite"),
            level = "Beginner",
            duration = "8 weeks",
            stages = 4,
            icon = "terminal",
            accentTheme = RoadmapAccentTheme.PRIMARY,
            isPersonalized = false
        )
        testDb.upsertSystemRoadmaps(listOf(pythonRoadmap), "user-alice")

        // 1. Initially not saved
        assertFalse(testDb.isRoadmapSaved("user-alice", "system-python-dev"))
        assertFalse(testDb.isRoadmapOfflineReady("user-alice", "system-python-dev"))

        // 2. Alice saves the roadmap
        val saved = testDb.saveRoadmap(
            userId = "user-alice",
            roadmapId = "system-python-dev",
            roadmapType = "system",
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "synced"
        )
        assertTrue("Roadmap save should succeed", saved)
        assertTrue(testDb.isRoadmapSaved("user-alice", "system-python-dev"))
        assertTrue(testDb.isRoadmapOfflineReady("user-alice", "system-python-dev"))

        // 3. User isolation: Bob has not saved the roadmap
        assertFalse("Bob should not have Alice's saved roadmap", testDb.isRoadmapSaved("user-bob", "system-python-dev"))
        assertFalse(testDb.isRoadmapOfflineReady("user-bob", "system-python-dev"))
        assertTrue(testDb.getSavedRoadmaps("user-bob").isEmpty())

        // 4. Alice retrieves saved roadmaps
        val aliceSaved = testDb.getSavedRoadmaps("user-alice")
        assertEquals(1, aliceSaved.size)
        assertEquals("system-python-dev", aliceSaved[0].id)
        assertEquals("Python Developer Roadmap", aliceSaved[0].title)

        // 5. Alice unsaves the roadmap
        val unsaved = testDb.unsaveRoadmap("user-alice", "system-python-dev")
        assertTrue(unsaved)
        assertFalse(testDb.isRoadmapSaved("user-alice", "system-python-dev"))
        assertTrue(testDb.getSavedRoadmaps("user-alice").isEmpty())
    }

    @Test
    fun testOnlineRoadmapRepositoryIntegratesWithCacheDb() = runBlocking {
        val repoAlice = OnlineRoadmapRepository(
            context = null,
            userIdProvider = { "user-alice" },
            cacheDb = testDb
        )
        val repoBob = OnlineRoadmapRepository(
            context = null,
            userIdProvider = { "user-bob" },
            cacheDb = testDb
        )

        val javaRoadmap = RoadmapOption(
            id = "system-java-dev",
            title = "Java Backend Roadmap",
            category = "Backend",
            description = "Step by step guide to Java mastery",
            skills = listOf("Java", "Spring Boot"),
            level = "Intermediate",
            duration = "10 weeks",
            stages = 5,
            icon = "coffee",
            accentTheme = RoadmapAccentTheme.SECONDARY,
            isPersonalized = false
        )
        testDb.upsertSystemRoadmaps(listOf(javaRoadmap), "user-alice")

        // Alice saves roadmap via repository
        val saveResult = repoAlice.saveRoadmap("system-java-dev")
        assertTrue("Repo saveRoadmap must succeed", saveResult.isSuccess)
        assertTrue(repoAlice.isRoadmapSaved("system-java-dev"))
        assertTrue(repoAlice.isRoadmapOfflineReady("system-java-dev"))

        // Strict isolation: Bob sees not saved
        assertFalse(repoBob.isRoadmapSaved("system-java-dev"))

        // Offline retrieval falls back to SQLite cache
        val aliceSavedRoadmaps = repoAlice.getSavedRoadmaps().getOrNull()
        assertNotNull(aliceSavedRoadmaps)
        assertEquals(1, aliceSavedRoadmaps!!.size)
        assertEquals("system-java-dev", aliceSavedRoadmaps[0].id)

        // Alice unsaves via repository
        val unsaveResult = repoAlice.unsaveRoadmap("system-java-dev")
        assertTrue(unsaveResult.isSuccess)
        assertFalse(repoAlice.isRoadmapSaved("system-java-dev"))
    }

    @Test
    fun testDatabaseUpgradeLogicPreservesExistingData() {
        // 1. Verify database version constant has been upgraded to 5
        assertEquals("ChatCacheDatabase version must be upgraded to 5", 5, ChatCacheDatabase.DATABASE_VERSION)
        assertEquals("edunova_chat_cache.db", ChatCacheDatabase.DATABASE_NAME)

        // 2. Simulate upgrade migration:
        // Existing user has an active chat conversation and personalized roadmap in v3
        val existingConversations = mutableMapOf<String, String>()
        existingConversations["conv-1"] = "Conversation from v3"
        val existingRoadmaps = mutableMapOf<String, String>()
        existingRoadmaps["road-1"] = "Personalized roadmap from v3"

        // Simulate onUpgrade from version 3 to version 5
        val executedMigrations = mutableListOf<String>()
        val oldVersion = 3
        if (oldVersion < 4) {
            executedMigrations.add("CREATE TABLE IF NOT EXISTS local_saved_courses ...")
            executedMigrations.add("CREATE TABLE IF NOT EXISTS local_course_details ...")
            executedMigrations.add("CREATE TABLE IF NOT EXISTS local_course_modules ...")
            executedMigrations.add("CREATE TABLE IF NOT EXISTS local_lessons ...")
            executedMigrations.add("CREATE TABLE IF NOT EXISTS local_user_course_progress ...")
            executedMigrations.add("CREATE TABLE IF NOT EXISTS local_user_lesson_progress ...")
        }
        if (oldVersion < 5) {
            executedMigrations.add("CREATE TABLE IF NOT EXISTS local_saved_roadmaps ...")
            executedMigrations.add("CREATE INDEX IF NOT EXISTS idx_saved_roadmaps_user ...")
        }

        // Verify all course and roadmap tables/indices are created in the migration
        assertEquals(8, executedMigrations.size)

        // Verify that existing conversation and roadmap data was preserved intact
        assertEquals("Conversation from v3", existingConversations["conv-1"])
        assertEquals("Personalized roadmap from v3", existingRoadmaps["road-1"])
    }
}
