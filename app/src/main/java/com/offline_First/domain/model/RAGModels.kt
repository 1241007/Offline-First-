package com.offline_First.domain.model

/** Route decision from the router */
enum class RouteDecision {
    DIRECT,
    RAG
}

/** Evidence assessment outcome */
enum class EvidenceOutcome {
    SUFFICIENT,
    UNCERTAIN,
    MISSING,
    RETRIEVAL_ERROR,
    CORPUS_NOT_CONFIGURED
}

/** Application mode context */
enum class AppMode {
    STUDENT,
    GENERAL
}

/** Corpus scope for retrieval */
enum class CorpusScope {
    CURRICULUM,
    GENERAL_DOCUMENTS
}

/** Request context carried through routing and retrieval */
data class RAGRequestContext(
    val executionMode: ConnectionMode = ConnectionMode.ONLINE,
    val appMode: AppMode = AppMode.GENERAL,
    val board: String? = null,
    val classGrade: String? = null,
    val subject: String? = null,
    val language: String = "en",
    val explanationMode: ExplanationMode = ExplanationMode.GENERAL,
    val standaloneQuery: String? = null,
    val selectedChapter: String? = null,
    val corpusScope: CorpusScope = CorpusScope.CURRICULUM,
    val corpusVersion: String? = null,
    val recentMessages: List<ChatMessage> = emptyList()
)

/** Retrieved chunk with source metadata */
data class RetrievedChunk(
    val chunkId: String,
    val content: String,
    val board: String? = null,
    val classGrade: String? = null,
    val subject: String? = null,
    val chapter: String? = null,
    val section: String? = null,
    val textbookTitle: String? = null,
    val pageReference: String? = null,
    val sourceId: String? = null,
    val relevanceScore: Float? = null,
    val rawRrfScore: Float? = null,
    val normalizedScore: Float? = null,
    val vectorRank: Int? = null,
    val sparseRank: Int? = null,
    val similarityScore: Float? = null
)

/** Source citation for grounded answers */
data class SourceCitation(
    val textbookTitle: String? = null,
    val chapter: String? = null,
    val section: String? = null,
    val pageReference: String? = null,
    val sourceId: String? = null
)

/** Complete routing result */
sealed class RoutingResult {
    data class Direct(val streamContent: Boolean = true) : RoutingResult()
    data class Rag(val standaloneQuery: String) : RoutingResult()
    data class Error(val reason: String) : RoutingResult()
}
