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
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private const val DB_NAME = "edunova_chat_cache.db"
private const val DB_VERSION = 3

data class PendingMilestoneSync(
    val roadmapId: String,
    val userId: String,
    val milestoneKey: String,
    val isCompleted: Boolean,
    val updatedAt: Long
)

class ChatCacheDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

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

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_conv_user ON local_conversations(user_id, is_archived, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_conv_id ON local_messages(user_id, conversation_id, created_at ASC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_memories_user ON local_memories(user_id, active)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sys_roadmaps_user_cat ON local_system_roadmaps(user_id, category, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pers_roadmaps_user ON local_personalized_roadmaps(user_id, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_milestone_sync ON local_milestone_progress(user_id, sync_status)")
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
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Do not drop tables
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
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
        writableDatabase.insertWithOnConflict(
            "local_conversations", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
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

    // --- Memory operations (Strictly scoped by user_id) ---

    fun upsertMemory(memory: UserMemoryItem, userId: String, sourceConvId: String? = null) {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("id", memory.id)
            put("user_id", userId)
            put("category", memory.category)
            put("content", memory.content)
            put("importance", memory.importance)
            put("confidence", memory.confidence)
            put("source_conversation_id", sourceConvId)
            put("created_at", now)
            put("updated_at", now)
            put("active", if (memory.active) 1 else 0)
            put("sync_status", "synced")
        }
        writableDatabase.insertWithOnConflict("local_memories", null, values, SQLiteDatabase.CONFLICT_REPLACE)
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

    fun upsertSystemRoadmaps(roadmaps: List<RoadmapOption>, userId: String) {
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

    fun getSystemRoadmaps(userId: String, category: String? = null): List<RoadmapOption> {
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

    fun getPersonalizedRoadmapById(roadmapId: String, userId: String): PersonalizedRoadmapDetail? {
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
            )
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
}

