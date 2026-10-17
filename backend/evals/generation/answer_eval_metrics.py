"""
Metrics and rubric evaluation algorithms for chatbot generation quality.

Provides atomic scoring for:
- Factual correctness against reference facts.
- Unsupported claim rate and evidence groundedness.
- Instruction adherence per explanation mode (teacher, explainable, general).
- Abstention correctness and refusal integrity.
- Citation validity and source attribution.
"""

import re
from typing import Any, Dict, List, Optional, Set, Tuple


def _tokenize_text(text: str) -> List[str]:
    """Extracts lowercase alphabetic/numeric tokens of length >= 3."""
    return re.findall(r"\b[a-zA-Z0-9]{3,}\b", text.lower())


def compute_factual_correctness(
    response: str,
    reference_facts: List[str],
    overlap_threshold: float = 0.50
) -> Tuple[float, List[str], List[str]]:
    """
    Evaluates factual correctness against a list of atomic reference facts.
    
    A reference fact is considered matched if at least `overlap_threshold`
    of its key informative tokens (>= 3 chars) appear in the response.
    
    Returns:
        (score, matched_facts, missing_facts)
        score is a float in [0.0, 1.0]. If reference_facts is empty, returns (1.0, [], []).
    """
    if not reference_facts:
        return 1.0, [], []
    if not response or not response.strip():
        return 0.0, [], list(reference_facts)

    response_lower = response.lower()
    response_tokens = set(_tokenize_text(response_lower))

    matched: List[str] = []
    missing: List[str] = []

    for fact in reference_facts:
        fact_tokens = _tokenize_text(fact)
        if not fact_tokens:
            matched.append(fact)
            continue

        overlap_count = sum(1 for tok in fact_tokens if tok in response_tokens or tok in response_lower)
        overlap_ratio = overlap_count / len(fact_tokens)

        if overlap_ratio >= overlap_threshold:
            matched.append(fact)
        else:
            missing.append(fact)

    score = len(matched) / len(reference_facts)
    return score, matched, missing


def compute_unsupported_claim_rate(
    response: str,
    evidence_chunks: List[str],
    min_sentence_len: int = 20
) -> Tuple[float, List[str]]:
    """
    Detects factual claims in response that have no grounding in supplied evidence chunks.
    
    Splits response into declarative sentences. A sentence with substantive length
    (>= min_sentence_len) is deemed unsupported if none of its key content words appear
    in any evidence chunk.
    
    Returns:
        (unsupported_rate, unsupported_sentences)
        unsupported_rate is float in [0.0, 1.0]. 0.0 means 100% grounded.
    """
    if not response or not response.strip():
        return 0.0, []
    if not evidence_chunks:
        # If no evidence was provided, all substantive statements are ungrounded
        sentences = [s.strip() for s in re.split(r"[.!?\n]+", response) if len(s.strip()) >= min_sentence_len]
        return (1.0, sentences) if sentences else (0.0, [])

    combined_evidence = " ".join(evidence_chunks).lower()
    evidence_tokens = set(_tokenize_text(combined_evidence))

    raw_sentences = [s.strip() for s in re.split(r"[.!\n]+", response)]
    
    # Filter out pedagogical framing, questions, greetings, and very short phrases
    pedagogical_pattern = re.compile(r"^(let'?s|great|good|try|remember|hello|hi|step\s*\d|excellent|well done|practice|notice|can you)", re.IGNORECASE)
    substantive_sentences = [
        s for s in raw_sentences 
        if len(s) >= min_sentence_len and not pedagogical_pattern.search(s) and "?" not in s
    ]

    if not substantive_sentences:
        return 0.0, []

    unsupported: List[str] = []
    for sent in substantive_sentences:
        sent_tokens = _tokenize_text(sent)
        if not sent_tokens:
            continue
        # Stopwords filter
        content_tokens = [
            t for t in sent_tokens 
            if t not in {"this", "that", "these", "those", "with", "from", "have", "been", "were", "what", "when", "where"}
        ]
        if not content_tokens:
            continue

        hits = sum(1 for t in content_tokens if t in evidence_tokens or t in combined_evidence)
        if (hits / len(content_tokens)) < 0.25:
            unsupported.append(sent)

    rate = len(unsupported) / len(substantive_sentences)
    return rate, unsupported


