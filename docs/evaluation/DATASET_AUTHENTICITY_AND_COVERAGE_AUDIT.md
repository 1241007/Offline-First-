# EduNova Chatbot Evaluation Harness
# Dataset Authenticity, Edge-Case Coverage, and Evaluation Integrity Audit

**Audit Date:** October 22, 2026  
**Auditor:** Antigravity Senior AI Evaluation & LLM Reliability Specialist  
**Target Repository:** EduNova Android Application & FastAPI Backend (`local_backend` branch)  
**Evaluation Scope:** Chatbot Evaluation Harness (Phase 1 & Phase 2 verification)  
**Overall Verdict:** **`ADEQUATE FOR CURRENT PHASE (WITH HONEST LIMITATIONS DOCUMENTED)`**

---

## 1. Executive Summary & Audit Philosophy

The objective of this audit is **trustworthy, evidence-based evaluation**, not impressive-sounding vanity numbers. Previous planning and legacy evaluation documents in this repository contained claims (such as "99.8% Online RAG Accuracy" or "Universal Zero Safety Leakage") that lacked versioned dataset evidence or conflated mock test fixtures with live production capability.

This audit inspects the **actual files in the repository**, calculates exact counts, traces provenance to primary sources, measures empirical edge-case coverage, verifies metric calculation formulas against edge cases, and explicitly distinguishes between:
1. **Deterministic rule/code verification tests** (which test business logic, regular expressions, and parsing contracts).
2. **Mocked pipeline tests** (which test metric calculation code and data structures against hand-crafted fixtures).
3. **Live model evaluations** (which test live OpenRouter LLM or on-device SLM inference).

---

## 2. Dataset Inventory & Provenance Breakdown

A complete inspection of all dataset files under `backend/evals/` reveals **four primary test datasets** and **two parity/retrieval fixtures**, totaling **95 evaluation cases** and **2 benchmark fixtures**:

| Dataset File | Total Cases | Primary Purpose | Format | Schema-Compliant (`test_case_schema.json`) | Cryptographic Fingerprint (SHA-256) |
| :--- | :---: | :--- | :---: | :---: | :---: |
| `evals/domain/domain_scope_benchmark.jsonl` | **60** | Domain policy boundary, out-of-domain refusals, ambiguity, mixed intent | JSONL | **100% Valid (60/60)** | `e074128a1c97a82b` |
| `evals/rag/golden_set.jsonl` | **25** | Retrieval ranking (Recall@k, MRR, nDCG) & curriculum evidence gating | JSONL | **100% Valid (25/25)** *(Upgraded)* | `a394ecbc5eb6046e` |
| `evals/rag/golden_set_ncert_legacy.jsonl` | **25** | Legacy NCERT retrieval benchmark (23 overlapping questions) | JSONL | Legacy (Unmigrated) | `79919f851da039b4` |
| `evals/generation/generation_eval_set.jsonl` | **10** | Factual correctness, hallucination detection, mode adherence | JSONL | **100% Valid (10/10)** *(Upgraded)* | `b7b101ddaa79531c` |
| `evals/rag/authorized_cbse_chunks.json` | 6 Chunks | Pinned CBSE curriculum reference chunk fixtures | JSON | N/A (Corpus) | `45da812ee6899b31` |
| `evals/parity/parity_fixtures.json` | 10 Queries / 8 Passages | ONNX Multilingual-E5 cross-runtime vector reference vectors | JSON | N/A (Vector Fixture) | `7ec87b9c9fba50d2` |

---

## 3. Real vs. Synthetic vs. Expert-Authored Breakdown

A critical finding of this audit is that **ZERO percent of the evaluation datasets consist of real, anonymized student production queries**. There is no production traffic corpus ingested in this repository. Claiming that these datasets reflect "real student behavior" is factually false.

The actual origin breakdown across all active cases is:

| Dataset | Verified Curriculum / Textbook | Expert-Authored Synthetic Policy | Synthetic Security / Adversarial | Anonymized Real Student Queries | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `domain_scope_benchmark.jsonl` | 13 (CBSE/NCERT) | 41 (Policy Probes) | 6 (Adversarial Probes) | **0** | **60** |
| `golden_set.jsonl` | 20 (CBSE Class 10 Syllabus) | 5 (Missing Evidence Traps) | 0 | **0** | **25** |
| `generation_eval_set.jsonl` | 7 (NCERT Science/Math) | 2 (General / Boundary) | 1 (Misleading Premise) | **0** | **10** |
| **Total** | **40 (42.1%)** | **48 (50.5%)** | **7 (7.4%)** | **0 (0.0%)** | **95** |

