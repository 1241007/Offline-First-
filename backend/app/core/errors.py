"""Structured error codes and exception types for auth responses."""

from dataclasses import dataclass
from fastapi import HTTPException

# ── Error code constants ───────────────────────────────────────────────────────
INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
ACCOUNT_EXISTS = "ACCOUNT_EXISTS"
INVALID_TOKEN = "INVALID_TOKEN"
TOKEN_EXPIRED = "TOKEN_EXPIRED"
SESSION_REVOKED = "SESSION_REVOKED"
VALIDATION_ERROR = "VALIDATION_ERROR"
RATE_LIMITED = "RATE_LIMITED"
ACCOUNT_DISABLED = "ACCOUNT_DISABLED"
PASSWORD_RESET_INVALID = "PASSWORD_RESET_INVALID"
PASSWORD_RESET_EXPIRED = "PASSWORD_RESET_EXPIRED"
NOT_FOUND = "NOT_FOUND"


@dataclass
class AuthError:
    code: str
    message: str


class AuthException(HTTPException):
    """HTTPException subclass that carries a structured auth error code."""

    def __init__(self, status_code: int, code: str, message: str):
        super().__init__(status_code=status_code, detail={"code": code, "message": message})
        self.code = code
        self.auth_message = message
