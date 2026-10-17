import json
import re
from app.services.supabase_retrieval_service import CORROBORATION_STOPWORDS

with open('backend/evals/rag/golden_set.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        case = json.loads(line)
        q = case['question']
        words = re.findall(r'\b[a-zA-Z]{3,}\b', q.lower())
        domain_words = [w for w in words if w not in CORROBORATION_STOPWORDS]
        print(f"{case['case_id']} ({len(domain_words)} terms): {domain_words}")
