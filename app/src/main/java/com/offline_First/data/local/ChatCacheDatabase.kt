package com.offline_First.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.UserMemoryItem

private const val DB_NAME = "edunova_chat_cache.db"
private const val DB_VERSION = 2

class ChatCacheDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

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

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_conv_user ON local_conversations(user_id, is_archived, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_conv_id ON local_messages(user_id, conversation_id, created_at ASC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_memories_user ON local_memories(user_id, active)")
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

    fun updateConversationDraft(conversationId: String, userId: String, draftText: String) {
        val values = ContentValues().apply { put("draft_text", draftText) }
        writableDatabase.update("local_conversations", values, "id = ? AND user_id = ?", arrayOf(conversationId, userId))
    }

    fun updateConversationTitle(conversationId: String, userId: String, newTitle: String) {
        val values = ContentValues().apply {
            put("title", newTitle)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update("local_conversations", values, "id = ? AND user_id = ?", arrayOf(conversationId, userId))
    }

    fun toggleArchive(conversationId: String, userId: String, isArchived: Boolean) {
        val values = ContentValues().apply { put("is_archived", if (isArchived) 1 else 0) }
        writableDatabase.update("local_conversations", values, "id = ? AND user_id = ?", arrayOf(conversationId, userId))
    }

    fun togglePin(conversationId: String, userId: String, isPinned: Boolean) {
        val values = ContentValues().apply { put("is_pinned", if (isPinned) 1 else 0) }
        writableDatabase.update("local_conversations", values, "id = ? AND user_id = ?", arrayOf(conversationId, userId))
    }

    fun deleteConversation(conversationId: String, userId: String) {
        writableDatabase.delete("local_conversations", "id = ? AND user_id = ?", arrayOf(conversationId, userId))
        writableDatabase.delete("local_messages", "conversation_id = ? AND user_id = ?", arrayOf(conversationId, userId))
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
}
