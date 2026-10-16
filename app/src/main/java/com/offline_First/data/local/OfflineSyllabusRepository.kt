package com.offline_First.data.local

import android.content.Context
import com.offline_First.domain.model.AppMode
import com.offline_First.domain.model.EvidenceOutcome
import com.offline_First.domain.model.RAGRequestContext
import com.offline_First.domain.model.RetrievedChunk

/**
 * Repository for offline curriculum retrieval using SQLite LIKE keyword search
 * and local 384-dimensional vector similarity search.
 *
 * Implements the Phase 4 evidence policy offline to guarantee consistency with
 * backend supabase_retrieval_service.py.
 */
open class OfflineSyllabusRepository(
    private val context: Context? = null,
    val database: SyllabusDatabase? = context?.let { SyllabusDatabase(it) }
) {
    companion object {
        const val MIN_CONTENT_LENGTH_BYTES = 80
        const val SUFFICIENT_NORMALIZED_THRESHOLD = 0.70f
        const val DENSE_ONLY_SUFFICIENT_NORMALIZED_THRESHOLD = 0.50f
        const val DENSE_ONLY_MIN_SIMILARITY = 0.75f
        const val UNCERTAIN_NORMALIZED_THRESHOLD = 0.35f

        private val CORROBORATION_STOPWORDS = setOf(
            "what", "when", "where", "which", "who", "whom", "whose", "why", "how",
            "explain", "describe", "define", "state", "give", "name", "write",
            "does", "class", "ncert", "cbse", "curriculum", "syllabus", "textbook",
            "please", "tell", "about", "briefly", "short", "note", "between",
            "using", "points", "point", "answer", "question", "diagram",
            "determine", "identify", "discuss"
        )

        /**
         * Verifies lexical and domain corroboration between query and retrieved content.
         * Prevents dense-vector nearest-neighbor distractors (such as out-of-syllabus
         * advanced science topics) from falsely becoming SUFFICIENT.
         */
        fun checkLexicalCorroboration(query: String?, content: String): Boolean {
            if (query.isNullOrBlank() || content.isBlank()) return false
            val wordRegex = Regex("\\b[a-zA-Z]{3,}\\b")
            val words = wordRegex.findAll(query.lowercase()).map { it.value }.toList()
            val contentLower = content.lowercase()
            val domainWords = words.filter { it !in CORROBORATION_STOPWORDS }
            if (domainWords.isEmpty()) return true

            val matches = domainWords.filter { w ->
                w in contentLower || (w.length >= 6 && w.substring(0, 5) in contentLower)
            }

            return if (domainWords.size <= 2) {
                matches.isNotEmpty()
            } else {
                matches.size >= 2 || (matches.size.toFloat() / domainWords.size.toFloat()) >= 0.30f
            }
        }

        /**
         * Assesses whether retrieved chunks provide SUFFICIENT, UNCERTAIN, or MISSING evidence
         * adhering strictly to the Phase 4 evidence policy.
         */
        fun assessEvidenceOutcome(chunks: List<RetrievedChunk>, query: String? = null): EvidenceOutcome {
            if (chunks.isEmpty()) return EvidenceOutcome.MISSING

            val topChunk = chunks[0]
            val score = topChunk.normalizedScore ?: 0.0f
            val contentLen = topChunk.content.trim().length

            // 1. Content length guard: minimum 80 chars
            if (contentLen < MIN_CONTENT_LENGTH_BYTES) {
                return if (score >= UNCERTAIN_NORMALIZED_THRESHOLD) {
                    EvidenceOutcome.UNCERTAIN
                } else {
                    EvidenceOutcome.MISSING
                }
            }

            val hasDense = topChunk.vectorRank != null && topChunk.vectorRank > 0
            val hasKeyword = topChunk.sparseRank != null && topChunk.sparseRank > 0
            val simScore = topChunk.similarityScore

            // 2. Dual retriever match (score >= 0.70)
            if (score >= SUFFICIENT_NORMALIZED_THRESHOLD) {
                return EvidenceOutcome.SUFFICIENT
            }

            // 3. Dense-only match: requires dual criteria (score >= 0.50 AND similarity >= 0.75)
            // plus lexical corroboration when query is provided
            if ((hasDense || topChunk.vectorRank == null) && !hasKeyword) {
                if (score >= DENSE_ONLY_SUFFICIENT_NORMALIZED_THRESHOLD &&
                    simScore != null && simScore >= DENSE_ONLY_MIN_SIMILARITY
                ) {
                    if (query != null) {
                        return if (checkLexicalCorroboration(query, topChunk.content)) {
                            EvidenceOutcome.SUFFICIENT
                        } else {
                            EvidenceOutcome.MISSING
                        }
                    }
                    return EvidenceOutcome.SUFFICIENT
                }
            }

            // 4. Keyword-only match: max score 0.50 in RRF; cannot reach SUFFICIENT (0.70);
            // evaluated against UNCERTAIN (>= 0.35)
            if (score >= UNCERTAIN_NORMALIZED_THRESHOLD) {
                return EvidenceOutcome.UNCERTAIN
            }

            return EvidenceOutcome.MISSING
        }
    }

    open fun hasPack(board: String?, classGrade: String?, subject: String?): Boolean {
        return database?.hasPack(board, classGrade, subject) ?: false
    }

    open fun retrieve(
        query: String,
        context: RAGRequestContext,
        topK: Int = 5,
        queryEmbedding: FloatArray? = null
    ): Pair<List<RetrievedChunk>, EvidenceOutcome> {
        // General mode RAG is disabled without configured general corpus
        if (context.appMode == AppMode.GENERAL) {
            return emptyList<RetrievedChunk>() to EvidenceOutcome.CORPUS_NOT_CONFIGURED
        }

        // Student mode requires board and classGrade to prevent cross-curriculum broadening
        if (context.board.isNullOrBlank() || context.classGrade.isNullOrBlank()) {
            return emptyList<RetrievedChunk>() to EvidenceOutcome.MISSING
        }

        val db = database ?: return emptyList<RetrievedChunk>() to EvidenceOutcome.MISSING

        val chunks = if (queryEmbedding != null && queryEmbedding.isNotEmpty()) {
            db.hybridSearch(
                query = query,
                queryEmbedding = queryEmbedding,
                board = context.board,
                classGrade = context.classGrade,
                subject = context.subject,
                limit = topK
            )
        } else {
            db.searchChunks(
                query = query,
                board = context.board,
                classGrade = context.classGrade,
                subject = context.subject,
                limit = topK
            )
        }

        if (chunks.isEmpty()) {
            return emptyList<RetrievedChunk>() to EvidenceOutcome.MISSING
        }

        val outcome = assessEvidenceOutcome(chunks, query)
        return chunks to outcome
    }
}
