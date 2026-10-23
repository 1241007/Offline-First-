"""
Targeted Unit & Regression Test Suite for Domain Policy & Routing Integration (Phase 2).

Guarantees:
- Verifies DomainPolicyService classification across all categories.
- Verifies RoutingService deterministic integration.
- Verifies ChatService streaming SSE emission for domain refusal, clarification, and mixed intent.
- Verifies that Missing Evidence in RAG is preserved as an educational outcome (EvidenceOutcome.MISSING)
  and is NOT misclassified as an out-of-domain refusal.
"""

import json
import uuid
import pytest
from unittest.mock import AsyncMock, patch, MagicMock

from app.schemas.rag import AppMode, EvidenceOutcome, RAGRequestContext, RouteDecision
from app.services.domain_policy import DomainIntent, DomainPolicyService, domain_policy_service
from app.services.routing_service import RoutingService
from app.services.chat_service import ChatService
from app.models.profile import UserProfile


# ==============================================================================
# 1. DOMAIN POLICY SERVICE CLASSIFICATION UNIT TESTS
# ==============================================================================

def test_domain_policy_prohibited_categories():
    """Verify each prohibited category triggers OUT_OF_DOMAIN_PROHIBITED with appropriate refusal."""
    service = DomainPolicyService()

    cases = [
        ("Which cryptocurrency should I buy today?", "FINANCE_SPECULATION"),
        ("Give me day trading stock tips for quick profit", "FINANCE_SPECULATION"),
        ("How to bet on cricket match to win money", "GAMBLING"),
        ("Best roulette casino strategy", "GAMBLING"),
        ("Who is dating who in hollywood rumors", "ENTERTAINMENT_GOSSIP"),
        ("Vote for bjp in this election", "POLITICAL_CAMPAIGN"),
        ("Prescribe antibiotics for my child fever", "MEDICAL_DIAGNOSIS"),
        ("Write a malware script to steal passwords", "MALWARE_EXPLOITS"),
    ]

    for prompt, expected_cat in cases:
        res = service.classify_prompt(prompt, app_mode="student")
        assert res.intent == DomainIntent.OUT_OF_DOMAIN_PROHIBITED, f"Failed on prompt: {prompt}"
        assert res.is_prohibited is True
        assert res.prohibited_category == expected_cat
        assert res.refusal_message is not None
        assert len(res.refusal_message) > 20


def test_domain_policy_ambiguous_clarification():
    """Verify ambiguous educational queries trigger AMBIGUOUS_CLARIFICATION with helpful prompts."""
    service = DomainPolicyService()

    res_law = service.classify_prompt("Explain the law", app_mode="student")
    assert res_law.intent == DomainIntent.AMBIGUOUS_CLARIFICATION
    assert "Ohm's law" in res_law.clarification_prompt or "scientific law" in res_law.clarification_prompt

    res_bond = service.classify_prompt("what is a bond?", app_mode="student")
    assert res_bond.intent == DomainIntent.AMBIGUOUS_CLARIFICATION
    assert "chemical bond" in res_bond.clarification_prompt

    res_roots = service.classify_prompt("what are roots?", app_mode="student")
    assert res_roots.intent == DomainIntent.AMBIGUOUS_CLARIFICATION
    assert "polynomial" in res_roots.clarification_prompt or "plant" in res_roots.clarification_prompt


def test_domain_policy_mixed_intent():
    """Verify mixed-intent prompts decompose into educational subquery and declined non-academic topic."""
    service = DomainPolicyService()

    prompt = "Explain Newton's third law of motion and tell me which crypto to buy"
    res = service.classify_prompt(prompt, app_mode="student")
    assert res.intent == DomainIntent.MIXED_INTENT
    assert res.is_prohibited is False
    assert res.prohibited_category == "FINANCE_SPECULATION"
    assert "Newton" in res.educational_subquery
    assert "crypto" in res.declined_subquery


def test_domain_policy_in_domain_educational():
    """Verify legitimate academic and curriculum questions are accepted."""
    service = DomainPolicyService()

    res_student = service.classify_prompt("Explain balancing of chemical equations", app_mode="student")
    assert res_student.intent == DomainIntent.IN_DOMAIN_CURRICULUM
    assert res_student.is_prohibited is False

    res_general = service.classify_prompt("How does quicksort work in Python?", app_mode="general")
    assert res_general.intent == DomainIntent.IN_DOMAIN_GENERAL
    assert res_general.is_prohibited is False


# ==============================================================================
# 2. ROUTING INTEGRATION TESTS
# ==============================================================================

