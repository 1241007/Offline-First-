# EduNova Syllabus Sources Directory

This directory is the designated staging area for authorized educational syllabus documents (e.g., NCERT, CBSE textbooks, chapters, and curriculum reference materials).

> [!IMPORTANT]
> **Corpus Ingestion Pending Authorized Sources**:
> In accordance with educational compliance and copyright integrity, no fabricated or simulated textbook content is committed to this repository. Actual corpus ingestion remains pending until authorized syllabus sources (PDF, Markdown, or TXT) are provided by authorized curriculum administrators.

---

## Directory Organization

Place authorized syllabus files using the hierarchical curriculum folder structure:

```
syllabus_sources/
├── CBSE/
│   ├── 10/
│   │   ├── Science/
│   │   │   ├── Chapter_01_Chemical_Reactions_and_Equations.pdf
│   │   │   ├── Chapter_02_Acids_Bases_and_Salts.pdf
│   │   │   ├── Chapter_03_Metals_and_Non_Metals.pdf
│   │   │   └── Chapter_06_Life_Processes.pdf
│   │   └── Mathematics/
│   │       ├── Chapter_01_Real_Numbers.pdf
│   │       └── Chapter_02_Polynomials.pdf
│   └── 12/
│       └── Physics/
│           └── Chapter_01_Electric_Charges_and_Fields.pdf
└── ICSE/
    └── 10/
        └── Biology/
```

---

## Filename Conventions and Metadata

The ingestion pipeline automatically parses curriculum metadata from the directory path and filenames:

- **Board**: Derived from top-level directory (`CBSE`, `ICSE`, `StateBoard`, etc.)
- **Class / Grade**: Derived from grade directory (`9`, `10`, `11`, `12`)
- **Subject**: Derived from subject directory (`Science`, `Mathematics`, `Physics`, etc.)
- **Chapter / Title**: Extracted from filename (e.g., `Chapter_06_Life_Processes.pdf` -> `Life Processes`)

---

## Running the Ingestion Pipeline

To run the syllabus ingestion pipeline once authorized sources are placed:

```bash
# Dry run (validates files, text extraction, chunking, and metadata without database insertion):
python -m scripts.ingest_syllabus --source-dir ./syllabus_sources --dry-run

# Full ingestion (extracts, embeds with 384-dim E5 model, deduplicates by content hash, and inserts into Supabase):
python -m scripts.ingest_syllabus --source-dir ./syllabus_sources
```
