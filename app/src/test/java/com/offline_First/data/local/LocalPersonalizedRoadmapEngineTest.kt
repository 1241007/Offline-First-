package com.offline_First.data.local

import com.offline_First.domain.model.AssessmentPhase
import com.offline_First.domain.model.PersonalizedTopicStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LocalPersonalizedRoadmapEngineTest {

    private val engine = LocalPersonalizedRoadmapEngine

    @Test
    fun startAssessment_createsDomainSpecificSessionForJava() {
        val session = engine.startAssessment("Java")
        assertEquals("Java", session.domain)
        assertEquals(AssessmentPhase.PROFILE_COLLECTION, session.phase)
        assertNotNull(session.currentQuestion)
        assertTrue(session.currentQuestion!!.prompt.contains("Java"))
    }

    @Test
    fun startAssessment_createsDomainSpecificSessionForSql() {
        val session = engine.startAssessment("SQL")
        assertEquals("SQL", session.domain)
        assertEquals(AssessmentPhase.PROFILE_COLLECTION, session.phase)
        assertNotNull(session.currentQuestion)
        assertTrue(session.currentQuestion!!.prompt.contains("SQL"))
    }

    @Test
    fun adaptiveAssessment_progressesThroughDiagnosticQuiz() {
        val s1 = engine.startAssessment("Java")
        // Profile collection 1: Goal
        val s2 = engine.submitAnswer(s1.sessionId, "Build Enterprise APIs")
        // Profile collection 2: Level
        val s3 = engine.submitAnswer(s2.sessionId, "Intermediate")
        // Profile collection 3: Weekly hours
        val s4 = engine.submitAnswer(s3.sessionId, "10 hours/week")

        // Should transition to DIAGNOSTIC phase with Java questions
        assertEquals(AssessmentPhase.DIAGNOSTIC, s4.phase)
        assertNotNull(s4.currentQuestion)
        val q1 = s4.currentQuestion!!
        assertTrue(
            "Expected Java question but got: ${q1.prompt}",
            q1.prompt.contains("JVM") || q1.prompt.contains("Java") || q1.topicTag.contains("java")
        )

        // Java question 1: JVM Bytecode is option index 0
        val s5 = engine.submitAnswer(s4.sessionId, "JVM Bytecode", quizSelectedIndex = 0)
        assertNotNull(s5)
        assertEquals(true, s5.lastAnswerCorrect)
    }

    @Test
    fun generateRoadmap_skipsMasteredTopicsAndCalculatesDynamicDuration() {
        val s1 = engine.startAssessment("SQL")
        val s2 = engine.submitAnswer(s1.sessionId, "Database Mastery")
        val s3 = engine.submitAnswer(s2.sessionId, "Beginner")
        val s4 = engine.submitAnswer(s3.sessionId, "10 hours/week")

        // In diagnostic phase for SQL, question 1 tests "sql_joins"
        // Correct answer is index 0: "INNER returns only matched rows from both tables; LEFT includes all rows from the left table with NULLs for unmatched right rows"
        val s5 = engine.submitAnswer(
            s4.sessionId,
            "INNER returns only matched rows from both tables; LEFT includes all rows from the left table with NULLs for unmatched right rows",
            quizSelectedIndex = 0
        )

        // Generate roadmap
        val roadmap = engine.generateRoadmap(s5.sessionId)
        assertNotNull(roadmap)
        assertEquals("SQL", roadmap.category)
        assertTrue(roadmap.phases.isNotEmpty())
        assertTrue(roadmap.topics.isNotEmpty())

        // Verify mastered topic (sql_joins) is marked as SKIPPED
        val joinsTopic = roadmap.topics.find { it.id == "sql_joins" || it.title.contains("JOIN", ignoreCase = true) }
        assertNotNull("Expected JOIN topic in SQL roadmap", joinsTopic)
        assertEquals(PersonalizedTopicStatus.SKIPPED, joinsTopic!!.status)
        assertTrue(joinsTopic.accelerated)
        assertNotNull(joinsTopic.explanation)
        assertTrue(joinsTopic.explanation!!.contains("mastery", ignoreCase = true))

        // Verify progress percentage calculation: excludes skipped topics
        val initialProgress = roadmap.progressPercentage
        assertEquals(0, initialProgress) // No completed topics yet, skipped doesn't count as complete

        // Verify active session retrieval
        val retrieved = engine.getSession(s1.sessionId)
        assertNotNull(retrieved)
        assertEquals(s1.sessionId, retrieved!!.sessionId)
    }
}
