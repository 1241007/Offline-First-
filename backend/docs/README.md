# Backend Research & Architectural Notes (`backend/docs/`)

This directory contains research papers, mathematical studies, and engineering evaluation reports focused on the EduNova backend, with particular emphasis on retrieval-augmented generation (RAG) and scoring algorithms.

---

## Documents in this Directory

- **[`research/RETRIEVAL_THRESHOLD_EVALUATION.md`](file:///e:/Offline%20First/backend/docs/research/RETRIEVAL_THRESHOLD_EVALUATION.md)**:
  - Deep empirical and mathematical study of Reciprocal Rank Fusion (RRF) score normalization bounds under two-retriever settings ($k=60$).
  - Explains the Single-Retriever Ceiling where dense-only matches are capped at `0.5000`.
  - Details the empirical calibration that adjusted the `SUFFICIENT` threshold from `0.70` to `0.48 - 0.50`, eliminating false abstentions on student paraphrases while preventing hallucinations on trap queries.
