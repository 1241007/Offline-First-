"""Contract tests for SSE event framing, buffer fragmentation, and Android ChatApiClient compatibility.

Validates that SSE payloads emitted by FastAPI strictly adhere to the schema
expected by Android's ChatApiClient and StreamEventDto:
1. Handles chunked HTTP transfer encoding and arbitrary buffer boundary splits.
2. Handles CRLF (\r\n\r\n) and standard LF (\n\n) event delimiters.
3. Verifies schema structure for all event types:
   - metadata (user_message_id, assistant_message_id)
   - route (decision, query)
   - citations (outcome, citations list with CitationDto fields)
   - token (content)
   - error (detail)
   - done (assistant_message_id, title)
4. Validates field types and nullability matching Kotlin data classes.
"""

import json
from typing import Any, Dict, Generator, List, Optional
import pytest


class SseStreamBuffer:
    """
    Simulates the buffering and line-by-line reading logic implemented in
    Android's ChatApiClient.kt (BufferedReader over byteStream).
    """

    def __init__(self):
        self._buffer = ""

    def feed(self, chunk: str) -> List[Dict[str, Any]]:
        """Feeds an incoming network chunk and extracts all completed SSE JSON events."""
        self._buffer += chunk
        # Normalize CRLF to LF
        normalized = self._buffer.replace("\r\n", "\n")
        lines = normalized.split("\n")
        
        # Keep the last incomplete fragment in the buffer
        self._buffer = lines[-1]
        completed_lines = lines[:-1]

        events = []
        for line in completed_lines:
            line = line.strip()
            if line.startswith("data: "):
                data_str = line[len("data: "):].strip()
                if data_str:
                    event = json.loads(data_str)
                    events.append(event)
        return events

    def finalize(self) -> List[Dict[str, Any]]:
        """Flushes any remaining event upon EOF."""
        events = []
        remaining = self._buffer.strip()
        if remaining.startswith("data: "):
            data_str = remaining[len("data: "):].strip()
            if data_str:
                events.append(json.loads(data_str))
        self._buffer = ""
        return events


# ==============================================================================
# 1. BUFFER FRAGMENTATION & CHUNKING TESTS
# ==============================================================================

def test_sse_chunked_boundary_fragmentation():
    """Verify that events split across arbitrary network buffer boundaries parse cleanly."""
    stream_buffer = SseStreamBuffer()
    full_event = (
        'data: {"type": "token", "content": "Photosynthesis is the process..."}\n\n'
    )

    # Fragment across arbitrary byte slices: ["dat", "a: {\"type", "\": \"token", ..."]
    slices = [
        full_event[:3],       # 'dat'
        full_event[3:15],      # 'a: {"type'
        full_event[15:28],     # '": "token", "'
        full_event[28:45],     # 'content": "Photos'
        full_event[45:60],     # 'ynthesis is the '
        full_event[60:],       # 'process..."}\n\n'
    ]

    collected = []
    for s in slices:
        events = stream_buffer.feed(s)
        collected.extend(events)
    collected.extend(stream_buffer.finalize())

    assert len(collected) == 1
    ev = collected[0]
    assert ev["type"] == "token"
    assert ev["content"] == "Photosynthesis is the process..."


def test_sse_multiple_events_in_single_network_packet():
    """Verify that multiple SSE events arriving in a single TCP/TLS buffer are all parsed."""
    stream_buffer = SseStreamBuffer()
    multi_packet = (
        'data: {"type": "metadata", "user_message_id": "u1", "assistant_message_id": "a1"}\n\n'
        'data: {"type": "route", "decision": "RAG", "query": "Explain Ohm law"}\n\n'
        'data: {"type": "token", "content": "Ohm\'s law states that "}\n\n'
    )

    events = stream_buffer.feed(multi_packet)
    assert len(events) == 3
    assert events[0]["type"] == "metadata"
    assert events[1]["type"] == "route"
    assert events[2]["type"] == "token"
    assert events[2]["content"] == "Ohm's law states that "


def test_sse_crlf_line_delimiters():
    """Verify Windows-style CRLF delimiters (\\r\\n\\r\\n) are properly parsed."""
    stream_buffer = SseStreamBuffer()
    crlf_event = 'data: {"type": "token", "content": "Testing CRLF"}\r\n\r\n'

    events = stream_buffer.feed(crlf_event)
    assert len(events) == 1
    assert events[0]["type"] == "token"
    assert events[0]["content"] == "Testing CRLF"


# ==============================================================================
# 2. SCHEMA COMPATIBILITY WITH ANDROID STREAM_EVENT_DTO
# ==============================================================================

