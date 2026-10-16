# EduNova Evaluation Framework (`backend/evals/`)

This directory houses the version-controlled evaluation suites and quality benchmarks for EduNova's intelligence pipelines, including RAG retrieval accuracy, grounding metrics, and model response quality.

---

## Directory Overview

```
backend/evals/
├── README.md                   # This evaluation suite overview
└── rag/                        # Retrieval-Augmented Generation evaluation harness
    ├── README.md               # Detailed metric formulas & execution guide
    ├── golden_set.jsonl        # Verified ground-truth evaluation benchmark cases
    ├── retrieval_metrics.py    # Deterministic metrics: Recall@K, MRR@K, nDCG@10, Leakage
    ├── answer_metrics.py       # Groundedness, citation accuracy, and hallucination metrics
    ├── run_eval.py             # Reproducible CLI evaluation test runner
    ├── threshold_analysis.py   # RRF score threshold sensitivity sweep tool
    └── results/                # Machine-readable JSON reports & Markdown summaries
```

---

## Core Principles

1. **Grounded in Genuine Curriculum**: All benchmark cases in `golden_set.jsonl` are derived strictly from genuine official NCERT and CBSE textbook records.
2. **Zero Synthetic Syllabus Contamination**: Test fixtures are evaluated against live database chunks without inserting synthetic test records into the production corpus.
3. **Cross-Curriculum Leakage Detection**: Tracks whether queries targeted at Class 10 inadvertently retrieve chunks from Class 12 or other boards.
4. **Empty-Evidence Abstention**: Evaluates whether unanswerable or out-of-syllabus questions trigger an honest `MISSING` evidence classification rather than an ungrounded hallucination.

---

## Quick Start: Running RAG Evaluations

To run the automated retrieval evaluation against the active Supabase database:

```powershell
cd backend
.\.venv\Scripts\python.exe evals/rag/run_eval.py --suite retrieval --output-dir evals/rag/results/
```

For full details on metric formulas, baseline results, and threshold calibration, see:
👉 **[`backend/evals/rag/README.md`](file:///e:/Offline%20First/backend/evals/rag/README.md)**
