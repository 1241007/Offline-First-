---
phase: 3
plan: 1
wave: 2
---

# Plan 3.1: Create Durable Source-Grounded RAG Evaluation Suite

## Objective
Establish a version-controlled, reproducible evaluation framework under `backend/evals/rag/` with a source-grounded golden dataset derived strictly from verified NCERT Class 10 Science chapters, accompanied by deterministic retrieval and answer quality metrics.

## Context
- .gsd/SPEC.md
- backend/syllabus_sources/CBSE/10/Science/sources.json
- backend/syllabus_sources/CBSE/10/Science/Chapter_01_Chemical_Reactions_and_Equations.txt
- backend/app/services/supabase_retrieval_service.py
- backend/app/schemas/rag.py

## Tasks

<task type="auto">
  <name>Build Source-Grounded Golden Dataset (golden_set.jsonl)</name>
  <files>backend/evals/rag/golden_set.jsonl,backend/evals/rag/README.md</files>
  <action>
    Create `backend/evals/rag/golden_set.jsonl` containing 25-35 manually reviewed, source-grounded test cases from verified NCERT Class 10 Science chapters (Chemical Reactions, Acids Bases Salts, Electricity, Life Processes, etc.).
    Each entry must specify:
    - `case_id`: Stable identifier (e.g., `EVAL-SCI10-001`)
    - `question`: Query text (mix of direct fact, concept explanation, multi-passage, and unanswerable questions)
    - `curriculum`: `{"board": "CBSE", "class_grade": "10", "subject": "Science", "chapter": "..."}`
    - `expected_document_title`: Verified textbook title
    - `expected_passage_reference`: Verifiable textbook section/page
    - `expected_facts`: Array of factual assertions proven by NCERT source text
    - `expected_outcome`: `SUFFICIENT`, `UNCERTAIN`, `MISSING`, or `CORPUS_NOT_CONFIGURED`
    - `category`: `fact_retrieval`, `concept`, `paraphrased`, `cross_curriculum_trap`, `unanswerable`
    Create `backend/evals/rag/README.md` documenting dataset schema and contributor guidelines.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -c "import json; lines = open('backend/evals/rag/golden_set.jsonl', encoding='utf-8').readlines(); print(f'Validated {len(lines)} gold cases'); assert len(lines) >= 20"</verify>
  <done>Golden dataset created with at least 20 valid JSONL records grounded in official NCERT text.</done>
</task>

<task type="auto">
  <name>Implement Deterministic Retrieval and Answer Metrics</name>
  <files>backend/evals/rag/retrieval_metrics.py,backend/evals/rag/answer_metrics.py</files>
  <action>
    Create `retrieval_metrics.py` implementing:
    - Recall@5, Recall@10: Fraction of ground-truth passages retrieved in top K
    - MRR@10: Reciprocal rank of the first relevant chunk
    - nDCG@10: Discounted cumulative gain for ranked relevance
    - Cross-curriculum leakage rate: Frequency of retrieving non-matching board/class
    - Empty-result correctness: Correct abstention on unanswerable/out-of-scope queries
    Create `answer_metrics.py` implementing:
    - Citation correctness: Verified that cited chunks match ground-truth chunk IDs
    - Citation coverage: Ratio of verifiable claims with citations
    - Factual overlap / groundedness check against expected facts
    - Correct abstention rate when evidence is MISSING
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_eval_metrics.py -v</verify>
  <done>Metric computation functions verified by unit tests with deterministic fixtures.</done>
</task>

## Success Criteria
- [ ] Golden dataset authored with 100% verified provenance against NCERT source text (zero synthetic hallucinations).
- [ ] Retrieval metrics compute Recall@K, MRR@10, and curriculum leakage accurately.
- [ ] Answer metrics evaluate groundedness and abstention without conflating LLM phrasing with factual failure.
