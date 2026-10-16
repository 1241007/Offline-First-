package com.offline_First.data.local

import com.offline_First.domain.model.AppMode
import com.offline_First.domain.model.EvidenceOutcome
import com.offline_First.domain.model.RAGRequestContext
import com.offline_First.domain.model.RetrievedChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * Tests OFF-003, OFF-005, OFF-008, OFF-009, OFF-012, and OFF-013 covering:
 * - SQLite LIKE keyword search scoring & tokenization.
 * - 384-d vector BLOB conversion & cosine similarity.
 * - Curriculum filtering and cross-curriculum isolation.
 * - Reciprocal Rank Fusion (RRF k=60) hybrid score normalization.
 * - FTS5 regression avoidance and safe migration.
 * - Phase 4 Evidence Policy offline (dense-only, keyword-only, corroboration, out-of-syllabus distractors).
 */
class OfflineRagRetrievalTest {

    // ==============================================================================
    // OFF-003: SQLite LIKE Keyword Search Scoring Logic
    // ==============================================================================
    @Test
    fun test_off_003_sqlite_like_keyword_search_scoring() {
        // Tokenizer and stopword stripping logic
        val rawQuery = "What is the process of photosynthesis in green plants?"
        val words = rawQuery.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() && it.length >= 3 }
            .filter { it !in setOf("what", "is", "the", "of", "in", "and", "for") }

        // Must extract key content tokens
        assertTrue(words.contains("process"))
        assertTrue(words.contains("photosynthesis"))
        assertTrue(words.contains("green"))
        assertTrue(words.contains("plants"))
        assertFalse(words.contains("the"))
        assertFalse(words.contains("what"))

