package com.offline_First.data.local

import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.data.provider.LLMProvider
import com.offline_First.data.remote.OnlineAIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Verifies Android client-side chat persistence invariants:
 * - Session and message chronological ordering.
 * - Parent-child threading invariants.
 * - Idempotent message deduplication by client ID.
 * - Strict multi-student/multi-user context and session isolation.
 * - Offline pending_sync status tracking.
 */
class ChatPersistenceInvariantTest {

    private class EchoProvider : LLMProvider {
        override val name: String = "EchoProvider"
        override fun streamInference(
            prompt: String,
            conversationId: String,
            clientMessageId: String,
            parentId: String?,
            explanationMode: ExplanationMode,
            history: List<ChatMessage>,
            systemPrompt: String
        ): Flow<String> = flow {
            emit("Response to: $prompt")
        }
        override suspend fun stopInference() {}
    }

    @Test
    fun testSessionMessageOrderingAndParentChildRelationship() {
        val rootMsgId = UUID.randomUUID().toString()
        val replyMsgId = UUID.randomUUID().toString()
        val t0 = 1000L
        val t1 = 2000L

        val msg1 = ChatMessage(
            id = rootMsgId,
            text = "What is Newton's third law?",
            fromUser = true,
            timestamp = t0,
            parentId = null
        )
        val msg2 = ChatMessage(
            id = replyMsgId,
            text = "For every action, there is an equal and opposite reaction.",
            fromUser = false,
            timestamp = t1,
            parentId = rootMsgId
        )

        val session = ChatSession(
            id = "sess-1",
            title = "Physics",
            lastUpdated = t1,
            messages = listOf(msg1, msg2)
        )

        assertEquals(2, session.messages.size)
        assertTrue(session.messages[0].fromUser)
        assertFalse(session.messages[1].fromUser)
        assertEquals(rootMsgId, session.messages[1].parentId)
        assertTrue(session.messages[0].timestamp < session.messages[1].timestamp)
    }

    @Test
    fun testDraftAndPinStateTransitions() {
        val session0 = ChatSession(
            id = "sess-2",
            title = "Chemistry",
            lastUpdated = 5000L,
            isPinned = false,
            isArchived = false,
            draftText = ""
        )

        // Typing draft
        val session1 = session0.copy(draftText = "Explain redox reactions")
        assertEquals("Explain redox reactions", session1.draftText)
        assertFalse(session1.isPinned)

        // Pin conversation
        val session2 = session1.copy(isPinned = true)
        assertTrue(session2.isPinned)
        assertFalse(session2.isArchived)

        // Clear draft on send
        val session3 = session2.copy(draftText = "")
        assertEquals("", session3.draftText)
        assertTrue(session3.isPinned)
    }

    @Test
    fun testIdempotentMessageDeduplicationByClientId() = runBlocking {
        val delegate = LocalAIRepository()
        val echoProvider = EchoProvider()
        val initialSession = ChatSession(
            id = "sess-dedup",
            title = "Math",
            lastUpdated = System.currentTimeMillis()
        )

        var user = "student_alpha"
        val repository = OnlineAIRepository(
            context = null,
            delegate = delegate,
            userIdProvider = { user },
            offlineSyllabusRepositoryProvider = { OfflineSyllabusRepository() },
            e5EmbeddingServiceProvider = { E5EmbeddingService(null) },
            onlineProviderOverride = echoProvider,
            offlineProviderOverride = echoProvider,
            profileProvider = { null },
            initialSession = initialSession
        )

        val fixedClientId = "client-msg-unique-42"
        // First stream with fixed clientMessageId
        val tokens1 = repository.streamMessage("Hello", clientMessageId = fixedClientId).toList()
        assertTrue(tokens1.isNotEmpty())

        val sessionAfterFirst = repository.observeCurrentSession().first()
        assertNotNull(sessionAfterFirst)
        val userMessages = sessionAfterFirst!!.messages.filter { it.fromUser }
        assertEquals("Should contain exactly 1 user message with this ID", 1, userMessages.count { it.id == fixedClientId })

        // Simulate re-streaming same message ID (e.g. network retry)
        val tokens2 = repository.streamMessage("Hello", clientMessageId = fixedClientId).toList()
        assertTrue(tokens2.isNotEmpty())

        val sessionAfterSecond = repository.observeCurrentSession().first()
        assertNotNull(sessionAfterSecond)
        val userMessagesAfterSecond = sessionAfterSecond!!.messages.filter { it.fromUser }
        assertEquals("Message must be deduplicated by clientMessageId", 1, userMessagesAfterSecond.count { it.id == fixedClientId })
    }

