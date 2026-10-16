"""Tests for user profile, learning progress, and email delivery"""

import pytest
from httpx import AsyncClient, ASGITransport

from app.main import app
from app.core.database import get_db
from app.models.course import Course, CourseCategory
from app.core.rate_limit import limiter


@pytest.fixture(autouse=True)
def reset_rate_limits():
    limiter.enabled = False
    yield
    limiter.enabled = True


@pytest.mark.asyncio
async def test_get_and_update_profile(db_session):
    async def override_db():
        yield db_session
    app.dependency_overrides[get_db] = override_db

    try:
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            # 1. Register a user
            reg_resp = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "Aarav Sharma",
                    "email": "aarav@example.com",
                    "mobile": "9876543210",
                    "password": "Password123!",
                },
            )
            assert reg_resp.status_code == 201
            token = reg_resp.json()["accessToken"]

            # 2. GET profile
            get_resp = await client.get(
                "/api/v1/profile",
                headers={"Authorization": f"Bearer {token}"},
            )
            assert get_resp.status_code == 200
            data = get_resp.json()
            assert data["fullName"] == "Aarav Sharma"
            assert data["email"] == "aarav@example.com"
            assert data["educationMode"] == "general"

            # 3. PUT profile with school mode
            put_resp = await client.put(
                "/api/v1/profile",
                headers={"Authorization": f"Bearer {token}"},
                json={
                    "fullName": "Aarav Sharma",
                    "email": "aarav@example.com",
                    "mobile": "9876543210",
                    "interests": "Science, Physics",
                    "level": "Class 10",
                    "educationMode": "school",
                },
            )
            assert put_resp.status_code == 200
            put_data = put_resp.json()
            assert put_data["educationMode"] == "school"
            assert put_data["level"] == "Class 10"

            # 4. Verify GET profile returns updated values
            get_resp_after = await client.get(
                "/api/v1/profile",
                headers={"Authorization": f"Bearer {token}"},
            )
            assert get_resp_after.status_code == 200
            assert get_resp_after.json()["educationMode"] == "school"
    finally:
        app.dependency_overrides.clear()


@pytest.mark.asyncio
async def test_reset_password_web_page():
    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test",
    ) as client:
        resp = await client.get("/api/v1/auth/reset-password?token=sample-test-token")
        assert resp.status_code == 200
        assert "text/html" in resp.headers["content-type"]
        assert "EduNova" in resp.text
        assert "sample-test-token" in resp.text


@pytest.mark.asyncio
async def test_learning_progress_endpoints_and_isolation(db_session):
    async def override_db():
        yield db_session
    app.dependency_overrides[get_db] = override_db

    try:
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            # Register User A
            reg_a = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "User A",
                    "email": "user_a_learn@example.com",
                    "password": "Password123!",
                },
            )
            assert reg_a.status_code == 201
            token_a = reg_a.json()["accessToken"]

            # Register User B
            reg_b = await client.post(
                "/api/v1/auth/register",
                json={
                    "fullName": "User B",
                    "email": "user_b_learn@example.com",
                    "password": "Password123!",
                },
            )
            assert reg_b.status_code == 201
            token_b = reg_b.json()["accessToken"]

            # Check in-progress for User A (empty)
            res_a_empty = await client.get(
                "/api/v1/learning/courses/in-progress",
                headers={"Authorization": f"Bearer {token_a}"},
            )
            assert res_a_empty.status_code == 200
            assert res_a_empty.json() == []

            # Create category and course in DB
            cat = CourseCategory(id="cat-math", name="Mathematics", description="Math courses")
            db_session.add(cat)
            course = Course(
                id="course-algebra-101",
                name="Algebra 101",
                slug="algebra-101",
                category_id="cat-math",
                description="Basic Algebra",
                level="Beginner",
                is_featured=True,
                icon="calc",
                accent_color="primary",
            )
            db_session.add(course)
            await db_session.commit()

            # Enroll User A in course
            enroll_res = await client.post(
                f"/api/v1/learning/courses/{course.id}/enroll",
                headers={"Authorization": f"Bearer {token_a}"},
            )
            assert enroll_res.status_code == 200

            # User A should now see this in in-progress
            res_a = await client.get(
                "/api/v1/learning/courses/in-progress",
                headers={"Authorization": f"Bearer {token_a}"},
            )
            assert res_a.status_code == 200
            courses_a = res_a.json()
            assert len(courses_a) == 1
            assert courses_a[0]["id"] == "course-algebra-101"
            assert courses_a[0]["name"] == "Algebra 101"

            # User B should NOT see User A's enrolled course
            res_b = await client.get(
                "/api/v1/learning/courses/in-progress",
                headers={"Authorization": f"Bearer {token_b}"},
            )
            assert res_b.status_code == 200
            assert res_b.json() == []
    finally:
        app.dependency_overrides.clear()
