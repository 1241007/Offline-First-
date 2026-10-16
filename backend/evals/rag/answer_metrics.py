"""
Answer quality, citation correctness, and groundedness metrics for EduNova RAG framework.

Evaluates:
- Citation Correctness: Whether cited chunk IDs exist in the retrieved passages and match ground truth.
- Citation Coverage: Proportion of factual claims that include supporting citations.
- Abstention Correctness: Whether ungrounded queries correctly abstain without making synthetic claims.
- Fact Overlap / Groundedness: Precision and recall of key expected factual assertions in generated text.
"""

import re
from typing import Any, Dict, List, Optional, Set, Tuple


def compute_citation_correctness(
    cited_chunk_ids: List[str],
    retrieved_chunk_ids: List[str],
    expected_chunk_ids: Optional[List[str]] = None,
) -> Dict[str, float]:
    """
    Evaluates whether citations returned by the assistant:
    1. Actually exist in the retrieved context (validity).
    2. Belong to the ground-truth relevant passages (precision), if ground truth is provided.
    """
    if not cited_chunk_ids:
        # If no citations produced:
        # Valid if expected is also empty, else 0.0
        is_expected_empty = not expected_chunk_ids
        return {
            "citation_validity": 1.0 if is_expected_empty else 0.0,
            "citation_precision": 1.0 if is_expected_empty else 0.0,
            "total_citations": 0,
        }

    retrieved_set = set(retrieved_chunk_ids)
    valid_citations = [cid for cid in cited_chunk_ids if cid in retrieved_set]
    validity_ratio = len(valid_citations) / float(len(cited_chunk_ids))

    precision_ratio = 1.0
    if expected_chunk_ids:
        expected_set = set(expected_chunk_ids)
        grounded_citations = [cid for cid in cited_chunk_ids if cid in expected_set]
        precision_ratio = len(grounded_citations) / float(len(cited_chunk_ids))

    return {
        "citation_validity": validity_ratio,
        "citation_precision": precision_ratio,
        "total_citations": len(cited_chunk_ids),
    }


def compute_citation_coverage(
    generated_text: str,
    cited_chunks: List[Any],
) -> float:
    """
    Estimates citation coverage by measuring presence of citation markers
    (e.g., '[1]', '[2]', or direct chunk references) across substantive paragraphs.
    """
    paragraphs = [p.strip() for p in generated_text.split("\n\n") if len(p.strip()) > 30]
    if not paragraphs:
        return 1.0 if not cited_chunks else 0.0

    cited_paragraphs = 0
    marker_pattern = re.compile(r"\[(?:\d+|chunk-[a-zA-Z0-9\-]+)\]")
    for para in paragraphs:
        if marker_pattern.search(para):
            cited_paragraphs += 1

    return cited_paragraphs / float(len(paragraphs))


def evaluate_answer_abstention(
    generated_text: str,
    expected_behavior: str,
) -> bool:
    """
    Verifies that the generated answer explicitly abstains when expected behavior is MISSING
    or CORPUS_NOT_CONFIGURED, rather than attempting to fabricate facts.
    """
    expected_upper = expected_behavior.upper()
    abstention_indicators = [
        "not found in the official curriculum",
        "curriculum corpus",
        "general mode knowledge retrieval is currently not configured",
        "unable to retrieve curriculum",
        "not available in the official textbook",
    ]

    lower_text = generated_text.lower()
    has_abstention = any(ind in lower_text for ind in abstention_indicators)

    if expected_upper in ("MISSING", "CORPUS_NOT_CONFIGURED"):
        return has_abstention
    else:
        # If expected to be answerable, it should NOT abstain
        return not has_abstention


def compute_fact_overlap(
    generated_text: str,
    expected_facts: List[str],
) -> Dict[str, float]:
    """
    Evaluates key fact recall in the generated answer text.
    Each expected fact is checked for keyword presence / overlap.
    """
    if not expected_facts:
        return {
            "fact_recall": 1.0,
            "facts_matched": 0,
            "total_facts": 0,
        }

    lower_text = generated_text.lower()
    matched = 0

    for fact in expected_facts:
        # Check if the core terms of the fact appear in the answer
        fact_tokens = [w.lower() for w in re.findall(r"\b\w{3,}\b", fact) if w.lower() not in ("the", "and", "that", "this", "for", "with")]
        if not fact_tokens:
            continue
        token_matches = sum(1 for token in fact_tokens if token in lower_text)
        if (token_matches / float(len(fact_tokens))) >= 0.60:
            matched += 1

    return {
        "fact_recall": matched / float(len(expected_facts)),
        "facts_matched": matched,
        "total_facts": len(expected_facts),
    }