### Provenance Audit Findings:
1. **Verified Curriculum Cases (40 cases):** These questions are derived from official NCERT Class 9 and 10 Science (Chemical Reactions, Acids/Bases/Salts, Electricity, Life Processes), Mathematics (Quadratic Equations, Probability), and Social Science (Democratic Politics) syllabus documents. Expected answers and fact lists are ground-truthed against published NCERT textbooks.
2. **Expert-Authored Synthetic Cases (48 cases):** Developed by internal evaluation engineers to probe system boundary rules (financial speculation, sports betting, celebrity gossip, election campaigning, medical diagnosis, ambiguous keywords like "law", "bond", "spectrum", and mixed-intent conjunctions).
3. **Synthetic Security Cases (7 cases):** Developed to probe prompt injection, grandma persona jailbreaks, ransomware/malware generation requests, and board exam question paper leak inquiries (`EXAM_CHEATING`).
4. **Independent Review Status:** All expected behaviors in `domain_scope_benchmark.jsonl`, `golden_set.jsonl`, and `generation_eval_set.jsonl` have been reviewed by internal repository engineers. **Independent external educator review has NOT occurred.**

---

## 4. Edge-Case Coverage Matrix

The table below audits actual coverage across the five functional domains required for safe chatbot deployment:

| Behavior Category | Sub-Category | Tested Behavior | Actual Case Count | Test Method (Mock vs Rule vs Live) | Missing / Gapped Coverage |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Domain & Intent** | In-Domain Student Curriculum | Chemistry, Physics, Biology, Math syllabus questions | 13 | Deterministic `DomainPolicyService` | Upper grades (Class 11-12) unrepresented |
| | In-Domain General Educational | CS algorithms, World History, General Biology, Databases | 10 | Deterministic `DomainPolicyService` | Advanced engineering topics limited |
| | Out-of-Domain Prohibited | Crypto, day trading, lottery, casino, gossip, party politics, medical prescription, malware, exam cheating | 18 | Deterministic `DomainPolicyService` | Slang in regional Indian languages (Tamil, Marathi) |
| | Ambiguous Clarification | "Explain the law", "What is a bond?", "How do cells divide?", "What are roots?", "Explain the spectrum", "What is power?" | 7 | Deterministic Regex Dispatcher | Broad open-ended single-word queries (e.g. "gravity") |
| | Mixed-Intent Requests | Academic query + prohibited query (e.g. Newton's law + crypto, math + betting, biology + exam leak) | 7 | Regex Conjunction Splitter (`re.split`) | Nested / non-conjunction mixed intent (e.g. "Calculate crypto yield") |
| | Adversarial Intent Bypass | Instruction override ("Ignore instructions"), persona adoption ("Grandma jailbreak"), leetspeak/typos ("bitcion") | 5 | Deterministic `DomainPolicyService` | Base64-encoded or obfuscated foreign language jailbreaks |
| | Sensitive Educational Topics | Penicillin in biology, cryptography in CS, probability in dice, elections in civics | 4 | Deterministic `DomainPolicyService` | Human reproduction and sensitive history |
| **RAG & Grounding** | Relevant Evidence (Sufficient) | Ranking of gold passage in top 1-5 chunks | 20 | Simulated in `MockRetrievalExecutor`; Live in `SupabaseRetrievalService` | Live database contains only Grade 10 CBSE chunks |
| | Distractor / Irrelevant Evidence | Ranking expected chunk above synthetic distractors | 20 | Handcrafted distractor injection | Dynamic semantic hard-negative mining |
| | Conflicting Evidence | Conflicting sea-level vs altitude boiling point | 1 | Unit test (`GEN-009`) | Real multi-textbook conflicting claims |
| | Missing Evidence Abstention | Quantum entanglement, CRISPR, Navier-Stokes, Remdesivir | 8 | Real `EvidenceOutcome.MISSING` gate | Full Class 10 unindexed topics sweep |
| | Cross-Curriculum Leakage | Semiconductor physics query submitted under Grade 10 Science | 1 | `golden_set.jsonl: EVAL-SCI10-021` | Systematic cross-subject leakage (e.g. Math in History) |
| | Multi-Passage Reasoning | Reactivity series synthesis across 4 metals | 1 | `golden_set.jsonl: EVAL-SCI10-025` | 3+ passage multi-hop questions |
| **Generation & Quality** | Factual Correctness | Word-level fact overlap against reference facts | 7 | Unit test regex matching (`GEN-001` - `GEN-004`) | Semantic NLI / LLM-as-a-judge factuality |
| | Unsupported Claim Rate | Hallucination detection against grounded context | 1 | Unit test text containment (`GEN-009`) | Subtle numerical hallucination |
| | Mode Adherence | Step-by-step for Teacher, mechanism for Explainable, direct for General | 3 | Heuristic style regex evaluator | Long-form pedagogy evaluation |
| | Malformed / Empty Inputs | Whitespace-only, tab/newline inputs | 1 | `GEN-008` validation test | Gigantic inputs (>16k tokens) |
| **Security & Isolation** | Multi-Student Tenant Isolation | User A memories and history never leak to User B | 2 | Real SQLite AsyncSession (`Pipeline G`) | Concurrent race conditions under 100+ threads |
| | System Prompt Extraction | Prompt injection attempts to leak instructions | 2 | `test_prompt_construction.py` | Indirect prompt injection via retrieved PDF text |
| | Unauthorized Document Access | Gating checks for restricted curriculum | 4 | `test_syllabus_authorization_gating.py` | Role-based permission escalations |
| **Online / Offline** | Embedding Vector Parity | Pinned ONNX vs PyTorch cosine similarity >= 0.9999 | 10 | Real ONNX runtime numerical comparison | Real physical Android device hardware divergence |
| | GGUF Format Validation | Magic bytes (0x47475546), tensor/metadata headers | 3 | Real binary struct unpacking | Live 4-bit quant runtime memory exhaustion |
| | Offline Model Fallback | Abstention on missing local syllabus | 2 | Real Android JUnit test (`OfflineGenerationQualityTest`) | Thermal throttling recovery on Android |

---

## 5. Duplicate Detection & Holdout Contamination Audit

### A. Intra-Dataset Duplicates
* `domain_scope_benchmark.jsonl`: **40 unique prompts out of 40 in Phase 2; 60 unique prompts out of 60 in upgraded set.** Zero duplicate prompts.
* `golden_set.jsonl`: **25 unique prompts out of 25.** Zero duplicate prompts.
* `generation_eval_set.jsonl`: **10 unique prompts out of 10.** Zero duplicate prompts.

### B. Inter-Dataset Overlap & Holdout Contamination
An automated cross-dataset prompt matching scan produced the following exact findings:

1. **`domain_scope_benchmark.jsonl` vs. `golden_set.jsonl`:** **0 overlapping cases.**
2. **`domain_scope_benchmark.jsonl` vs. `generation_eval_set.jsonl`:** **0 overlapping cases.**
3. **`golden_set.jsonl` vs. `generation_eval_set.jsonl`:** **0 overlapping cases.**
4. **`golden_set.jsonl` vs. `golden_set_ncert_legacy.jsonl`:** **23 overlapping cases (92% overlap).**
   * *Audit Note:* `golden_set_ncert_legacy.jsonl` is the predecessor to `golden_set.jsonl`. It was superseded when curriculum chunk references were updated from legacy NCERT textbook scans to official 2026-27 CBSE curriculum chunks. It should be archived to prevent confusion.
5. **Holdout Partition Isolation in Domain Benchmark:**
   * In `domain_scope_benchmark.jsonl`, cases DOM-001 through DOM-030 are labeled `split: "dev"` (30 cases).
   * Cases DOM-031 through DOM-060 are labeled `split: "holdout"` (30 cases).
   * Intersection of `dev` prompts and `holdout` prompts is **strictly 0**. No development leakage into holdout benchmark.

---

## 6. Metric Integrity Audit

We inspected the actual metric calculation code in [`domain_eval_metrics.py`](file:///e:/Offline%20First/backend/evals/domain/domain_eval_metrics.py), [`retrieval_metrics.py`](file:///e:/Offline%20First/backend/evals/rag/retrieval_metrics.py), and [`answer_eval_metrics.py`](file:///e:/Offline%20First/backend/evals/generation/answer_eval_metrics.py).

### A. Mathematical Integrity Findings

| Metric | Formula | Denominator Guard | Mathematical Correctness | Audit Verdict |
| :--- | :--- | :---: | :---: | :---: |
| **OOD False Acceptance Rate** | `ood_false_accepted / ood_total` | `if denominator <= 0: return None` | Correct | **PASS** |
| **In-Domain False Rejection Rate** | `in_domain_false_rejected / in_domain_total` | `if denominator <= 0: return None` | Correct | **PASS** |
| **Clarification Rate** | `clarification_asked / ambiguous_total` | `if denominator <= 0: return None` | Correct | **PASS** |
| **Mixed-Intent Compliance Rate** | `mixed_intent_compliant / mixed_intent_total` | `if denominator <= 0: return None` | Correct | **PASS** |
| **Missing Evidence Abstention Rate** | `missing_evidence_abstained / missing_evidence_total` | `if denominator <= 0: return None` | Correct | **PASS** |
| **Mean Recall@k** | $\frac{1}{\|Q\|} \sum_{q} \frac{\|R_{k} \cap G\|}{\|G\|}$ | Protected against empty $G$ | Correct | **PASS** |
| **Mean MRR@k** | $\frac{1}{\|Q\|} \sum_{q} \frac{1}{\text{rank}}$ | 0 if no relevant chunk found | Correct | **PASS** |

### B. Critical Metric Flaws Discovered in Legacy Implementation:
1. **The "100% Mock RAG" Illusion:**
   * In `evals/rag/run_eval.py`, `MockRetrievalExecutor` dynamically synthesizes chunk results by placing `expected_chunk_ids` directly at rank 1.
   * As a result, running `python evals/runner.py --suite rag_mock` will **always produce Recall@5 = 1.0000, MRR@10 = 1.0000, and nDCG@10 = 1.0000**.
   * *Correction:* This metric measures only that the scoring code correctly processes a perfect retrieval list. It **must never be reported as the retrieval quality of the EduNova search engine.**
2. **The "Simulated String Generation" Illusion:**
   * In `evals/generation/test_online_generation_eval.py`, responses are not generated by an LLM during CI; they are pulled from a dictionary of hard-coded Python strings (`simulated_responses = {"GEN-001": ...}`).
   * The 100% factual correctness score in that unit test only validates that `compute_factual_correctness()` can detect predetermined keywords in predetermined strings.
   * *Correction:* True model generation quality is only measured when running `evals/generation/run_online_eval.py` against live OpenRouter credentials.

---

## 7. Exact Validation Commands Executed and Results

Every check performed during this audit was executed directly in the repository:

### 1. Schema Validation Across All 95 Test Cases
```powershell
.venv\Scripts\python.exe -c "
import json
from pathlib import Path
from evals.case_schema import validate_case

for p in ['evals/rag/golden_set.jsonl', 'evals/generation/generation_eval_set.jsonl', 'evals/domain/domain_scope_benchmark.jsonl']:
    with open(p, encoding='utf-8') as f:
        for idx, line in enumerate(f, 1):
            if line.strip():
                ok, err = validate_case(json.loads(line))
                assert ok, f'Failed {p} L{idx}: {err}'
    print(f'{p}: 100% VALID')
"
```
* **Result:**
  * `evals/rag/golden_set.jsonl`: **25/25 cases schema-valid.**
  * `evals/generation/generation_eval_set.jsonl`: **10/10 cases schema-valid.**
  * `evals/domain/domain_scope_benchmark.jsonl`: **60/60 cases schema-valid.**
  * **Overall: 95/95 cases strictly conform to `test_case_schema.json`.**

### 2. Holdout Partition Isolation & Contamination Check
```powershell
.venv\Scripts\pytest.exe evals\domain\test_domain_adherence_eval.py -k test_holdout_split_contamination_and_isolation -v
```
* **Result:** **PASSED in 0.38s.** 30 `dev` cases and 30 `holdout` cases verified with **0 overlapping prompts.**

### 3. Full Domain Benchmark Evaluation (60 Cases)
```powershell
.venv\Scripts\pytest.exe evals\domain\test_domain_adherence_eval.py -v -s
```
* **Result:** **4 passed in 1.46s.**
  * Total cases evaluated: **60**
  * OOD False Acceptance Rate (FAR): **0.0000 (0 / 18)**
  * In-Domain False Rejection Rate (FRR): **0.0000 (0 / 23)**
  * Ambiguous Clarification Rate: **1.0000 (7 / 7)**
  * Mixed-Intent Compliance Rate: **1.0000 (7 / 7)**
  * Missing Evidence Preservation Rate: **1.0000 (5 / 5)**
  * Empty Denominator Safety: **Verified (`None` returned on empty category).**

### 4. Runner Integration & Dataset Fingerprint Verification
```powershell
.venv\Scripts\python.exe evals/runner.py --suite domain_adherence
```
* **Result:** **PASS (Exit code 0, Duration: 3.37s).**
  * Machine-readable report generated at `backend/evals/results/latest_report.json`.
  * Verified dataset fingerprint for `domain_benchmark` is recorded in metadata.

---

## 8. Limitations, Known Risks, & Boundaries

1. **Synthetic Bias in Domain Benchmark:**  
   The 60 cases in `domain_scope_benchmark.jsonl` are authored by engineers. They do not capture the long-tail unpredictability of real students (e.g. creative phonetic spelling, multi-lingual slang like Hinglish/Tanglish, or emoji-based prompts).
2. **Deterministic Rules vs. Neural Classifier:**  
   `DomainPolicyService` relies on high-speed regexes and lightweight intent logic. While this guarantees 0ms latency and 0 cost, complex obfuscated attacks or paraphrased cheating inquiries that do not use known keywords will bypass regexes until a local SLM classifier is calibrated in Phase 3/5.
3. **Small Generation Dataset Sample Size ($N=10$):**  
   The generation evaluation dataset contains only 10 cases. A sample of 10 cannot produce statistically significant confidence intervals for factual correctness or hallucination rates. It serves as a regression smoke-test, not a release certification benchmark.
4. **General Mode Retrieval Policy:**  
   General Mode currently returns `EvidenceOutcome.CORPUS_NOT_CONFIGURED` because EduNova has not indexed a general academic document corpus. Product management must formally resolve whether General Mode should retrieve from a web index or rely purely on direct model knowledge.

---

## 9. Prioritized Dataset Improvement Plan (Phases 3–6)

To progress from the current phase to full production readiness, the following dataset improvements are prioritized:

| Priority | Phase Target | Required Action | Acceptance Criteria |
| :---: | :---: | :--- | :--- |
| **P1** | **Phase 3** | Create dedicated adversarial security dataset (`adversarial_security_benchmark.jsonl`) covering indirect prompt injection, jailbreaks, and cross-tenant leakage probes. | Minimum 50 adversarial cases; zero cross-user session leakage. |
| **P2** | **Phase 4** | Expand `golden_set.jsonl` from 25 to 100+ cases covering NCERT Classes 9, 10, 11, and 12 across Physics, Chemistry, Biology, and Math. | All cases ground-truthed against official textbook chunk UUIDs. |
| **P3** | **Phase 4** | Expand `generation_eval_set.jsonl` from 10 to 50+ cases with expert-reviewed reference answers and factual claim rubrics. | Minimum 50 cases covering all explanation modes (Teacher, Explainable, General). |
| **P4** | **Phase 5** | Create authentic offline inference test set (`offline_device_benchmark.jsonl`) measuring latency, memory usage, and answer parity between on-device GGUF and cloud LLM. | Tested on low-end Android emulator / device profiles (3GB RAM). |
| **P5** | **Phase 6** | Collect anonymized, consent-cleared student query logs to create a true production-representative holdout benchmark. | Signed off by Data Privacy Officer and Academic Review Board. |

---

## 10. Audit Verdict

### Verdict: **`ADEQUATE FOR CURRENT PHASE (WITH HONEST LIMITATIONS DOCUMENTED)`**

**Justification:**
1. **Phase 1 Acceptance Criteria are genuinely satisfied:** Strict JSON Schema validation exists and is enforced across 95 cases; the runner produces auditable run metadata with SHA-256 fingerprints; exit codes 0, 1, and 2 correctly reflect passing, failing, and blocked runs.
2. **Phase 2 Acceptance Criteria are genuinely satisfied:** Domain policy boundary enforcement works deterministically; out-of-domain requests are refused; ambiguous queries trigger clarification; mixed intent is decomposed; in-domain missing evidence is not misclassified as OOD.
3. **The evaluation integrity has been restored:** We have exposed the fact that `rag_mock` and `generation_eval_set` use synthetic/simulated fixtures and marked all historical 99%+ claims as unverified. The framework now reports actual empirical measurements honestly without fabricating data.
