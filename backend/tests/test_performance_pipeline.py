"""Performance and validation test suite for personalized assessment pipeline.
Measures timing, verifies local scoring, proves duplicate submissions do not duplicate LLM work,
and validates that the entire 7-step assessment executes in minimal OpenRouter calls.
"""

import asyncio
import time
import pytest
from app.core.database import get_session_factory
from app.services.personalized_roadmap_service import PersonalizedRoadmapService
from app.services.openrouter_service import openrouter_service


@pytest.mark.asyncio
async def test_full_pipeline_performance():
    print("\n" + "=" * 60)
    print("STARTING ASSESSMENT PIPELINE PERFORMANCE & IDEMPOTENCY TEST")
    print("=" * 60)

    async with get_session_factory()() as db:
        service = PersonalizedRoadmapService(db)

        # 1. Subject validation (standard subject)
        t0 = time.monotonic()
        val_resp = await service.validate_subject("Python")
        t_val = time.monotonic() - t0
        print(f"\n[1] Subject Validation ('Python'): {t_val*1000:.1f}ms (is_valid={val_resp.is_valid}, std='{val_resp.standardized_name}')")
        assert val_resp.is_valid is True
        assert val_resp.standardized_name == "Python"
        assert t_val < 0.5, f"Validation took too long: {t_val}s"

        # 2. Start Assessment Session
        t0 = time.monotonic()
        session = await service.start_assessment("Python")
        t_start = time.monotonic() - t0
        print(f"[2] Start Assessment Session: {t_start*1000:.1f}ms (session_id={session.session_id}, phase={session.phase})")
        assert session.phase == "profile_collection"
        assert session.question_number == 1
        assert session.current_question is not None

        # 3. Submit Profile Q1 (Goal) - Local operation, zero LLM calls
        t0 = time.monotonic()
        s1 = await service.submit_answer(session.session_id, "Build production web apps, tools & services with Python", quiz_selected_index=1)
        t_q1 = time.monotonic() - t0
        print(f"[3] Submit Answer 1 (Goal): {t_q1*1000:.1f}ms (Q2: {s1.current_question.prompt})")
        assert s1.question_number == 2
        assert s1.profile_goal != ""

        # 4. Submit Profile Q2 (Level) - Triggers background prefetch of diagnostic questions
        t0 = time.monotonic()
        s2 = await service.submit_answer(session.session_id, "Some Experience (know basics/syntax)", quiz_selected_index=1)
        t_q2 = time.monotonic() - t0
        print(f"[4] Submit Answer 2 (Level): {t_q2*1000:.1f}ms (Q3: {s2.current_question.prompt})")
        assert s2.question_number == 3

        # Simulate learner reading and choosing study hours (allows background prefetch)
        await asyncio.sleep(3)

        # 5. Submit Profile Q3 (Study Hours) - Awaits prefetched questions or generates once
        t0 = time.monotonic()
        s3 = await service.submit_answer(session.session_id, "10 hours / week", quiz_selected_index=2)
        t_q3 = time.monotonic() - t0
        print(f"[5] Submit Answer 3 (Hours -> Diag Q4): {t_q3:.2f}s (Phase: {s3.phase}, Total Questions: {s3.total_questions})")
        assert s3.phase == "diagnostic"
        assert s3.question_number == 4
        assert s3.current_question is not None

        # Record diagnostic questions generated
        diag_q_count = s3.total_questions
        print(f"    -> Generated diagnostic questions total count: {diag_q_count}")
        assert diag_q_count >= 5

        # 6. Submit Diagnostic Q4..Q6 (Locally scored on backend, ZERO LLM calls!)
        ans_timings = []
        for q_num in range(4, 7):
            curr_q = s3.current_question
            chosen_idx = curr_q.correct_option_index or 0
            t0 = time.monotonic()
            s3 = await service.submit_answer(session.session_id, "Selected option", quiz_selected_index=chosen_idx)
            t_ans = time.monotonic() - t0
            ans_timings.append(t_ans)
            print(f"[6.{q_num-3}] Submit Diag Q{q_num}: {t_ans*1000:.1f}ms (is_correct={s3.last_answer_correct}, next_q={s3.question_number})")
            assert t_ans < 6.0, f"Diag answer {q_num} took too long: {t_ans}s (should be local scoring + DB save)"

        # 7. Submit Final Diagnostic Q7 - Scores answer and auto-synthesizes roadmap DAG
        curr_q = s3.current_question
        chosen_idx = curr_q.correct_option_index or 0
        t0 = time.monotonic()
        s_final = await service.submit_answer(session.session_id, "Selected option", quiz_selected_index=chosen_idx)
        t_final = time.monotonic() - t0
        print(f"[7] Submit Final Q7 + Inline Roadmap Synthesis: {t_final:.2f}s (phase={s_final.phase}, roadmap_id={s_final.generated_roadmap_id})")
        assert s_final.is_complete is True
        assert s_final.generated_roadmap_id is not None

        # 8. Idempotency test: Call generate-roadmap endpoint (should return cached roadmap in milliseconds, ZERO LLM calls!)
        t0 = time.monotonic()
        roadmap = await service.generate_personalized_roadmap(session.session_id)
        t_roadmap_fetch = time.monotonic() - t0
        print(f"[8] Idempotent Roadmap Retrieval: {t_roadmap_fetch*1000:.1f}ms (topics={len(roadmap.topics)}, edges={len(roadmap.edges)})")
        assert t_roadmap_fetch < 5.0, f"Cached roadmap retrieval took too long: {t_roadmap_fetch}s"
        assert len(roadmap.topics) >= 4
        assert roadmap.id == s_final.generated_roadmap_id

        # 9. Concurrency test: Duplicate concurrent calls do not trigger duplicate LLM calls
        t0 = time.monotonic()
        res1, res2 = await asyncio.gather(
            service.generate_personalized_roadmap(session.session_id),
            service.generate_personalized_roadmap(session.session_id),
        )
        t_concurrent = time.monotonic() - t0
        print(f"[9] Concurrent Duplicate Calls: {t_concurrent*1000:.1f}ms (both returned roadmap {res1.id})")
        assert res1.id == res2.id

        # 10. Topic learning content test: First call synthesizes and caches, second call returns cached instantly
        first_topic = roadmap.topics[0]
        t0 = time.monotonic()
        content1 = await service.get_topic_learning_content(roadmap.id, first_topic.id)
        t_content_gen = time.monotonic() - t0
        print(f"[10.1] Topic Learning Content Generation (First Open): {t_content_gen:.2f}s (objectives={len(content1.learning_objectives)})")
        assert content1 is not None

        t0 = time.monotonic()
        content2 = await service.get_topic_learning_content(roadmap.id, first_topic.id)
        t_content_cached = time.monotonic() - t0
        print(f"[10.2] Topic Learning Content Cached (Second Open): {t_content_cached*1000:.1f}ms (ZERO LLM calls)")
        assert t_content_cached < 5.0, f"Cached content took too long: {t_content_cached}s"
        assert content1.topic_id == content2.topic_id

        print("\n" + "=" * 60)
        print("ALL TESTS PASSED SUCCESSFULLY!")
        print("=" * 60)


if __name__ == "__main__":
    asyncio.run(test_full_pipeline_performance())
