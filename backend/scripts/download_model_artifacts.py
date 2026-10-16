"""
EduNova Model Artifact Downloader and Integrity Verifier.

Downloads the exact pinned Xenova/multilingual-e5-small quantized ONNX model
and tokenizer files shared between FastAPI and Android ARM64, and verifies
strict SHA-256 checksums before enabling live inference.
"""

import hashlib
import logging
import math
import os
import sys
import time
from pathlib import Path
import httpx

# Ensure backend root is on sys.path
_BACKEND_ROOT = Path(__file__).resolve().parents[1]
if str(_BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(_BACKEND_ROOT))

from app.services.local_e5_service import (
    CACHE_DIR,
    EMBEDDING_DIM,
    ONNX_QUANTIZED_SHA256,
    ONNX_QUANTIZED_SIZE_BYTES,
    ONNX_QUANTIZED_URL,
    TOKENIZER_SHA256,
    TOKENIZER_SIZE_BYTES,
    TOKENIZER_URL,
    verify_file_sha256,
)

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("download_model")


def download_file(url: str, dest_path: Path, expected_size: int, expected_sha256: str) -> bool:
    """Downloads a file via streaming HTTP GET and verifies its SHA-256 checksum."""
    if dest_path.exists():
        if verify_file_sha256(dest_path, expected_sha256):
            logger.info(f"Verified cached artifact: {dest_path.name} (SHA-256 matches)")
            return True
        logger.warning(f"Cached {dest_path.name} failed SHA-256 verification. Re-downloading...")
        dest_path.unlink()

    dest_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = dest_path.with_suffix(".tmp")
    if tmp_path.exists():
        tmp_path.unlink()

    logger.info(f"Downloading {dest_path.name} ({expected_size / (1024 * 1024):.1f} MB) from {url}...")
    t0 = time.time()

    hasher = hashlib.sha256()
    downloaded = 0

    with httpx.Client(follow_redirects=True, timeout=120.0) as client:
        with client.stream("GET", url) as response:
            if response.status_code != 200:
                logger.error(f"HTTP {response.status_code} fetching {url}")
                return False

            with open(tmp_path, "wb") as f:
                for chunk in response.iter_bytes(chunk_size=65536):
                    if chunk:
                        f.write(chunk)
                        hasher.update(chunk)
                        downloaded += len(chunk)
                        if downloaded % (10 * 1024 * 1024) < 65536:
                            elapsed = max(0.1, time.time() - t0)
                            speed_mb = (downloaded / (1024 * 1024)) / elapsed
                            pct = (downloaded / expected_size * 100) if expected_size > 0 else 0
                            print(f"\r  Progress: {pct:.1f}% ({downloaded / (1024 * 1024):.1f} MB, {speed_mb:.2f} MB/s)", end="", flush=True)

    print()
    computed_sha256 = hasher.hexdigest().lower()
    if computed_sha256 != expected_sha256.lower():
        logger.error(f"SHA-256 mismatch for {dest_path.name}!\n  Expected: {expected_sha256}\n  Got:      {computed_sha256}")
        if tmp_path.exists():
            tmp_path.unlink()
        return False

    tmp_path.replace(dest_path)
    logger.info(f"Successfully downloaded and verified {dest_path.name} in {time.time() - t0:.1f}s")
    return True


def verify_onnx_inference() -> bool:
    """Verifies that the downloaded ONNX model loads and runs real inference."""
    import onnxruntime as ort
    import tokenizers

    model_file = CACHE_DIR / "model_quantized.onnx"
    tokenizer_file = CACHE_DIR / "tokenizer.json"

    if not model_file.exists() or not tokenizer_file.exists():
        logger.error("Artifacts missing from cache directory.")
        return False

    logger.info("Initializing ONNX Runtime InferenceSession...")
    sess_opts = ort.SessionOptions()
    sess_opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
    session = ort.InferenceSession(str(model_file), sess_opts, providers=["CPUExecutionProvider"])
    tokenizer = tokenizers.Tokenizer.from_file(str(tokenizer_file))

    # Test passage encoding
    sample_text = "passage: Magnesium ribbon burns in oxygen with a dazzling white flame to form magnesium oxide."
    enc = tokenizer.encode(sample_text)
    input_ids = [enc.ids]
    attention_mask = [enc.attention_mask]

    outputs = session.run(None, {"input_ids": input_ids, "attention_mask": attention_mask})
    last_hidden = outputs[0][0]  # shape: (seq_len, hidden_dim)
    mask = attention_mask[0]

    sum_emb = [0.0] * EMBEDDING_DIM
    count = sum(mask)
    for i, m in enumerate(mask):
        if m > 0:
            for j in range(EMBEDDING_DIM):
                sum_emb[j] += float(last_hidden[i][j])
    mean_pooled = [x / count for x in sum_emb]
    norm = math.sqrt(sum(x * x for x in mean_pooled))
    normalized = [x / norm for x in mean_pooled]

    assert len(normalized) == EMBEDDING_DIM, f"Expected {EMBEDDING_DIM} dims, got {len(normalized)}"
    assert all(math.isfinite(x) for x in normalized), "Vector contains NaN or Inf"
    assert math.isclose(math.sqrt(sum(x * x for x in normalized)), 1.0, abs_tol=1e-5), "Vector is not L2 normalized"

    logger.info(f"Verified live ONNX inference: produced valid 384-dimensional normalized vector.")
    return True


def main():
    logger.info(f"Checking model artifacts in target directory: {CACHE_DIR}")
    model_file = CACHE_DIR / "model_quantized.onnx"
    tokenizer_file = CACHE_DIR / "tokenizer.json"

    tok_ok = download_file(TOKENIZER_URL, tokenizer_file, TOKENIZER_SIZE_BYTES, TOKENIZER_SHA256)
    if not tok_ok:
        logger.error("Failed to download or verify tokenizer.json")
        sys.exit(1)

    model_ok = download_file(ONNX_QUANTIZED_URL, model_file, ONNX_QUANTIZED_SIZE_BYTES, ONNX_QUANTIZED_SHA256)
    if not model_ok:
        logger.error("Failed to download or verify model_quantized.onnx")
        sys.exit(1)

    if not verify_onnx_inference():
        logger.error("ONNX inference self-test failed!")
        sys.exit(1)

    logger.info("All model artifacts verified and ready for live production inference.")


if __name__ == "__main__":
    main()
