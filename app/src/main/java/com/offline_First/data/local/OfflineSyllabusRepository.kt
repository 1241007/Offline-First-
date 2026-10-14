package com.offline_First.data.local

import android.content.Context
import com.offline_First.domain.model.AppMode
import com.offline_First.domain.model.EvidenceOutcome
import com.offline_First.domain.model.RAGRequestContext
import com.offline_First.domain.model.RetrievedChunk

/**
 * Repository for offline curriculum retrieval using SQLite FTS5.
 */
class OfflineSyllabusRepository(
    private val context: Context,
    val database: SyllabusDatabase = SyllabusDatabase(context)
) {
    fun hasPack(board: String?, classGrade: String?, subject: String?): Boolean {
        return database.hasPack(board, classGrade, subject)
    }

    fun retrieve(
        query: String,
        context: RAGRequestContext,
        topK: Int = 5
    ): Pair<List<RetrievedChunk>, EvidenceOutcome> {
        // General mode RAG is disabled without configured general corpus
        if (context.appMode == AppMode.GENERAL) {
            return emptyList<RetrievedChunk>() to EvidenceOutcome.CORPUS_NOT_CONFIGURED
        }

        // Student mode requires board and classGrade to prevent cross-curriculum broadening
        if (context.board.isNullOrBlank() || context.classGrade.isNullOrBlank()) {
            return emptyList<RetrievedChunk>() to EvidenceOutcome.MISSING
        }

        val chunks = database.searchChunks(
            query = query,
            board = context.board,
            classGrade = context.classGrade,
            subject = context.subject,
            limit = topK
        )

        if (chunks.isEmpty()) {
            return emptyList<RetrievedChunk>() to EvidenceOutcome.MISSING
        }

        val topChunk = chunks[0]
        val score = topChunk.normalizedScore ?: 0.0f
        val contentLen = topChunk.content.trim().length

        val outcome = when {
            score >= 0.70f && contentLen >= 80 -> EvidenceOutcome.SUFFICIENT
            score >= 0.35f -> EvidenceOutcome.UNCERTAIN
            else -> EvidenceOutcome.MISSING
        }

        return chunks to outcome
    }
}
