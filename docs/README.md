# Project Documentation & System Audits (`docs/`)

This directory serves as the centralized documentation hub for system-wide architectural audits, request flow traces, and corpus inventory reports.

---

## Directory Overview

```
docs/
├── README.md                   # This documentation index
├── architecture/               # High-level architecture and empirical audits
│   └── EDUNOVA_REQUEST_FLOW_AUDIT.md # End-to-end request lifecycle audit (Online & Offline)
└── reports/                    # Inventory audits and data reports
    └── corpus_inventory.json   # Machine-readable inventory of ingested syllabus chunks
```

---

## Key Documents

### 1. [`architecture/EDUNOVA_REQUEST_FLOW_AUDIT.md`](file:///e:/Offline%20First/docs/architecture/EDUNOVA_REQUEST_FLOW_AUDIT.md)
A comprehensive architectural audit tracing the exact execution pathways of user requests across both:
- **Online Cloud Pipeline**: Android UI $\to$ ViewModel $\to$ OkHttp SSE $\to$ FastAPI $\to$ Supabase Hybrid Retrieval $\to$ OpenRouter Gemini LLM $\to$ Client Stream.
- **Offline Edge Pipeline**: Android UI $\to$ ViewModel $\to$ ModeAwareAIRepository $\to$ OfflineModelManager $\to$ JNI C++ $\to$ `llama.cpp` CPU inference $\to$ SQLite cache.
- Identifies critical gaps, RRF mathematical ceilings, and citation metadata handling.

### 2. [`reports/corpus_inventory.json`](file:///e:/Offline%20First/docs/reports/corpus_inventory.json)
Automated audit report cataloging all 14 official CBSE curriculum documents ingested into the Supabase pgvector database, detailing:
- Document UUIDs, academic board, grade levels, and subject names.
- Chunk counts, page ranges, and vector indexing status.
- Verification checksums confirming corpus authenticity.
