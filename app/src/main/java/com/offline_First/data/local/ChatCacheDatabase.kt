package com.offline_First.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.UserMemoryItem
import com.offline_First.domain.model.RoadmapOption
import com.offline_First.domain.model.RoadmapAccentTheme
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.domain.model.PersonalizedPhase
import com.offline_First.domain.model.PersonalizedTask
import com.offline_First.domain.model.PersonalizedMilestone
import com.offline_First.domain.model.WeeklyScheduleItem
import com.offline_First.domain.model.AssessmentSummary
import com.offline_First.domain.model.SkillGapItem
import com.offline_First.data.remote.PersonalizedPhaseDto
import com.offline_First.data.remote.PersonalizedTaskDto
import com.offline_First.data.remote.PersonalizedMilestoneDto
import com.offline_First.data.remote.WeeklyScheduleItemDto
import com.offline_First.data.remote.AssessmentSummaryDto
import com.offline_First.data.remote.SkillGapItemDto
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.CourseModule
import com.offline_First.domain.model.LessonItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private const val DB_NAME = "edunova_chat_cache.db"
private const val DB_VERSION = 5

data class LocalSavedCourse(
    val userId: String,
    val courseId: String,
    val savedAt: Long,
    val downloadStatus: String = "completed",
    val isOfflineReady: Boolean = false,
    val syncStatus: String = "synced"
)

data class LocalSavedRoadmap(
    val userId: String,
    val roadmapId: String,
    val roadmapType: String = "system",
    val savedAt: Long,
    val downloadStatus: String = "completed",
    val isOfflineReady: Boolean = true,
    val syncStatus: String = "synced"
)


data class LocalLessonProgressRecord(
    val userId: String,
    val courseId: String,
    val lessonId: String,
    val isCompleted: Boolean,
    val completionTimestamp: Long,
    val syncStatus: String = "synced"
)

data class PendingMilestoneSync(
    val roadmapId: String,
    val userId: String,
    val milestoneKey: String,
    val isCompleted: Boolean,
    val updatedAt: Long
)

