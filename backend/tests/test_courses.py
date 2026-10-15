"""Tests for Course API endpoints and schemas"""

import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from app.models import CourseCategory, Course, CourseModule, Lesson


@pytest.mark.asyncio
async def test_get_courses_empty(client: AsyncClient):
    """Test get courses returns empty list when no courses exist"""
    response = await client.get("/api/v1/courses")
    assert response.status_code == 200
    assert response.json() == []


@pytest.mark.asyncio
async def test_get_courses_catalogue_and_filtering(client: AsyncClient, db_session: AsyncSession):
    """Test course catalogue, field population, category filtering, and featured filtering"""
    # 1. Create categories
    cat_prog = CourseCategory(
        id="cat-1",
        name="Programming",
        slug="programming",
        description="Programming languages",
        display_order=0
    )
    cat_data = CourseCategory(
        id="cat-2",
        name="Data & AI",
        slug="data-ai",
        description="Data and artificial intelligence",
        display_order=1
    )
    db_session.add_all([cat_prog, cat_data])
    await db_session.flush()

    # 2. Create courses
    course1 = Course(
        id="course-cpp-uuid",
        category_id="cat-1",
        name="C++ Programming",
        slug="cpp-programming",
        description="High performance systems",
        icon="cpp",
        level="Intermediate",
        duration_hours=40,
        accent_color="blue",
        is_featured=True,
        prerequisites=["Basic literacy"],
        status="active"
    )
    course2 = Course(
        id="course-py-uuid",
        category_id="cat-1",
        name="Python Fundamentals",
        slug="python-fundamentals",
        description="Python basics",
        icon="python",
        level="Beginner",
        duration_hours=30,
        accent_color="blue",
        is_featured=False,
        prerequisites=["None"],
        status="active"
    )
    course3 = Course(
        id="course-ds-uuid",
        category_id="cat-2",
        name="Data Science with Python",
        slug="data-science-python",
        description="Data science workflows",
        icon="data_science",
        level="Intermediate",
        duration_hours=60,
        accent_color="purple",
        is_featured=True,
        prerequisites=["Python proficiency"],
        status="active"
    )
    db_session.add_all([course1, course2, course3])
    await db_session.commit()

    # Test full catalogue
    response = await client.get("/api/v1/courses")
    assert response.status_code == 200
    data = response.json()
    assert len(data) == 3

    # Check required fields on first course
    first = next(c for c in data if c["id"] == "course-cpp-uuid")
    assert first["name"] == "C++ Programming"
    assert first["slug"] == "cpp-programming"
    assert first["description"] == "High performance systems"
    assert first["icon"] == "cpp"
    assert first["accentColor"] == "blue"
    assert first["accent"] == "blue"  # Backwards compatibility alias
    assert first["category"] == "Programming"
    assert first["categorySlug"] == "programming"
    assert first["level"] == "Intermediate"
    assert first["durationHours"] == 40
    assert first["isFeatured"] is True

    # Test featured filter True
    res_featured = await client.get("/api/v1/courses?featured=true")
    assert res_featured.status_code == 200
    featured_data = res_featured.json()
    assert len(featured_data) == 2
    assert all(c["isFeatured"] is True for c in featured_data)

    # Test featured filter False
    res_non_featured = await client.get("/api/v1/courses?featured=false")
    assert res_non_featured.status_code == 200
    non_featured_data = res_non_featured.json()
    assert len(non_featured_data) == 1
    assert non_featured_data[0]["slug"] == "python-fundamentals"
    assert non_featured_data[0]["isFeatured"] is False

    # Test category slug filter
    res_category = await client.get("/api/v1/courses?category=data-ai")
    assert res_category.status_code == 200
    category_data = res_category.json()
    assert len(category_data) == 1
    assert category_data[0]["slug"] == "data-science-python"
    assert category_data[0]["categorySlug"] == "data-ai"


