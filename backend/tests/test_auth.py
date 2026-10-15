import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy import select
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker
from sqlalchemy.pool import StaticPool
from datetime import datetime, timezone, timedelta

from app.core.database import Base, get_db
from app.core.config import settings
from app.core.security import (
    create_access_token,
    create_refresh_token,
    hash_token,
    get_password_hash,
)
from app.models.user import User, RefreshToken, PasswordResetToken
from app.models.profile import UserProfile
from app.core.rate_limit import limiter
from app.main import app


@pytest.fixture(autouse=True)
def reset_rate_limits():
    limiter.enabled = False
    yield
    limiter.enabled = True


@pytest.mark.asyncio
class TestAuthSuite:

    async def test_registration_valid(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            resp = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "Test User",
                    "email": "register_test@example.com",
                    "mobile": "+1234567890",
                    "password": "Password123!",
                },
            )
            assert resp.status_code == 201, resp.text
            data = resp.json()
            assert "accessToken" in data
            assert "refreshToken" in data
            assert data["user"]["email"] == "register_test@example.com"
            assert data["user"]["fullName"] == "Test User"
        app.dependency_overrides.clear()

    async def test_registration_duplicate_email(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            # First registration
            await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "User One",
                    "email": "duplicate@example.com",
                    "password": "Password123!",
                },
            )
            # Duplicate
            resp = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "User Two",
                    "email": "DUPLICATE@EXAMPLE.COM",
                    "password": "Password123!",
                },
            )
            assert resp.status_code == 409
            data = resp.json()
            assert data["code"] == "ACCOUNT_EXISTS"
        app.dependency_overrides.clear()

    async def test_registration_duplicate_mobile(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "User Mobile 1",
                    "email": "mobile1@example.com",
                    "mobile": "+999111222333",
                    "password": "Password123!",
                },
            )
            resp = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "User Mobile 2",
                    "email": "mobile2@example.com",
                    "mobile": "+999111222333",
                    "password": "Password123!",
                },
            )
            assert resp.status_code == 409
            assert resp.json()["code"] == "ACCOUNT_EXISTS"
        app.dependency_overrides.clear()

    async def test_registration_invalid_email_format(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            resp = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "Invalid Email",
                    "email": "not-an-email",
                    "password": "Password123!",
                },
            )
            assert resp.status_code == 422
            assert resp.json()["code"] == "VALIDATION_ERROR"
        app.dependency_overrides.clear()

    async def test_registration_password_too_short(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            resp = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "Short Password",
                    "email": "shortpass@example.com",
                    "password": "123",
                },
            )
            assert resp.status_code == 422
            assert resp.json()["code"] == "VALIDATION_ERROR"
        app.dependency_overrides.clear()

    async def test_login_valid_email_and_mobile(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "Login Tester",
                    "email": "login_tester@example.com",
                    "mobile": "+4412345678",
                    "password": "SecretPassword123!",
                },
            )

            # Login with email
            resp_email = await client.post(
                "/api/v1/auth/login",
                json={"contact": "LOGIN_TESTER@example.com", "password": "SecretPassword123!"},
            )
            assert resp_email.status_code == 200
            assert "accessToken" in resp_email.json()

            # Login with mobile
            resp_mobile = await client.post(
                "/api/v1/auth/login",
                json={"contact": "+4412345678", "password": "SecretPassword123!"},
            )
            assert resp_mobile.status_code == 200
            assert "accessToken" in resp_mobile.json()

            # Login with wrong password
            resp_bad_pw = await client.post(
                "/api/v1/auth/login",
                json={"contact": "login_tester@example.com", "password": "WrongPassword!"},
            )
            assert resp_bad_pw.status_code == 401
            assert resp_bad_pw.json()["code"] == "INVALID_CREDENTIALS"

            # Login non existent
            resp_no_user = await client.post(
                "/api/v1/auth/login",
                json={"contact": "nobody@example.com", "password": "AnyPassword!"},
            )
            assert resp_no_user.status_code == 401
            assert resp_no_user.json()["code"] == "INVALID_CREDENTIALS"
        app.dependency_overrides.clear()

    async def test_jwt_validation_and_me(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            reg = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "Me Tester",
                    "email": "me_tester@example.com",
                    "password": "Password123!",
                },
            )
            token = reg.json()["accessToken"]

            # Valid /me
            resp_me = await client.get(
                "/api/v1/auth/me",
                headers={"Authorization": f"Bearer {token}"},
            )
            assert resp_me.status_code == 200
            assert resp_me.json()["email"] == "me_tester@example.com"
            assert resp_me.json()["fullName"] == "Me Tester"

            # Tampered token
            tampered = token[:-4] + "fake"
            resp_tampered = await client.get(
                "/api/v1/auth/me",
                headers={"Authorization": f"Bearer {tampered}"},
            )
            assert resp_tampered.status_code == 401

            # Expired token
            user = await db_session.scalar(select(User).where(User.email == "me_tester@example.com"))
            expired_token = create_access_token(
                user_id=user.id,
                email=user.email,
                expires_delta=timedelta(seconds=-10),
            )
            resp_expired = await client.get(
                "/api/v1/auth/me",
                headers={"Authorization": f"Bearer {expired_token}"},
            )
            assert resp_expired.status_code == 401
        app.dependency_overrides.clear()

    async def test_refresh_token_rotation_and_reuse_revocation(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            reg = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "Rotate Tester",
                    "email": "rotate@example.com",
                    "password": "Password123!",
                },
            )
            t1 = reg.json()["refreshToken"]

            # Rotate t1 -> t2
            resp_rot = await client.post(
                "/api/v1/auth/refresh",
                json={"refreshToken": t1},
            )
            assert resp_rot.status_code == 200
            t2 = resp_rot.json()["refreshToken"]
            assert t2 != t1

            # Re-presenting t1 triggers reuse detection
            resp_reuse = await client.post(
                "/api/v1/auth/refresh",
                json={"refreshToken": t1},
            )
            assert resp_reuse.status_code == 401
            assert resp_reuse.json()["code"] == "SESSION_REVOKED"

            # Now t2 should also be revoked because it belongs to the same family
            resp_t2 = await client.post(
                "/api/v1/auth/refresh",
                json={"refreshToken": t2},
            )
            assert resp_t2.status_code == 401
            assert resp_t2.json()["code"] == "SESSION_REVOKED"
        app.dependency_overrides.clear()

    async def test_logout_single_session_and_all_sessions(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            reg = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "Logout Tester",
                    "email": "logout@example.com",
                    "password": "Password123!",
                },
            )
            access1 = reg.json()["accessToken"]
            refresh1 = reg.json()["refreshToken"]

            # Login on second "device"
            login2 = await client.post(
                "/api/v1/auth/login",
                json={"contact": "logout@example.com", "password": "Password123!"},
            )
            access2 = login2.json()["accessToken"]
            refresh2 = login2.json()["refreshToken"]

            # Logout single session (pass refresh1)
            resp_out1 = await client.post(
                "/api/v1/auth/logout",
                headers={"Authorization": f"Bearer {access1}"},
                json={"refreshToken": refresh1},
            )
            assert resp_out1.status_code == 200

            # refresh1 is revoked
            resp_ref1 = await client.post("/api/v1/auth/refresh", json={"refreshToken": refresh1})
            assert resp_ref1.status_code == 401

            # refresh2 should still be valid!
            resp_ref2 = await client.post("/api/v1/auth/refresh", json={"refreshToken": refresh2})
            assert resp_ref2.status_code == 200
            new_ref2 = resp_ref2.json()["refreshToken"]

            # Logout all sessions (no refreshToken passed)
            resp_out_all = await client.post(
                "/api/v1/auth/logout",
                headers={"Authorization": f"Bearer {access2}"},
                json={},
            )
            assert resp_out_all.status_code == 200

            # new_ref2 should now also be revoked
            resp_ref_final = await client.post("/api/v1/auth/refresh", json={"refreshToken": new_ref2})
            assert resp_ref_final.status_code == 401
        app.dependency_overrides.clear()

    async def test_forgot_and_reset_password(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "Reset Tester",
                    "email": "reset@example.com",
                    "password": "OldPassword123!",
                },
            )

            # Request reset
            resp_forgot = await client.post(
                "/api/v1/auth/forgot-password",
                json={"contact": "reset@example.com"},
            )
            assert resp_forgot.status_code == 200
            data = resp_forgot.json()
            assert "devResetToken" in data
            token = data["devResetToken"]
            assert token is not None

            # Reset password
            resp_reset = await client.post(
                "/api/v1/auth/reset-password",
                json={"token": token, "newPassword": "NewPassword123!"},
            )
            assert resp_reset.status_code == 200

            # Old password fails
            resp_login_old = await client.post(
                "/api/v1/auth/login",
                json={"contact": "reset@example.com", "password": "OldPassword123!"},
            )
            assert resp_login_old.status_code == 401

            # New password succeeds
            resp_login_new = await client.post(
                "/api/v1/auth/login",
                json={"contact": "reset@example.com", "password": "NewPassword123!"},
            )
            assert resp_login_new.status_code == 200

            # Reusing reset token fails
            resp_reuse = await client.post(
                "/api/v1/auth/reset-password",
                json={"token": token, "newPassword": "AnotherPassword123!"},
            )
            assert resp_reuse.status_code == 400
        app.dependency_overrides.clear()

    async def test_cross_user_profile_and_chat_isolation(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            # Register User A
            reg_a = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "User Alpha",
                    "email": "alpha@example.com",
                    "password": "Password123!",
                },
            )
            token_a = reg_a.json()["accessToken"]

            # Register User B
            reg_b = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "User Beta",
                    "email": "beta@example.com",
                    "password": "Password123!",
                },
            )
            token_b = reg_b.json()["accessToken"]

            # User A creates a conversation
            conv_resp = await client.post(
                "/api/v1/chat/conversations",
                headers={"Authorization": f"Bearer {token_a}"},
            )
            assert conv_resp.status_code == 201
            conv_id = conv_resp.json()["id"]

            # User B attempts to access User A's conversation
            b_access = await client.get(
                f"/api/v1/chat/conversations/{conv_id}",
                headers={"Authorization": f"Bearer {token_b}"},
            )
            assert b_access.status_code in (403, 404)

            # User B attempts to send message to User A's conversation
            b_send = await client.post(
                f"/api/v1/chat/conversations/{conv_id}/messages",
                headers={"Authorization": f"Bearer {token_b}"},
                json={"content": "Hacking into conv", "explanation_mode": "general"},
            )
            assert b_send.status_code in (403, 404)
        app.dependency_overrides.clear()

    async def test_auth_bypass_security(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        async with AsyncClient(transport=transport, base_url="http://test") as client:
            # When allow_dev_user_id is False
            original_allow = settings.allow_dev_user_id
            original_env = settings.environment
            try:
                settings.allow_dev_user_id = False
                resp1 = await client.get("/api/v1/profile?user_id=some-dev-id")
                assert resp1.status_code == 401

                # When allow_dev_user_id is True but environment is production
                settings.allow_dev_user_id = True
                settings.environment = "production"
                resp2 = await client.get("/api/v1/profile?user_id=some-dev-id")
                assert resp2.status_code == 401
            finally:
                settings.allow_dev_user_id = original_allow
                settings.environment = original_env
        app.dependency_overrides.clear()

    async def test_rate_limiting_login(self, db_session):
        transport = ASGITransport(app=app)
        async def override_db():
            yield db_session
        app.dependency_overrides[get_db] = override_db

        limiter.enabled = True
        try:
            async with AsyncClient(transport=transport, base_url="http://test") as client:
                statuses = []
                for _ in range(7):
                    r = await client.post(
                        "/api/v1/auth/login",
                        json={"contact": "ratelimit_tester@example.com", "password": "WrongPassword!"},
                    )
                    statuses.append(r.status_code)
                # At least one request should have hit 429
                assert 429 in statuses
                idx_429 = statuses.index(429)
                assert idx_429 <= 5  # limit is 5/minute
        finally:
            limiter.enabled = False
        app.dependency_overrides.clear()
