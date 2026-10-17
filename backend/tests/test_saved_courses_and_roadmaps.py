"""Tests for Saved Courses and Saved Roadmaps API endpoints and access control"""

import pytest
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import AsyncSession
from app.main import app
from app.core.database import get_db
from app.core.deps import get_current_user_id
from app.core.security import create_access_token
from app.models import CourseCategory, Course, CourseModule, Lesson, Roadmap, RoadmapItem


def auth_headers(user_id: str) -> dict:
    """Helper to generate authentic Bearer token headers for a user"""
    token = create_access_token(user_id=user_id, email=f"{user_id}@example.com")
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def raw_client(db_session: AsyncSession):
    """Client with DB override only, no auth overrides"""
    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides.pop(get_current_user_id, None)
    transport = ASGITransport(app=app)
    return AsyncClient(transport=transport, base_url="http://test")


@pytest.fixture(autouse=True)
async def cleanup_db(db_session: AsyncSession):
    from sqlalchemy import text
    # Clean before and after
    await db_session.execute(text("DELETE FROM user_saved_roadmaps"))
    await db_session.execute(text("DELETE FROM user_saved_courses"))
    await db_session.execute(text("DELETE FROM roadmap_items"))
    await db_session.execute(text("DELETE FROM roadmaps"))
    await db_session.execute(text("DELETE FROM lessons"))
    await db_session.execute(text("DELETE FROM course_modules"))
    await db_session.execute(text("DELETE FROM courses"))
    await db_session.execute(text("DELETE FROM course_categories"))
    await db_session.commit()
    yield
    await db_session.execute(text("DELETE FROM user_saved_roadmaps"))
    await db_session.execute(text("DELETE FROM user_saved_courses"))
    await db_session.execute(text("DELETE FROM roadmap_items"))
    await db_session.execute(text("DELETE FROM roadmaps"))
    await db_session.execute(text("DELETE FROM lessons"))
    await db_session.execute(text("DELETE FROM course_modules"))
    await db_session.execute(text("DELETE FROM courses"))
    await db_session.execute(text("DELETE FROM course_categories"))
    await db_session.commit()