@pytest.mark.asyncio
async def test_get_course_detail_with_modules_lessons_and_ordering(client: AsyncClient, db_session: AsyncSession):
    """Test course detail loads prerequisites, ordered modules, and ordered lessons with resources"""
    category = CourseCategory(
        id="cat-detail-1",
        name="Computer Science",
        slug="computer-science",
        description="CS Core",
        display_order=0
    )
    db_session.add(category)
    await db_session.flush()

    course = Course(
        id="course-dsa-uuid",
        category_id="cat-detail-1",
        name="Data Structures & Algorithms",
        slug="data-structures-algorithms",
        description="Master computational problem-solving",
        icon="algorithms",
        level="Intermediate",
        duration_hours=50,
        accent_color="blue",
        is_featured=False,
        prerequisites=["Basic programming in any language", "Algebra"],
        status="active"
    )
    db_session.add(course)
    await db_session.flush()

    # Add modules in intentionally non-sequential insert order to test ordering
    mod2 = CourseModule(
        id="mod-dsa-2",
        course_id=course.id,
        title="Non-Linear Data Structures",
        description="Trees and Graphs",
        display_order=2
    )
    mod1 = CourseModule(
        id="mod-dsa-1",
        course_id=course.id,
        title="Linear Data Structures",
        description="Arrays and Linked Lists",
        display_order=1
    )
    db_session.add_all([mod2, mod1])
    await db_session.flush()

    # Add lessons in non-sequential insert order
    les1_2 = Lesson(
        id="les-1-2",
        module_id=mod1.id,
        title="Linked Lists",
        description="Nodes and pointers",
        content_type="article",
        duration_minutes=30,
        display_order=2,
        resource_url="https://www.geeksforgeeks.org/data-structures/linked-list/",
        resource_provider="GeeksforGeeks"
    )
    les1_1 = Lesson(
        id="les-1-1",
        module_id=mod1.id,
        title="Big-O Analysis",
        description="Asymptotic bounds",
        content_type="article",
        duration_minutes=20,
        display_order=1,
        resource_url="https://en.wikipedia.org/wiki/Big_O_notation",
        resource_provider="Wikipedia"
    )
    les2_1 = Lesson(
        id="les-2-1",
        module_id=mod2.id,
        title="Binary Trees",
        description="Tree traversal",
        content_type="video",
        duration_minutes=25,
        display_order=1,
        resource_url="invalid-not-http-url",  # Should be validated to None
        resource_provider="YouTube"
    )
    les2_2 = Lesson(
        id="les-2-2",
        module_id=mod2.id,
        title="Graph Traversals",
        description="BFS and DFS",
        content_type="quiz",
        duration_minutes=15,
        display_order=2,
        resource_url=None,  # Null resource
        resource_provider=None
    )
    db_session.add_all([les1_2, les1_1, les2_1, les2_2])
    await db_session.commit()

    # 1. Test by primary key ID
    res_by_id = await client.get(f"/api/v1/courses/{course.id}")
    assert res_by_id.status_code == 200
    detail = res_by_id.json()

    assert detail["id"] == course.id
    assert detail["name"] == "Data Structures & Algorithms"
    assert detail["slug"] == "data-structures-algorithms"
    assert detail["accentColor"] == "blue"
    assert detail["accent"] == "blue"
    assert detail["category"] == "Computer Science"
    assert detail["categorySlug"] == "computer-science"
    assert detail["prerequisites"] == ["Basic programming in any language", "Algebra"]

    # Verify modules order
    modules = detail["modules"]
    assert len(modules) == 2
    assert modules[0]["id"] == "mod-dsa-1"
    assert modules[0]["displayOrder"] == 1
    assert modules[1]["id"] == "mod-dsa-2"
    assert modules[1]["displayOrder"] == 2

    # Verify lessons order and resource validation
    mod1_lessons = modules[0]["lessons"]
    assert len(mod1_lessons) == 2
    assert mod1_lessons[0]["id"] == "les-1-1"
    assert mod1_lessons[0]["displayOrder"] == 1
    assert mod1_lessons[0]["resourceUrl"] == "https://en.wikipedia.org/wiki/Big_O_notation"
    assert mod1_lessons[0]["resourceProvider"] == "Wikipedia"

    assert mod1_lessons[1]["id"] == "les-1-2"
    assert mod1_lessons[1]["displayOrder"] == 2
    assert mod1_lessons[1]["resourceUrl"] == "https://www.geeksforgeeks.org/data-structures/linked-list/"
    assert mod1_lessons[1]["resourceProvider"] == "GeeksforGeeks"

    # Verify invalid URL is filtered to None without fabricating
    mod2_lessons = modules[1]["lessons"]
    assert mod2_lessons[0]["id"] == "les-2-1"
    assert mod2_lessons[0]["resourceUrl"] is None  # invalid-not-http-url filtered to None
    assert mod2_lessons[1]["id"] == "les-2-2"
    assert mod2_lessons[1]["resourceUrl"] is None

    assert detail["totalLessons"] == 4
    assert detail["modulesCount"] == 2

    # 2. Test lookup by slug
    res_by_slug = await client.get("/api/v1/courses/data-structures-algorithms")
    assert res_by_slug.status_code == 200
    assert res_by_slug.json()["id"] == course.id

    # 3. Test lookup by canonical Android track alias ("c-dsa")
    res_by_alias = await client.get("/api/v1/courses/c-dsa")
    assert res_by_alias.status_code == 200
    assert res_by_alias.json()["id"] == course.id


