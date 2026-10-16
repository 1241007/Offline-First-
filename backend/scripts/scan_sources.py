import os
from pathlib import Path
import json

def scan_sources():
    sources_dir = Path("syllabus_sources")
    all_files = list(sources_dir.rglob("*"))
    files_only = [f for f in all_files if f.is_file()]
    
    pdfs = [f for f in files_only if f.suffix.lower() == ".pdf"]
    metas = [f for f in files_only if f.name.endswith(".meta.json")]
    manifests = [f for f in files_only if f.name == "manifest.json" or f.name.endswith("_manifest.json") or f.name == "sources.json"]
    txts = [f for f in files_only if f.suffix.lower() in [".txt", ".md"]]
    others = [f for f in files_only if f not in pdfs and f not in metas and f not in manifests and f not in txts]
    
    print(f"Total files in syllabus_sources: {len(files_only)}")
    print(f"PDFs: {len(pdfs)}")
    print(f"Meta json sidecars: {len(metas)}")
    print(f"Manifests: {len(manifests)}")
    print(f"Txt/Md files: {len(txts)}")
    print(f"Other files: {len(others)}")
    
    # Categorize PDFs:
    # 1. CBSE curriculum framework PDFs
    # 2. Textbook PDFs
    # 3. Any other PDFs
    curriculum_pdfs = []
    textbook_pdfs = []
    other_pdfs = []
    
    for p in pdfs:
        p_str = str(p).lower()
        if "curriculum" in p.name.lower() or "cbseacademic" in p_str:
            curriculum_pdfs.append(p)
        elif "ch" in p.name.lower() or "ncert" in p_str or "textbook" in p_str:
            textbook_pdfs.append(p)
        else:
            other_pdfs.append(p)
            
    print(f"\nCurriculum PDFs: {len(curriculum_pdfs)}")
    print(f"Textbook PDFs: {len(textbook_pdfs)}")
    print(f"Other PDFs: {len(other_pdfs)}")
    if other_pdfs:
        for p in other_pdfs:
            print("  Other PDF:", p)

    # Let's inspect sidecars for any non-NCERT or authorized textbooks
    authorized_textbooks = []
    gated_textbooks = []
    no_meta_textbooks = []
    
    for p in textbook_pdfs:
        sidecar = p.with_name(p.name + ".meta.json")
        if not sidecar.exists():
            sidecar = p.with_suffix(".meta.json")
        if sidecar.exists():
            try:
                with open(sidecar, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    status = data.get("authorization_status", "")
                    if "AUTHORIZED" in status and "GATED" not in status:
                        authorized_textbooks.append((p, status))
                    else:
                        gated_textbooks.append((p, status))
            except Exception as e:
                gated_textbooks.append((p, f"Error: {e}"))
        else:
            no_meta_textbooks.append(p)
            
    print(f"\nTextbook breakdown:")
    print(f"  Authorized according to sidecar: {len(authorized_textbooks)}")
    print(f"  Gated according to sidecar: {len(gated_textbooks)}")
    print(f"  No sidecar found: {len(no_meta_textbooks)}")
    if no_meta_textbooks:
        print("  Sample no-sidecar textbooks:")
        for p in no_meta_textbooks[:15]:
            print("   ", p)

if __name__ == "__main__":
    scan_sources()