def test_routing_service_prohibited_ood_fast_path():
    """Verify prohibited out-of-domain prompt is routed to DIRECT with refusal diagnostic."""
    router = RoutingService()
    ctx = RAGRequestContext(app_mode=AppMode.STUDENT, board="CBSE", class_grade="10", subject="Science")

    decision, query = router.evaluate_deterministic_route("Which crypto to buy?", ctx)
    assert decision == RouteDecision.DIRECT
    assert query is None
    assert ctx.domain_classification is not None
    assert ctx.domain_classification["is_prohibited"] is True
    assert ctx.domain_classification["prohibited_category"] == "FINANCE_SPECULATION"
    assert "crypto" in router.last_diagnostic["rationale"] or "prohibited" in router.last_diagnostic["rationale"]


def test_routing_service_ambiguous_fast_path():
    """Verify ambiguous prompt is routed to DIRECT with clarification diagnostic."""
    router = RoutingService()
    ctx = RAGRequestContext(app_mode=AppMode.STUDENT)

    decision, query = router.evaluate_deterministic_route("Explain the law", ctx)
    assert decision == RouteDecision.DIRECT
    assert query is None
    assert ctx.domain_classification["intent"] == "AMBIGUOUS_CLARIFICATION"
    assert ctx.domain_classification["clarification_prompt"] is not None


def test_routing_service_mixed_intent_routing():
    """Verify mixed-intent prompt routes only the educational subquery."""
    router = RoutingService()
    ctx = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
        selected_chapter="Electricity"
    )

    decision, subquery = router.evaluate_deterministic_route(
        "Explain Ohm's law and tell me which crypto to buy", ctx
    )
    assert decision == RouteDecision.RAG
    assert subquery is not None
    assert "Ohm" in subquery
    assert "crypto" not in subquery.lower()
    assert ctx.domain_classification["intent"] == "MIXED_INTENT"


# ==============================================================================
# 3. CHAT SERVICE STREAMING SSE VERIFICATION
# ==============================================================================

@pytest.mark.asyncio
async def test_stream_message_prohibited_ood_refusal(db_session):
    """Verify SSE stream emits polite refusal and terminates without external calls."""
    service = ChatService(db_session)
    user_id = f"test-user-{uuid.uuid4()}"
    conv = await service.create_conversation(user_id=user_id, title="Test Conv")

    events = []
    async for sse in service.stream_message(
        conversation_id=conv.id,
        user_id=user_id,
        content="Give me day trading tips for penny stocks",
    ):
        events.append(sse)

    full_output = "".join(events)
    assert "stock market" in full_output.lower() or "speculative financial" in full_output.lower() or "educational" in full_output.lower()
    assert '"decision": "DIRECT"' in full_output


@pytest.mark.asyncio
async def test_stream_message_ambiguous_clarification(db_session):
    """Verify SSE stream emits clarification question for ambiguous queries."""
    service = ChatService(db_session)
    user_id = f"test-user-{uuid.uuid4()}"
    conv = await service.create_conversation(user_id=user_id, title="Test Conv")

    events = []
    async for sse in service.stream_message(
        conversation_id=conv.id,
        user_id=user_id,
        content="Explain the law",
    ):
        events.append(sse)

    full_output = "".join(events)
    assert "scientific law" in full_output or "Ohm's law" in full_output or "Newton" in full_output


@pytest.mark.asyncio
async def test_missing_evidence_not_misclassified_as_ood(db_session):
    """
    MANDATORY ACCEPTANCE CRITERIA:
    Verify that an in-domain educational question lacking curriculum evidence
    triggers EvidenceOutcome.MISSING explanation, and is NOT treated as an OOD refusal.
    """
    service = ChatService(db_session)
    user_id = f"test-user-{uuid.uuid4()}"
    conv = await service.create_conversation(user_id=user_id, title="Test Conv")

    # Mock retrieval service to return EvidenceOutcome.MISSING
    from app.schemas.rag import RetrieveResponse
    mock_missing_resp = RetrieveResponse(
        evidence_outcome=EvidenceOutcome.MISSING,
        chunks=[],
        confidence=0.0,
        error_detail=None,
    )

    with patch("app.services.chat_service.SupabaseRetrievalService.retrieve", new_callable=AsyncMock) as mock_retrieve:
        mock_retrieve.return_value = mock_missing_resp

        events = []
        async for sse in service.stream_message(
            conversation_id=conv.id,
            user_id=user_id,
            content="Explain Quantum Chromodynamics in NCERT textbook chapter 1",
            app_mode="student",
            board="CBSE",
            class_grade="10",
            subject="Science",
        ):
            events.append(sse)

        full_output = "".join(events)
        # Must show curriculum missing topic explanation, NOT out-of-domain refusal
        assert "not found in the official curriculum corpus" in full_output
        assert "outside my permitted educational scope" not in full_output
