"""Generate reference cross-runtime parity fixtures using pinned ONNX artifact.

Runs actual inference with LocalE5EmbeddingService on verified NCERT Class 10
Science queries and passages in English and Hindi, along with out-of-syllabus distractors.
Outputs backend/evals/parity/parity_fixtures.json with exact token IDs, attention masks,
L2-normalized embeddings, and metadata.
"""

import hashlib
import json
import math
from pathlib import Path
import sys

# Ensure backend root is on sys.path
backend_dir = Path(__file__).resolve().parent.parent.parent
if str(backend_dir) not in sys.path:
    sys.path.insert(0, str(backend_dir))

import onnxruntime as ort
import tokenizers
from app.services.local_e5_service import (
    local_e5_service,
    ONNX_QUANTIZED_SHA256,
    TOKENIZER_SHA256,
    EMBEDDING_DIM,
    MODEL_VERSION,
    DEFAULT_MODEL_NAME,
)

FIXTURE_ITEMS = [
    # --- Item 1: Chemical Reactions (English) ---
    {
        "id": "PARITY-001",
        "language": "en",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "Chemical Reactions and Equations",
        "source_ref": "NCERT Class 10 Science Chapter 1 Section 1.1 p.2",
        "query": "What happens when magnesium ribbon is burned in air?",
        "passage": "When a magnesium ribbon is burnt in oxygen or air, it burns with a dazzling white flame and changes into a white powder. This powder is magnesium oxide (MgO), formed due to the reaction between magnesium and oxygen present in the air.",
        "is_out_of_syllabus": False,
    },
    # --- Item 2: Acids, Bases & Salts (English) ---
    {
        "id": "PARITY-002",
        "language": "en",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "Acids, Bases and Salts",
        "source_ref": "NCERT Class 10 Science Chapter 2 Section 2.1 p.19",
        "query": "How do acids and bases react with each other in neutralization?",
        "passage": "The reaction between an acid and a base to give a salt and water is known as a neutralization reaction. In general, a neutralization reaction can be written as Base + Acid gives Salt + Water, for example NaOH + HCl gives NaCl + H2O.",
        "is_out_of_syllabus": False,
    },
    # --- Item 3: Life Processes - Photosynthesis (English) ---
    {
        "id": "PARITY-003",
        "language": "en",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "Life Processes",
        "source_ref": "NCERT Class 10 Science Chapter 6 Section 6.2 p.95",
        "query": "What are the events that occur during the process of photosynthesis?",
        "passage": "During photosynthesis, absorption of light energy by chlorophyll occurs, followed by conversion of light energy to chemical energy and splitting of water molecules into hydrogen and oxygen, and finally reduction of carbon dioxide to carbohydrates.",
        "is_out_of_syllabus": False,
    },
    # --- Item 4: Light - Reflection and Refraction (English) ---
    {
        "id": "PARITY-004",
        "language": "en",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "Light - Reflection and Refraction",
        "source_ref": "NCERT Class 10 Science Chapter 10 Section 10.3 p.171",
        "query": "State Snell's law of refraction of light.",
        "passage": "The ratio of sine of angle of incidence to the sine of angle of refraction is a constant, for the light of a given colour and for the given pair of media. This law is also known as Snell's law of refraction: sin(i) / sin(r) = constant.",
        "is_out_of_syllabus": False,
    },
    # --- Item 5: Electricity - Ohm's Law (English) ---
    {
        "id": "PARITY-005",
        "language": "en",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "Electricity",
        "source_ref": "NCERT Class 10 Science Chapter 12 Section 12.4 p.204",
        "query": "State Ohm's law and write the formula relating potential difference and current.",
        "passage": "Ohm's law states that the electric current flowing through a metallic wire is directly proportional to the potential difference across its ends, provided its temperature remains the same: V = IR, where R is the constant of proportionality called resistance.",
        "is_out_of_syllabus": False,
    },
    # --- Item 6: Chemical Reactions (Hindi target language) ---
    {
        "id": "PARITY-006",
        "language": "hi",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "रासायनिक अभिक्रियाएं एवं समीकरण",
        "source_ref": "NCERT Class 10 Science (Hindi) Chapter 1 Section 1.1 p.2",
        "query": "मैग्नीशियम रिबन को वायु में जलाने पर क्या होता है?",
        "passage": "चमकदार श्वेत लौ के साथ मैग्नीशियम रिबन का दहन होता है और यह श्वेत चूर्ण में परिवर्तित हो जाता है। यह मैग्नीशियम ऑक्साइड का चूर्ण है। वायु में उपस्थित ऑक्सीजन तथा मैग्नीशियम के बीच होने वाली अभिक्रिया के कारण यह बनता है।",
        "is_out_of_syllabus": False,
    },
    # --- Item 7: Electricity - Ohm's Law (Hindi target language) ---
    {
        "id": "PARITY-007",
        "language": "hi",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "विद्युत",
        "source_ref": "NCERT Class 10 Science (Hindi) Chapter 12 Section 12.4 p.204",
        "query": "ओम का नियम क्या है और विभवान्तर तथा विद्युत धारा का संबंध क्या है?",
        "passage": "एक धातु के तार में प्रवाहित होने वाली विद्युत धारा उस तार के सिरों के बीच विभवान्तर के समानुपाती होती है, बशर्ते कि तार का ताप नियत रहे। इसे ओम का नियम कहते हैं: V = IR, जहाँ R चालक का प्रतिरोध है।",
        "is_out_of_syllabus": False,
    },
    # --- Item 8: Out-of-syllabus distractor 1 ---
    {
        "id": "PARITY-008",
        "language": "en",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "N/A (Out of Syllabus)",
        "source_ref": "External Distractor",
        "query": "Explain quantum entanglement and EPR paradox in photosynthesis light harvesting complexes.",
        "passage": "Quantum entanglement describes quantum states of two or more objects that must be described with reference to each other, even when separated by large spatial intervals, contradicting local realism under Bell inequality formulations.",
        "is_out_of_syllabus": True,
    },
    # --- Item 9: Out-of-syllabus distractor 2 ---
    {
        "id": "PARITY-009",
        "language": "en",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "N/A (Out of Syllabus)",
        "source_ref": "External Distractor",
        "query": "Describe CRISPR Cas9 guide RNA targeted double strand breaks and non-homologous end joining repair.",
        "passage": "The CRISPR-Cas9 endonuclease complex utilizes a single guide RNA to introduce site-specific double-strand breaks in target DNA sequences, which are repaired by either non-homologous end joining or homology-directed repair pathways.",
        "is_out_of_syllabus": True,
    },
    # --- Item 10: Out-of-syllabus distractor 3 ---
    {
        "id": "PARITY-010",
        "language": "en",
        "board": "CBSE",
        "class_grade": "10",
        "subject": "Science",
        "chapter": "N/A (Out of Syllabus)",
        "source_ref": "External Distractor",
        "query": "Derive the Fermi Dirac distribution function for degenerate electron gas in solid state physics.",
        "passage": "The Fermi-Dirac distribution defines the probability that an available energy state E will be occupied by an electron at thermodynamic temperature T: f(E) = 1 / (exp((E - mu)/(k_B * T)) + 1), adhering to the Pauli exclusion principle for fermions.",
        "is_out_of_syllabus": True,
    },
]


