"""
EduNova Evaluation Case Schema Validator & Run Metadata Collector.

Provides:
- Strict JSON Schema validation for test cases.
- Dataset file validation and cryptographic content fingerprinting (SHA-256).
- Run metadata extraction (Git commit SHA, branch, Python runtime, model settings).
"""

import hashlib
import json
import os
import platform
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import jsonschema

SCHEMA_PATH = Path(__file__).resolve().parent / "schemas" / "test_case_schema.json"


def load_schema() -> Dict[str, Any]:
    """Loads the test case JSON schema."""
    if not SCHEMA_PATH.exists():
        raise FileNotFoundError(f"Evaluation test case schema not found at {SCHEMA_PATH}")
    with open(SCHEMA_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


_SCHEMA_CACHE: Optional[Dict[str, Any]] = None


def get_cached_schema() -> Dict[str, Any]:
    global _SCHEMA_CACHE
    if _SCHEMA_CACHE is None:
        _SCHEMA_CACHE = load_schema()
    return _SCHEMA_CACHE


def validate_case(case_data: Dict[str, Any]) -> Tuple[bool, Optional[str]]:
    """
    Validates a single test case dictionary against the evaluation schema.
    Returns (True, None) if valid, or (False, error_message).
    """
    schema = get_cached_schema()
    try:
        jsonschema.validate(instance=case_data, schema=schema)
        return True, None
    except jsonschema.ValidationError as err:
        return False, f"Case '{case_data.get('case_id', 'unknown')}' validation error: {err.message}"
    except Exception as exc:
        return False, f"Unexpected validation exception: {exc}"


def compute_file_fingerprint(file_path: Path) -> str:
    """Computes SHA-256 hex digest of a dataset file."""
    if not file_path.exists():
        return "missing"
    h = hashlib.sha256()
    with open(file_path, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()[:16]


def validate_dataset_file(file_path: Path) -> Tuple[int, int, List[str]]:
    """
    Validates all lines in a JSONL dataset file.
    Returns (valid_count, total_count, error_messages).
    """
    if not file_path.exists():
        return 0, 0, [f"Dataset file does not exist: {file_path}"]

    valid = 0
    total = 0
    errors = []

    with open(file_path, "r", encoding="utf-8") as f:
        for line_num, line in enumerate(f, start=1):
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            total += 1
            try:
                data = json.loads(line)
            except Exception as e:
                errors.append(f"Line {line_num}: Invalid JSON syntax: {e}")
                continue

            ok, msg = validate_case(data)
            if ok:
                valid += 1
            else:
                errors.append(f"Line {line_num}: {msg}")

    return valid, total, errors


def get_git_info(repo_root: Path) -> Dict[str, str]:
    """Retrieves git commit SHA and branch name gracefully."""
    sha = "unknown"
    branch = "unknown"
    try:
        out_sha = subprocess.check_output(
            ["git", "rev-parse", "HEAD"],
            cwd=str(repo_root),
            stderr=subprocess.DEVNULL,
            text=True,
        )
        sha = out_sha.strip()
    except Exception:
        pass

    try:
        out_branch = subprocess.check_output(
            ["git", "branch", "--show-current"],
            cwd=str(repo_root),
            stderr=subprocess.DEVNULL,
            text=True,
        )
        branch = out_branch.strip() or "detached_head"
    except Exception:
        pass

    return {"commit_sha": sha, "branch": branch}


def collect_run_metadata(
    profile: str,
    repo_root: Path,
    dataset_paths: Optional[Dict[str, Path]] = None,
    model_config: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    """
    Constructs a comprehensive, auditable execution metadata packet.
    """
    now = datetime.now(timezone.utc)
    git_info = get_git_info(repo_root)

    dataset_fingerprints: Dict[str, str] = {}
    if dataset_paths:
        for name, p in dataset_paths.items():
            dataset_fingerprints[name] = compute_file_fingerprint(p)

    meta = {
        "run_id": f"eval-{now.strftime('%Y%m%d_%H%M%S')}-{git_info['commit_sha'][:7]}",
        "timestamp": now.isoformat(),
        "profile": profile,
        "git": git_info,
        "runtime": {
            "python_version": sys.version.split()[0],
            "os": platform.system(),
            "platform": platform.platform(),
            "architecture": platform.architecture()[0],
        },
        "dataset_fingerprints": dataset_fingerprints,
        "model_parameters": model_config or {
            "provider": "openrouter",
            "primary_model": "google/gemini-3.8-flash",
            "fallback_model": "google/gemini-2.5-flash",
            "temperature": 0.0,
            "seed": 42,
            "max_tokens": 1024,
            "deterministic_enforced": True,
        },
        "dependency_status": {
            "openrouter_api_key_configured": bool(os.environ.get("OPENROUTER_API_KEY") or os.environ.get("GEMINI_API_KEY")),
            "supabase_configured": bool(os.environ.get("SUPABASE_URL") and os.environ.get("SUPABASE_SERVICE_ROLE_KEY")),
            "android_sdk_present": bool(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or Path("C:/Users/Rushikesh Pathare/AppData/Local/Android/Sdk").exists()),
        },
    }
    return meta