open class ChatCacheDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DATABASE_VERSION = DB_VERSION
        const val DATABASE_NAME = DB_NAME
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_conversations (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                title TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                is_archived INTEGER NOT NULL DEFAULT 0,
                is_pinned INTEGER NOT NULL DEFAULT 0,
                draft_text TEXT NOT NULL DEFAULT '',
                sync_status TEXT NOT NULL DEFAULT 'synced'
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_messages (
                id TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                parent_id TEXT DEFAULT NULL,
                created_at INTEGER NOT NULL,
                sync_status TEXT NOT NULL DEFAULT 'synced',
                FOREIGN KEY (conversation_id) REFERENCES local_conversations(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_memories (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                category TEXT NOT NULL,
                content TEXT NOT NULL,
                importance REAL NOT NULL DEFAULT 1.0,
                confidence REAL NOT NULL DEFAULT 1.0,
                source_conversation_id TEXT DEFAULT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                sync_status TEXT NOT NULL DEFAULT 'synced'
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_system_roadmaps (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT NOT NULL,
                skills_json TEXT NOT NULL DEFAULT '[]',
                level TEXT NOT NULL,
                duration TEXT NOT NULL,
                stages INTEGER NOT NULL,
                icon TEXT NOT NULL,
                accent_theme TEXT NOT NULL DEFAULT 'primary',
                is_personalized INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL,
                sync_status TEXT NOT NULL DEFAULT 'synced'
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_personalized_roadmaps (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                title TEXT NOT NULL,
                goal TEXT NOT NULL,
                category TEXT NOT NULL,
                level TEXT NOT NULL,
                duration TEXT NOT NULL,
                stages INTEGER NOT NULL,
                icon TEXT NOT NULL,
                accent_theme TEXT NOT NULL DEFAULT 'primary',
                weekly_hours REAL NOT NULL DEFAULT 0.0,
                assessment_summary_json TEXT NOT NULL DEFAULT '{}',
                phases_json TEXT NOT NULL DEFAULT '[]',
                weekly_schedule_json TEXT NOT NULL DEFAULT '[]',
                assumptions_json TEXT NOT NULL DEFAULT '[]',
                capstone_project TEXT NOT NULL DEFAULT '',
                next_action TEXT NOT NULL DEFAULT '',
                created_at TEXT DEFAULT NULL,
                updated_at INTEGER NOT NULL,
                sync_status TEXT NOT NULL DEFAULT 'synced'
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_milestone_progress (
                roadmap_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                milestone_key TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL,
                sync_status TEXT NOT NULL DEFAULT 'synced',
                PRIMARY KEY (roadmap_id, user_id, milestone_key)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_roadmap_categories (
                name TEXT PRIMARY KEY,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_saved_courses (
                user_id TEXT NOT NULL,
                course_id TEXT NOT NULL,
                saved_at INTEGER NOT NULL,
                download_status TEXT NOT NULL DEFAULT 'completed',
                is_offline_ready INTEGER NOT NULL DEFAULT 0,
                sync_status TEXT NOT NULL DEFAULT 'synced',
                PRIMARY KEY (user_id, course_id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_course_details (
                course_id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                slug TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                icon TEXT NOT NULL DEFAULT '',
                accent TEXT NOT NULL DEFAULT 'primary',
                category TEXT NOT NULL DEFAULT '',
                level TEXT NOT NULL DEFAULT '',
                duration_hours INTEGER DEFAULT NULL,
                total_lessons INTEGER NOT NULL DEFAULT 0,
                modules_count INTEGER NOT NULL DEFAULT 0,
                prerequisites_json TEXT NOT NULL DEFAULT '[]',
                updated_at INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_course_modules (
                module_id TEXT PRIMARY KEY,
                course_id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT DEFAULT '',
                display_order INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_lessons (
                lesson_id TEXT PRIMARY KEY,
                module_id TEXT NOT NULL,
                course_id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT DEFAULT '',
                content_type TEXT NOT NULL DEFAULT 'article',
                duration_minutes INTEGER DEFAULT 0,
                display_order INTEGER NOT NULL DEFAULT 0,
                resource_url TEXT DEFAULT NULL,
                resource_provider TEXT DEFAULT NULL,
                video_url TEXT DEFAULT NULL,
                video_provider TEXT DEFAULT NULL,
                content_body TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_user_course_progress (
                user_id TEXT NOT NULL,
                course_id TEXT NOT NULL,
                completion_percentage REAL NOT NULL DEFAULT 0.0,
                status TEXT NOT NULL DEFAULT 'in_progress',
                last_accessed INTEGER NOT NULL DEFAULT 0,
                sync_status TEXT NOT NULL DEFAULT 'synced',
                PRIMARY KEY (user_id, course_id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_user_lesson_progress (
                user_id TEXT NOT NULL,
                course_id TEXT NOT NULL,
                lesson_id TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                completion_timestamp INTEGER NOT NULL DEFAULT 0,
                sync_status TEXT NOT NULL DEFAULT 'synced',
                PRIMARY KEY (user_id, lesson_id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_saved_roadmaps (
                user_id TEXT NOT NULL,
                roadmap_id TEXT NOT NULL,
                roadmap_type TEXT NOT NULL DEFAULT 'system',
                saved_at INTEGER NOT NULL,
                download_status TEXT NOT NULL DEFAULT 'completed',
                is_offline_ready INTEGER NOT NULL DEFAULT 1,
                sync_status TEXT NOT NULL DEFAULT 'synced',
                PRIMARY KEY (user_id, roadmap_id)
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_conv_user ON local_conversations(user_id, is_archived, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_conv_id ON local_messages(user_id, conversation_id, created_at ASC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_memories_user ON local_memories(user_id, active)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sys_roadmaps_user_cat ON local_system_roadmaps(user_id, category, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pers_roadmaps_user ON local_personalized_roadmaps(user_id, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_milestone_sync ON local_milestone_progress(user_id, sync_status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_saved_courses_user ON local_saved_courses(user_id, is_offline_ready)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_saved_roadmaps_user ON local_saved_roadmaps(user_id, is_offline_ready)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_modules_course ON local_course_modules(course_id, display_order)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lessons_module ON local_lessons(module_id, display_order)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lessons_course ON local_lessons(course_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_lesson_progress ON local_user_lesson_progress(user_id, course_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Proper migration: never wipe existing user data
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE local_conversations ADD COLUMN user_id TEXT NOT NULL DEFAULT 'default_user'")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE local_conversations ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE local_conversations ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE local_conversations ADD COLUMN draft_text TEXT NOT NULL DEFAULT ''")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE local_conversations ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'synced'")
            } catch (_: Exception) {}

            try {
                db.execSQL("ALTER TABLE local_messages ADD COLUMN user_id TEXT NOT NULL DEFAULT 'default_user'")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE local_messages ADD COLUMN parent_id TEXT DEFAULT NULL")
            } catch (_: Exception) {}

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_memories (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    category TEXT NOT NULL,
                    content TEXT NOT NULL,
                    importance REAL NOT NULL DEFAULT 1.0,
                    confidence REAL NOT NULL DEFAULT 1.0,
                    source_conversation_id TEXT DEFAULT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'synced'
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_conv_user ON local_conversations(user_id, is_archived, updated_at DESC)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_conv_id ON local_messages(user_id, conversation_id, created_at ASC)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_memories_user ON local_memories(user_id, active)")
        }

        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_system_roadmaps (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT NOT NULL,
                    skills_json TEXT NOT NULL DEFAULT '[]',
                    level TEXT NOT NULL,
                    duration TEXT NOT NULL,
                    stages INTEGER NOT NULL,
                    icon TEXT NOT NULL,
                    accent_theme TEXT NOT NULL DEFAULT 'primary',
                    is_personalized INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL,
                    sync_status TEXT NOT NULL DEFAULT 'synced'
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_personalized_roadmaps (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    goal TEXT NOT NULL,
                    category TEXT NOT NULL,
                    level TEXT NOT NULL,
                    duration TEXT NOT NULL,
                    stages INTEGER NOT NULL,
                    icon TEXT NOT NULL,
                    accent_theme TEXT NOT NULL DEFAULT 'primary',
                    weekly_hours REAL NOT NULL DEFAULT 0.0,
                    assessment_summary_json TEXT NOT NULL DEFAULT '{}',
                    phases_json TEXT NOT NULL DEFAULT '[]',
                    weekly_schedule_json TEXT NOT NULL DEFAULT '[]',
                    assumptions_json TEXT NOT NULL DEFAULT '[]',
                    capstone_project TEXT NOT NULL DEFAULT '',
                    next_action TEXT NOT NULL DEFAULT '',
                    created_at TEXT DEFAULT NULL,
                    updated_at INTEGER NOT NULL,
                    sync_status TEXT NOT NULL DEFAULT 'synced'
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_milestone_progress (
                    roadmap_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    milestone_key TEXT NOT NULL,
                    is_completed INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL,
                    sync_status TEXT NOT NULL DEFAULT 'synced',
                    PRIMARY KEY (roadmap_id, user_id, milestone_key)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_roadmap_categories (
                    name TEXT PRIMARY KEY,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_sys_roadmaps_user_cat ON local_system_roadmaps(user_id, category, updated_at DESC)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_pers_roadmaps_user ON local_personalized_roadmaps(user_id, updated_at DESC)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_milestone_sync ON local_milestone_progress(user_id, sync_status)")
        }

        if (oldVersion < 4) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_saved_courses (
                    user_id TEXT NOT NULL,
                    course_id TEXT NOT NULL,
                    saved_at INTEGER NOT NULL,
                    download_status TEXT NOT NULL DEFAULT 'completed',
                    is_offline_ready INTEGER NOT NULL DEFAULT 0,
                    sync_status TEXT NOT NULL DEFAULT 'synced',
                    PRIMARY KEY (user_id, course_id)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_course_details (
                    course_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    slug TEXT NOT NULL DEFAULT '',
                    description TEXT NOT NULL DEFAULT '',
                    icon TEXT NOT NULL DEFAULT '',
                    accent TEXT NOT NULL DEFAULT 'primary',
                    category TEXT NOT NULL DEFAULT '',
                    level TEXT NOT NULL DEFAULT '',
                    duration_hours INTEGER DEFAULT NULL,
                    total_lessons INTEGER NOT NULL DEFAULT 0,
                    modules_count INTEGER NOT NULL DEFAULT 0,
                    prerequisites_json TEXT NOT NULL DEFAULT '[]',
                    updated_at INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_course_modules (
                    module_id TEXT PRIMARY KEY,
                    course_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT DEFAULT '',
                    display_order INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_lessons (
                    lesson_id TEXT PRIMARY KEY,
                    module_id TEXT NOT NULL,
                    course_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT DEFAULT '',
                    content_type TEXT NOT NULL DEFAULT 'article',
                    duration_minutes INTEGER DEFAULT 0,
                    display_order INTEGER NOT NULL DEFAULT 0,
                    resource_url TEXT DEFAULT NULL,
                    resource_provider TEXT DEFAULT NULL,
                    video_url TEXT DEFAULT NULL,
                    video_provider TEXT DEFAULT NULL,
                    content_body TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_user_course_progress (
                    user_id TEXT NOT NULL,
                    course_id TEXT NOT NULL,
                    completion_percentage REAL NOT NULL DEFAULT 0.0,
                    status TEXT NOT NULL DEFAULT 'in_progress',
                    last_accessed INTEGER NOT NULL DEFAULT 0,
                    sync_status TEXT NOT NULL DEFAULT 'synced',
                    PRIMARY KEY (user_id, course_id)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_user_lesson_progress (
                    user_id TEXT NOT NULL,
                    course_id TEXT NOT NULL,
                    lesson_id TEXT NOT NULL,
                    is_completed INTEGER NOT NULL DEFAULT 0,
                    completion_timestamp INTEGER NOT NULL DEFAULT 0,
                    sync_status TEXT NOT NULL DEFAULT 'synced',
                    PRIMARY KEY (user_id, lesson_id)
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_saved_courses_user ON local_saved_courses(user_id, is_offline_ready)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_modules_course ON local_course_modules(course_id, display_order)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lessons_module ON local_lessons(module_id, display_order)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lessons_course ON local_lessons(course_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_lesson_progress ON local_user_lesson_progress(user_id, course_id)")
        }

        if (oldVersion < 5) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_saved_roadmaps (
                    user_id TEXT NOT NULL,
                    roadmap_id TEXT NOT NULL,
                    roadmap_type TEXT NOT NULL DEFAULT 'system',
                    saved_at INTEGER NOT NULL,
                    download_status TEXT NOT NULL DEFAULT 'completed',
                    is_offline_ready INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'synced',
                    PRIMARY KEY (user_id, roadmap_id)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_saved_roadmaps_user ON local_saved_roadmaps(user_id, is_offline_ready)")
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Do not drop tables
        onUpgrade(db, oldVersion, newVersion)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        runCatching {
            db.execSQL("UPDATE local_course_details SET slug = LOWER(REPLACE(name, ' ', '-')) WHERE slug IS NULL OR slug = course_id OR slug = ''")
        }
    }

    // --- Conversation operations (Strictly scoped by user_id) ---

    fun upsertConversation(
        id: String,
        userId: String,
        title: String,
        createdAt: Long,
        updatedAt: Long,
        isArchived: Boolean = false,
        isPinned: Boolean = false,
        draftText: String = "",
        syncStatus: String = "synced"
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("user_id", userId)
            put("title", title)
            put("created_at", createdAt)
            put("updated_at", updatedAt)
            put("is_archived", if (isArchived) 1 else 0)
            put("is_pinned", if (isPinned) 1 else 0)
            put("draft_text", draftText)
            put("sync_status", syncStatus)
        }
        val rowsUpdated = writableDatabase.update(
            "local_conversations", values, "id = ? AND user_id = ?", arrayOf(id, userId)
        )
        if (rowsUpdated == 0) {
            writableDatabase.insertWithOnConflict(
                "local_conversations", null, values, SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    fun getConversations(
        userId: String,
        limit: Int = 20,
        cursorUpdatedAt: Long? = null,
        includeArchived: Boolean = false
    ): List<ChatSession> {
        val result = mutableListOf<ChatSession>()
        val selectionArgs = mutableListOf<String>()
        val queryBuilder = StringBuilder(
            "SELECT id, title, updated_at, is_archived, is_pinned, draft_text FROM local_conversations WHERE user_id = ?"
        )
        selectionArgs.add(userId)

        if (!includeArchived) {
            queryBuilder.append(" AND is_archived = 0")
        }

        if (cursorUpdatedAt != null) {
            queryBuilder.append(" AND updated_at < ?")
            selectionArgs.add(cursorUpdatedAt.toString())
        }

        queryBuilder.append(" ORDER BY is_pinned DESC, updated_at DESC LIMIT ?")
        selectionArgs.add(limit.toString())

        val cursor: Cursor = readableDatabase.rawQuery(queryBuilder.toString(), selectionArgs.toTypedArray())
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    ChatSession(
                        id = it.getString(0),
                        title = it.getString(1),
                        lastUpdated = it.getLong(2),
                        isArchived = it.getInt(3) == 1,
                        isPinned = it.getInt(4) == 1,
                        draftText = it.getString(5).orEmpty()
                    )
                )
            }
        }
        return result
    }

    fun getConversationWithMessages(conversationId: String, userId: String): ChatSession? {
        val cursor = readableDatabase.rawQuery(
            "SELECT id, title, updated_at, is_archived, is_pinned, draft_text FROM local_conversations WHERE id = ? AND user_id = ?",
            arrayOf(conversationId, userId)
        )
        val session = cursor.use {
            if (!it.moveToFirst()) return null
            ChatSession(
                id = it.getString(0),
                title = it.getString(1),
                lastUpdated = it.getLong(2),
                isArchived = it.getInt(3) == 1,
                isPinned = it.getInt(4) == 1,
                draftText = it.getString(5).orEmpty()
            )
        } ?: return null

        val messages = getMessagesForConversation(conversationId, userId)
        return session.copy(messages = messages)
    }

    fun updateConversationDraft(conversationId: String, userId: String, draftText: String, syncStatus: String? = null) {
        val values = ContentValues().apply {
            put("draft_text", draftText)
            if (syncStatus != null) put("sync_status", syncStatus)
        }
        writableDatabase.update("local_conversations", values, "id = ? AND user_id = ?", arrayOf(conversationId, userId))
    }

    fun updateConversationTitle(conversationId: String, userId: String, newTitle: String, syncStatus: String? = null) {
        val values = ContentValues().apply {
            put("title", newTitle)
            put("updated_at", System.currentTimeMillis())
            if (syncStatus != null) put("sync_status", syncStatus)
        }
        writableDatabase.update("local_conversations", values, "id = ? AND user_id = ?", arrayOf(conversationId, userId))
    }

    fun toggleArchive(conversationId: String, userId: String, isArchived: Boolean, syncStatus: String? = null) {
        val values = ContentValues().apply {
            put("is_archived", if (isArchived) 1 else 0)
            if (syncStatus != null) put("sync_status", syncStatus)
        }
        writableDatabase.update("local_conversations", values, "id = ? AND user_id = ?", arrayOf(conversationId, userId))
    }

    fun togglePin(conversationId: String, userId: String, isPinned: Boolean, syncStatus: String? = null) {
        val values = ContentValues().apply {
            put("is_pinned", if (isPinned) 1 else 0)
            if (syncStatus != null) put("sync_status", syncStatus)
        }
        writableDatabase.update("local_conversations", values, "id = ? AND user_id = ?", arrayOf(conversationId, userId))
    }

    fun deleteConversation(conversationId: String, userId: String) {
        writableDatabase.delete("local_conversations", "id = ? AND user_id = ?", arrayOf(conversationId, userId))
        writableDatabase.delete("local_messages", "conversation_id = ? AND user_id = ?", arrayOf(conversationId, userId))
    }

    fun getUnsyncedConversations(userId: String): List<ChatSession> {
        val result = mutableListOf<ChatSession>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, title, updated_at, is_archived, is_pinned, draft_text FROM local_conversations WHERE user_id = ? AND sync_status = 'pending_sync'",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    ChatSession(
                        id = it.getString(0),
                        title = it.getString(1),
                        lastUpdated = it.getLong(2),
                        isArchived = it.getInt(3) == 1,
                        isPinned = it.getInt(4) == 1,
                        draftText = it.getString(5).orEmpty()
                    )
                )
            }
        }
        return result
    }

    fun markConversationSynced(id: String, userId: String) {
        val values = ContentValues().apply { put("sync_status", "synced") }
        writableDatabase.update("local_conversations", values, "id = ? AND user_id = ?", arrayOf(id, userId))
    }

    // --- Message operations (Strictly scoped by user_id) ---

    fun upsertMessage(
        id: String,
        conversationId: String,
        userId: String,
        role: String,
        content: String,
        createdAt: Long,
        parentId: String? = null,
        syncStatus: String = "synced"
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("conversation_id", conversationId)
            put("user_id", userId)
            put("role", role)
            put("content", content)
            put("parent_id", parentId)
            put("created_at", createdAt)
            put("sync_status", syncStatus)
        }
        writableDatabase.insertWithOnConflict(
            "local_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun upsertMessages(
        messages: List<Pair<ChatMessage, String>>,
        conversationId: String,
        userId: String
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((msg, role) in messages) {
                val values = ContentValues().apply {
                    put("id", msg.id)
                    put("conversation_id", conversationId)
                    put("user_id", userId)
                    put("role", role)
                    put("content", msg.text)
                    put("parent_id", msg.parentId)
                    put("created_at", msg.timestamp)
                    put("sync_status", msg.syncStatus)
                }
                db.insertWithOnConflict("local_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getMessagesForConversation(
        conversationId: String,
        userId: String,
        limit: Int = 50,
        beforeTimestamp: Long? = null
    ): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        val selectionArgs = mutableListOf<String>()
        val queryBuilder = StringBuilder(
            "SELECT id, role, content, created_at, parent_id, sync_status FROM local_messages WHERE conversation_id = ? AND user_id = ?"
        )
        selectionArgs.add(conversationId)
        selectionArgs.add(userId)

        if (beforeTimestamp != null) {
            queryBuilder.append(" AND created_at < ?")
            selectionArgs.add(beforeTimestamp.toString())
        }

        queryBuilder.append(" ORDER BY created_at DESC LIMIT ?")
        selectionArgs.add(limit.toString())

        val cursor = readableDatabase.rawQuery(queryBuilder.toString(), selectionArgs.toTypedArray())
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    ChatMessage(
                        id = it.getString(0),
                        fromUser = it.getString(1) == "user",
                        text = it.getString(2),
                        timestamp = it.getLong(3),
                        parentId = it.getString(4),
                        syncStatus = it.getString(5).orEmpty()
                    )
                )
            }
        }
        // Return in ascending chronological order for chat view
        return result.reversed()
    }

    fun getUnsyncedMessages(userId: String): List<Pair<ChatMessage, String>> {
        val result = mutableListOf<Pair<ChatMessage, String>>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, role, content, created_at, parent_id, sync_status, conversation_id FROM local_messages WHERE user_id = ? AND sync_status = 'pending_sync' ORDER BY created_at ASC",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                val role = it.getString(1)
                val msg = ChatMessage(
                    id = it.getString(0),
                    fromUser = role == "user",
                    text = it.getString(2),
                    timestamp = it.getLong(3),
                    parentId = it.getString(4),
                    syncStatus = it.getString(5).orEmpty()
                )
                val convId = it.getString(6)
                result.add(Pair(msg, convId))
            }
        }
        return result
    }

    fun markMessagesSynced(ids: List<String>, userId: String) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (id in ids) {
                val values = ContentValues().apply { put("sync_status", "synced") }
                db.update("local_messages", values, "id = ? AND user_id = ?", arrayOf(id, userId))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteMessage(messageId: String, userId: String) {
        writableDatabase.delete("local_messages", "id = ? AND user_id = ?", arrayOf(messageId, userId))
    }

    // --- Memory operations (Strictly scoped by user_id) ---

    fun upsertMemory(memory: UserMemoryItem, userId: String, sourceConvId: String? = null) {
        val now = System.currentTimeMillis()
        val cursor = readableDatabase.rawQuery(
            "SELECT id FROM local_memories WHERE user_id = ? AND category = ? AND content = ? LIMIT 1",
            arrayOf(userId, memory.category, memory.content.trim())
        )
        val existingId: String? = cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }

        val values = ContentValues().apply {
            put("user_id", userId)
            put("category", memory.category)
            put("content", memory.content.trim())
            put("importance", memory.importance)
            put("confidence", memory.confidence)
            if (sourceConvId != null) put("source_conversation_id", sourceConvId)
            put("updated_at", now)
            put("active", if (memory.active) 1 else 0)
            put("sync_status", "synced")
        }

        if (existingId != null) {
            writableDatabase.update("local_memories", values, "id = ?", arrayOf(existingId))
        } else {
            values.put("id", memory.id)
            values.put("created_at", now)
            writableDatabase.insertWithOnConflict("local_memories", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getMemories(userId: String): List<UserMemoryItem> {
        val result = mutableListOf<UserMemoryItem>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, category, content, importance, confidence, active FROM local_memories WHERE user_id = ? ORDER BY importance DESC, updated_at DESC",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    UserMemoryItem(
                        id = it.getString(0),
                        category = it.getString(1),
                        content = it.getString(2),
                        importance = it.getFloat(3),
                        confidence = it.getFloat(4),
                        active = it.getInt(5) == 1
                    )
                )
            }
        }
        return result
    }

    /**
     * Retrieves top relevant memories for prompt context using keyword overlap and importance weight.
     * Enforces userId and active status.
     */
    fun getRelevantMemories(userId: String, prompt: String, limit: Int = 5): List<UserMemoryItem> {
        val all = getMemories(userId).filter { it.active }
        if (all.isEmpty()) return emptyList()

        val promptTokens = prompt.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 }
            .toSet()

        return all.sortedByDescending { memory ->
            val memoryTokens = memory.content.lowercase()
                .split(Regex("[^a-zA-Z0-9]+"))
                .toSet()
            val overlap = promptTokens.intersect(memoryTokens).size
            (overlap * 2.0f) + memory.importance
        }.take(limit)
    }

    fun deleteMemory(memoryId: String, userId: String) {
        writableDatabase.delete("local_memories", "id = ? AND user_id = ?", arrayOf(memoryId, userId))
    }

    fun clearAllMemories(userId: String) {
        writableDatabase.delete("local_memories", "user_id = ?", arrayOf(userId))
    }

    // --- System Roadmaps & Categories ---

    open fun upsertSystemRoadmaps(roadmaps: List<RoadmapOption>, userId: String) {
        if (roadmaps.isEmpty()) return
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for (r in roadmaps) {
                val values = ContentValues().apply {
                    put("id", r.id)
                    put("user_id", userId)
                    put("title", r.title)
                    put("category", r.category)
                    put("description", r.description)
                    put("skills_json", json.encodeToString(r.skills))
                    put("level", r.level)
                    put("duration", r.duration)
                    put("stages", r.stages)
                    put("icon", r.icon)
                    put("accent_theme", r.accentTheme.name)
                    put("is_personalized", if (r.isPersonalized) 1 else 0)
                    put("updated_at", now)
                    put("sync_status", "synced")
                }
                db.insertWithOnConflict("local_system_roadmaps", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    open fun getSystemRoadmaps(userId: String, category: String? = null): List<RoadmapOption> {
        val result = mutableListOf<RoadmapOption>()
        val sql = if (category != null) {
            "SELECT id, title, category, description, skills_json, level, duration, stages, icon, accent_theme, is_personalized FROM local_system_roadmaps WHERE (user_id = ? OR user_id = 'default_user') AND LOWER(category) = LOWER(?) ORDER BY updated_at DESC"
        } else {
            "SELECT id, title, category, description, skills_json, level, duration, stages, icon, accent_theme, is_personalized FROM local_system_roadmaps WHERE (user_id = ? OR user_id = 'default_user') ORDER BY updated_at DESC"
        }
        val args = if (category != null) arrayOf(userId, category) else arrayOf(userId)
        val cursor = readableDatabase.rawQuery(sql, args)
        cursor.use {
            while (it.moveToNext()) {
                val skillsJson = it.getString(4) ?: "[]"
                val skills = runCatching { json.decodeFromString<List<String>>(skillsJson) }.getOrDefault(emptyList())
                val themeStr = it.getString(9) ?: "PRIMARY"
                val accent = mapAccentTheme(themeStr)
                result.add(
                    RoadmapOption(
                        id = it.getString(0),
                        title = it.getString(1),
                        category = it.getString(2),
                        description = it.getString(3),
                        skills = skills,
                        level = it.getString(5),
                        duration = it.getString(6),
                        stages = it.getInt(7),
                        icon = it.getString(8),
                        accentTheme = accent,
                        isPersonalized = it.getInt(10) == 1
                    )
                )
            }
        }
        return result.distinctBy { it.id }
    }

    fun upsertRoadmapCategories(categories: List<String>) {
        if (categories.isEmpty()) return
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for (cat in categories) {
                val values = ContentValues().apply {
                    put("name", cat)
                    put("updated_at", now)
                }
                db.insertWithOnConflict("local_roadmap_categories", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getRoadmapCategories(): List<String> {
        val result = mutableListOf<String>()
        val cursor = readableDatabase.rawQuery(
            "SELECT name FROM local_roadmap_categories ORDER BY name ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(it.getString(0))
            }
        }
        return result
    }

    // --- Personalized Roadmaps ---

    fun upsertPersonalizedRoadmaps(
        roadmaps: List<PersonalizedRoadmapDetail>,
        userId: String,
        syncStatus: String = "synced"
    ) {
        if (roadmaps.isEmpty()) return
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for (r in roadmaps) {
                val phaseDtos = r.phases.map { p ->
                    PersonalizedPhaseDto(
                        title = p.title,
                        objective = p.objective,
                        durationWeeks = p.durationWeeks,
                        topics = p.topics,
                        tasks = p.tasks.map { t ->
                            PersonalizedTaskDto(
                                id = t.id,
                                title = t.title,
                                description = t.description,
                                instructions = t.instructions,
                                estimatedHours = t.estimatedHours,
                                resources = t.resources,
                                completionCriteria = t.completionCriteria,
                                isCompleted = t.isCompleted,
                                dependencies = t.dependencies
                            )
                        },
                        activities = p.activities,
                        resources = p.resources,
                        milestones = p.milestones.map { m ->
                            PersonalizedMilestoneDto(
                                title = m.title,
                                completionCriteria = m.completionCriteria,
                                assessment = m.assessment,
                                passingCriteria = m.passingCriteria
                            )
                        },
                        recommendedCourseIds = p.recommendedCourseIds
                    )
                }
                val schedDtos = r.weeklySchedule.map { s ->
                    WeeklyScheduleItemDto(
                        dayOrWeek = s.dayOrWeek,
                        focusTopic = s.focusTopic,
                        estimatedHours = s.estimatedHours,
                        tasks = s.tasks
                    )
                }
                val sumDto = AssessmentSummaryDto(
                    strengths = r.assessmentSummary.strengths,
                    skillGaps = r.assessmentSummary.skillGaps,
                    verifiedEvidence = r.assessmentSummary.verifiedEvidence,
                    selfReportedInformation = r.assessmentSummary.selfReportedInformation,
                    unknowns = r.assessmentSummary.unknowns,
                    skillGapBreakdown = r.assessmentSummary.skillGapBreakdown.map {
                        SkillGapItemDto(
                            skill = it.skill,
                            status = it.status,
                            source = it.source,
                            confidence = it.confidence,
                            rationale = it.rationale
                        )
                    },
                    curriculumRationale = r.assessmentSummary.curriculumRationale
                )

                val values = ContentValues().apply {
                    put("id", r.id)
                    put("user_id", userId)
                    put("title", r.title)
                    put("goal", r.goal)
                    put("category", r.category)
                    put("level", r.level)
                    put("duration", r.duration)
                    put("stages", r.stages)
                    put("icon", r.icon)
                    put("accent_theme", r.accentTheme.name)
                    put("weekly_hours", r.weeklyHours)
                    put("assessment_summary_json", json.encodeToString(sumDto))
                    put("phases_json", json.encodeToString(phaseDtos))
                    put("weekly_schedule_json", json.encodeToString(schedDtos))
                    put("assumptions_json", json.encodeToString(r.assumptions))
                    put("capstone_project", r.capstoneProject)
                    put("next_action", r.nextAction)
                    put("created_at", r.createdAt)
                    put("updated_at", now)
                    put("sync_status", syncStatus)
                }
                db.insertWithOnConflict("local_personalized_roadmaps", null, values, SQLiteDatabase.CONFLICT_REPLACE)

                // Batch upsert completed milestones if provided and not already pending local toggle
                if (r.completedMilestones.isNotEmpty()) {
                    for (mKey in r.completedMilestones) {
                        // Check if local DB has a pending unsynced state for this milestone
                        val checkCursor = db.rawQuery(
                            "SELECT sync_status FROM local_milestone_progress WHERE roadmap_id = ? AND user_id = ? AND milestone_key = ?",
                            arrayOf(r.id, userId, mKey)
                        )
                        val isPending = checkCursor.use {
                            if (it.moveToFirst()) it.getString(0) == "pending_sync" else false
                        }
                        if (!isPending) {
                            val mValues = ContentValues().apply {
                                put("roadmap_id", r.id)
                                put("user_id", userId)
                                put("milestone_key", mKey)
                                put("is_completed", 1)
                                put("updated_at", now)
                                put("sync_status", "synced")
                            }
                            db.insertWithOnConflict("local_milestone_progress", null, mValues, SQLiteDatabase.CONFLICT_REPLACE)
                        }
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun savePersonalizedRoadmap(
        roadmap: PersonalizedRoadmapDetail,
        userId: String,
        syncStatus: String = "pending_sync"
    ) {
        upsertPersonalizedRoadmaps(listOf(roadmap), userId, syncStatus = syncStatus)
    }

    fun getPersonalizedRoadmaps(userId: String): List<PersonalizedRoadmapDetail> {
        val result = mutableListOf<PersonalizedRoadmapDetail>()
        val cursor = readableDatabase.rawQuery(
            """
            SELECT id, title, goal, category, level, duration, stages, icon, accent_theme, 
                   weekly_hours, assessment_summary_json, phases_json, weekly_schedule_json, 
                   assumptions_json, capstone_project, next_action, created_at 
            FROM local_personalized_roadmaps 
            WHERE user_id = ? OR user_id = 'default_user' 
            ORDER BY updated_at DESC
            """.trimIndent(),
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                val roadmapId = it.getString(0)
                val completed = getCompletedMilestones(roadmapId, userId).toList()
                val item = parsePersonalizedRoadmapCursor(it, completed)
                if (item != null) {
                    result.add(item)
                }
            }
        }
        return result.distinctBy { it.id }
    }

    open fun getPersonalizedRoadmapById(roadmapId: String, userId: String): PersonalizedRoadmapDetail? {
        val cursor = readableDatabase.rawQuery(
            """
            SELECT id, title, goal, category, level, duration, stages, icon, accent_theme, 
                   weekly_hours, assessment_summary_json, phases_json, weekly_schedule_json, 
                   assumptions_json, capstone_project, next_action, created_at 
            FROM local_personalized_roadmaps 
            WHERE id = ? AND (user_id = ? OR user_id = 'default_user')
            """.trimIndent(),
            arrayOf(roadmapId, userId)
        )
        cursor.use {
            if (it.moveToFirst()) {
                val completed = getCompletedMilestones(roadmapId, userId).toList()
                return parsePersonalizedRoadmapCursor(it, completed)
            }
        }
        return null
    }

    fun getUnsyncedPersonalizedRoadmaps(userId: String): List<PersonalizedRoadmapDetail> {
        val result = mutableListOf<PersonalizedRoadmapDetail>()
        val cursor = readableDatabase.rawQuery(
            """
            SELECT id, title, goal, category, level, duration, stages, icon, accent_theme, 
                   weekly_hours, assessment_summary_json, phases_json, weekly_schedule_json, 
                   assumptions_json, capstone_project, next_action, created_at 
            FROM local_personalized_roadmaps 
            WHERE (user_id = ? OR user_id = 'default_user') AND sync_status = 'pending_sync'
            ORDER BY updated_at ASC
            """.trimIndent(),
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                val roadmapId = it.getString(0)
                val completed = getCompletedMilestones(roadmapId, userId).toList()
                val item = parsePersonalizedRoadmapCursor(it, completed)
                if (item != null) {
                    result.add(item)
                }
            }
        }
        return result
    }

    fun markPersonalizedRoadmapSynced(roadmapId: String, userId: String) {
        val values = ContentValues().apply { put("sync_status", "synced") }
        writableDatabase.update(
            "local_personalized_roadmaps",
            values,
            "id = ? AND (user_id = ? OR user_id = 'default_user')",
            arrayOf(roadmapId, userId)
        )
    }

    fun deletePersonalizedRoadmap(roadmapId: String, userId: String) {
        writableDatabase.delete(
            "local_personalized_roadmaps",
            "id = ? AND (user_id = ? OR user_id = 'default_user')",
            arrayOf(roadmapId, userId)
        )
        writableDatabase.delete(
            "local_milestone_progress",
            "roadmap_id = ? AND (user_id = ? OR user_id = 'default_user')",
            arrayOf(roadmapId, userId)
        )
    }

    fun updatePersonalizedRoadmapTitle(roadmapId: String, newTitle: String, userId: String) {
        val values = ContentValues().apply {
            put("title", newTitle)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update(
            "local_personalized_roadmaps",
            values,
            "id = ? AND (user_id = ? OR user_id = 'default_user')",
            arrayOf(roadmapId, userId)
        )
    }


    private fun parsePersonalizedRoadmapCursor(
        cursor: Cursor,
        completedMilestones: List<String>
    ): PersonalizedRoadmapDetail? {
        return runCatching {
            val id = cursor.getString(0)
            val title = cursor.getString(1)
            val goal = cursor.getString(2)
            val category = cursor.getString(3)
            val level = cursor.getString(4)
            val duration = cursor.getString(5)
            val stages = cursor.getInt(6)
            val icon = cursor.getString(7)
            val accentTheme = mapAccentTheme(cursor.getString(8) ?: "PRIMARY")
            val weeklyHours = cursor.getDouble(9)
            val sumJson = cursor.getString(10) ?: "{}"
            val phasesJson = cursor.getString(11) ?: "[]"
            val schedJson = cursor.getString(12) ?: "[]"
            val assumptionsJson = cursor.getString(13) ?: "[]"
            val capstone = cursor.getString(14).orEmpty()
            val nextAction = cursor.getString(15).orEmpty()
            val createdAt = cursor.getString(16)

            val sumDto = json.decodeFromString<AssessmentSummaryDto>(sumJson)
            val phaseDtos = json.decodeFromString<List<PersonalizedPhaseDto>>(phasesJson)
            val schedDtos = json.decodeFromString<List<WeeklyScheduleItemDto>>(schedJson)
            val assumptions = json.decodeFromString<List<String>>(assumptionsJson)

            val phases = phaseDtos.mapIndexed { pIdx, p ->
                PersonalizedPhase(
                    title = p.title,
                    objective = p.objective,
                    durationWeeks = p.durationWeeks,
                    topics = p.topics,
                    tasks = p.tasks.map { t ->
                        val isDone = completedMilestones.contains("${id}_task_${t.id}") || completedMilestones.contains(t.id)
                        PersonalizedTask(
                            id = t.id,
                            title = t.title,
                            description = t.description.orEmpty(),
                            instructions = t.instructions,
                            estimatedHours = t.estimatedHours,
                            resources = t.resources,
                            completionCriteria = t.completionCriteria,
                            isCompleted = isDone,
                            dependencies = t.dependencies
                        )
                    },
                    activities = p.activities,
                    resources = p.resources,
                    milestones = p.milestones.mapIndexed { mIdx, m ->
                        val mKey = "${id}_${pIdx}_${mIdx}_${m.title}"
                        val isDone = completedMilestones.contains(mKey) || completedMilestones.contains(m.title)
                        PersonalizedMilestone(
                            title = m.title,
                            completionCriteria = m.completionCriteria,
                            assessment = m.assessment,
                            passingCriteria = m.passingCriteria,
                            isCompleted = isDone
                        )
                    },
                    recommendedCourseIds = p.recommendedCourseIds
                )
            }

            val schedules = schedDtos.map { s ->
                WeeklyScheduleItem(
                    dayOrWeek = s.dayOrWeek,
                    focusTopic = s.focusTopic,
                    estimatedHours = s.estimatedHours,
                    tasks = s.tasks
                )
            }

            val summary = AssessmentSummary(
                strengths = sumDto.strengths,
                skillGaps = sumDto.skillGaps,
                verifiedEvidence = sumDto.verifiedEvidence,
                selfReportedInformation = sumDto.selfReportedInformation,
                unknowns = sumDto.unknowns,
                skillGapBreakdown = sumDto.skillGapBreakdown.map {
                    SkillGapItem(
                        skill = it.skill,
                        status = it.status,
                        source = it.source,
                        confidence = it.confidence,
                        rationale = it.rationale
                    )
                },
                curriculumRationale = sumDto.curriculumRationale
            )

            PersonalizedRoadmapDetail(
                id = id,
                title = title,
                goal = goal,
                category = category,
                level = level,
                duration = duration,
                stages = stages,
                icon = icon,
                accentTheme = accentTheme,
                weeklyHours = weeklyHours,
                assessmentSummary = summary,
                phases = phases,
                weeklySchedule = schedules,
                assumptions = assumptions,
                capstoneProject = capstone,
                nextAction = nextAction,
                completedMilestones = completedMilestones,
                createdAt = createdAt
            ).withSynthesizedTopicsIfEmpty()
        }.getOrNull()
    }


    // --- Milestone Progress & Sync Queue ---

    fun saveMilestoneProgress(
        roadmapId: String,
        userId: String,
        milestoneKey: String,
        isCompleted: Boolean,
        syncStatus: String = "pending_sync"
    ) {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("roadmap_id", roadmapId)
            put("user_id", userId)
            put("milestone_key", milestoneKey)
            put("is_completed", if (isCompleted) 1 else 0)
            put("updated_at", now)
            put("sync_status", syncStatus)
        }
        writableDatabase.insertWithOnConflict("local_milestone_progress", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCompletedMilestones(roadmapId: String, userId: String): Set<String> {
        val result = mutableSetOf<String>()
        val cursor = readableDatabase.rawQuery(
            "SELECT milestone_key FROM local_milestone_progress WHERE roadmap_id = ? AND (user_id = ? OR user_id = 'default_user') AND is_completed = 1",
            arrayOf(roadmapId, userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(it.getString(0))
            }
        }
        return result
    }

    fun getAllCompletedMilestones(userId: String): Map<String, Set<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()
        val cursor = readableDatabase.rawQuery(
            "SELECT roadmap_id, milestone_key FROM local_milestone_progress WHERE (user_id = ? OR user_id = 'default_user') AND is_completed = 1",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                val rId = it.getString(0)
                val mKey = it.getString(1)
                result.getOrPut(rId) { mutableSetOf() }.add(mKey)
            }
        }
        return result
    }

    fun getPendingSyncMilestones(userId: String): List<PendingMilestoneSync> {
        val result = mutableListOf<PendingMilestoneSync>()
        val cursor = readableDatabase.rawQuery(
            "SELECT roadmap_id, user_id, milestone_key, is_completed, updated_at FROM local_milestone_progress WHERE user_id = ? AND sync_status = 'pending_sync' ORDER BY updated_at ASC",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    PendingMilestoneSync(
                        roadmapId = it.getString(0),
                        userId = it.getString(1),
                        milestoneKey = it.getString(2),
                        isCompleted = it.getInt(3) == 1,
                        updatedAt = it.getLong(4)
                    )
                )
            }
        }
        return result
    }

    fun markMilestonesSynced(roadmapId: String, userId: String, milestoneKeys: List<String>) {
        if (milestoneKeys.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (mKey in milestoneKeys) {
                val values = ContentValues().apply {
                    put("sync_status", "synced")
                }
                db.update(
                    "local_milestone_progress",
                    values,
                    "roadmap_id = ? AND user_id = ? AND milestone_key = ?",
                    arrayOf(roadmapId, userId, mKey)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun mapAccentTheme(themeStr: String): RoadmapAccentTheme {
        return try {
            RoadmapAccentTheme.valueOf(themeStr.uppercase())
        } catch (_: Exception) {
            when (themeStr.lowercase()) {
                "secondary", "purple", "teal", "orange" -> RoadmapAccentTheme.SECONDARY
                "accent", "red" -> RoadmapAccentTheme.ACCENT
                "success", "green" -> RoadmapAccentTheme.SUCCESS
                else -> RoadmapAccentTheme.PRIMARY
            }
        }
    }

    // --- Saved Courses & Offline Content ---

    open fun saveCourseForOffline(
        userId: String,
        detail: CourseDetail,
        downloadStatus: String = "completed",
        syncStatus: String = "synced"
    ): Boolean {
        if (detail.id.isBlank() || detail.modules.isEmpty()) return false
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            // 1. Persist Course Detail Metadata
            val canonicalSlug = CourseIdNormalizer.toSlug(detail.name).ifBlank { detail.id }
            val courseValues = ContentValues().apply {
                put("course_id", detail.id)
                put("name", detail.name)
                put("slug", canonicalSlug)
                put("description", detail.description)
                put("icon", detail.icon)
                put("accent", detail.accent.name.lowercase())
                put("category", detail.category)
                put("level", detail.level)
                if (detail.durationHours != null) {
                    put("duration_hours", detail.durationHours)
                } else {
                    putNull("duration_hours")
                }
                put("total_lessons", detail.totalLessonsCount)
                put("modules_count", detail.modules.size)
                put("prerequisites_json", json.encodeToString(detail.prerequisites))
                put("updated_at", now)
            }
            db.insertWithOnConflict("local_course_details", null, courseValues, SQLiteDatabase.CONFLICT_REPLACE)
            CourseIdNormalizer.registerRemoteCourse(detail.id, canonicalSlug, detail.name)

            var persistedLessonsCount = 0
            // 2. Persist Modules and Lessons
            for (module in detail.modules) {
                val modValues = ContentValues().apply {
                    put("module_id", module.id)
                    put("course_id", detail.id)
                    put("title", module.title)
                    put("description", module.description.orEmpty())
                    put("display_order", module.displayOrder)
                }
                db.insertWithOnConflict("local_course_modules", null, modValues, SQLiteDatabase.CONFLICT_REPLACE)

                for (lesson in module.lessons) {
                    val lessonValues = ContentValues().apply {
                        put("lesson_id", lesson.id)
                        put("module_id", module.id)
                        put("course_id", detail.id)
                        put("title", lesson.title)
                        put("description", lesson.description.orEmpty())
                        put("content_type", lesson.contentType)
                        if (lesson.durationMinutes != null) {
                            put("duration_minutes", lesson.durationMinutes)
                        } else {
                            putNull("duration_minutes")
                        }
                        put("display_order", lesson.displayOrder)
                        put("resource_url", lesson.resourceUrl)
                        put("resource_provider", lesson.resourceProvider)
                        put("video_url", lesson.videoUrl)
                        put("video_provider", lesson.videoProvider)
                        put("content_body", lesson.description.orEmpty())
                    }
                    db.insertWithOnConflict("local_lessons", null, lessonValues, SQLiteDatabase.CONFLICT_REPLACE)
                    persistedLessonsCount++
                }
            }

            // Only mark offline-ready if all modules and lessons are persisted and downloadStatus is completed
            val isOfflineReady = (persistedLessonsCount == detail.totalLessonsCount && persistedLessonsCount > 0 && downloadStatus == "completed")

            val savedCourseValues = ContentValues().apply {
                put("user_id", userId)
                put("course_id", detail.id)
                put("saved_at", now)
                put("download_status", downloadStatus)
                put("is_offline_ready", if (isOfflineReady) 1 else 0)
                put("sync_status", syncStatus)
            }
            db.insertWithOnConflict("local_saved_courses", null, savedCourseValues, SQLiteDatabase.CONFLICT_REPLACE)

            db.setTransactionSuccessful()
            return isOfflineReady
        } catch (_: Exception) {
            return false
        } finally {
            db.endTransaction()
        }
    }

    open fun unsaveCourse(userId: String, courseId: String): Boolean {
        val rows = writableDatabase.delete(
            "local_saved_courses",
            "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND (course_id = ? OR course_id IN (SELECT course_id FROM local_course_details WHERE slug = ? OR course_id = ? OR LOWER(name) = LOWER(?) OR LOWER(REPLACE(name, ' ', '-')) = LOWER(REPLACE(?, ' ', '-')) OR LOWER(REPLACE(slug, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))))",
            arrayOf(userId, userId, courseId, courseId, courseId, courseId, courseId, courseId)
        )
        return rows > 0
    }

    open fun queueUnsaveCourse(userId: String, courseId: String): Boolean {
        val values = ContentValues().apply {
            put("sync_status", "pending_unsave")
            put("is_offline_ready", 0)
        }
        val rows = writableDatabase.update(
            "local_saved_courses",
            values,
            "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND (course_id = ? OR course_id IN (SELECT course_id FROM local_course_details WHERE slug = ? OR course_id = ? OR LOWER(name) = LOWER(?) OR LOWER(REPLACE(name, ' ', '-')) = LOWER(REPLACE(?, ' ', '-')) OR LOWER(REPLACE(slug, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))))",
            arrayOf(userId, userId, courseId, courseId, courseId, courseId, courseId, courseId)
        )
        return rows > 0
    }

    open fun markSavedCourseSynced(userId: String, courseId: String): Boolean {
        val values = ContentValues().apply { put("sync_status", "synced") }
        return writableDatabase.update("local_saved_courses", values, "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND course_id = ?", arrayOf(userId, userId, courseId)) > 0
    }

    open fun markSavedCourseFailed(userId: String, courseId: String): Boolean {
        val values = ContentValues().apply { put("sync_status", "failed") }
        return writableDatabase.update("local_saved_courses", values, "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND course_id = ?", arrayOf(userId, userId, courseId)) > 0
    }

    open fun deleteSavedCourse(userId: String, courseId: String, onlyIfPendingUnsave: Boolean = false): Boolean {
        val whereClause = if (onlyIfPendingUnsave) {
            "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND sync_status = 'pending_unsave' AND (course_id = ? OR course_id IN (SELECT course_id FROM local_course_details WHERE slug = ? OR course_id = ? OR LOWER(name) = LOWER(?) OR LOWER(REPLACE(name, ' ', '-')) = LOWER(REPLACE(?, ' ', '-')) OR LOWER(REPLACE(slug, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))))"
        } else {
            "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND (course_id = ? OR course_id IN (SELECT course_id FROM local_course_details WHERE slug = ? OR course_id = ? OR LOWER(name) = LOWER(?) OR LOWER(REPLACE(name, ' ', '-')) = LOWER(REPLACE(?, ' ', '-')) OR LOWER(REPLACE(slug, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))))"
        }
        return writableDatabase.delete("local_saved_courses", whereClause, arrayOf(userId, userId, courseId, courseId, courseId, courseId, courseId, courseId)) > 0
    }

    open fun getPendingUnsavedCourses(userId: String): List<LocalSavedCourse> {
        val result = mutableListOf<LocalSavedCourse>()
        val cursor = readableDatabase.rawQuery(
            "SELECT user_id, course_id, saved_at, download_status, is_offline_ready, sync_status FROM local_saved_courses WHERE user_id = ? AND sync_status = 'pending_unsave'",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    LocalSavedCourse(
                        userId = it.getString(0),
                        courseId = it.getString(1),
                        savedAt = it.getLong(2),
                        downloadStatus = it.getString(3),
                        isOfflineReady = it.getInt(4) == 1,
                        syncStatus = it.getString(5)
                    )
                )
            }
        }
        return result
    }

    open fun isCourseSaved(userId: String, courseId: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            """
            SELECT 1 FROM local_saved_courses sc
            LEFT JOIN local_course_details cd ON sc.course_id = cd.course_id
            WHERE (sc.user_id = ? OR ? = 'default_user' OR sc.user_id = 'default_user')
              AND (
                  sc.course_id = ? 
                  OR cd.course_id = ? 
                  OR cd.slug = ? 
                  OR LOWER(cd.name) = LOWER(?)
                  OR LOWER(REPLACE(cd.name, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))
                  OR LOWER(REPLACE(cd.slug, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))
              )
              AND sc.sync_status != 'pending_unsave'
            LIMIT 1
            """.trimIndent(),
            arrayOf(userId, userId, courseId, courseId, courseId, courseId, courseId, courseId)
        )
        return cursor.use { it.moveToFirst() }
    }

    open fun recordFailedDownload(userId: String, courseId: String, syncStatus: String = "pending_sync"): Boolean {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("course_id", courseId)
            put("saved_at", System.currentTimeMillis())
            put("download_status", "failed")
            put("is_offline_ready", 0)
            put("sync_status", syncStatus)
        }
        val rows = writableDatabase.insertWithOnConflict(
            "local_saved_courses",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        return rows != -1L
    }

    open fun isCourseOfflineReady(userId: String, courseId: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            """
            SELECT 1 FROM local_saved_courses sc
            JOIN local_course_details cd ON sc.course_id = cd.course_id
            WHERE (sc.user_id = ? OR ? = 'default_user' OR sc.user_id = 'default_user')
              AND (
                  sc.course_id = ? 
                  OR cd.course_id = ? 
                  OR cd.slug = ? 
                  OR LOWER(cd.name) = LOWER(?)
                  OR LOWER(REPLACE(cd.name, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))
                  OR LOWER(REPLACE(cd.slug, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))
              )
              AND sc.is_offline_ready = 1 
              AND sc.download_status = 'completed'
              AND sc.sync_status != 'pending_unsave'
              AND (
                  cd.total_lessons = 0 
                  OR (SELECT COUNT(*) FROM local_lessons l WHERE l.course_id = sc.course_id OR l.course_id = cd.course_id) >= cd.total_lessons
                  OR (SELECT COUNT(*) FROM local_lessons l WHERE l.course_id = sc.course_id OR l.course_id = cd.course_id) > 0
              )
            LIMIT 1
            """.trimIndent(),
            arrayOf(userId, userId, courseId, courseId, courseId, courseId, courseId, courseId)
        )
        return cursor.use { it.moveToFirst() }
    }

    open fun getSavedCourses(userId: String): List<Course> {
        val result = mutableListOf<Course>()
        val sql = """
            SELECT cd.course_id, cd.name, cd.description, cd.icon, cd.accent, cd.category, cd.level,
                   cd.duration_hours, cd.total_lessons, cd.modules_count, cd.slug
            FROM local_saved_courses sc
            JOIN local_course_details cd ON sc.course_id = cd.course_id
            WHERE (sc.user_id = ? OR ? = 'default_user' OR sc.user_id = 'default_user')
              AND sc.is_offline_ready = 1 
              AND sc.download_status = 'completed'
              AND sc.sync_status != 'pending_unsave'
              AND (
                  cd.total_lessons = 0 
                  OR (SELECT COUNT(*) FROM local_lessons l WHERE l.course_id = sc.course_id OR l.course_id = cd.course_id) >= cd.total_lessons
                  OR (SELECT COUNT(*) FROM local_lessons l WHERE l.course_id = sc.course_id OR l.course_id = cd.course_id) > 0
              )
            ORDER BY sc.saved_at DESC
        """.trimIndent()

        val cursor = readableDatabase.rawQuery(sql, arrayOf(userId, userId))
        cursor.use {
            while (it.moveToNext()) {
                val cId = it.getString(0)
                val name = it.getString(1)
                val desc = it.getString(2)
                val icon = it.getString(3)
                val accent = mapCourseAccent(it.getString(4) ?: "primary")
                val category = it.getString(5) ?: ""
                val level = it.getString(6) ?: "Beginner"
                val duration = if (it.isNull(7)) null else it.getInt(7)
                val totalLessons = it.getInt(8)
                val modulesCount = it.getInt(9)
                val slug = if (it.isNull(10)) null else it.getString(10)

                CourseIdNormalizer.registerRemoteCourse(cId, slug, name)

                val completedCount = getUserCompletedLessonCount(userId, cId)
                val prog = if (totalLessons > 0) (completedCount.toFloat() / totalLessons.toFloat()).coerceIn(0f, 1f) else 0f

                result.add(
                    Course(
                        id = cId,
                        name = name,
                        description = desc,
                        icon = icon,
                        accent = accent,
                        category = category,
                        level = level,
                        durationHours = duration,
                        totalLessons = totalLessons,
                        modulesCount = modulesCount,
                        completedLessons = completedCount,
                        progress = prog
                    )
                )
            }
        }
        return result
    }

    open fun getOfflineCourseDetail(courseId: String, userId: String? = null): CourseDetail? {
        val aliases = CourseIdNormalizer.getAllAliases(courseId)
        if (userId != null) {
            val anyReady = aliases.any { isCourseOfflineReady(userId, it) }
            if (!anyReady) {
                return null
            }
        }
        val cursor = readableDatabase.rawQuery(
            """
            SELECT course_id, name, description, icon, accent, category, level, duration_hours, prerequisites_json 
            FROM local_course_details 
            WHERE course_id = ? 
               OR slug = ? 
               OR LOWER(name) = LOWER(?)
               OR LOWER(REPLACE(name, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))
               OR LOWER(REPLACE(slug, ' ', '-')) = LOWER(REPLACE(?, ' ', '-'))
            LIMIT 1
            """.trimIndent(),
            arrayOf(courseId, courseId, courseId, courseId, courseId)
        )
        val meta = cursor.use {
            if (!it.moveToFirst()) return null
            val resolvedId = it.getString(0)
            val prereqJson = it.getString(8) ?: "[]"
            val prereqs = runCatching { json.decodeFromString<List<String>>(prereqJson) }.getOrDefault(emptyList())
            CourseDetail(
                id = resolvedId,
                name = it.getString(1),
                description = it.getString(2),
                icon = it.getString(3),
                accent = mapCourseAccent(it.getString(4) ?: "primary"),
                category = it.getString(5) ?: "",
                level = it.getString(6) ?: "Beginner",
                durationHours = if (it.isNull(7)) null else it.getInt(7),
                prerequisites = prereqs,
                modules = emptyList()
            )
        } ?: return null

        val resolvedCourseId = meta.id

        val completedLessonIds = if (userId != null) {
            getUserCompletedLessonIds(userId, resolvedCourseId)
        } else {
            emptySet()
        }

        // Fetch modules
        val modulesList = mutableListOf<CourseModule>()
        val modCursor = readableDatabase.rawQuery(
            "SELECT module_id, title, description, display_order FROM local_course_modules WHERE course_id = ? ORDER BY display_order ASC",
            arrayOf(resolvedCourseId)
        )
        val moduleTuples = mutableListOf<Triple<String, String, String>>()
        modCursor.use {
            while (it.moveToNext()) {
                moduleTuples.add(Triple(it.getString(0), it.getString(1), it.getString(2) ?: ""))
            }
        }

        for ((mId, mTitle, mDesc) in moduleTuples) {
            val lessonsList = mutableListOf<LessonItem>()
            val lessonCursor = readableDatabase.rawQuery(
                """
                    SELECT lesson_id, title, description, content_type, duration_minutes, display_order,
                           resource_url, resource_provider, video_url, video_provider
                    FROM local_lessons
                    WHERE module_id = ?
                    ORDER BY display_order ASC
                """.trimIndent(),
                arrayOf(mId)
            )
            lessonCursor.use {
                while (it.moveToNext()) {
                    val lId = it.getString(0)
                    lessonsList.add(
                        LessonItem(
                            id = lId,
                            moduleId = mId,
                            courseId = resolvedCourseId,
                            title = it.getString(1),
                            description = it.getString(2),
                            contentType = it.getString(3) ?: "article",
                            durationMinutes = if (it.isNull(4)) null else it.getInt(4),
                            displayOrder = it.getInt(5),
                            resourceUrl = it.getString(6),
                            resourceProvider = it.getString(7),
                            videoUrl = it.getString(8),
                            videoProvider = it.getString(9),
                            isCompleted = completedLessonIds.contains(lId)
                        )
                    )
                }
            }
            modulesList.add(
                CourseModule(
                    id = mId,
                    courseId = resolvedCourseId,
                    title = mTitle,
                    description = mDesc,
                    displayOrder = modulesList.size + 1,
                    lessons = lessonsList
                )
            )
        }

        return meta.copy(modules = modulesList)
    }

    open fun recordUserLessonProgress(
        userId: String,
        courseId: String,
        lessonId: String,
        isCompleted: Boolean,
        syncStatus: String = "synced"
    ) {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("user_id", userId)
                put("course_id", courseId)
                put("lesson_id", lessonId)
                put("is_completed", if (isCompleted) 1 else 0)
                put("completion_timestamp", now)
                put("sync_status", syncStatus)
            }
            db.insertWithOnConflict("local_user_lesson_progress", null, values, SQLiteDatabase.CONFLICT_REPLACE)

            // Update course progress summary
            val completedCount = getUserCompletedLessonCount(userId, courseId)
            val totalCursor = db.rawQuery(
                "SELECT total_lessons FROM local_course_details WHERE course_id = ? LIMIT 1",
                arrayOf(courseId)
            )
            val totalLessons = totalCursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            val percentage = if (totalLessons > 0) (completedCount.toFloat() / totalLessons.toFloat() * 100f).coerceIn(0f, 100f) else 0f
            val status = if (percentage >= 100f) "completed" else "in_progress"

            val courseProgValues = ContentValues().apply {
                put("user_id", userId)
                put("course_id", courseId)
                put("completion_percentage", percentage)
                put("status", status)
                put("last_accessed", now)
                put("sync_status", syncStatus)
            }
            db.insertWithOnConflict("local_user_course_progress", null, courseProgValues, SQLiteDatabase.CONFLICT_REPLACE)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    open fun getUserCompletedLessonCount(userId: String, courseId: String): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM local_user_lesson_progress WHERE user_id = ? AND course_id = ? AND is_completed = 1",
            arrayOf(userId, courseId)
        )
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    open fun getUserCompletedLessonIds(userId: String, courseId: String): Set<String> {
        val result = mutableSetOf<String>()
        val cursor = readableDatabase.rawQuery(
            "SELECT lesson_id FROM local_user_lesson_progress WHERE user_id = ? AND course_id = ? AND is_completed = 1",
            arrayOf(userId, courseId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(it.getString(0))
            }
        }
        return result
    }

    open fun isUserLessonCompleted(userId: String, courseId: String, lessonId: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT 1 FROM local_user_lesson_progress WHERE user_id = ? AND course_id = ? AND lesson_id = ? AND is_completed = 1 LIMIT 1",
            arrayOf(userId, courseId, lessonId)
        )
        return cursor.use { it.moveToFirst() }
    }

    open fun getPendingSavedCourses(userId: String): List<LocalSavedCourse> {
        val result = mutableListOf<LocalSavedCourse>()
        val cursor = readableDatabase.rawQuery(
            "SELECT user_id, course_id, saved_at, download_status, is_offline_ready, sync_status FROM local_saved_courses WHERE user_id = ? AND sync_status = 'pending_sync'",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    LocalSavedCourse(
                        userId = it.getString(0),
                        courseId = it.getString(1),
                        savedAt = it.getLong(2),
                        downloadStatus = it.getString(3),
                        isOfflineReady = it.getInt(4) == 1,
                        syncStatus = it.getString(5)
                    )
                )
            }
        }
        return result
    }

    open fun saveRoadmap(
        userId: String,
        roadmapId: String,
        roadmapType: String = "system",
        downloadStatus: String = "completed",
        isOfflineReady: Boolean = true,
        syncStatus: String = "synced"
    ): Boolean {
        return try {
            val values = ContentValues().apply {
                put("user_id", userId)
                put("roadmap_id", roadmapId)
                put("roadmap_type", roadmapType)
                put("saved_at", System.currentTimeMillis())
                put("download_status", downloadStatus)
                put("is_offline_ready", if (isOfflineReady) 1 else 0)
                put("sync_status", syncStatus)
            }
            val rowId = writableDatabase.insertWithOnConflict(
                "local_saved_roadmaps",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            rowId != -1L
        } catch (_: Exception) {
            false
        }
    }

    open fun unsaveRoadmap(userId: String, roadmapId: String): Boolean {
        return try {
            val rows = writableDatabase.delete(
                "local_saved_roadmaps",
                "user_id = ? AND roadmap_id = ?",
                arrayOf(userId, roadmapId)
            )
            rows > 0
        } catch (_: Exception) {
            false
        }
    }

    open fun isRoadmapSaved(userId: String, roadmapId: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT 1 FROM local_saved_roadmaps WHERE (user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND roadmap_id = ? AND sync_status != 'pending_unsave' LIMIT 1",
            arrayOf(userId, userId, roadmapId)
        )
        return cursor.use { it.moveToFirst() }
    }

    open fun isRoadmapOfflineReady(userId: String, roadmapId: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT 1 FROM local_saved_roadmaps WHERE (user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND roadmap_id = ? AND is_offline_ready = 1 AND sync_status != 'pending_unsave' LIMIT 1",
            arrayOf(userId, userId, roadmapId)
        )
        return cursor.use { it.moveToFirst() }
    }

    open fun queueUnsaveRoadmap(userId: String, roadmapId: String): Boolean {
        val values = ContentValues().apply {
            put("sync_status", "pending_unsave")
            put("is_offline_ready", 0)
        }
        val rows = writableDatabase.update(
            "local_saved_roadmaps",
            values,
            "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND roadmap_id = ?",
            arrayOf(userId, userId, roadmapId)
        )
        return rows > 0
    }

    open fun markSavedRoadmapSynced(userId: String, roadmapId: String): Boolean {
        val values = ContentValues().apply { put("sync_status", "synced") }
        return writableDatabase.update("local_saved_roadmaps", values, "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND roadmap_id = ?", arrayOf(userId, userId, roadmapId)) > 0
    }

    open fun markSavedRoadmapFailed(userId: String, roadmapId: String): Boolean {
        val values = ContentValues().apply { put("sync_status", "failed") }
        return writableDatabase.update("local_saved_roadmaps", values, "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND roadmap_id = ?", arrayOf(userId, userId, roadmapId)) > 0
    }

    open fun deleteSavedRoadmap(userId: String, roadmapId: String, onlyIfPendingUnsave: Boolean = false): Boolean {
        val whereClause = if (onlyIfPendingUnsave) {
            "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND roadmap_id = ? AND sync_status = 'pending_unsave'"
        } else {
            "(user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND roadmap_id = ?"
        }
        return writableDatabase.delete("local_saved_roadmaps", whereClause, arrayOf(userId, userId, roadmapId)) > 0
    }

    open fun getPendingUnsavedRoadmaps(userId: String): List<LocalSavedRoadmap> {
        val result = mutableListOf<LocalSavedRoadmap>()
        val cursor = readableDatabase.rawQuery(
            "SELECT user_id, roadmap_id, roadmap_type, saved_at, download_status, is_offline_ready, sync_status FROM local_saved_roadmaps WHERE (user_id = ? OR ? = 'default_user' OR user_id = 'default_user') AND sync_status = 'pending_unsave'",
            arrayOf(userId, userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    LocalSavedRoadmap(
                        userId = it.getString(0),
                        roadmapId = it.getString(1),
                        roadmapType = it.getString(2),
                        savedAt = it.getLong(3),
                        downloadStatus = it.getString(4),
                        isOfflineReady = it.getInt(4) == 1,
                        syncStatus = it.getString(5)
                    )
                )
            }
        }
        return result
    }

    open fun getSavedRoadmaps(userId: String): List<RoadmapOption> {
        val result = mutableListOf<RoadmapOption>()
        // 1. Get saved system roadmaps
        val sysSql = """
            SELECT sr_saved.roadmap_id, COALESCE(sr.title, sr_saved.roadmap_id), COALESCE(sr.category, 'Skill-based'),
                   COALESCE(sr.description, ''), COALESCE(sr.skills_json, '[]'), COALESCE(sr.level, 'Beginner to Advanced'),
                   COALESCE(sr.duration, '8 weeks'), COALESCE(sr.stages, 4), COALESCE(sr.icon, 'school'),
                   COALESCE(sr.accent_theme, 'primary'), COALESCE(sr.is_personalized, 0)
            FROM local_saved_roadmaps sr_saved
            LEFT JOIN local_system_roadmaps sr ON sr_saved.roadmap_id = sr.id
            WHERE (sr_saved.user_id = ? OR ? = 'default_user' OR sr_saved.user_id = 'default_user') AND sr_saved.is_offline_ready = 1 AND sr_saved.sync_status != 'pending_unsave'
            ORDER BY sr_saved.saved_at DESC
        """.trimIndent()
        val sysCursor = readableDatabase.rawQuery(sysSql, arrayOf(userId, userId))
        sysCursor.use {
            while (it.moveToNext()) {
                val skillsJson = it.getString(4) ?: "[]"
                val skills = runCatching { json.decodeFromString<List<String>>(skillsJson) }.getOrDefault(emptyList())
                val themeStr = it.getString(9) ?: "PRIMARY"
                result.add(
                    RoadmapOption(
                        id = it.getString(0),
                        title = it.getString(1),
                        category = it.getString(2),
                        description = it.getString(3),
                        skills = skills,
                        level = it.getString(5),
                        duration = it.getString(6),
                        stages = it.getInt(7),
                        icon = it.getString(8),
                        accentTheme = mapAccentTheme(themeStr),
                        isPersonalized = it.getInt(10) == 1
                    )
                )
            }
        }

        // 2. Get saved personalized roadmaps
        val persSql = """
            SELECT pr.id, pr.title, pr.category, pr.goal, pr.level,
                   pr.duration, pr.stages, pr.icon, pr.accent_theme
            FROM local_saved_roadmaps pr_saved
            JOIN local_personalized_roadmaps pr ON pr_saved.roadmap_id = pr.id
            WHERE (pr_saved.user_id = ? OR ? = 'default_user' OR pr_saved.user_id = 'default_user') AND pr_saved.is_offline_ready = 1 AND pr_saved.sync_status != 'pending_unsave'
            ORDER BY pr_saved.saved_at DESC
        """.trimIndent()
        val persCursor = readableDatabase.rawQuery(persSql, arrayOf(userId, userId))
        persCursor.use {
            while (it.moveToNext()) {
                val themeStr = it.getString(8) ?: "PRIMARY"
                result.add(
                    RoadmapOption(
                        id = it.getString(0),
                        title = it.getString(1),
                        category = it.getString(2),
                        description = it.getString(3),
                        skills = emptyList(),
                        level = it.getString(4),
                        duration = it.getString(5),
                        stages = it.getInt(6),
                        icon = it.getString(7),
                        accentTheme = mapAccentTheme(themeStr),
                        isPersonalized = true
                    )
                )
            }
        }

        // 3. Also include user-specific personalized roadmaps
        val persLocalSql = """
            SELECT id, title, category, goal, level, duration, stages, icon, accent_theme
            FROM local_personalized_roadmaps
            WHERE user_id = ? OR ? = 'default_user' OR user_id = 'default_user'
            ORDER BY updated_at DESC
        """.trimIndent()
        val persLocalCursor = readableDatabase.rawQuery(persLocalSql, arrayOf(userId, userId))
        persLocalCursor.use {
            while (it.moveToNext()) {
                val themeStr = it.getString(8) ?: "PRIMARY"
                result.add(
                    RoadmapOption(
                        id = it.getString(0),
                        title = it.getString(1),
                        category = it.getString(2),
                        description = it.getString(3),
                        skills = emptyList(),
                        level = it.getString(4),
                        duration = it.getString(5),
                        stages = it.getInt(6),
                        icon = it.getString(7),
                        accentTheme = mapAccentTheme(themeStr),
                        isPersonalized = true
                    )
                )
            }
        }

        return result.distinctBy { it.id }
    }

    open fun getPendingSavedRoadmaps(userId: String): List<LocalSavedRoadmap> {
        val result = mutableListOf<LocalSavedRoadmap>()
        val cursor = readableDatabase.rawQuery(
            "SELECT user_id, roadmap_id, roadmap_type, saved_at, download_status, is_offline_ready, sync_status FROM local_saved_roadmaps WHERE user_id = ? AND sync_status = 'pending_sync'",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    LocalSavedRoadmap(
                        userId = it.getString(0),
                        roadmapId = it.getString(1),
                        roadmapType = it.getString(2),
                        savedAt = it.getLong(3),
                        downloadStatus = it.getString(4),
                        isOfflineReady = it.getInt(5) == 1,
                        syncStatus = it.getString(6)
                    )
                )
            }
        }
        return result
    }

    open fun getPendingUserLessonProgress(userId: String): List<LocalLessonProgressRecord> {
        val result = mutableListOf<LocalLessonProgressRecord>()
        val cursor = readableDatabase.rawQuery(
            "SELECT user_id, course_id, lesson_id, is_completed, completion_timestamp, sync_status FROM local_user_lesson_progress WHERE user_id = ? AND sync_status = 'pending_sync'",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    LocalLessonProgressRecord(
                        userId = it.getString(0),
                        courseId = it.getString(1),
                        lessonId = it.getString(2),
                        isCompleted = it.getInt(3) == 1,
                        completionTimestamp = it.getLong(4),
                        syncStatus = it.getString(5)
                    )
                )
            }
        }
        return result
    }

    open fun markUserLessonProgressSynced(userId: String, courseId: String, lessonId: String): Boolean {
        val values = ContentValues().apply { put("sync_status", "synced") }
        return writableDatabase.update(
            "local_user_lesson_progress",
            values,
            "user_id = ? AND course_id = ? AND lesson_id = ?",
            arrayOf(userId, courseId, lessonId)
        ) > 0
    }

    open fun markUserLessonProgressFailed(userId: String, courseId: String, lessonId: String): Boolean {
        val values = ContentValues().apply { put("sync_status", "failed") }
        return writableDatabase.update(
            "local_user_lesson_progress",
            values,
            "user_id = ? AND course_id = ? AND lesson_id = ?",
            arrayOf(userId, courseId, lessonId)
        ) > 0
    }

    open fun getPendingOperationsCount(userId: String): Int {
        var count = 0
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM local_saved_courses WHERE user_id = ? AND sync_status IN ('pending_sync', 'pending_unsave')",
            arrayOf(userId)
        ).use { if (it.moveToFirst()) count += it.getInt(0) }

        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM local_saved_roadmaps WHERE user_id = ? AND sync_status IN ('pending_sync', 'pending_unsave')",
            arrayOf(userId)
        ).use { if (it.moveToFirst()) count += it.getInt(0) }

        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM local_user_lesson_progress WHERE user_id = ? AND sync_status = 'pending_sync'",
            arrayOf(userId)
        ).use { if (it.moveToFirst()) count += it.getInt(0) }

        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM local_milestone_progress WHERE user_id = ? AND sync_status = 'pending_sync'",
            arrayOf(userId)
        ).use { if (it.moveToFirst()) count += it.getInt(0) }

        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM local_personalized_roadmaps WHERE (user_id = ? OR user_id = 'default_user') AND sync_status = 'pending_sync'",
            arrayOf(userId)
        ).use { if (it.moveToFirst()) count += it.getInt(0) }

        return count
    }

    private fun mapCourseAccent(accentStr: String): CourseAccent {
        return when (accentStr.lowercase()) {
            "secondary" -> CourseAccent.SECONDARY
            "accent" -> CourseAccent.ACCENT
            else -> CourseAccent.PRIMARY
        }
    }
}