def run_quantized_onnx_inference(session, tokenizer, text: str):
    """Executes verified quantized ONNX model inference and returns tokens, mask, and embedding."""
    enc = tokenizer.encode(text)
    input_ids = enc.ids[:512]
    attention_mask = enc.attention_mask[:512]
    token_type_ids = enc.type_ids[:512] if hasattr(enc, "type_ids") else [0] * len(input_ids)

    feed = {
        "input_ids": [input_ids],
        "attention_mask": [attention_mask],
    }
    model_inputs = [inp.name for inp in session.get_inputs()]
    if "token_type_ids" in model_inputs:
        feed["token_type_ids"] = [token_type_ids]

    outputs = session.run(None, feed)
    last_hidden = outputs[0][0]  # shape: (seq_len, hidden_dim)

    # Mean pooling over attention mask
    sum_emb = [0.0] * EMBEDDING_DIM
    count = sum(attention_mask)
    for i, m in enumerate(attention_mask):
        if m > 0:
            for j in range(EMBEDDING_DIM):
                sum_emb[j] += float(last_hidden[i][j])
    if count > 0:
        sum_emb = [x / count for x in sum_emb]

    # L2 normalize
    norm = math.sqrt(sum(x * x for x in sum_emb))
    if norm > 1e-12:
        sum_emb = [round(x / norm, 7) for x in sum_emb]

    return {
        "token_ids": input_ids,
        "tokens_text": [tokenizer.id_to_token(i) for i in input_ids],
        "attention_mask": attention_mask,
        "embedding": sum_emb,
        "token_count": len(input_ids),
        "l2_norm": round(math.sqrt(sum(x * x for x in sum_emb)), 6),
    }


