"""Correlation ID middleware — generates a UUID per request and propagates it."""
import contextvars
import uuid

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

# ContextVar so log calls anywhere in the request chain can access it
correlation_id_var: contextvars.ContextVar[str] = contextvars.ContextVar(
    "correlation_id", default=""
)

HEADER_NAME = "X-Correlation-ID"


class CorrelationIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        # Use client-supplied ID if present; otherwise generate a new one
        cid = request.headers.get(HEADER_NAME) or str(uuid.uuid4())
        correlation_id_var.set(cid)
        request.state.correlation_id = cid

        response: Response = await call_next(request)
        response.headers[HEADER_NAME] = cid
        return response
