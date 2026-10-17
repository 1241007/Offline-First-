package com.offline_First.data.sync

import android.content.Context
import android.util.Log
import com.offline_First.data.AuthState
import com.offline_First.data.SessionManager
import com.offline_First.data.local.ChatCacheDatabase
import com.offline_First.data.remote.ChatApiClient
import com.offline_First.data.remote.OnlineCourseRepository
import com.offline_First.data.remote.OnlineRoadmapRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    FAILED
}

data class SyncStatus(
    val state: SyncState = SyncState.IDLE,
    val lastSyncTime: Long? = null,
    val pendingOperationsCount: Int = 0,
    val lastError: String? = null
)

class EduNovaSyncManager(
    val context: Context? = null,
    val networkMonitor: NetworkMonitor,
    val sessionManager: SessionManager,
    val cacheDb: ChatCacheDatabase? = context?.let { ChatCacheDatabase(it.applicationContext) },
    val onlineCourseRepository: OnlineCourseRepository? = null,
    val onlineRoadmapRepository: OnlineRoadmapRepository? = null,
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val syncMutex = Mutex()
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        // Automatically sync when online connectivity and authenticated online session are both active
        scope.launch {
            combine(networkMonitor.isOnline, sessionManager.authState) { online, auth ->
                online && auth is AuthState.Authenticated && !auth.isOffline
            }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                Log.d("EduNovaSyncManager", "Online authenticated session active. Triggering auto-sync...")
                syncNow()
            }
        }
    }

    suspend fun syncNow(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) {
            Log.d("EduNovaSyncManager", "Sync already running, waiting for active execution to complete")
            syncMutex.withLock {
                val current = _syncStatus.value
                return@withContext if (current.state == SyncState.FAILED) {
                    Result.failure(Exception(current.lastError ?: "Sync failed"))
                } else {
                    Result.success(Unit)
                }
            }
        }

        try {
            val authState = sessionManager.authState.value
            if (authState !is AuthState.Authenticated || authState.isOffline) {
                Log.d("EduNovaSyncManager", "Sync skipped: offline or unauthenticated session")
                val uId = (authState as? AuthState.Authenticated)?.userId ?: "default_user"
                val pending = cacheDb?.getPendingOperationsCount(uId) ?: 0
                _syncStatus.update { it.copy(pendingOperationsCount = pending) }
                return@withContext Result.success(Unit)
            }

            val userId = authState.userId
            val db = cacheDb ?: return@withContext Result.success(Unit)

            val initialPending = db.getPendingOperationsCount(userId)
            if (initialPending == 0) {
                _syncStatus.update {
                    it.copy(
                        state = SyncState.SUCCESS,
                        pendingOperationsCount = 0,
                        lastSyncTime = it.lastSyncTime ?: System.currentTimeMillis()
                    )
                }
                return@withContext Result.success(Unit)
            }

            _syncStatus.update {
                it.copy(
                    state = SyncState.SYNCING,
                    pendingOperationsCount = initialPending,
                    lastError = null
                )
            }

            var hasFailures = false
            var failureMessage: String? = null

            // 1. Pending course saves
            val pendingCourses = db.getPendingSavedCourses(userId)
            for (saved in pendingCourses) {
                val remoteRes = ChatApiClient.saveCourse(saved.courseId)
                if (remoteRes.isSuccess) {
                    db.markSavedCourseSynced(userId, saved.courseId)
                } else {
                    val ex = remoteRes.exceptionOrNull()
                    if (isPermanentError(ex)) {
                        db.markSavedCourseFailed(userId, saved.courseId)
                    } else {
                        hasFailures = true
                        failureMessage = ex?.message
                    }
                }
            }

            // 2. Pending course unsaves
            val pendingUnsavedCourses = db.getPendingUnsavedCourses(userId)
            for (unsaved in pendingUnsavedCourses) {
                val remoteRes = ChatApiClient.unsaveCourse(unsaved.courseId)
                if (remoteRes.isSuccess || isNotFoundError(remoteRes.exceptionOrNull())) {
                    db.deleteSavedCourse(userId, unsaved.courseId, onlyIfPendingUnsave = true)
                } else {
                    val ex = remoteRes.exceptionOrNull()
                    if (isPermanentError(ex)) {
                        db.markSavedCourseFailed(userId, unsaved.courseId)
                    } else {
                        hasFailures = true
                        failureMessage = ex?.message
                    }
                }
            }

            // 3. Pending roadmap saves
            val pendingRoadmaps = db.getPendingSavedRoadmaps(userId)
            for (saved in pendingRoadmaps) {
                val remoteRes = ChatApiClient.saveRoadmap(saved.roadmapId)
                if (remoteRes.isSuccess) {
                    db.markSavedRoadmapSynced(userId, saved.roadmapId)
                } else {
                    val ex = remoteRes.exceptionOrNull()
                    if (isPermanentError(ex)) {
                        db.markSavedRoadmapFailed(userId, saved.roadmapId)
                    } else {
                        hasFailures = true
                        failureMessage = ex?.message
                    }
                }
            }

            // 4. Pending roadmap unsaves
            val pendingUnsavedRoadmaps = db.getPendingUnsavedRoadmaps(userId)
            for (unsaved in pendingUnsavedRoadmaps) {
                val remoteRes = ChatApiClient.unsaveRoadmap(unsaved.roadmapId)
                if (remoteRes.isSuccess || isNotFoundError(remoteRes.exceptionOrNull())) {
                    db.deleteSavedRoadmap(userId, unsaved.roadmapId, onlyIfPendingUnsave = true)
                } else {
                    val ex = remoteRes.exceptionOrNull()
                    if (isPermanentError(ex)) {
                        db.markSavedRoadmapFailed(userId, unsaved.roadmapId)
                    } else {
                        hasFailures = true
                        failureMessage = ex?.message
                    }
                }
            }

            // 5. Pending lesson progress
            val pendingLessons = db.getPendingUserLessonProgress(userId)
            for (prog in pendingLessons) {
                if (prog.isCompleted) {
                    val remoteRes = ChatApiClient.completeLesson(prog.lessonId)
                    if (remoteRes.isSuccess) {
                        db.markUserLessonProgressSynced(userId, prog.courseId, prog.lessonId)
                    } else {
                        val ex = remoteRes.exceptionOrNull()
                        if (isPermanentError(ex)) {
                            db.markUserLessonProgressFailed(userId, prog.courseId, prog.lessonId)
                        } else {
                            hasFailures = true
                            failureMessage = ex?.message
                        }
                    }
                } else {
                    // Backend does not support uncompleting lessons. Retained locally.
                }
            }

            // 6. Pending roadmap milestones
            val milestoneRes = onlineRoadmapRepository?.syncPendingMilestoneProgress()
            if (milestoneRes?.isFailure == true) {
                hasFailures = true
                failureMessage = milestoneRes.exceptionOrNull()?.message
            }

            // 7. Unsynced personalized roadmaps
            val roadmapSyncRes = onlineRoadmapRepository?.syncUnsyncedPersonalizedRoadmaps()
            if (roadmapSyncRes?.isFailure == true) {
                hasFailures = true
                failureMessage = roadmapSyncRes.exceptionOrNull()?.message
            }

            // 8. Refresh remote metadata after clean sync
            if (!hasFailures) {
                runCatching { onlineCourseRepository?.getSavedCourses(userId) }
                runCatching { onlineRoadmapRepository?.getSavedRoadmaps() }
            }

            val remainingCount = db.getPendingOperationsCount(userId)
            val now = System.currentTimeMillis()

            if (hasFailures) {
                _syncStatus.value = SyncStatus(
                    state = SyncState.FAILED,
                    lastSyncTime = now,
                    pendingOperationsCount = remainingCount,
                    lastError = failureMessage ?: "Sync partially failed"
                )
                Result.failure(Exception(failureMessage ?: "Sync partially failed"))
            } else {
                _syncStatus.value = SyncStatus(
                    state = SyncState.SUCCESS,
                    lastSyncTime = now,
                    pendingOperationsCount = remainingCount,
                    lastError = null
                )
                Result.success(Unit)
            }
        } finally {
            syncMutex.unlock()
        }
    }

    private fun isPermanentError(throwable: Throwable?): Boolean {
        val msg = throwable?.message ?: return false
        return msg.contains("HTTP 400") || msg.contains("HTTP 422") || msg.contains("HTTP 403")
    }

    private fun isNotFoundError(throwable: Throwable?): Boolean {
        val msg = throwable?.message ?: return false
        return msg.contains("HTTP 404")
    }
}
