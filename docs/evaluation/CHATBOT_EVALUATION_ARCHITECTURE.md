# CHATBOT_EVALUATION_ARCHITECTURE.md — Evaluation Harness Architecture (Revised)

> **Document Version**: 2.1.0  
> **Repository**: EduNova (Offline First)  
> **Core Mandate**: Unify existing evaluators, eliminate duplicated runners, and establish 4 execution profiles

---

## 1. System Architecture Diagram

```mermaid
flowchart TD
    subgraph CLI ["Unified Evaluation CLI (backend/evals/runner.py)"]
        Parser[Argument & Profile Parser]
        Metadata[Run Metadata Collector: Git SHA, Env, Config]
        Orchestrator[Execution Orchestrator]
    end

    subgraph Profiles ["4 Distinct Execution Profiles"]
        P_CI["1. --profile ci (Deterministic PR Checks < 30s)"]
        P_Live["2. --profile scheduled-live (Nightly Live Cloud API)"]
        P_Device["3. --profile device-benchmark (Android AVD Hardware Metrics)"]
        P_RC["4. --profile release-candidate (Full Matrix + 30-case Holdout)"]
    end

    subgraph Adapters ["Unified Evaluation Adapters (Preserving Existing Infrastructure)"]
        A_Domain[Domain & Boundary Adapter]
        A_RAG[RAG Retrieval Adapter (Existing evals/rag/)]
        A_Gen[Generation Quality Adapter (Existing evals/generation/)]
        A_Sec[Security & Adversarial Adapter]
        A_Parity[Cross-Runtime Parity Adapter (Existing evals/parity/)]
    end

    subgraph DecisionEngine ["Regression & Exit Code Engine"]
        Comparator[Directionality Baseline Comparator]
        ExitCodeHandler{Exit Code Logic}
    end

    subgraph OutputArtifacts ["Standardized Auditable Outputs"]
        JSONReport["results/eval_report_TIMESTAMP.json"]
        MDReport["docs/reports/EVALUATION_SUMMARY.md"]
        Exit0["Exit 0: PASS"]
        Exit1["Exit 1: FAIL / REGRESSION"]
        Exit2["Exit 2: BLOCKED / ENV MISSING"]
    end

    Parser --> Orchestrator
    Metadata --> Orchestrator
    Orchestrator --> Profiles
    Profiles --> Adapters
    Adapters --> Comparator
    Comparator --> ExitCodeHandler
    ExitCodeHandler --> JSONReport
    ExitCodeHandler --> MDReport
    ExitCodeHandler --> Exit0
    ExitCodeHandler --> Exit1
    ExitCodeHandler --> Exit2
```

---

## 2. The 4 Dedicated Execution Profiles

To prevent slow network calls from delaying PRs and to ensure comprehensive release audits:

### Profile 1: Deterministic PR Checks (`--profile ci`)
- **Execution Target**: Runs on every GitHub Actions Pull Request.
- **Duration Budget**: $< 30$ seconds.
- **Network Requirement**: Zero external network (all mock / in-memory).
- **Suites Executed**:
  - `domain_adherence` (Mock mode)
  - `security_adversarial` (Mock injection & secret scrubbing)
  - `rag_retrieval_mock` (Deterministic arithmetic invariant on 100 cases)
  - `provider_resilience` (HTTP status codes via `respx`)
  - `pipeline_matrix` (Pipelines A–G contract tests via ASGI)
  - `model_parity` (Python ONNX vs JVM fixture)

### Profile 2: Scheduled Live Evaluation (`--profile scheduled-live`)
- **Execution Target**: Scheduled nightly or weekly in CI.
- **Duration Budget**: 3–5 minutes.
- **Network Requirement**: Active internet with Supabase PostgreSQL and OpenRouter API credentials.
- **Suites Executed**:
  - `rag_retrieval_live` (Live pgvector hybrid search against production syllabus)
  - `online_generation` (Live inference on `gemini-3.8-flash` evaluating factual correctness and latency)

### Profile 3: Android Device Benchmarks (`--profile device-benchmark`)
- **Execution Target**: Triggered prior to mobile releases or nightly on connected emulator (`Pixel_9(AVD)`).
- **Suites Executed**:
  - `OfflineModelAndParityInstrumentedTest` (JNI `llama.cpp` inference speed, TTFT, PSS RAM, exact consistency)
  - `ChatbotLowRamBenchmarkTest` (Memory stress test under 4 GB device profile)

### Profile 4: Release-Candidate Holdout Evaluation (`--profile release-candidate`)
- **Execution Target**: Manual trigger before tag release.
- **Suites Executed**:
  - Runs all suites from Profiles 1, 2, and 3.
  - Ingests the **unexposed 30-case holdout dataset** (`holdout_evaluation_set.jsonl`).
  - Compares results against `backend/evals/baselines/baseline_v2.0.json`.
  - **Zero regressions permitted** across any metric.

---

## 3. Exit Code Semantics

The runner strictly enforces 3 distinct exit codes to avoid masking infrastructure outages as test passes:

| Exit Code | Verdict | Operational Meaning | Action |
|:---:|---|---|---|
| **0** | **PASS** | All targeted test cases executed and passed; regression gates satisfied. | PR approved for merge; release unblocked. |
| **1** | **FAIL / REGRESSION** | Test assertion failure, security breach, or metric dropped below baseline tolerance. | PR blocked; immediate engineer remediation required. |
| **2** | **BLOCKED** | Required external environment unavailable (e.g. Supabase credentials absent, Android AVD not connected, network offline). | Logged as `BLOCKED`; does NOT count as a pass. |

---

## 4. Machine-Readable Run Metadata Schema

Every evaluation run outputs a standardized JSON artifact:

```json
{
  "run_id": "eval-20261022-225010-f4b2",
  "timestamp": "2026-10-22T22:50:10.512Z",
  "git_commit": "dee300271e4161f903d32b2ecda1401c6f52008f",
  "branch": "master",
  "profile": "ci",
  "python_version": "3.11.9",
  "model_config": {
    "provider": "openrouter",
    "primary_model": "google/gemini-3.8-flash",
    "temperature": 0.0,
    "seed": 42,
    "max_tokens": 1024
  },
  "dataset_versions": {
    "domain_scope": "2.1.0",
    "rag_golden_set": "3.0.0",
    "generation_eval_set": "2.0.0",
    "security_adversarial": "2.1.0",
    "holdout_set": "1.0.0"
  },
  "results_by_category": {
    "domain_adherence": { "status": "PASS", "passed": 80, "failed": 0, "blocked": 0 },
    "security_adversarial": { "status": "PASS", "passed": 50, "failed": 0, "blocked": 0 },
    "rag_retrieval": { "status": "PASS", "passed": 100, "failed": 0, "blocked": 0 },
    "generation_quality": { "status": "PASS", "passed": 50, "failed": 0, "blocked": 0 }
  },
  "verdict": "PASS",
  "exit_code": 0
}
```
