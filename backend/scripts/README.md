# Backend Operational & Ingestion Scripts (`backend/scripts/`)

This directory contains standalone Python CLI scripts for database seeding, syllabus corpus ingestion, embedding generation, model artifact downloads, and inference parity verification.

---

## Script Inventory

| Script | Purpose | Usage Command |
|---|---|---|
| [`ingest_syllabus.py`](file:///e:/Offline%20First/backend/scripts/ingest_syllabus.py) | Parses CBSE/NCERT curriculum PDFs, splits text into semantically cohesive chunks, generates 384-dim E5 embeddings, and upserts into Supabase `syllabus_documents` and `syllabus_chunks`. | `python backend/scripts/ingest_syllabus.py --dir backend/syllabus_sources/CBSE/10/Science` |
| [`seed_data.py`](file:///e:/Offline%20First/backend/scripts/seed_data.py) | Populates the database with initial mock catalog courses, modules, lessons, and career roadmaps for development. | `python backend/scripts/seed_data.py` |
| [`reembed_corpus.py`](file:///e:/Offline%20First/backend/scripts/reembed_corpus.py) | Iterates over existing syllabus chunks in the database and recomputes embeddings using updated ONNX weights or prefix standards. | `python backend/scripts/reembed_corpus.py` |
| [`download_model_artifacts.py`](file:///e:/Offline%20First/backend/scripts/download_model_artifacts.py) | Downloads quantized E5 ONNX model weights and tokenizer configurations from Hugging Face into `backend/models/`. | `python backend/scripts/download_model_artifacts.py` |
| [`verify_inference_parity.py`](file:///e:/Offline%20First/backend/scripts/verify_inference_parity.py) | Compares cosine similarity outputs between standard PyTorch `intfloat/multilingual-e5-small` and the INT8 quantized ONNX runtime to ensure mathematical parity ($> 0.999$). | `python backend/scripts/verify_inference_parity.py` |
| [`audit_corpus_inventory.py`](file:///e:/Offline%20First/backend/scripts/audit_corpus_inventory.py) | Scans the database and exports an audit report of all ingested documents, page ranges, chunk counts, and vector health. | `python backend/scripts/audit_corpus_inventory.py` |

---

## Standard Workflow: Ingesting New Syllabus PDFs

1. Place the official PDF into `backend/syllabus_sources/CBSE/{Grade}/{Subject}/`.
2. Create or verify `sources.json` and `.meta.json` in that directory.
3. Run the ingestion script:
   ```powershell
   cd backend
   .\.venv\Scripts\python.exe scripts/ingest_syllabus.py --dir syllabus_sources/CBSE/10/Science
   ```
4. Verify ingestion by checking the corpus inventory:
   ```powershell
   .\.venv\Scripts\python.exe scripts/audit_corpus_inventory.py
   ```
