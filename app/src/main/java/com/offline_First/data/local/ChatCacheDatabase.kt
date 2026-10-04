package com.offline_First.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession

private const val DB_NAME = "edunova_chat_cache.db"
private const val DB_VERSION = 1

class ChatCacheDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_conversations (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_messages (
                id TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                sync_status TEXT NOT NULL DEFAULT 'synced',
                FOREIGN KEY (conversation_id) REFERENCES local_conversations(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_messages_conv_id ON local_messages(conversation_id)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS local_messages")
        db.execSQL("DROP TABLE IF EXISTS local_conversations")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // --- Conversation operations ---

    fun upsertConversation(id: String, title: String, createdAt: Long, updatedAt: Long) {
        val values = ContentValues().apply {
            put("id", id)
            put("title", title)
            put("created_at", createdAt)
            put("updated_at", updatedAt)
        }
        writableDatabase.insertWithOnConflict(
            "local_conversations", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getAllConversations(): List<ChatSession> {
        val result = mutableListOf<ChatSession>()
        val cursor: Cursor = readableDatabase.rawQuery(
            "SELECT id, title, updated_at FROM local_conversations ORDER BY updated_at DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    ChatSession(
                        id = it.getString(0),
                        title = it.getString(1),
                        lastUpdated = it.getLong(2)
                    )
                )
            }
        }
        return result
    }

    fun getConversationWithMessages(conversationId: String): ChatSession? {
        val cursor = readableDatabase.rawQuery(
            "SELECT id, title, updated_at FROM local_conversations WHERE id = ?",
            arrayOf(conversationId)
        )
        val session = cursor.use {
            if (!it.moveToFirst()) return null
            ChatSession(
                id = it.getString(0),
                title = it.getString(1),
                lastUpdated = it.getLong(2)
            )
        }
        val messages = getMessagesForConversation(conversationId)
        return session.copy(messages = messages)
    }

    // --- Message operations ---

    fun upsertMessage(
        id: String,
        conversationId: String,
        role: String,
        content: String,
        createdAt: Long,
        syncStatus: String = "synced"
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("conversation_id", conversationId)
            put("role", role)
            put("content", content)
            put("created_at", createdAt)
            put("sync_status", syncStatus)
        }
        writableDatabase.insertWithOnConflict(
            "local_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun upsertMessages(
        messages: List<Pair<ChatMessage, String>>,
        conversationId: String
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((msg, role) in messages) {
                val values = ContentValues().apply {
                    put("id", msg.id)
                    put("conversation_id", conversationId)
                    put("role", role)
                    put("content", msg.text)
                    put("created_at", msg.timestamp)
                    put("sync_status", "synced")
                }
                db.insertWithOnConflict("local_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getMessagesForConversation(conversationId: String): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, role, content, created_at FROM local_messages WHERE conversation_id = ? ORDER BY created_at ASC",
            arrayOf(conversationId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    ChatMessage(
                        id = it.getString(0),
                        text = it.getString(2),
                        fromUser = it.getString(1) == "user",
                        timestamp = it.getLong(3)
                    )
                )
            }
        }
        return result
    }

    fun deleteConversation(conversationId: String) {
        writableDatabase.delete("local_conversations", "id = ?", arrayOf(conversationId))
    }

    fun updateConversationTitle(conversationId: String, newTitle: String) {
        val values = ContentValues().apply { put("title", newTitle) }
        writableDatabase.update("local_conversations", values, "id = ?", arrayOf(conversationId))
    }
}