@pytest.mark.asyncio
async def test_get_course_detail_unknown_id(client: AsyncClient):
    """Test unknown course ID returns standard 404 response"""
    response = await client.get("/api/v1/courses/non-existent-course-id-999")
    assert response.status_code == 404
    assert response.json() == {"detail": "Course not found"}


@pytest.mark.asyncio
async def test_all_canonical_track_aliases_map_correctly():
    """Verify Android 7 canonical track IDs map to valid backend slugs"""
    from app.repositories.course_repository import ANDROID_TRACK_SLUG_MAP
    canonical_ids = ["c-cpp", "c-dsa", "c-ds", "c-py", "c-ml", "c-fs", "c-sde"]
    for cid in canonical_ids:
        assert cid in ANDROID_TRACK_SLUG_MAP
        assert len(ANDROID_TRACK_SLUG_MAP[cid]) > 0


@pytest.mark.asyncio
async def test_lesson_video_resources_serialization_and_validation(client: AsyncClient, db_session: AsyncSession):
    """Test lessons with video resources serialize correctly, validate URLs, and preserve reading links"""
    cat = CourseCategory(
        id="cat-video-test",
        name="Video Test Category",
        slug="programming-video",
        description="Programming category",
        display_order=0
    )
    db_session.add(cat)
    await db_session.flush()

    course = Course(
        id="course-video-test",
        category_id=cat.id,
        name="C++ Video Test Course",
        slug="cpp-video-test",
        description="Testing video resources",
        icon="cpp",
        level="Beginner",
        duration_hours=20,
        accent_color="blue",
        is_featured=False,
        status="active"
    )
    db_session.add(course)
    await db_session.flush()

    mod = CourseModule(
        id="mod-video-test",
        course_id=course.id,
        title="Intro Module",
        display_order=1
    )
    db_session.add(mod)
    await db_session.flush()

    # Lesson 1: Has BOTH reading resource and valid YouTube video resource
    les1 = Lesson(
        id="les-v1",
        module_id=mod.id,
        title="Lesson 1 - Dual Resources",
        content_type="video",
        display_order=1,
        resource_url="https://en.cppreference.com/w/cpp/language/basic_concepts",
        resource_provider="cppreference.com",
        video_url="https://www.youtube.com/watch?v=vLnPwxZdW4Y",
        video_provider="freeCodeCamp.org"
    )
    # Lesson 2: Has reading resource only (backward compatibility test - video_url is None)
    les2 = Lesson(
        id="les-v2",
        module_id=mod.id,
        title="Lesson 2 - Reading Only",
        content_type="article",
        display_order=2,
        resource_url="https://www.learncpp.com/cpp-tutorial/introduction-to-functions/",
        resource_provider="LearnCpp.com",
        video_url=None,
        video_provider=None
    )
    # Lesson 3: Has invalid video URL (should sanitize to None) and youtu.be shortlink
    les3 = Lesson(
        id="les-v3",
        module_id=mod.id,
        title="Lesson 3 - YouTu.be Shortlink",
        content_type="video",
        display_order=3,
        resource_url=None,
        resource_provider=None,
        video_url="https://youtu.be/_uQrJ0TkZlc",
        video_provider="Programming with Mosh"
    )
    # Lesson 4: Has non-YouTube URL in video_url (should sanitize to None)
    les4 = Lesson(
        id="les-v4",
        module_id=mod.id,
        title="Lesson 4 - Invalid Non-YouTube Video URL",
        content_type="video",
        display_order=4,
        resource_url="https://docs.python.org/3/",
        resource_provider="Python Docs",
        video_url="https://vimeo.com/12345678",  # Non-YouTube domain
        video_provider="Vimeo"
    )
    db_session.add_all([les1, les2, les3, les4])
    await db_session.commit()

    res = await client.get(f"/api/v1/courses/{course.id}")
    assert res.status_code == 200
    data = res.json()
    lessons = data["modules"][0]["lessons"]

    # Verify Lesson 1: Dual resources preserved
    assert lessons[0]["id"] == "les-v1"
    assert lessons[0]["resourceUrl"] == "https://en.cppreference.com/w/cpp/language/basic_concepts"
    assert lessons[0]["resourceProvider"] == "cppreference.com"
    assert lessons[0]["videoUrl"] == "https://www.youtube.com/watch?v=vLnPwxZdW4Y"
    assert lessons[0]["videoProvider"] == "freeCodeCamp.org"

    # Verify Lesson 2: Reading only, video is null
    assert lessons[1]["id"] == "les-v2"
    assert lessons[1]["resourceUrl"] == "https://www.learncpp.com/cpp-tutorial/introduction-to-functions/"
    assert lessons[1]["resourceProvider"] == "LearnCpp.com"
    assert lessons[1]["videoUrl"] is None
    assert lessons[1]["videoProvider"] is None

    # Verify Lesson 3: youtu.be domain accepted
    assert lessons[2]["id"] == "les-v3"
    assert lessons[2]["resourceUrl"] is None
    assert lessons[2]["videoUrl"] == "https://youtu.be/_uQrJ0TkZlc"
    assert lessons[2]["videoProvider"] == "Programming with Mosh"

    # Verify Lesson 4: Non-YouTube video URL sanitized to None, reading resource intact
    assert lessons[3]["id"] == "les-v4"
    assert lessons[3]["resourceUrl"] == "https://docs.python.org/3/"
    assert lessons[3]["resourceProvider"] == "Python Docs"
    assert lessons[3]["videoUrl"] is None  # Non-YouTube filtered out