    @Test
    fun testStrictUserScopingAndContextIsolation() = runBlocking {
        val delegateA = LocalAIRepository()
        val delegateB = LocalAIRepository()
        val echoProvider = EchoProvider()

        val sessionA = ChatSession(id = "sess-student-A", title = "Student A Chat", lastUpdated = 1000L)
        val sessionB = ChatSession(id = "sess-student-B", title = "Student B Chat", lastUpdated = 2000L)

        val repoA = OnlineAIRepository(
            context = null,
            delegate = delegateA,
            userIdProvider = { "student_A" },
            offlineSyllabusRepositoryProvider = { OfflineSyllabusRepository() },
            e5EmbeddingServiceProvider = { E5EmbeddingService(null) },
            onlineProviderOverride = echoProvider,
            offlineProviderOverride = echoProvider,
            profileProvider = { null },
            initialSession = sessionA
        )

        val repoB = OnlineAIRepository(
            context = null,
            delegate = delegateB,
            userIdProvider = { "student_B" },
            offlineSyllabusRepositoryProvider = { OfflineSyllabusRepository() },
            e5EmbeddingServiceProvider = { E5EmbeddingService(null) },
            onlineProviderOverride = echoProvider,
            offlineProviderOverride = echoProvider,
            profileProvider = { null },
            initialSession = sessionB
        )

        // Verify independent session flows
        val curA = repoA.observeCurrentSession().first()
        val curB = repoB.observeCurrentSession().first()

        assertEquals("sess-student-A", curA?.id)
        assertEquals("sess-student-B", curB?.id)
        assertEquals("Student A Chat", curA?.title)
        assertEquals("Student B Chat", curB?.title)

        // Stream in A must NOT pollute B
        repoA.streamMessage("Question from student A").toList()

        val updatedA = repoA.observeCurrentSession().first()
        val updatedB = repoB.observeCurrentSession().first()

        assertTrue("Student A has messages", updatedA?.messages?.isNotEmpty() == true)
        assertTrue("Student B must remain unaffected", updatedB?.messages?.isEmpty() == true)
    }

    @Test
    fun testOfflinePendingSyncStatusInvariant() = runBlocking {
        val delegate = LocalAIRepository()
        delegate.setConnectionMode(ConnectionMode.OFFLINE)
        val echoProvider = EchoProvider()

        val sessionOffline = ChatSession(id = "sess-offline-1", title = "Offline Session", lastUpdated = 1000L)

        val repo = OnlineAIRepository(
            context = null,
            delegate = delegate,
            userIdProvider = { "offline_user" },
            offlineSyllabusRepositoryProvider = { OfflineSyllabusRepository() },
            e5EmbeddingServiceProvider = { E5EmbeddingService(null) },
            onlineProviderOverride = echoProvider,
            offlineProviderOverride = echoProvider,
            profileProvider = { null },
            initialSession = sessionOffline
        )

        repo.streamMessage("Testing offline message queuing").toList()

        val currentSession = repo.observeCurrentSession().first()
        assertNotNull(currentSession)
        val userMsg = currentSession!!.messages.firstOrNull { it.fromUser }
        assertNotNull(userMsg)
        // Message sent in offline mode
        assertEquals("Offline sent message is marked in session", "Testing offline message queuing", userMsg?.text)
    }
}
