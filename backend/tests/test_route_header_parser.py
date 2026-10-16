"""Tests for the incremental RouteHeaderParser state machine."""

import pytest
from app.schemas.rag import RouteDecision
from app.services.route_header_parser import RouteHeaderParser, ParserState


def test_direct_route_single_chunk():
    parser = RouteHeaderParser()
    body = parser.feed("ROUTE: DIRECT\nHello student!")
    assert parser.is_complete
    assert parser.is_direct
    assert parser.route == RouteDecision.DIRECT
    assert body == "Hello student!"

    # Subsequent chunks pass straight through
    assert parser.feed(" How are you?") == " How are you?"


def test_direct_route_char_by_char():
    parser = RouteHeaderParser()
    raw = "ROUTE: DIRECT\nHere is your response."
    accumulated_body = []

    for char in raw:
        token = parser.feed(char)
        if token:
            accumulated_body.append(token)

    assert parser.is_complete
    assert parser.is_direct
    assert parser.route == RouteDecision.DIRECT
    assert "".join(accumulated_body) == "Here is your response."


def test_rag_route_invariant_not_complete_until_query():
    """Verify RAG header is NEVER marked complete until full QUERY line received."""
    parser = RouteHeaderParser()

    # Feed ROUTE: RAG\n
    token = parser.feed("ROUTE: RAG\n")
    assert token is None
    assert parser.state == ParserState.AWAITING_QUERY_LINE
    assert not parser.is_complete  # CRUCIAL INVARIANT: Must NOT be complete yet!
    assert parser.query is None

    # Feed partial query
    parser.feed("QUE")
    assert not parser.is_complete
    assert parser.query is None

    # Feed rest of query line without newline
    parser.feed("RY: Photosynthesis in plants")
    assert not parser.is_complete
    assert parser.query is None

    # Feed the terminating newline
    parser.feed("\n")
    assert parser.is_complete  # NOW it is complete!
    assert parser.is_rag
    assert parser.route == RouteDecision.RAG
    assert parser.query == "Photosynthesis in plants"


def test_rag_route_char_by_char():
    parser = RouteHeaderParser()
    raw = "ROUTE: RAG\nQUERY: laws of motion\n"

    for char in raw:
        parser.feed(char)

    assert parser.is_complete
    assert parser.is_rag
    assert parser.route == RouteDecision.RAG
    assert parser.query == "laws of motion"


def test_arbitrary_chunk_splits():
    raw = "ROUTE: RAG\nQUERY: chemical reactions and equations\n"
    # Test every single split point from 1 to len(raw)
    for split_idx in range(1, len(raw)):
        chunk1 = raw[:split_idx]
        chunk2 = raw[split_idx:]

        parser = RouteHeaderParser()
        parser.feed(chunk1)
        parser.feed(chunk2)

        assert parser.is_complete, f"Failed at split index {split_idx}"
        assert parser.is_rag
        assert parser.query == "chemical reactions and equations"


def test_empty_query_raises_error():
    parser = RouteHeaderParser()
    with pytest.raises(ValueError, match="Empty QUERY"):
        parser.feed("ROUTE: RAG\nQUERY:   \n")


def test_missing_query_line_raises_error():
    parser = RouteHeaderParser()
    with pytest.raises(ValueError, match="Expected 'QUERY:"):
        parser.feed("ROUTE: RAG\nSomething else instead\n")


def test_invalid_route_raises_error():
    parser = RouteHeaderParser()
    with pytest.raises(ValueError, match="Unsupported route"):
        parser.feed("ROUTE: INVALID\n")


def test_buffer_overflow_raises_error():
    parser = RouteHeaderParser()
    with pytest.raises(ValueError, match="exceeded limit"):
        parser.feed("A" * 600)


def test_premature_eof_finalize():
    parser = RouteHeaderParser()
    parser.feed("ROUTE: RA")
    with pytest.raises(ValueError, match="Stream ended before"):
        parser.finalize()


def test_crlf_line_endings_and_splits():
    parser = RouteHeaderParser()
    p1 = "ROUTE: RAG\r"
    p2 = "\nQUERY: Chemical reactions\r\n"
    assert parser.feed(p1) is None
    assert not parser.is_complete
    assert parser.feed(p2) is None
    assert parser.is_complete
    assert parser.is_rag
    assert parser.query == "Chemical reactions"


def test_direct_route_crlf_split():
    parser = RouteHeaderParser()
    p1 = "ROUTE: DIRECT\r"
    p2 = "\nBody starts here"
    assert parser.feed(p1) is None
    assert not parser.is_complete
    assert parser.feed(p2) == "Body starts here"
    assert parser.is_complete
    assert parser.is_direct

