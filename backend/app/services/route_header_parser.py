"""Incremental state machine parser for streaming route headers."""

import logging
from enum import Enum, auto
from typing import Optional

from app.schemas.rag import RouteDecision

logger = logging.getLogger(__name__)


class ParserState(Enum):
    AWAITING_ROUTE_LINE = auto()
    AWAITING_QUERY_LINE = auto()
    DIRECT_BODY = auto()
    RAG_COMPLETE = auto()
    ERROR = auto()


class RouteHeaderParser:
    """
    Incremental state machine for parsing streaming route headers.
    
    Expected protocols:
      ROUTE: DIRECT\\n<streamed tokens...>
      ROUTE: RAG\\nQUERY: <standalone query>\\n
      
    Guarantees:
    - Robust against arbitrary streaming chunk splits (character-by-character, line splits, etc.)
    - A RAG header is NEVER marked complete until a valid, non-empty QUERY line is fully received.
    - Leftover tokens after `ROUTE: DIRECT\\n` are emitted immediately as body text.
    - Buffer size is capped to prevent memory leaks from malformed infinite headers.
    """

    MAX_HEADER_BYTES: int = 512
    MAX_QUERY_LENGTH: int = 300

    def __init__(self):
        self._state = ParserState.AWAITING_ROUTE_LINE
        self._buffer: str = ""
        self._route: Optional[RouteDecision] = None
        self._query: Optional[str] = None
        self._error_reason: Optional[str] = None

    @property
    def state(self) -> ParserState:
        return self._state

    @property
    def route(self) -> Optional[RouteDecision]:
        return self._route

    @property
    def query(self) -> Optional[str]:
        return self._query

    @property
    def is_complete(self) -> bool:
        return self._state in (ParserState.DIRECT_BODY, ParserState.RAG_COMPLETE)

    @property
    def is_direct(self) -> bool:
        return self._state == ParserState.DIRECT_BODY

    @property
    def is_rag(self) -> bool:
        return self._state == ParserState.RAG_COMPLETE

    @property
    def is_error(self) -> bool:
        return self._state == ParserState.ERROR

    @property
    def error_reason(self) -> Optional[str]:
        return self._error_reason

    def feed(self, chunk: str) -> Optional[str]:
        """
        Feed an incoming chunk of streaming text.
        
        Returns:
            - Body text to forward to the consumer if in DIRECT_BODY mode.
            - None if still parsing header or if RAG route selected.
            
        Raises:
            ValueError if header is malformed or exceeds max buffer size.
        """
        if not chunk:
            return None

        # Once in direct body mode, stream chunks pass straight through
        if self._state == ParserState.DIRECT_BODY:
            return chunk

        if self._state == ParserState.RAG_COMPLETE:
            return None

        if self._state == ParserState.ERROR:
            raise ValueError(self._error_reason or "Route header parser is in error state")

        self._buffer += chunk

        if len(self._buffer) > self.MAX_HEADER_BYTES:
            self._state = ParserState.ERROR
            self._error_reason = f"Route header exceeded limit of {self.MAX_HEADER_BYTES} characters"
            raise ValueError(self._error_reason)

        # State 1: Awaiting ROUTE: DIRECT or ROUTE: RAG
        if self._state == ParserState.AWAITING_ROUTE_LINE:
            if "\n" in self._buffer:
                line, rest = self._buffer.split("\n", 1)
                line_clean = line.strip()

                if line_clean == "ROUTE: DIRECT":
                    self._route = RouteDecision.DIRECT
                    self._state = ParserState.DIRECT_BODY
                    self._buffer = ""
                    # Any characters following the newline belong to the body
                    return rest if rest else None

                elif line_clean == "ROUTE: RAG":
                    self._route = RouteDecision.RAG
                    self._state = ParserState.AWAITING_QUERY_LINE
                    self._buffer = rest  # Continue buffering for QUERY line
                    # Note: We do NOT mark complete yet! Fall through to check if rest contains QUERY
                elif line_clean.startswith("ROUTE:"):
                    self._state = ParserState.ERROR
                    self._error_reason = f"Unsupported route line: {line_clean}"
                    raise ValueError(self._error_reason)
                else:
                    self._state = ParserState.ERROR
                    self._error_reason = f"Malformed header, expected 'ROUTE: ...', got: {line_clean[:50]}"
                    raise ValueError(self._error_reason)

        # State 2: Awaiting QUERY: <standalone query>
        if self._state == ParserState.AWAITING_QUERY_LINE:
            if "\n" in self._buffer:
                line, rest = self._buffer.split("\n", 1)
                line_clean = line.strip()

                if not line_clean:
                    # Allow optional blank line between ROUTE and QUERY
                    self._buffer = rest
                elif line_clean.startswith("QUERY:"):
                    query_text = line_clean[6:].strip()
                    if not query_text:
                        self._state = ParserState.ERROR
                        self._error_reason = "Empty QUERY line in RAG route header"
                        raise ValueError(self._error_reason)
                    self._query = query_text[:self.MAX_QUERY_LENGTH]
                    self._state = ParserState.RAG_COMPLETE
                    self._buffer = rest
                    return None  # RAG route does not emit body to UI stream
                else:
                    self._state = ParserState.ERROR
                    self._error_reason = f"Expected 'QUERY: ...' after 'ROUTE: RAG', got: {line_clean[:50]}"
                    raise ValueError(self._error_reason)

        return None

    def finalize(self) -> None:
        """
        Called when the stream ends. Validates that the header wasn't left half-read.
        """
        if self._state == ParserState.AWAITING_ROUTE_LINE:
            self._state = ParserState.ERROR
            self._error_reason = "Stream ended before complete ROUTE line was received"
            raise ValueError(self._error_reason)

        if self._state == ParserState.AWAITING_QUERY_LINE:
            # Check if there is an un-terminated QUERY line in the buffer
            line_clean = self._buffer.strip()
            if line_clean.startswith("QUERY:"):
                query_text = line_clean[6:].strip()
                if query_text:
                    self._query = query_text[:self.MAX_QUERY_LENGTH]
                    self._state = ParserState.RAG_COMPLETE
                    self._buffer = ""
                    return
            self._state = ParserState.ERROR
            self._error_reason = "Stream ended before complete QUERY line was received for RAG route"
            raise ValueError(self._error_reason)
