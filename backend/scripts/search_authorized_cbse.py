import json

with open('backend/evals/rag/authorized_cbse_chunks.json', 'r', encoding='utf-8') as f:
    chunks = json.load(f)

for c in chunks:
    content = c['content']
    found = []
    for word in ['magnesium', 'oxygen', 'burn', 'copper', 'calcium', 'slaked', 'iron', 'quicklime']:
        if word in content.lower():
            found.append(word)
    if found:
        print(f"Chunk {c['id']} ({c['page_reference']}): found {found}")
