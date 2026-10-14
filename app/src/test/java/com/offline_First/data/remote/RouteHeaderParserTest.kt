package com.offline_First.data.remote

import com.offline_First.domain.model.RouteDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteHeaderParserTest {

    @Test
    fun testDirectRouteSingleChunk() {
        val parser = RouteHeaderParser()
        val body = parser.feed("ROUTE: DIRECT\nHello student!")
        assertTrue(parser.isComplete)
        assertTrue(parser.isDirect)
        assertEquals(RouteDecision.DIRECT, parser.route)
        assertEquals("Hello student!", body)

        assertEquals(" Next chunk", parser.feed(" Next chunk"))
    }

    @Test
    fun testDirectRouteCharByChar() {
        val parser = RouteHeaderParser()
        val raw = "ROUTE: DIRECT\nFirst body chunk"
        val accumulated = StringBuilder()

        for (ch in raw) {
            val token = parser.feed(ch.toString())
            if (token != null) {
                accumulated.append(token)
            }
        }

        assertTrue(parser.isComplete)
        assertTrue(parser.isDirect)
        assertEquals("First body chunk", accumulated.toString())
    }

    @Test
    fun testRagRouteInvariantNotCompleteUntilQueryLine() {
        val parser = RouteHeaderParser()

        // 1. Feed ROUTE: RAG\n
        val token1 = parser.feed("ROUTE: RAG\n")
        assertNull(token1)
        assertFalse(parser.isComplete) // CRUCIAL INVARIANT: Must NOT be complete yet!
        assertNull(parser.query)
        assertEquals(ParserState.AWAITING_QUERY_LINE, parser.state)

        // 2. Feed partial query text
        val token2 = parser.feed("QUE")
        assertNull(token2)
        assertFalse(parser.isComplete)
        assertNull(parser.query)

        // 3. Feed rest of query line without newline
        val token3 = parser.feed("RY: Chemical reactions and equations")
        assertNull(token3)
        assertFalse(parser.isComplete)
        assertNull(parser.query)

        // 4. Feed terminating newline
        val token4 = parser.feed("\n")
        assertNull(token4)
        assertTrue(parser.isComplete) // NOW and ONLY now complete!
        assertTrue(parser.isRag)
        assertEquals(RouteDecision.RAG, parser.route)
        assertEquals("Chemical reactions and equations", parser.query)
    }

    @Test
    fun testRagRouteCharByChar() {
        val parser = RouteHeaderParser()
        val raw = "ROUTE: RAG\nQUERY: Laws of motion\n"

        for (ch in raw) {
            parser.feed(ch.toString())
        }

        assertTrue(parser.isComplete)
        assertTrue(parser.isRag)
        assertEquals(RouteDecision.RAG, parser.route)
        assertEquals("Laws of motion", parser.query)
    }

    @Test
    fun testArbitraryChunkSplits() {
        val raw = "ROUTE: RAG\nQUERY: Photosynthesis in plants\n"
        for (splitIdx in 1 until raw.length) {
            val chunk1 = raw.substring(0, splitIdx)
            val chunk2 = raw.substring(splitIdx)

            val parser = RouteHeaderParser()
            parser.feed(chunk1)
            parser.feed(chunk2)

            assertTrue("Failed at split index $splitIdx", parser.isComplete)
            assertTrue(parser.isRag)
            assertEquals("Photosynthesis in plants", parser.query)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testEmptyQueryRaises() {
        val parser = RouteHeaderParser()
        parser.feed("ROUTE: RAG\nQUERY:   \n")
    }

    @Test(expected = IllegalStateException::class)
    fun testMissingQueryLineRaises() {
        val parser = RouteHeaderParser()
        parser.feed("ROUTE: RAG\nNot a query line\n")
    }

    @Test(expected = IllegalStateException::class)
    fun testInvalidRouteRaises() {
        val parser = RouteHeaderParser()
        parser.feed("ROUTE: INVALID\n")
    }
}
