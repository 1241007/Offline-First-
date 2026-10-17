# EduNova RAG Evaluation Golden Set & Metric Governance

> **Document Version**: 1.0  
> **Status**: Active & Enforced  
> **Target Subsystem**: `backend/evals/rag/`  

---

## 1. Principles of Golden Set Integrity

The EduNova RAG Evaluation Framework provides durable, version-controlled ground truth for both online and offline retrieval, grounding, citation accuracy, and curriculum isolation. To prevent metric distortion, overfitting, and synthetic hallucination, all additions and modifications to `golden_set.jsonl` must adhere to these governance rules.

### 1.1 Zero Synthetic Syllabus Ingestion
- **Rule**: Evaluation test cases, synthetic questions, and expected chunk IDs must NEVER be inserted into the production Supabase `syllabus_documents` or `syllabus_chunks` tables as synthetic fixtures.
- **Rationale**: Inserting synthetic syllabus records pollutes the production knowledge base, artificially inflates retrieval metrics, and invalidates cross-runtime sync hashes.
- **Verification**: Production corpus audits must confirm that only genuine official NCERT textbooks and authorized CBSE curriculum documents exist in the database.

### 1.2 Required Source Provenance for New Cases
Every entry in `golden_set.jsonl` must provide verified source provenance:
```json
{
  "case_id": "EVAL-SCI10-026",
  "question": "What is the function of the bile juice in the digestion of fats?",
  "language": "en",
  "curriculum": {
    "board": "CBSE",
    "class_grade": "10",
    "subject": "Science",
    "chapter": "Life Processes"
  },
  "expected_document_title": "Chapter 06 Life Processes",
  "expected_passage_reference": "Section 6.2.4",
  "expected_chunk_ids": ["<uuid-matching-production-chunk>"],
  "expected_facts": [
    "bile juice makes the acidic food alkaline",
    "bile salts break down large fat globules into smaller globules (emulsification)"
  ],
  "expected_behavior": "SUFFICIENT",
  "category": "concept_explanation",
  "difficulty": "medium",
  "reviewer_status": "verified",
  "dataset_version": "1.0"
}
```

Required fields and criteria:
1. `case_id`: Follows `EVAL-<SUBJECT><GRADE>-<NUM>` sequence (e.g. `EVAL-SCI10-026`).
2. `curriculum`: Must map to an officially approved board, grade, and subject.
3. `expected_document_title`: Name of the official textbook or syllabus document.
4. `expected_passage_reference`: Section or page reference from the official curriculum.
5. `expected_chunk_ids`: Verified chunk UUID(s) existing in the real corpus (empty `[]` for unanswerable/out-of-syllabus cases).
6. `expected_facts`: Distinct, verifiable factual propositions that must appear in the generated answer.
7. `expected_behavior`: One of `SUFFICIENT`, `UNCERTAIN`, `MISSING`, or `CORPUS_NOT_CONFIGURED`.
8. `reviewer_status`: Must be set to `verified` following peer review.

---

## 2. Separation of Deterministic Contracts vs Stochastic Evaluations

1. **Deterministic Contracts**:
   - Retrieval metrics (`Recall@5`, `Recall@10`, `MRR@10`, `nDCG@10`, `Leakage`, `Empty-Result Correctness`) are 100% deterministic mathematical evaluations.
   - Routing contracts (ROUTE-001 through ROUTE-012) are deterministic API contracts executed in `test_routing_contract.py`.
   - Syllabus authorization gating is a deterministic security boundary enforced via database filters and foreign key joins.

2. **Stochastic Evaluations**:
   - LLM generative answer scoring relies on exact token/phrase matching (`compute_fact_overlap`), citation marker parsing (`compute_citation_coverage`), and regex-based abstention detection (`evaluate_answer_abstention`).
   - Mock execution runners must always be available for deterministic local CI/CD pipelines without incurring external LLM costs or network dependencies.
   - Live evaluations (`--mode live`) must clearly distinguish live remote Supabase / API verification from mock results. Never conflate or report a mock test as live verification.

---

## 3. Baseline Update & Regression Detection Workflow

### 3.1 Establishing a Baseline
When an architectural milestone or approved re-indexing phase completes, generate a baseline report:
```powershell
python -m evals.rag.run_eval --suite all --mode live --output-dir backend/evals/rag/results/
```
The resulting `latest_eval_report.json` serves as the baseline for subsequent pull requests and phase executions.

### 3.2 Regression Thresholds
The evaluation command `--fail-on-regression` enforces:
| Evaluation Metric | Baseline Tolerance | Action on Regression |
| :--- | :--- | :--- |
| **Recall@5** | $\le 0.05$ drop allowed | Build failure (Exit code 1) |
| **MRR@10** | $\le 0.05$ drop allowed | Build failure (Exit code 1) |
| **Cross-Curriculum Leakage** | Must remain $0.0\%$ | Build failure (Exit code 1) |
| **Abstention Correctness** | Must remain $100.0\%$ | Build failure (Exit code 1) |
| **Pytest Suites** | 0 failed tests allowed | Build failure (Exit code 1) |
| **Android Offline Suites** | 0 failed tests allowed | Build failure (Exit code 1) |

---

## 4. Security & Privacy Guidelines

1. **Zero Secret Exposure**:
   - Never log, print, or commit API keys (OpenRouter, Supabase Service Role, JWT secret).
   - Evaluation reports and summaries must scrub credentials and display only masked configuration.
2. **Zero Raw Textbook Passage Dumps**:
   - Test cases and evaluation reports must contain only short verbatim phrases or factual assertions needed for metric calculation.
   - Do not dump entire chapters or copyrighted textbook contents into evaluation repositories.
3. **Curriculum Isolation Enforcement**:
   - Out-of-syllabus traps (`cross_curriculum_trap`, `missing_profile_trap`, `unconfigured_general_mode`) must verify that no unauthorized or irrelevant chunks are returned to the user or device.