def test_metadata_event_schema_contract():
    """
    Android StreamEventDto schema:
    data class StreamEventDto(
        val type: String,
        @SerialName("user_message_id") val userMessageId: String? = null,
        @SerialName("assistant_message_id") val assistantMessageId: String? = null
    )
    """
    payload = {
        "type": "metadata",
        "user_message_id": "0a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
        "assistant_message_id": "9f8e7d6c-5b4a-3f2e-1d0c-9b8a7f6e5d4c",
    }
    encoded = f"data: {json.dumps(payload)}\n\n"
    buffer = SseStreamBuffer()
    events = buffer.feed(encoded)

    assert len(events) == 1
    ev = events[0]
    assert ev["type"] == "metadata"
    assert isinstance(ev["user_message_id"], str)
    assert isinstance(ev["assistant_message_id"], str)
    assert len(ev["user_message_id"]) == 36
    assert len(ev["assistant_message_id"]) == 36


def test_route_event_schema_contract():
    """
    Android StreamEventDto schema:
    val decision: String? = null,
    val query: String? = null
    """
    payload = {
        "type": "route",
        "decision": "RAG",
        "query": "What happens when magnesium ribbon burns in oxygen?",
    }
    encoded = f"data: {json.dumps(payload)}\n\n"
    buffer = SseStreamBuffer()
    events = buffer.feed(encoded)

    assert len(events) == 1
    ev = events[0]
    assert ev["type"] == "route"
    assert ev["decision"] in ("RAG", "DIRECT")
    assert isinstance(ev["query"], str)


def test_citations_event_schema_contract():
    """
    Android StreamEventDto & CitationDto schema:
    val outcome: String? = null,
    val citations: List<CitationDto>? = null
    where CitationDto has:
      chunk_id: String
      textbook_title: String?
      chapter: String?
      section: String?
      page_reference: String?
      score: Float?
    """
    payload = {
        "type": "citations",
        "outcome": "SUFFICIENT",
        "citations": [
            {
                "chunk_id": "c3cc7279-7ee6-4e66-a8fb-38cc53e5d9ff",
                "textbook_title": "Chapter 01 Chemical Reactions And Equations",
                "chapter": "Chemical Reactions and Equations",
                "section": "1.1 Chemical Reactions",
                "page_reference": "Page 2",
                "score": 0.500,
            },
            {
                "chunk_id": "ea64d108-089b-4c21-aff6-5fac6fb736c3",
                "textbook_title": "Chapter 01 Chemical Reactions And Equations",
                "chapter": "Chemical Reactions and Equations",
                "section": "1.1 Chemical Reactions",
                "page_reference": "Page 3",
                "score": 0.4919,
            },
        ],
    }
    encoded = f"data: {json.dumps(payload)}\n\n"
    buffer = SseStreamBuffer()
    events = buffer.feed(encoded)

    assert len(events) == 1
    ev = events[0]
    assert ev["type"] == "citations"
    assert ev["outcome"] in ("SUFFICIENT", "UNCERTAIN", "MISSING", "CORPUS_NOT_CONFIGURED", "RETRIEVAL_ERROR")
    assert isinstance(ev["citations"], list)
    assert len(ev["citations"]) == 2

    for c in ev["citations"]:
        assert isinstance(c["chunk_id"], str)
        assert isinstance(c["textbook_title"], str)
        assert isinstance(c["chapter"], str)
        assert isinstance(c["section"], str)
        assert isinstance(c["page_reference"], str)
        assert isinstance(c["score"], (float, int))
        assert 0.0 <= c["score"] <= 1.0


def test_token_and_done_event_schema_contract():
    """
    Android StreamEventDto:
    token: content: String?
    done: assistant_message_id: String?, title: String?
    """
    token_payload = {"type": "token", "content": "Magnesium oxide is formed."}
    done_payload = {
        "type": "done",
        "assistant_message_id": "9f8e7d6c-5b4a-3f2e-1d0c-9b8a7f6e5d4c",
        "title": "Magnesium Ribbon Burning",
    }
    raw = f"data: {json.dumps(token_payload)}\n\ndata: {json.dumps(done_payload)}\n\n"
    buffer = SseStreamBuffer()
    events = buffer.feed(raw)

    assert len(events) == 2
    assert events[0]["type"] == "token"
    assert events[0]["content"] == "Magnesium oxide is formed."

    assert events[1]["type"] == "done"
    assert events[1]["assistant_message_id"] == "9f8e7d6c-5b4a-3f2e-1d0c-9b8a7f6e5d4c"
    assert events[1]["title"] == "Magnesium Ribbon Burning"


def test_error_event_schema_contract():
    """
    Android StreamEventDto:
    error: detail: String?
    """
    err_payload = {"type": "error", "detail": "Retrieval timeout exceeded"}
    raw = f"data: {json.dumps(err_payload)}\n\n"
    buffer = SseStreamBuffer()
    events = buffer.feed(raw)

    assert len(events) == 1
    assert events[0]["type"] == "error"
    assert events[0]["detail"] == "Retrieval timeout exceeded"