@pytest.mark.asyncio
async def test_saved_courses_crud_and_idempotency(raw_client: AsyncClient, db_session: AsyncSession):
    """Test authenticated save, list, duplicate save, unsave, and repeated unsave for courses"""
    headers = auth_headers("user-alice")

    # 1. Setup category and course
    cat = CourseCategory(id="cat-py", name="Python", slug="python", display_order=0)
    course = Course(
        id="course-py-101",
        category_id="cat-py",
        name="Python Programming",
        slug="python-programming",
        description="Learn Python from scratch",
        icon="python",
        accent_color="blue",
        level="Beginner",
        duration_hours=20,
        is_featured=True,
        status="active"
    )
    mod = CourseModule(id="mod-1", course_id="course-py-101", title="Basics", display_order=0)
    les = Lesson(id="les-1", module_id="mod-1", title="Intro", content_type="video", display_order=0)
    db_session.add_all([cat, course, mod, les])
    await db_session.commit()

    # 2. Initially empty
    resp = await raw_client.get("/api/v1/courses/saved", headers=headers)
    assert resp.status_code == 200
    assert resp.json() == []

    # 3. Save course
    resp = await raw_client.post("/api/v1/courses/course-py-101/save", headers=headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["courseId"] == "course-py-101"
    assert data["userId"] == "user-alice"
    assert data["name"] == "Python Programming"
    assert data["accent"] == "blue"
    assert data["totalLessons"] == 1
    assert "savedAt" in data

    # 4. List saved courses
    resp = await raw_client.get("/api/v1/courses/saved", headers=headers)
    assert resp.status_code == 200
    saved_list = resp.json()
    assert len(saved_list) == 1
    assert saved_list[0]["courseId"] == "course-py-101"

    # 5. Duplicate save is safe and idempotent
    resp = await raw_client.post("/api/v1/courses/course-py-101/save", headers=headers)
    assert resp.status_code == 200
    assert resp.json()["courseId"] == "course-py-101"

    # List still has exactly 1 course
    resp = await raw_client.get("/api/v1/courses/saved", headers=headers)
    assert len(resp.json()) == 1

    # 6. Unsave course
    resp = await raw_client.delete("/api/v1/courses/course-py-101/save", headers=headers)
    assert resp.status_code == 200
    assert resp.json()["success"] is True
    assert resp.json()["isSaved"] is False

    # List is now empty
    resp = await raw_client.get("/api/v1/courses/saved", headers=headers)
    assert resp.status_code == 200
    assert resp.json() == []

    # 7. Repeated unsave is safe and idempotent
    resp = await raw_client.delete("/api/v1/courses/course-py-101/save", headers=headers)
    assert resp.status_code == 200
    assert resp.json()["success"] is True
    assert resp.json()["isSaved"] is False


@pytest.mark.asyncio
async def test_saved_courses_user_isolation(raw_client: AsyncClient, db_session: AsyncSession):
    """Test that saved courses are strictly isolated between users"""
    headers_a = auth_headers("user-a")
    headers_b = auth_headers("user-b")

    cat = CourseCategory(id="cat-iso", name="Isolation", slug="isolation", display_order=0)
    course_a = Course(
        id="c-iso-a", category_id="cat-iso", name="Course A", slug="course-a",
        description="A", icon="icon", accent_color="blue", level="Beginner", status="active"
    )
    course_b = Course(
        id="c-iso-b", category_id="cat-iso", name="Course B", slug="course-b",
        description="B", icon="icon", accent_color="green", level="Beginner", status="active"
    )
    db_session.add_all([cat, course_a, course_b])
    await db_session.commit()

    # User A saves Course A
    await raw_client.post("/api/v1/courses/c-iso-a/save", headers=headers_a)

    # User B should have 0 saved courses
    resp_b = await raw_client.get("/api/v1/courses/saved", headers=headers_b)
    assert resp_b.status_code == 200
    assert resp_b.json() == []

    # User B saves Course B
    await raw_client.post("/api/v1/courses/c-iso-b/save", headers=headers_b)

    # User A should only see Course A
    resp_a = await raw_client.get("/api/v1/courses/saved", headers=headers_a)
    assert len(resp_a.json()) == 1
    assert resp_a.json()[0]["courseId"] == "c-iso-a"

    # User B should only see Course B
    resp_b = await raw_client.get("/api/v1/courses/saved", headers=headers_b)
    assert len(resp_b.json()) == 1
    assert resp_b.json()[0]["courseId"] == "c-iso-b"


@pytest.mark.asyncio
async def test_saved_courses_errors(raw_client: AsyncClient):
    """Test 401 unauthenticated and 404 invalid course ID"""
    headers = auth_headers("user-test")

    # Invalid ID on save
    resp = await raw_client.post("/api/v1/courses/non-existent-id/save", headers=headers)
    assert resp.status_code == 404
    assert "Course not found" in str(resp.json())

    # Invalid ID on unsave
    resp = await raw_client.delete("/api/v1/courses/non-existent-id/save", headers=headers)
    assert resp.status_code == 404
    assert "Course not found" in str(resp.json())

    # Unauthenticated requests
    resp = await raw_client.get("/api/v1/courses/saved")
    assert resp.status_code == 401

    resp = await raw_client.post("/api/v1/courses/any-id/save")
    assert resp.status_code == 401

    resp = await raw_client.delete("/api/v1/courses/any-id/save")
    assert resp.status_code == 401


@pytest.mark.asyncio
async def test_saved_system_roadmaps_crud(raw_client: AsyncClient, db_session: AsyncSession):
    """Test saving, listing, and unsaving public system roadmaps"""
    headers = auth_headers("user-bob")

    roadmap = Roadmap(
        id="sys-roadmap-1",
        title="Frontend Web Developer",
        slug="frontend-web-developer",
        category="Web Development",
        description="Learn HTML, CSS, JS, and React",
        level="Beginner",
        duration="12 weeks",
        icon="web",
        accent_theme="primary",
        is_system=True,
        user_id=None
    )
    item1 = RoadmapItem(id="r-item-1", roadmap_id="sys-roadmap-1", title="HTML & CSS", display_order=0, skills=["HTML", "CSS"])
    item2 = RoadmapItem(id="r-item-2", roadmap_id="sys-roadmap-1", title="JavaScript", display_order=1, skills=["JS"])
    db_session.add_all([roadmap, item1, item2])
    await db_session.commit()

    # 1. Initially empty
    resp = await raw_client.get("/api/v1/roadmaps/saved", headers=headers)
    assert resp.status_code == 200
    assert resp.json() == []

    # 2. Save system roadmap
    resp = await raw_client.post("/api/v1/roadmaps/sys-roadmap-1/save", headers=headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["roadmapId"] == "sys-roadmap-1"
    assert data["roadmapType"] == "system"
    assert data["title"] == "Frontend Web Developer"
    assert data["stages"] == 2
    assert "HTML" in data["skills"]

    # 3. List saved roadmaps
    resp = await raw_client.get("/api/v1/roadmaps/saved", headers=headers)
    assert len(resp.json()) == 1
    assert resp.json()[0]["roadmapId"] == "sys-roadmap-1"

    # 4. Duplicate save is idempotent
    resp = await raw_client.post("/api/v1/roadmaps/sys-roadmap-1/save", headers=headers)
    assert resp.status_code == 200
    assert len((await raw_client.get("/api/v1/roadmaps/saved", headers=headers)).json()) == 1

    # 5. Unsave system roadmap
    resp = await raw_client.delete("/api/v1/roadmaps/sys-roadmap-1/save", headers=headers)
    assert resp.status_code == 200
    assert resp.json()["success"] is True
    assert resp.json()["isSaved"] is False

    # List is empty
    resp = await raw_client.get("/api/v1/roadmaps/saved", headers=headers)
    assert resp.json() == []

    # 6. Repeated unsave is idempotent
    resp = await raw_client.delete("/api/v1/roadmaps/sys-roadmap-1/save", headers=headers)
    assert resp.status_code == 200


@pytest.mark.asyncio
async def test_saved_personalized_roadmaps_and_cross_user_denial(raw_client: AsyncClient, db_session: AsyncSession):
    """
    Test personalized roadmap saving:
    - Owner (user-owner) can save/unsave their personalized roadmap.
    - Other users (user-stranger) are denied with HTTP 403.
    """
    headers_owner = auth_headers("user-owner")
    headers_stranger = auth_headers("user-stranger")

    # Personalized roadmap created by user-owner
    pers_roadmap = Roadmap(
        id="pers-roadmap-123",
        title="Custom AI Engineer",
        slug="pers-custom-ai-123",
        category="AI",
        description="Personalized path for user-owner",
        level="Intermediate",
        duration="8 weeks",
        icon="school",
        accent_theme="primary",
        is_system=False,
        user_id="user-owner",
        structure={
            "phases": [
                {"title": "Phase 1: Math", "durationWeeks": 4},
                {"title": "Phase 2: PyTorch", "durationWeeks": 4}
            ]
        }
    )
    db_session.add(pers_roadmap)
    await db_session.commit()

    # 1. Stranger tries to save Owner's personalized roadmap -> 403 Forbidden
    resp = await raw_client.post("/api/v1/roadmaps/pers-roadmap-123/save", headers=headers_stranger)
    assert resp.status_code == 403
    assert "Access denied" in str(resp.json())

    # Stranger tries to unsave Owner's personalized roadmap -> 403 Forbidden
    resp = await raw_client.delete("/api/v1/roadmaps/pers-roadmap-123/save", headers=headers_stranger)
    assert resp.status_code == 403
    assert "Access denied" in str(resp.json())

    # Stranger's saved list is empty
    assert (await raw_client.get("/api/v1/roadmaps/saved", headers=headers_stranger)).json() == []

    # 2. Owner saves their personalized roadmap -> 200 OK
    resp = await raw_client.post("/api/v1/roadmaps/pers-roadmap-123/save", headers=headers_owner)
    assert resp.status_code == 200
    data = resp.json()
    assert data["roadmapId"] == "pers-roadmap-123"
    assert data["roadmapType"] == "personalized"
    assert data["stages"] == 2

    # Owner sees it in saved list
    saved_owner = (await raw_client.get("/api/v1/roadmaps/saved", headers=headers_owner)).json()
    assert len(saved_owner) == 1
    assert saved_owner[0]["roadmapId"] == "pers-roadmap-123"

    # Stranger STILL sees empty saved list
    assert (await raw_client.get("/api/v1/roadmaps/saved", headers=headers_stranger)).json() == []

    # 3. Owner unsaves their personalized roadmap
    resp = await raw_client.delete("/api/v1/roadmaps/pers-roadmap-123/save", headers=headers_owner)
    assert resp.status_code == 200
    assert (await raw_client.get("/api/v1/roadmaps/saved", headers=headers_owner)).json() == []


@pytest.mark.asyncio
async def test_saved_roadmaps_errors(raw_client: AsyncClient):
    """Test 401 unauthenticated and 404 invalid roadmap ID"""
    headers = auth_headers("user-test")

    # Invalid ID on save
    resp = await raw_client.post("/api/v1/roadmaps/non-existent-roadmap/save", headers=headers)
    assert resp.status_code == 404
    assert "Roadmap not found" in str(resp.json())

    # Invalid ID on unsave
    resp = await raw_client.delete("/api/v1/roadmaps/non-existent-roadmap/save", headers=headers)
    assert resp.status_code == 404
    assert "Roadmap not found" in str(resp.json())

    # Unauthenticated requests
    resp = await raw_client.get("/api/v1/roadmaps/saved")
    assert resp.status_code == 401

    resp = await raw_client.post("/api/v1/roadmaps/any-id/save")
    assert resp.status_code == 401

    resp = await raw_client.delete("/api/v1/roadmaps/any-id/save")
    assert resp.status_code == 401