def compute_instruction_adherence(
    response: str,
    mode: str
) -> Dict[str, Any]:
    """
    Checks mode-specific prompt constraints and formatting standards.
    
    - 'teacher': Encouraging/supportive tone, structured/step-by-step guidance, practice question.
    - 'explainable': Structured rationale, explicit principles or assumptions, 'why' explanation.
    - 'general': Direct and concise, informative without conversational padding.
    """
    resp_lower = response.lower()
    mode_lower = (mode or "general").lower()
    checks: Dict[str, bool] = {}

    if mode_lower == "teacher":
        checks["has_structured_steps"] = bool(
            re.search(r"(\bstep\s*\d|1\.|2\.|•|-|\bfirst\b|\bsecond\b)", resp_lower)
        )
        checks["has_encouragement_or_simple_tone"] = bool(
            re.search(r"(great|good|remember|let'?s|understand|notice|helpful|practice|observe)", resp_lower)
        )
        checks["has_practice_or_check_question"] = bool(
            re.search(r"(\?|practice|try this|question|can you)", resp_lower)
        )
        score = sum(1 for v in checks.values() if v) / len(checks)

    elif mode_lower == "explainable":
        checks["has_reasoning_markers"] = bool(
            re.search(r"(because|therefore|as a result|since|leads to|implies|due to)", resp_lower)
        )
        checks["explains_why_or_mechanism"] = bool(
            re.search(r"(why|mechanism|principle|concept|reason|factor|law)", resp_lower)
        )
        checks["has_structured_sections"] = bool(
            re.search(r"(\*\*|#|1\.|- |:\n)", response)
        )
        score = sum(1 for v in checks.values() if v) / len(checks)

    else:  # general mode
        checks["is_direct"] = not bool(
            re.search(r"^(hello!|hi there!|as an ai language model|as a patient teacher)", resp_lower)
        )
        checks["is_substantive"] = len(response.strip()) >= 15
        score = sum(1 for v in checks.values() if v) / len(checks)

    return {
        "mode": mode_lower,
        "score": score,
        "passed": score >= 0.65,
        "checks": checks,
    }


def evaluate_abstention(response: str, expected_behavior: str = "MISSING") -> Dict[str, Any]:
    """
    Verifies that a refusal or abstention response correctly informs the user
    and refrains from hallucinating ungrounded content.
    """
    resp_lower = response.lower()
    is_refusal = bool(
        re.search(r"(not found|not present|not configured|unable to retrieve|curriculum corpus|official curriculum|verify the chapter|out of syllabus)", resp_lower)
    )
    # Check that it doesn't give a confident hallucinated lecture
    not_hallucinating = len(response.strip().split()) <= 100

    passed = is_refusal if expected_behavior.upper() in ("MISSING", "CORPUS_NOT_CONFIGURED", "REFUSAL") else not is_refusal

    return {
        "is_refusal": is_refusal,
        "concise_abstention": not_hallucinating,
        "passed": passed,
    }


def compute_citation_validity(
    citations: List[Dict[str, Any]],
    available_chunk_ids: Set[str]
) -> Tuple[float, int, int]:
    """
    Verifies that every emitted citation corresponds to a real retrieved chunk ID
    and contains mandatory source metadata (textbook_title or chapter).
    
    Returns:
        (validity_ratio, valid_count, total_count)
    """
    if not citations:
        return 1.0, 0, 0

    valid = 0
    for cit in citations:
        cid = cit.get("chunk_id") or cit.get("source_id")
        has_id = cid in available_chunk_ids if available_chunk_ids else bool(cid)
        has_meta = bool(cit.get("textbook_title") or cit.get("chapter"))
        if has_id and has_meta:
            valid += 1

    ratio = valid / len(citations)
    return ratio, valid, len(citations)
