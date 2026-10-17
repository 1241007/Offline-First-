import json

with open('backend/evals/rag/authorized_cbse_chunks.json', 'r', encoding='utf-8') as f:
    cbse_chunks = json.load(f)

with open('backend/evals/rag/golden_set.jsonl', 'r', encoding='utf-8') as f:
    golden_cases = [json.loads(line) for line in f if line.strip() and not line.startswith('#')]

print("Analyzing 25 golden cases against 9 authorized CBSE chunks:")
for case in golden_cases:
    cid = case['case_id']
    q = case['question']
    exp_behavior = case['expected_behavior']
    
    matching_chunks = []
    for c in cbse_chunks:
        # Check if topic is in content
        c_id = c['id']
        page = c['page_reference']
        content = c['content']
        # simple keyword check
        keywords = []
        if 'quicklime' in q.lower() or 'calcium oxide' in q.lower():
            if 'quicklime' in content.lower(): matching_chunks.append((c_id, page, "quicklime"))
        if 'iron displace copper' in q.lower() or 'copper sulphate' in q.lower():
            if 'copper sulphate' in content.lower(): matching_chunks.append((c_id, page, "copper sulphate"))
        if 'sodium sulphate' in q.lower() or 'barium chloride' in q.lower():
            if 'sodium sulphate' in content.lower(): matching_chunks.append((c_id, page, "sodium sulphate"))
        if 'combination' in q.lower() or 'chemical reactions' in q.lower():
            if 'combination' in content.lower(): matching_chunks.append((c_id, page, "combination reaction"))
        if 'neutralization' in q.lower() or 'acid and base' in q.lower() or 'acids, bases' in q.lower():
            if 'neutralization' in content.lower(): matching_chunks.append((c_id, page, "neutralization"))
        if 'ph scale' in q.lower() or 'strength of an acid' in q.lower() or 'ph' in q.lower():
            if 'ph scale' in content.lower() or 'ph paper' in content.lower(): matching_chunks.append((c_id, page, "pH"))
        if 'electric current' in q.lower() or 'potential difference' in q.lower() or 'ohm' in q.lower():
            if "ohm’s law" in content.lower() or "ohm's law" in content.lower() or "electric current" in content.lower():
                matching_chunks.append((c_id, page, "electricity/ohm"))
        if 'resistance' in q.lower() or 'resistors' in q.lower():
            if "resistance" in content.lower() or "resistors" in content.lower():
                matching_chunks.append((c_id, page, "resistors"))

    print(f"{cid} | exp={exp_behavior} | {q[:55]}...")
    if matching_chunks:
        for mc in matching_chunks:
            print(f"   -> Match: {mc[0]} ({mc[1]}) on keyword '{mc[2]}'")
    else:
        print(f"   -> No direct keyword match found in 9 CBSE chunks")
