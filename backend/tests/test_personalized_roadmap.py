"""Tests for Personalized Roadmap and Assessment endpoints"""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_start_assessment(client: AsyncClient):
    response = await client.post("/api/v1/assessment/start?domain=Python")
    assert response.status_code == 200
    data = response.json()
    assert data["domain"] == "Python"
    assert data["phase"] == "profile_collection"
    assert data["question_number"] == 1
    assert data["current_question"] is not None
    assert "goal" in data["current_question"]["prompt"].lower()


@pytest.mark.asyncio
async def test_profile_to_diagnostic_flow(client: AsyncClient):
    # 1. Start
    res1 = await client.post("/api/v1/assessment/start?domain=Python")
    session_id = res1.json()["session_id"]

    # 2. Answer Goal
    res2 = await client.post(
        f"/api/v1/assessment/{session_id}/answer",
        json={"answer": "Get a Software Developer job"}
    )
    assert res2.status_code == 200
    d2 = res2.json()
    assert d2["question_number"] == 2
    assert "experience" in d2["current_question"]["prompt"].lower()

    # 3. Answer Level
    res3 = await client.post(
        f"/api/v1/assessment/{session_id}/answer",
        json={"answer": "Intermediate / Advanced (built apps)"}
    )
    assert res3.status_code == 200
    d3 = res3.json()
    assert d3["question_number"] == 3
    assert "hours" in d3["current_question"]["prompt"].lower()

    # 4. Answer Study Hours -> Transitions to Diagnostic!
    res4 = await client.post(
        f"/api/v1/assessment/{session_id}/answer",
        json={"answer": "10 hours / week"}
    )
    assert res4.status_code == 200
    d4 = res4.json()
    assert d4["phase"] == "diagnostic"
    assert d4["question_number"] == 4
    assert d4["current_question"] is not None
    assert d4["current_question"]["question_type"] in ["code_output", "multiple_choice", "debugging"]


@pytest.mark.asyncio
async def test_diagnostic_grading_and_roadmap_generation(client: AsyncClient):
    # Start and complete profile
    res1 = await client.post("/api/v1/assessment/start?domain=Python")
    sid = res1.json()["session_id"]
    await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "Build web projects"})
    await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "Some Experience"})
    await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "6 hours / week"})

    # Answer diagnostic questions (4 questions: numbers 4, 5, 6, 7)
    for _ in range(4):
        s_res = await client.get(f"/api/v1/assessment/{sid}")
        curr_q = s_res.json()["current_question"]
        if not curr_q:
            break
        # Submit correct answer for first question, incorrect for second
        corr_idx = curr_q.get("correct_option_index", 0)
        await client.post(
            f"/api/v1/assessment/{sid}/answer",
            json={"answer": "My Answer", "quiz_selected_index": corr_idx}
        )

    # Check that session completed
    final_sess = await client.get(f"/api/v1/assessment/{sid}")
    sess_data = final_sess.json()
    assert sess_data["is_complete"] is True
    assert sess_data["phase"] == "summary"
    assert len(sess_data["mastery_map"]) > 0

    # Generate Personalized Roadmap
    gen_res = await client.post(f"/api/v1/assessment/{sid}/generate-roadmap")
    assert gen_res.status_code == 200
    roadmap = gen_res.json()
    assert roadmap["domain"] == "Python"
    assert len(roadmap["topics"]) >= 7
    assert len(roadmap["edges"]) >= 5
    assert roadmap["assessment_session_id"] == sid
    assert roadmap["estimated_weeks"] > 0

    # Verify first topic is unlocked
    first_topic = next(t for t in roadmap["topics"] if not t["prerequisite_topic_ids"])
    assert first_topic["status"] == "unlocked"


@pytest.mark.asyncio
async def test_validate_subject(client: AsyncClient):
    res = await client.post("/api/v1/assessment/validate-subject", json={"subject": "React"})
    assert res.status_code == 200
    assert res.json()["is_valid"] is True
    assert res.json()["standardized_name"] == "React"

    res_inv = await client.post("/api/v1/assessment/validate-subject", json={"subject": "xyznotarealsubject123"})
    assert res_inv.status_code == 200
    assert res_inv.json()["is_valid"] is False


@pytest.mark.asyncio
async def test_topic_learning_content_and_completion(client: AsyncClient):
    r = await client.post("/api/v1/assessment/start?domain=React")
    sid = r.json()["session_id"]
    await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "Frontend job"})
    await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "Beginner"})
    await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "5 hours / week"})
    for _ in range(4):
        await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "A", "quiz_selected_index": 0})

    gen_res = await client.post(f"/api/v1/assessment/{sid}/generate-roadmap")
    assert gen_res.status_code == 200
    rm = gen_res.json()
    rm_id = rm["id"]
    first_topic_id = rm["topics"][0]["id"]

    # Test getting topic content
    content_res = await client.get(f"/api/v1/personalized-roadmaps/{rm_id}/topics/{first_topic_id}/content")
    assert content_res.status_code == 200
    content = content_res.json()
    assert content["topic_id"] == first_topic_id
    assert len(content["learning_objectives"]) > 0
    assert len(content["resources"]) > 0

    # Test topic completion and progression
    comp_res = await client.post(
        f"/api/v1/personalized-roadmaps/{rm_id}/topics/{first_topic_id}/complete",
        json={"quiz_passed": True}
    )
    assert comp_res.status_code == 200
    comp_data = comp_res.json()
    assert first_topic_id in comp_data["roadmap"]["completed_topic_ids"]
    assert comp_data["overall_progress_percent"] > 0



@pytest.mark.asyncio
async def test_personalized_roadmap_crud(client: AsyncClient):
    # Start and generate roadmap
    r = await client.post("/api/v1/assessment/start?domain=Python")
    sid = r.json()["session_id"]
    await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "Get a job"})
    await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "Beginner"})
    await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "5 hours / week"})
    for _ in range(4):
        await client.post(f"/api/v1/assessment/{sid}/answer", json={"answer": "A", "quiz_selected_index": 0})

    gen_res = await client.post(f"/api/v1/assessment/{sid}/generate-roadmap")
    rm_id = gen_res.json()["id"]

    # 1. Get Detail
    detail_res = await client.get(f"/api/v1/personalized-roadmaps/{rm_id}")
    assert detail_res.status_code == 200
    assert detail_res.json()["id"] == rm_id

    # 2. Rename
    rename_res = await client.patch(
        f"/api/v1/personalized-roadmaps/{rm_id}/rename",
        json={"title": "Updated Python Path"}
    )
    assert rename_res.status_code == 200
    assert rename_res.json()["title"] == "Updated Python Path"

    # 3. List
    list_res = await client.get("/api/v1/personalized-roadmaps")
    assert list_res.status_code == 200
    assert any(rm["id"] == rm_id for rm in list_res.json())

    # 4. Delete
    del_res = await client.delete(f"/api/v1/personalized-roadmaps/{rm_id}")
    assert del_res.status_code == 204

    # 5. Confirm Deleted
    not_found = await client.get(f"/api/v1/personalized-roadmaps/{rm_id}")
    assert not_found.status_code == 404
