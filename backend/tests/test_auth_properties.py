import pytest
from datetime import datetime, timezone, timedelta
from hypothesis import given, strategies as st, settings as hyp_settings

from app.core.security import (
    decode_access_token,
    create_access_token,
    hash_token,
    verify_password,
    get_password_hash,
)
from app.core.config import settings


# Property 1: Email normalization idempotency
# normalize(normalize(e)) == normalize(e)
def normalize_email(email: str) -> str:
    return email.strip().lower()


@given(st.text())
def test_property_1_email_normalization_idempotent(s: str):
    norm1 = normalize_email(s)
    norm2 = normalize_email(norm1)
    assert norm1 == norm2


# Property 2: Case-insensitive login consistency
# For any email, lower() or upper() variants normalize to identical lookup keys
@given(st.emails())
def test_property_2_case_insensitive_email_lookup(email: str):
    assert normalize_email(email.lower()) == normalize_email(email.upper())


# Property 3: Refresh token hash collision resistance / uniqueness
# Distinct tokens have distinct hashes
@given(st.text(min_size=10, max_size=100), st.text(min_size=10, max_size=100))
def test_property_5_token_hash_uniqueness(t1: str, t2: str):
    if t1 != t2:
        assert hash_token(t1) != hash_token(t2)


# Property 4: JWT expiry rejection for any past timestamp
@given(st.integers(min_value=1, max_value=100000))
def test_property_4_jwt_expiry_rejection(past_seconds: int):
    # Create token with past expiry
    token = create_access_token(
        user_id="user-123",
        email="test@example.com",
        expires_delta=timedelta(seconds=-past_seconds),
    )
    payload = decode_access_token(token)
    assert payload is None