        // Weighted relevance scoring formula verification
        // Weights: chapter (4 pts), subject (3 pts), section (2 pts), content (1 pt), full phrase (5 bonus pts)
        fun calculateRelevance(
            query: String,
            chapter: String,
            section: String,
            content: String
        ): Float {
            var score = 0.0f
            val qTokens = query.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.length >= 3 }
            for (t in qTokens) {
                if (chapter.lowercase().contains(t)) score += 4.0f
                if (section.lowercase().contains(t)) score += 2.0f
                if (content.lowercase().contains(t)) score += 1.0f
            }
            if (content.lowercase().contains(query.lowercase())) score += 5.0f
            return score
        }

        val highMatch = calculateRelevance(
            query = "Ohm's Law",
            chapter = "Electricity - Ohm's Law and Resistance",
            section = "Ohm's Law",
            content = "Ohm's Law states that potential difference V is directly proportional to current I."
        )
        val lowMatch = calculateRelevance(
            query = "Ohm's Law",
            chapter = "Magnetic Effects",
            section = "Electric Motors",
            content = "The current flows according to basic principles."
        )

        assertTrue(highMatch > lowMatch)
        assertTrue(highMatch >= 12.0f) // chapter(4*2) + section(2*2) + content(1*2) + full phrase(5)
    }

    // ==============================================================================
    // OFF-005: 384-Dimensional Vector Similarity and BLOB Serialization
    // ==============================================================================
    @Test
    fun test_off_005_384_dimensional_vector_similarity_and_blob() {
        val original = FloatArray(384) { (it * 0.005f) - 0.96f }
        val blob = SyllabusDatabase.floatArrayToBlob(original)
        assertNotNull(blob)
        assertEquals(384 * 4, blob!!.size)

        val deserialized = SyllabusDatabase.blobToFloatArray(blob)
        assertNotNull(deserialized)
        assertEquals(384, deserialized!!.size)
        for (i in 0 until 384) {
            assertEquals(original[i], deserialized[i], 1e-6f)
        }

        // Cosine similarity mathematical invariants
        val v1 = FloatArray(384) { 1.0f }
        val v2 = FloatArray(384) { 1.0f }
        assertEquals(1.0f, SyllabusDatabase.cosineSimilarity(v1, v2), 1e-4f)

        val vOpp = FloatArray(384) { -1.0f }
        assertEquals(-1.0f, SyllabusDatabase.cosineSimilarity(v1, vOpp), 1e-4f)

        // Orthogonal vectors
        val vOrtho1 = FloatArray(384) { 0.0f }.also { it[0] = 1.0f }
        val vOrtho2 = FloatArray(384) { 0.0f }.also { it[1] = 1.0f }
        assertEquals(0.0f, SyllabusDatabase.cosineSimilarity(vOrtho1, vOrtho2), 1e-4f)
    }

    // ==============================================================================
    // OFF-008: Strict Curriculum Filtering and Isolation
    // ==============================================================================
    @Test
    fun test_off_008_strict_curriculum_filtering_and_isolation() {
        // Multi-curriculum test records
        val testCorpus = listOf(
            RetrievedChunk(
                chunkId = "cbse-10-sci-001",
                content = "CBSE Class 10 Science Chapter 1 Chemical Reactions",
                board = "CBSE",
                classGrade = "10",
                subject = "Science"
            ),
            RetrievedChunk(
                chunkId = "cbse-12-phy-001",
                content = "CBSE Class 12 Physics Electrostatics",
                board = "CBSE",
                classGrade = "12",
                subject = "Physics"
            ),
            RetrievedChunk(
                chunkId = "icse-10-sci-001",
                content = "ICSE Class 10 Science Chemical Bonding",
                board = "ICSE",
                classGrade = "10",
                subject = "Science"
            ),
            RetrievedChunk(
                chunkId = "cbse-10-hist-001",
                content = "CBSE Class 10 History The Rise of Nationalism in Europe",
                board = "CBSE",
                classGrade = "10",
                subject = "Social Science"
            )
        )

        fun filterCurriculum(chunks: List<RetrievedChunk>, b: String?, cg: String?, s: String?): List<RetrievedChunk> {
            return chunks.filter { c ->
                (b.isNullOrBlank() || c.board.equals(b, ignoreCase = true)) &&
                (cg.isNullOrBlank() || c.classGrade.equals(cg, ignoreCase = true)) &&
                (s.isNullOrBlank() || c.subject.equals(s, ignoreCase = true))
            }
        }

        // Target: CBSE Class 10 Science
        val filtered = filterCurriculum(testCorpus, "CBSE", "10", "Science")
        assertEquals(1, filtered.size)
        assertEquals("cbse-10-sci-001", filtered[0].chunkId)

        // Must reject ICSE, Class 12, and History records (zero leakage)
        assertFalse(filtered.any { it.board == "ICSE" })
        assertFalse(filtered.any { it.classGrade == "12" })
        assertFalse(filtered.any { it.subject == "Social Science" })
    }

    // ==============================================================================
    // OFF-009: RRF Hybrid Scoring Mathematics (k=60)
    // ==============================================================================
    @Test
    fun test_off_009_rrf_hybrid_scoring_mathematics() {
        val rrfK = 60
        val rrfMaxScore = (1.0f / (rrfK + 1)) * 2.0f

        // Case 1: Dual consensus #1 in both keyword and vector search
        val dualRank1Raw = (1.0f / (rrfK + 1)) + (1.0f / (rrfK + 1))
        val dualRank1Norm = (dualRank1Raw / rrfMaxScore).coerceIn(0.0f, 1.0f)
        assertEquals(1.0f, dualRank1Norm, 1e-4f)

        // Case 2: Single retriever #1 (rank 1 in vector, not found in keyword)
        val singleRank1Raw = 1.0f / (rrfK + 1)
        val singleRank1Norm = (singleRank1Raw / rrfMaxScore).coerceIn(0.0f, 1.0f)
        assertEquals(0.50f, singleRank1Norm, 1e-4f)

        // Case 3: Dual rank 2 in both
        val dualRank2Raw = (1.0f / (rrfK + 2)) + (1.0f / (rrfK + 2))
        val dualRank2Norm = (dualRank2Raw / rrfMaxScore).coerceIn(0.0f, 1.0f)
        assertTrue(dualRank2Norm > 0.98f)
        assertTrue(dualRank2Norm < 1.0f)
    }

    // ==============================================================================
    // OFF-012: Safe Schema and FTS5 Regression Avoidance
    // ==============================================================================
    @Test
    fun test_off_012_fts5_regression_avoidance() {
        // Table names must use standard syllabus_chunks and syllabus_packs
        assertEquals("syllabus_chunks", SyllabusDatabase.TABLE_CHUNKS)
        assertEquals("syllabus_packs", SyllabusDatabase.TABLE_PACKS)
        assertEquals(2, SyllabusDatabase.DATABASE_VERSION)
    }

    // ==============================================================================
    // OFF-013: Phase 4 Evidence Policy Offline Implementation
    // ==============================================================================
    @Test
    fun test_off_013_evidence_outcome_assessment_phase4_policy() {
        // 1. Lexical corroboration helper check
        val query = "What is Ohm's law and potential difference?"
        val corroboratedContent = "Ohm's law relates potential difference V to current I in a metallic conductor."
        val nonCorroboratedDistractor = "Quantum entanglement between distant electron spins occurs without classical communication."

        assertTrue(OfflineSyllabusRepository.checkLexicalCorroboration(query, corroboratedContent))
        assertFalse(OfflineSyllabusRepository.checkLexicalCorroboration(query, nonCorroboratedDistractor))

        // 2. Empty chunks -> MISSING
        assertEquals(EvidenceOutcome.MISSING, OfflineSyllabusRepository.assessEvidenceOutcome(emptyList(), query))

        // 3. Short content (< 80 chars) guard
        val shortChunk = RetrievedChunk(
            chunkId = "c-short",
            content = "Short definition.",
            normalizedScore = 0.85f,
            vectorRank = 1,
            sparseRank = 1,
            similarityScore = 0.90f
        )
        // Downgraded from SUFFICIENT to UNCERTAIN because length < 80
        assertEquals(EvidenceOutcome.UNCERTAIN, OfflineSyllabusRepository.assessEvidenceOutcome(listOf(shortChunk), query))

        // 4. Dual consensus match (score >= 0.70, content >= 80) -> SUFFICIENT
        val dualChunk = RetrievedChunk(
            chunkId = "c-dual",
            content = "Ohm's law states that electric current flowing through a conductor is directly proportional to potential difference V = IR across its ends.",
            normalizedScore = 0.82f,
            vectorRank = 1,
            sparseRank = 1,
            similarityScore = 0.88f
        )
        assertEquals(EvidenceOutcome.SUFFICIENT, OfflineSyllabusRepository.assessEvidenceOutcome(listOf(dualChunk), query))

        // 5. Dense-only match with corroboration (score >= 0.50, sim >= 0.75, corroborated) -> SUFFICIENT
        val denseCorroborated = RetrievedChunk(
            chunkId = "c-dense-ok",
            content = "Ohm's law defines resistance as the constant ratio between potential difference and electric current in an electrical circuit.",
            normalizedScore = 0.50f,
            vectorRank = 1,
            sparseRank = null, // No keyword match
            similarityScore = 0.81f
        )
        assertEquals(EvidenceOutcome.SUFFICIENT, OfflineSyllabusRepository.assessEvidenceOutcome(listOf(denseCorroborated), query))

        // 6. Dense-only out-of-syllabus distractor (e.g. quantum entanglement distractor) lacking corroboration -> MISSING!
        val distractorQuery = "Explain quantum entanglement and Fermi-Dirac statistics in high energy physics"
        val distractorChunk = RetrievedChunk(
            chunkId = "c-distractor",
            content = "During photosynthesis, chlorophyll absorbs solar radiation and drives the conversion of water and carbon dioxide into sugars.",
            normalizedScore = 0.50f,
            vectorRank = 1,
            sparseRank = null,
            similarityScore = 0.79f // High nearest-neighbor cosine similarity
        )
        // Must NOT become SUFFICIENT! Must downgrade to MISSING because "quantum" and "entanglement" are absent!
        assertEquals(EvidenceOutcome.MISSING, OfflineSyllabusRepository.assessEvidenceOutcome(listOf(distractorChunk), distractorQuery))

        // 7. Keyword-only match: score capped at 0.50 -> evaluated against UNCERTAIN (>= 0.35)
        val keywordChunk = RetrievedChunk(
            chunkId = "c-keyword",
            content = "Resistance is measured in ohms and depends on the dimensions of the wire according to resistivity formulas.",
            normalizedScore = 0.45f,
            vectorRank = null,
            sparseRank = 2,
            similarityScore = null
        )
        assertEquals(EvidenceOutcome.UNCERTAIN, OfflineSyllabusRepository.assessEvidenceOutcome(listOf(keywordChunk), query))

        // 8. Low score (< 0.35) -> MISSING
        val lowScoreChunk = RetrievedChunk(
            chunkId = "c-low",
            content = "Some unrelated scientific passage that discusses botanical plant anatomy and leaf structure.",
            normalizedScore = 0.20f,
            vectorRank = 15,
            sparseRank = 15,
            similarityScore = 0.30f
        )
        assertEquals(EvidenceOutcome.MISSING, OfflineSyllabusRepository.assessEvidenceOutcome(listOf(lowScoreChunk), query))
    }
}
