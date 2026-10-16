# EduNova RAG Evaluation Framework

This directory contains the durable, version-controlled evaluation suite for EduNova's retrieval-augmented generation (RAG) pipeline.

---

## 1. Directory Structure

```text
backend/evals/rag/
├── README.md              # Framework documentation and metric formulas
├── golden_set.jsonl       # Source-grounded benchmark cases (NCERT Class 10 Science)
├── retrieval_metrics.py   # Deterministic retrieval evaluation (Recall, MRR, nDCG, Leakage)
├── answer_metrics.py      # Answer quality, citation precision, and groundedness metrics
├── run_eval.py            # Reproducible CLI evaluation runner
└── results/               # Machine-readable JSON and human-readable Markdown run reports
```

---

## 2. Core Principles & Grounding Rules

1. **Zero Synthetic Syllabus Ingestion**: Golden cases and expected chunk IDs are derived strictly from genuine official NCERT textbooks and CBSE curriculum documents already ingested into the database. Test fixtures are NEVER inserted as synthetic syllabus records into production Supabase.
2. **Deterministic & Isolated**: Retrieval metrics are computed using mathematically verified functions. Empty ground-truth cases do not inflate recall or reciprocal rank averages.
3. **Curriculum Isolation**: Evaluates whether retrieved passages leak across curriculum boundaries (e.g., retrieving Class 12 Physics when querying Class 10 Science).
4. **Empty-Evidence Abstention**: Assesses whether unanswerable queries properly abstain without triggering ungrounded generative hallucinations.

---

## 3. Metrics Specification

### A. Recall@K
$$\text{Recall@K} = \frac{|\text{Retrieved@K} \cap \text{Expected}|}{|\text{Expected}|}$$
*Note: If `Expected` is empty (unanswerable case), Recall is returned as `None` and excluded from the answerable mean to prevent metric distortion.*

### B. Mean Reciprocal Rank (MRR@K)
$$\text{MRR@K} = \frac{1}{\text{rank of first relevant chunk}} \quad (\text{or } 0.0 \text{ if none in top K})$$

### C. nDCG@10
$$\text{DCG@K} = \sum_{i=1}^{K} \frac{2^{\text{rel}_i} - 1}{\log_2(i + 1)}, \quad \text{nDCG@K} = \frac{\text{DCG@K}}{\text{IDCG@K}}$$

### D. Cross-Curriculum Leakage
$$\text{Leakage} = \frac{|\{c \in \text{Retrieved} : c.\text{board} \neq \text{target.board} \lor c.\text{class} \neq \text{target.class}\}|}{|\text{Retrieved}|}$$

### E. Empty-Result Correctness
Checks whether out-of-scope or unanswerable queries produce `MISSING` or `CORPUS_NOT_CONFIGURED` with 0 chunks retrieved.

---

## 4. Running the Evaluation Suite

### Run Unit Tests on Evaluation Metrics
```powershell
.\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_eval_metrics.py -v
```

### Run Full Retrieval Evaluation on Live Database
```powershell
.\backend\.venv\Scripts\python.exe backend/evals/rag/run_eval.py --suite retrieval --output-dir backend/evals/rag/results/
```