def main():
    print("Verifying quantized ONNX model and tokenizer in cache...")
    assert local_e5_service.ensure_quantized_model_cached(), "Cached artifacts failed SHA-256 check!"

    sess_options = ort.SessionOptions()
    sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
    session = ort.InferenceSession(
        str(local_e5_service.model_file), sess_options, providers=["CPUExecutionProvider"]
    )
    tokenizer = tokenizers.Tokenizer.from_file(str(local_e5_service.tokenizer_file))

    # Compute actual SHA-256 digests
    with open(local_e5_service.model_file, "rb") as f:
        actual_model_sha = hashlib.sha256(f.read()).hexdigest().lower()
    with open(local_e5_service.tokenizer_file, "rb") as f:
        actual_tok_sha = hashlib.sha256(f.read()).hexdigest().lower()

    output_items = []
    for item in FIXTURE_ITEMS:
        # Queries require 'query: ' prefix
        query_prefixed = f"query: {item['query'].strip()}"
        q_result = run_quantized_onnx_inference(session, tokenizer, query_prefixed)

        # Passages require 'passage: ' prefix
        passage_prefixed = f"passage: {item['passage'].strip()}"
        p_result = run_quantized_onnx_inference(session, tokenizer, passage_prefixed)

        # Calculate cosine similarity between query and its passage
        cos_sim = sum(q * p for q, p in zip(q_result["embedding"], p_result["embedding"]))

        output_items.append({
            "id": item["id"],
            "language": item["language"],
            "board": item["board"],
            "class_grade": item["class_grade"],
            "subject": item["subject"],
            "chapter": item["chapter"],
            "source_ref": item["source_ref"],
            "is_out_of_syllabus": item["is_out_of_syllabus"],
            "query": {
                "raw_text": item["query"],
                "prefixed_text": query_prefixed,
                "token_ids": q_result["token_ids"],
                "tokens_text": q_result["tokens_text"],
                "attention_mask": q_result["attention_mask"],
                "token_count": q_result["token_count"],
                "embedding": q_result["embedding"],
                "l2_norm": q_result["l2_norm"],
            },
            "passage": {
                "raw_text": item["passage"],
                "prefixed_text": passage_prefixed,
                "token_ids": p_result["token_ids"],
                "tokens_text": p_result["tokens_text"],
                "attention_mask": p_result["attention_mask"],
                "token_count": p_result["token_count"],
                "embedding": p_result["embedding"],
                "l2_norm": p_result["l2_norm"],
            },
            "direct_cosine_similarity": round(cos_sim, 5),
        })

    fixture_data = {
        "metadata": {
            "model_id": DEFAULT_MODEL_NAME,
            "model_version": MODEL_VERSION,
            "model_file_name": "model_quantized.onnx",
            "model_sha256": actual_model_sha,
            "tokenizer_file_name": "tokenizer.json",
            "tokenizer_sha256": actual_tok_sha,
            "embedding_dimension": EMBEDDING_DIM,
            "max_sequence_length": 512,
            "preprocessing": {
                "query_prefix": "query: ",
                "passage_prefix": "passage: ",
                "truncation": "max 512 tokens",
                "special_tokens": ["<s>", "</s>"],
            },
            "pooling": "mean_over_attention_mask",
            "normalization": "L2",
            "tolerances": {
                "cosine_min": 0.999,
                "max_absolute_difference": 0.005,
                "l2_norm_tolerance": 0.0001,
            },
        },
        "fixtures": output_items,
    }

    out_path = Path(__file__).resolve().parent / "parity_fixtures.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(fixture_data, f, indent=2, ensure_ascii=False)

    print(f"Successfully generated {len(output_items)} reference parity fixtures to {out_path}")


if __name__ == "__main__":
    main()