def test_lesson_response_schema_validation_unit():
    """Unit test Pydantic schema validation for videoUrl and resourceUrl"""
    from app.schemas.course import LessonResponse

    # Valid YouTube URLs
    l1 = LessonResponse(
        id="1",
        title="Test",
        video_url="https://www.youtube.com/watch?v=123",
        video_provider="YouTube"
    )
    assert l1.video_url == "https://www.youtube.com/watch?v=123"

    l2 = LessonResponse(
        id="2",
        title="Test",
        video_url="https://youtu.be/abc",
        video_provider="YouTube"
    )
    assert l2.video_url == "https://youtu.be/abc"

    # Playlist URL
    l3 = LessonResponse(
        id="3",
        title="Test",
        video_url="https://www.youtube.com/playlist?list=PL123",
        video_provider="YouTube"
    )
    assert l3.video_url == "https://www.youtube.com/playlist?list=PL123"

    # Invalid video URLs should be sanitized to None
    l4 = LessonResponse(id="4", title="Test", video_url="javascript:alert(1)")
    assert l4.video_url is None

    l5 = LessonResponse(id="5", title="Test", video_url="https://notyoutube.com/watch?v=1")
    assert l5.video_url is None

    l6 = LessonResponse(id="6", title="Test", video_url="   ")
    assert l6.video_url is None

    l7 = LessonResponse(id="7", title="Test", video_url=None)
    assert l7.video_url is None
