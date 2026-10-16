package com.offline_First.data.local

import com.offline_First.domain.model.CourseCertificate
import com.offline_First.domain.model.RevisionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Serializable
internal data class PersistentCourseState(
    val completedLessonIds: Set<String> = emptySet(),
    val revisionItems: List<RevisionItem> = emptyList(),
    val certificates: List<CourseCertificate> = emptyList()
)

/**
 * Thread-safe persistent storage for course progress (lesson completion), mark-for-revision items,
 * and locally verified course-completion certificates.
 *
 * Uses atomic JSON file persistence to internal application storage surviving process kills and device restarts.
 */
class CourseProgressStore(
    private var storageFile: File? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    private val progressVersion = AtomicLong(0L)
    private val _progressUpdates = MutableStateFlow(0L)
    val progressUpdates: StateFlow<Long> = _progressUpdates.asStateFlow()

    private val completedLessons = ConcurrentHashMap.newKeySet<String>()
    private val revisionItemsMap = ConcurrentHashMap<String, RevisionItem>()
    private val certificatesMap = ConcurrentHashMap<String, CourseCertificate>() // Keyed by courseId

    init {
        storageFile?.let { loadFromFile(it) }
    }

    fun bindStorageFile(file: File) {
        synchronized(this) {
            storageFile = file
            loadFromFile(file)
        }
    }

    private fun loadFromFile(file: File) {
        try {
            if (file.exists() && file.length() > 0) {
                val content = file.readText()
                val state = json.decodeFromString<PersistentCourseState>(content)
                completedLessons.addAll(state.completedLessonIds)
                for (item in state.revisionItems) {
                    revisionItemsMap[makeKey(item.courseId, item.lessonId)] = item
                }
                for (cert in state.certificates) {
                    certificatesMap[cert.courseId] = cert
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to in-memory state on read error
        }
    }

    private fun saveToFile() {
        val file = storageFile ?: return
        try {
            val state = PersistentCourseState(
                completedLessonIds = completedLessons.toSet(),
                revisionItems = revisionItemsMap.values.toList(),
                certificates = certificatesMap.values.toList()
            )
            val content = json.encodeToString(state)
            // Atomic write: write to temp file then rename
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(content)
            if (!tempFile.renameTo(file)) {
                // Fallback direct write
                file.writeText(content)
                tempFile.delete()
            }
        } catch (_: Exception) {
            // Keep in-memory state safe even if disk write fails
        }
    }

    private fun makeKey(courseId: String, lessonId: String): String = "$courseId:$lessonId"

    fun markLessonCompleted(courseId: String, lessonId: String, completed: Boolean) {
        synchronized(this) {
            val currentlyCompleted = isLessonCompleted(courseId, lessonId)
            if (currentlyCompleted == completed) {
                // Safeguard: Avoid duplicate or unnecessary emissions
                return
            }
            val aliases = CourseIdNormalizer.getAllAliases(courseId)
            if (completed) {
                for (alias in aliases) {
                    completedLessons.add(makeKey(alias, lessonId))
                }
                completedLessons.add(lessonId)
            } else {
                for (alias in aliases) {
                    completedLessons.remove(makeKey(alias, lessonId))
                }
                val isUsedByOtherCourse = completedLessons.any { it.endsWith(":$lessonId") }
                if (!isUsedByOtherCourse) {
                    completedLessons.remove(lessonId)
                }
            }
            saveToFile()
            _progressUpdates.value = progressVersion.incrementAndGet()
        }
    }

    fun isLessonCompleted(courseId: String, lessonId: String): Boolean {
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        val hasAliasKey = aliases.any { alias -> completedLessons.contains(makeKey(alias, lessonId)) }
        return hasAliasKey || completedLessons.contains(lessonId)
    }

    /**
     * Return count of completed lessons strictly belonging to the specified course (matching any known alias).
     */
    fun getCompletedLessonCount(courseId: String): Int {
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        return completedLessons.mapNotNull { entry ->
            val colonIndex = entry.indexOf(':')
            if (colonIndex > 0) {
                val prefix = entry.substring(0, colonIndex)
                val lessonId = entry.substring(colonIndex + 1)
                if (aliases.contains(prefix)) lessonId else null
            } else {
                null
            }
        }.distinct().count()
    }

    /**
     * Return count of completed lessons given a known collection of lesson IDs.
     * Prevents double-counting and ensures accuracy against canonical course models.
     */
    fun getCompletedLessonCount(courseId: String, lessonIds: Collection<String>): Int {
        if (lessonIds.isEmpty()) return 0
        return lessonIds.count { lessonId -> isLessonCompleted(courseId, lessonId) }
    }

    /**
     * Calculate course progress percentage safely between 0.0f and 1.0f.
     * Handles zero-lesson courses gracefully without division by zero.
     */
    fun getCourseProgressPercentage(courseId: String, lessonIds: Collection<String>): Float {
        if (lessonIds.isEmpty()) return 0.0f
        val completed = getCompletedLessonCount(courseId, lessonIds)
        return (completed.toFloat() / lessonIds.size.toFloat()).coerceIn(0.0f, 1.0f)
    }

    /**
     * Determines whether all required lessons in a course are completed.
     * A course with 0 lessons is never considered completed.
     */
    fun isCourseCompleted(courseId: String, lessonIds: Collection<String>): Boolean {
        if (lessonIds.isEmpty()) return false
        return lessonIds.all { lessonId -> isLessonCompleted(courseId, lessonId) }
    }

    // --- Certificates ---

    /**
     * Retrieve existing certificate for a course if already issued (matching any known alias).
     */
    fun getCertificate(courseId: String): CourseCertificate? {
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        for (alias in aliases) {
            val cert = certificatesMap[alias]
            if (cert != null) return cert
        }
        return null
    }

    /**
     * List all issued certificates sorted by issue timestamp.
     */
    fun getAllCertificates(): List<CourseCertificate> {
        return certificatesMap.values.distinctBy { it.id }.sortedByDescending { it.issuedAtTimestamp }
    }

    /**
     * Issue an EduNova completion certificate once criteria are verified.
     * Idempotent: returns existing certificate if one was already issued, preventing duplicates.
     * Rejects courses with 0 lessons.
     */
    fun issueCertificate(
        courseId: String,
        courseName: String,
        learnerName: String,
        totalLessons: Int
    ): CourseCertificate? {
        synchronized(this) {
            if (totalLessons <= 0) return null

            // Return existing certificate to prevent duplicate issuance across aliases
            getCertificate(courseId)?.let { return it }

            val randomHex = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
            val certId = "EDUNOVA-CERT-$randomHex"
            val formattedDate = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())

            val cert = CourseCertificate(
                id = certId,
                courseId = courseId,
                courseName = courseName,
                learnerName = learnerName.ifBlank { "Learner" },
                completedDate = formattedDate,
                totalLessons = totalLessons,
                verificationType = "LOCAL_PERSISTED",
                issuedAtTimestamp = System.currentTimeMillis()
            )
            val aliases = CourseIdNormalizer.getAllAliases(courseId)
            for (alias in aliases) {
                certificatesMap[alias] = cert
            }
            saveToFile()
            return cert
        }
    }

    // --- Revision items ---

    fun addRevisionItem(item: RevisionItem) {
        synchronized(this) {
            val key = makeKey(item.courseId, item.lessonId)
            revisionItemsMap[key] = item
            saveToFile()
        }
    }

    fun removeRevisionItem(courseId: String, lessonId: String) {
        synchronized(this) {
            val aliases = CourseIdNormalizer.getAllAliases(courseId)
            for (alias in aliases) {
                revisionItemsMap.remove(makeKey(alias, lessonId))
            }
            // Also clean up by lessonId if courseId alias was mapped
            val matchingKeys = revisionItemsMap.keys().toList().filter { key ->
                val parts = key.split(":")
                parts.size == 2 && parts[1] == lessonId && aliases.contains(parts[0])
            }
            matchingKeys.forEach { revisionItemsMap.remove(it) }
            saveToFile()
        }
    }

    fun isMarkedForRevision(courseId: String, lessonId: String): Boolean {
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        return aliases.any { alias -> revisionItemsMap.containsKey(makeKey(alias, lessonId)) }
    }

    fun getRevisionItems(): List<RevisionItem> {
        return revisionItemsMap.values.distinctBy { it.lessonId }.sortedByDescending { it.markedAt }
    }

    fun clearAll() {
        synchronized(this) {
            completedLessons.clear()
            revisionItemsMap.clear()
            certificatesMap.clear()
            storageFile?.delete()
        }
    }
}
