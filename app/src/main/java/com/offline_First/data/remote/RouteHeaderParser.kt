package com.offline_First.data.remote

import com.offline_First.domain.model.RouteDecision

enum class ParserState {
    AWAITING_ROUTE_LINE,
    AWAITING_QUERY_LINE,
    DIRECT_BODY,
    RAG_COMPLETE,
    ERROR
}

/**
 * Incremental state machine for parsing streaming route headers in Android.
 *
 * Guaranteed to handle arbitrary chunk splits, character-by-character emissions,
 * and never marks RAG complete until the full, non-empty QUERY line is parsed.
 */
class RouteHeaderParser(
    private val maxHeaderChars: Int = 512,
    private val maxQueryLength: Int = 300
) {
    var state: ParserState = ParserState.AWAITING_ROUTE_LINE
        private set

    var route: RouteDecision? = null
        private set

    var query: String? = null
        private set

    var errorReason: String? = null
        private set

    private var buffer: String = ""

    val isComplete: Boolean
        get() = state == ParserState.DIRECT_BODY || state == ParserState.RAG_COMPLETE

    val isDirect: Boolean
        get() = state == ParserState.DIRECT_BODY

    val isRag: Boolean
        get() = state == ParserState.RAG_COMPLETE

    val isError: Boolean
        get() = state == ParserState.ERROR

    fun feed(chunk: String): String? {
        if (chunk.isEmpty()) return null

        if (state == ParserState.DIRECT_BODY) {
            return chunk
        }
        if (state == ParserState.RAG_COMPLETE) {
            return null
        }
        if (state == ParserState.ERROR) {
            throw IllegalStateException(errorReason ?: "RouteHeaderParser is in error state")
        }

        buffer += chunk
        if (buffer.length > maxHeaderChars) {
            state = ParserState.ERROR
            errorReason = "Route header exceeded limit of $maxHeaderChars chars"
            throw IllegalStateException(errorReason)
        }

        // State 1: Awaiting ROUTE line
        if (state == ParserState.AWAITING_ROUTE_LINE) {
            val newlineIndex = buffer.indexOf('\n')
            if (newlineIndex != -1) {
                val line = buffer.substring(0, newlineIndex).trim()
                val rest = buffer.substring(newlineIndex + 1)

                when {
                    line == "ROUTE: DIRECT" -> {
                        route = RouteDecision.DIRECT
                        state = ParserState.DIRECT_BODY
                        buffer = ""
                        return if (rest.isNotEmpty()) rest else null
                    }
                    line == "ROUTE: RAG" -> {
                        route = RouteDecision.RAG
                        state = ParserState.AWAITING_QUERY_LINE
                        buffer = rest
                        // Note: do NOT mark complete. Check rest for QUERY
                    }
                    line.startsWith("ROUTE:") -> {
                        state = ParserState.ERROR
                        errorReason = "Unsupported route line: $line"
                        throw IllegalStateException(errorReason)
                    }
                    else -> {
                        state = ParserState.ERROR
                        errorReason = "Malformed header, expected 'ROUTE: ...', got: $line"
                        throw IllegalStateException(errorReason)
                    }
                }
            }
        }

        // State 2: Awaiting QUERY line
        if (state == ParserState.AWAITING_QUERY_LINE) {
            val newlineIndex = buffer.indexOf('\n')
            if (newlineIndex != -1) {
                val line = buffer.substring(0, newlineIndex).trim()
                val rest = buffer.substring(newlineIndex + 1)

                if (line.isEmpty()) {
                    buffer = rest
                } else if (line.startsWith("QUERY:")) {
                    val queryText = line.substring(6).trim()
                    if (queryText.isEmpty()) {
                        state = ParserState.ERROR
                        errorReason = "Empty QUERY line in RAG route header"
                        throw IllegalStateException(errorReason)
                    }
                    query = queryText.take(maxQueryLength)
                    state = ParserState.RAG_COMPLETE
                    buffer = rest
                    return null
                } else {
                    state = ParserState.ERROR
                    errorReason = "Expected 'QUERY: ...' after 'ROUTE: RAG', got: $line"
                    throw IllegalStateException(errorReason)
                }
            }
        }

        return null
    }

    fun finalize() {
        if (state == ParserState.AWAITING_ROUTE_LINE) {
            state = ParserState.ERROR
            errorReason = "Stream ended before complete ROUTE line was received"
            throw IllegalStateException(errorReason)
        }
        if (state == ParserState.AWAITING_QUERY_LINE) {
            val line = buffer.trim()
            if (line.startsWith("QUERY:")) {
                val queryText = line.substring(6).trim()
                if (queryText.isNotEmpty()) {
                    query = queryText.take(maxQueryLength)
                    state = ParserState.RAG_COMPLETE
                    buffer = ""
                    return
                }
            }
            state = ParserState.ERROR
            errorReason = "Stream ended before complete QUERY line was received for RAG route"
            throw IllegalStateException(errorReason)
        }
    }
}
