# Local Quantized Embedding Models (`backend/models/`)

This directory contains on-disk quantized neural network weights and tokenizer definitions used for offline and local semantic embedding generation.

---

## Directory Overview

```
backend/models/
├── README.md                   # Model architecture and specification document
└── e5_small_quantized/         # Quantized E5 Small ONNX Model
    ├── model_quantized.onnx    # 8-bit quantized ONNX model weights (~118 MB)
    └── tokenizer.json          # Fast Hugging Face tokenizer vocabulary (~17 MB)
```

---

## Model Specifications

| Parameter | Specification |
|---|---|
| **Base Architecture** | `intfloat/multilingual-e5-small` |
| **Quantization Format** | ONNX Runtime INT8 (Dynamic Quantization) |
| **Vector Dimension** | 384 dimensions (float32 normalized) |
| **Normalization** | Unit L2 Euclidean Normalization ($\|v\|_2 = 1.0$) |
| **Input Prefix** | Passages: `"passage: <text>"` / Queries: `"query: <text>"` |
| **Maximum Sequence Length** | 512 tokens |
| **Target Runtime** | CPU / Android ONNX Runtime / Python `onnxruntime` |

---

## Purpose & Parity Verification

This model is used by:
1. **Backend**: `backend/app/services/local_e5_service.py` to embed incoming chat questions before querying Supabase `pgvector`.
2. **Android**: `E5EmbeddingService.kt` to run identical semantic embeddings on-device for local offline search across cached syllabus chunks.

Running the identical quantized ONNX weights across both Python and Android guarantees **100% embedding parity**: a query embedded on a student's mobile phone produces the exact same vector representation as one embedded in the cloud backend.

To verify inference parity between PyTorch and this ONNX model:
```powershell
python backend/scripts/verify_inference_parity.py
```
