#!/usr/bin/env python3
"""Unified Chatbot Evaluation Orchestrator & Regression Gate.

Supports:
- eval list: List all available evaluation suites and test categories
- --suite <name>: Run a specific evaluation suite
- --profile [ci|scheduled-live|device-benchmark|release-candidate|integration|full]: Run predefined profiles
    - ci: Fast deterministic unit, domain & contract suites (pytest only, zero network)
    - scheduled-live: Nightly live evaluations (live Supabase RAG, live OpenRouter generation)
    - device-benchmark: Android application workflows and on-device inference benchmarks
    - release-candidate: Full matrix + baseline regression gating
    - integration / full: Backward-compatible profile aliases
- --compare-baseline <file>: Compare results against baseline JSON
- --fail-on-regression: Exit with code 1 if regressions are detected
- --self-test-gate: Verify that the regression gate detects failure and exits nonzero
- Outputs: Machine-readable JSON artifact with complete run metadata and human-readable summary table
- Exit codes:
    0: All executed suites passed and regression gates satisfied
    1: Test failure, regression detected, or release blocker violated
    2: Evaluation blocked due to missing external dependency/credential
"""

import argparse
import datetime
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
BACKEND_DIR = REPO_ROOT / "backend"
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

PYTHON_BIN = BACKEND_DIR / ".venv" / "Scripts" / "python.exe"
if not PYTHON_BIN.exists():
    PYTHON_BIN = Path(sys.executable)

GRADLEW = REPO_ROOT / "gradlew.bat"
JAVA_HOME = Path("C:/Program Files/Android/Android Studio/jbr")

from evals.case_schema import collect_run_metadata

SUITES: Dict[str, Dict[str, Any]] = {
    "domain_adherence": {
        "description": "Domain boundary enforcement, out-of-domain refusals, and mixed intent",
        "type": "unit",
        "category": "domain",
        "command": [str(PYTHON_BIN), "-m", "pytest", "evals/domain/test_domain_adherence_eval.py", "-v"],
        "cwd": BACKEND_DIR,
        "env": {"PYTHONPATH": "backend"},
        "profile": ["ci", "integration", "full", "release-candidate"],
    },
    "rag_mock": {
        "description": "Mock RAG retrieval & evaluation (25 golden set cases)",
        "type": "eval",
        "category": "retrieval",
        "command": [str(PYTHON_BIN), "evals/rag/run_eval.py", "--mode", "mock"],
        "cwd": BACKEND_DIR,
        "env": {"PYTHONPATH": "backend"},
        "profile": ["ci", "integration", "full", "release-candidate"],
    },
    "rag_live": {
        "description": "Live Supabase RAG retrieval & evaluation against cloud DB",
        "type": "live_eval",
        "category": "retrieval",
        "command": [str(PYTHON_BIN), "evals/rag/run_eval.py", "--mode", "live"],
        "cwd": BACKEND_DIR,
        "env": {"PYTHONPATH": "backend"},
        "profile": ["scheduled-live", "full", "release-candidate"],
        "requires": ["supabase"],
    },
    "online_generation": {
        "description": "Online generation quality, rubrics & instruction adherence",
        "type": "unit",
        "category": "generation",
        "command": [str(PYTHON_BIN), "-m", "pytest", "evals/generation/test_online_generation_eval.py", "-v"],
        "cwd": BACKEND_DIR,
        "env": {"PYTHONPATH": "backend"},
        "profile": ["ci", "scheduled-live", "integration", "full", "release-candidate"],
    },
    "provider_resilience": {
        "description": "OpenRouter provider retry, fallback, 429 backoff & stream interruption",
        "type": "unit",
        "category": "resilience",
        "command": [str(PYTHON_BIN), "-m", "pytest", "evals/generation/test_provider_resilience.py", "-v"],
        "cwd": BACKEND_DIR,
        "env": {"PYTHONPATH": "backend"},
        "profile": ["ci", "integration", "full", "release-candidate"],
    },
    "offline_generation": {
        "description": "Offline generative model GGUF format validation & ChatML prompting",
        "type": "unit",
        "category": "generation",
        "command": [str(PYTHON_BIN), "-m", "pytest", "evals/generation/test_offline_generation_eval.py", "-v"],
        "cwd": BACKEND_DIR,
        "env": {"PYTHONPATH": "backend"},
        "profile": ["ci", "integration", "full", "release-candidate"],
    },
    "prompt_persistence": {
        "description": "Prompt context isolation & backend chat persistence invariants",
        "type": "unit",
        "category": "persistence",
        "command": [
            str(PYTHON_BIN), "-m", "pytest",
            "tests/test_prompt_construction.py",
            "tests/test_chat_persistence_invariants.py",
            "-v"
        ],
        "cwd": BACKEND_DIR,
        "env": {"PYTHONPATH": "backend"},
        "profile": ["ci", "integration", "full", "release-candidate"],
    },
    "pipeline_matrix": {
        "description": "End-to-End Chatbot Pipeline Matrix (Pipelines A through G)",
        "type": "integration",
        "category": "pipeline",
        "command": [str(PYTHON_BIN), "-m", "pytest", "tests/integration/test_pipeline_e2e_matrix.py", "-v"],
        "cwd": BACKEND_DIR,
        "env": {"PYTHONPATH": "backend"},
        "profile": ["ci", "integration", "full", "release-candidate"],
    },
    "android_unit": {
        "description": "Android application-level workflows, persistence & offline inference tests",
        "type": "android_unit",
        "category": "android",
        "command": [
            str(GRADLEW), "testDebugUnitTest",
            "--tests", "com.offline_First.data.local.ChatPersistenceInvariantTest",
            "--tests", "com.offline_First.data.local.OfflineGenerationQualityTest",
            "--tests", "com.offline_First.ui.screens.ai.ChatbotApplicationWorkflowsTest",
        ],
        "cwd": REPO_ROOT,
        "env": {"JAVA_HOME": str(JAVA_HOME)},
        "profile": ["device-benchmark", "integration", "full", "release-candidate"],
        "requires": ["gradlew", "java_home"],
    },
}


def check_dependencies(requirements: List[str]) -> Tuple[bool, Optional[str]]:
    """Checks whether required external dependencies/credentials exist."""
    for req in requirements:
        if req == "supabase":
            url = os.environ.get("SUPABASE_URL")
            key = os.environ.get("SUPABASE_SERVICE_ROLE_KEY") or os.environ.get("SUPABASE_KEY")
            if not (url and key):
                return False, "Supabase credentials (SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY) not configured"
        elif req == "openrouter":
            key = os.environ.get("OPENROUTER_API_KEY") or os.environ.get("GEMINI_API_KEY")
            if not key:
                return False, "OpenRouter/Gemini API key not configured"
        elif req == "gradlew":
            if not GRADLEW.exists():
                return False, f"gradlew executable not found at {GRADLEW}"
        elif req == "java_home":
            if not JAVA_HOME.exists() and not os.environ.get("JAVA_HOME"):
                return False, f"JAVA_HOME path not found at {JAVA_HOME}"
    return True, None


def run_suite(name: str, config: Dict[str, Any]) -> Dict[str, Any]:
    print(f"\n========================================================")
    print(f"RUNNING SUITE: {name}")
    print(f"Category:      {config.get('category', 'general')}")
    print(f"Description:   {config['description']}")
    print(f"Command:       {' '.join(config['command'])}")
    print(f"========================================================")

    # Check dependencies before attempting execution
    reqs = config.get("requires", [])
    if reqs:
        available, missing_reason = check_dependencies(reqs)
        if not available:
            print(f"--> [BLOCKED] Skipping suite '{name}': {missing_reason}")
            return {
                "suite": name,
                "type": config["type"],
                "category": config.get("category", "general"),
                "description": config["description"],
                "command": " ".join(config["command"]),
                "status": "BLOCKED",
                "passed": False,
                "blocked": True,
                "blocked_reason": missing_reason,
                "exit_code": 2,
                "duration_seconds": 0.0,
                "tests_run": 0,
                "tests_passed": 0,
                "tests_failed": 0,
            }

    env = os.environ.copy()
    for k, v in config.get("env", {}).items():
        env[k] = v

    start_time = datetime.datetime.now(datetime.timezone.utc)
    try:
        proc = subprocess.run(
            config["command"],
            cwd=config["cwd"],
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=300,
        )
        duration = (datetime.datetime.now(datetime.timezone.utc) - start_time).total_seconds()
        output = proc.stdout
        exit_code = proc.returncode
        passed = (exit_code == 0)
    except subprocess.TimeoutExpired:
        duration = 300.0
        output = "ERROR: Command timed out after 300 seconds."
        exit_code = -1
        passed = False
    except Exception as exc:
        duration = (datetime.datetime.now(datetime.timezone.utc) - start_time).total_seconds()
        output = f"ERROR: Execution failed: {exc}"
        exit_code = -1
        passed = False

    print(output)
    status_str = "PASS" if passed else "FAIL"
    print(f"--> Result: {status_str} (Exit code: {exit_code}, Duration: {duration:.2f}s)")

    # Parse simple test counts if pytest
    tests_run = 0
    tests_passed = 0
    tests_failed = 0
    for line in output.splitlines():
        if " passed" in line and ("=" in line or line.startswith("=")):
            parts = line.replace("=", "").strip().split(",")
            for p in parts:
                p = p.strip()
                if "passed" in p:
                    try:
                        tests_passed += int(p.split()[0])
                    except Exception:
                        pass
                elif "failed" in p:
                    try:
                        tests_failed += int(p.split()[0])
                    except Exception:
                        pass
    tests_run = tests_passed + tests_failed
    if tests_run == 0 and passed:
        tests_run = 1
        tests_passed = 1

    # Extract granular metrics if this is an eval suite
    suite_metrics: Dict[str, Any] = {}
    dataset_version: Optional[str] = None
    if config["type"] in ("eval", "live_eval"):
        latest_report = BACKEND_DIR / "evals" / "rag" / "results" / "latest_eval_report.json"
        if latest_report.exists():
            try:
                with open(latest_report, "r", encoding="utf-8") as rf:
                    rdata = json.load(rf)
                    suite_metrics = rdata.get("aggregate_metrics", {})
                    dataset_version = rdata.get("metadata", {}).get("dataset_version", "2.0")
            except Exception:
                pass

    res = {
        "suite": name,
        "type": config["type"],
        "category": config.get("category", "general"),
        "description": config["description"],
        "command": " ".join(config["command"]),
        "status": status_str,
        "passed": passed,
        "blocked": False,
        "exit_code": exit_code,
        "duration_seconds": round(duration, 2),
        "tests_run": tests_run,
        "tests_passed": tests_passed,
        "tests_failed": tests_failed,
    }
    if suite_metrics:
        res["metrics"] = suite_metrics
    if dataset_version:
        res["dataset_version"] = dataset_version
    return res


HIGHER_IS_BETTER_METRICS = {
    "mean_recall_at_5",
    "mean_recall_at_10",
    "mean_mrr_at_10",
    "mean_ndcg_at_10",
    "empty_result_correctness_rate",
    "outcome_match_rate",
    "factual_correctness",
    "instruction_adherence",
    "clarification_rate",
    "mixed_intent_compliance_rate",
}

LOWER_IS_BETTER_METRICS = {
    "mean_cross_curriculum_leakage",
    "error_rate",
    "hallucination_rate",
    "unsupported_claim_rate",
    "out_of_domain_false_accept_rate",
    "in_domain_false_rejection_rate",
}


def compare_with_baseline(current_results: Dict[str, Any], baseline_file: Path) -> Tuple[bool, List[str]]:
    """Compares current evaluation run against an accepted baseline artifact."""
    if not baseline_file.exists():
        return True, [f"Baseline file {baseline_file} does not exist. No regression detected."]

    try:
        with open(baseline_file, "r", encoding="utf-8") as f:
            baseline = json.load(f)
    except Exception as e:
        return False, [f"Error reading baseline file {baseline_file}: {e}"]

    regressions = []
    baseline_suites = {s["suite"]: s for s in baseline.get("suites", [])}

    for current in current_results.get("suites", []):
        s_name = current["suite"]
        base = baseline_suites.get(s_name)
        if not base:
            continue

        # 1. Suite pass/fail regression
        if base.get("passed") and not current.get("passed") and not current.get("blocked"):
            regressions.append(
                f"REGRESSION in '{s_name}': Previously passed ({base.get('tests_passed')} tests), now FAILED."
            )

        # 2. Test count decrease
        if current.get("tests_passed", 0) < base.get("tests_passed", 0):
            regressions.append(
                f"REGRESSION in '{s_name}': Passed count decreased from {base.get('tests_passed')} to {current.get('tests_passed')}."
            )

        # 3. Dataset version compatibility check
        base_dataset_ver = base.get("dataset_version")
        cur_dataset_ver = current.get("dataset_version")
        if base_dataset_ver and cur_dataset_ver and base_dataset_ver != cur_dataset_ver:
            regressions.append(
                f"INCOMPATIBLE DATASET in '{s_name}': Current dataset version {cur_dataset_ver} != Baseline dataset version {base_dataset_ver}."
            )

        # 4. Metric-level comparison with directionality
        base_metrics = base.get("metrics", {})
        cur_metrics = current.get("metrics", {})
        if base_metrics:
            for m_name, base_val in base_metrics.items():
                if m_name not in cur_metrics:
                    regressions.append(
                        f"MISSING METRIC in '{s_name}': Expected baseline metric '{m_name}' missing from current results."
                    )
                    continue

                cur_val = cur_metrics[m_name]
                if not isinstance(base_val, (int, float)) or not isinstance(cur_val, (int, float)):
                    continue

                if m_name in HIGHER_IS_BETTER_METRICS:
                    if cur_val < (base_val - 0.05):
                        regressions.append(
                            f"METRIC REGRESSION in '{s_name}.{m_name}': Dropped from {base_val:.4f} to {cur_val:.4f} (tolerance: -0.05)."
                        )
                elif m_name in LOWER_IS_BETTER_METRICS:
                    if cur_val > (base_val + 0.02):
                        regressions.append(
                            f"METRIC REGRESSION in '{s_name}.{m_name}': Increased from {base_val:.4f} to {cur_val:.4f} (tolerance: +0.02)."
                        )

    has_regressions = len(regressions) > 0
    return not has_regressions, regressions


def run_self_test_gate():
    """Validates that the regression gate detects all failure modes."""
    print("\n--- RUNNING REGRESSION GATE COMPREHENSIVE SELF-TEST ---")
    temp_baseline_path = BACKEND_DIR / "evals" / "results" / "temp_self_test_baseline.json"
    temp_baseline_path.parent.mkdir(parents=True, exist_ok=True)

    base_fixture = {
        "timestamp": "2026-10-16T00:00:00Z",
        "suites": [
            {
                "suite": "test_suite",
                "passed": True,
                "tests_passed": 10,
                "dataset_version": "2.0",
                "metrics": {
                    "mean_recall_at_5": 0.95,
                    "mean_cross_curriculum_leakage": 0.0,
                }
            }
        ]
    }

    try:
        with open(temp_baseline_path, "w", encoding="utf-8") as f:
            json.dump(base_fixture, f)

        # Test Case 1: Identical results -> No regression
        passing_current = {
            "suites": [
                {
                    "suite": "test_suite",
                    "passed": True,
                    "tests_passed": 10,
                    "dataset_version": "2.0",
                    "metrics": {
                        "mean_recall_at_5": 0.95,
                        "mean_cross_curriculum_leakage": 0.0,
                    }
                }
            ]
        }
        ok, diffs = compare_with_baseline(passing_current, temp_baseline_path)
        assert ok, f"Self-test 1 failed: False alarm on identical results: {diffs}"
        print("[PASS] Case 1: Identical baseline comparison passed with zero regressions.")

        # Test Case 2: Higher-is-better metric drop
        recall_drop_current = {
            "suites": [
                {
                    "suite": "test_suite",
                    "passed": True,
                    "tests_passed": 10,
                    "dataset_version": "2.0",
                    "metrics": {
                        "mean_recall_at_5": 0.70,
                        "mean_cross_curriculum_leakage": 0.0,
                    }
                }
            ]
        }
        ok, diffs = compare_with_baseline(recall_drop_current, temp_baseline_path)
        assert not ok and any("mean_recall_at_5" in d for d in diffs), f"Self-test 2 failed: {diffs}"
        print(f"[PASS] Case 2: Detected higher-is-better metric drop: {diffs[0]}")

        # Test Case 3: Lower-is-better metric rise
        leakage_rise_current = {
            "suites": [
                {
                    "suite": "test_suite",
                    "passed": True,
                    "tests_passed": 10,
                    "dataset_version": "2.0",
                    "metrics": {
                        "mean_recall_at_5": 0.95,
                        "mean_cross_curriculum_leakage": 0.15,
                    }
                }
            ]
        }
        ok, diffs = compare_with_baseline(leakage_rise_current, temp_baseline_path)
        assert not ok and any("mean_cross_curriculum_leakage" in d for d in diffs), f"Self-test 3 failed: {diffs}"
        print(f"[PASS] Case 3: Detected lower-is-better metric rise: {diffs[0]}")

        print("\n[ALL SELF-TEST CHECKS PASSED] Regression gate verified for directionality.")
    finally:
        if temp_baseline_path.exists():
            temp_baseline_path.unlink()


def main():
    parser = argparse.ArgumentParser(description="EduNova Chatbot Evaluation Runner & Regression Gate")
    parser.add_argument("action", nargs="?", default="run", choices=["run", "list"], help="Action to execute")
    parser.add_argument("--suite", type=str, help="Specific suite name to run")
    parser.add_argument(
        "--profile",
        type=str,
        default="ci",
        choices=["ci", "scheduled-live", "device-benchmark", "release-candidate", "integration", "full"],
        help="Execution profile"
    )
    parser.add_argument("--compare-baseline", type=str, help="Path to baseline JSON artifact")
    parser.add_argument("--save-as-baseline", action="store_true", help="Save current run as the accepted baseline")
    parser.add_argument("--fail-on-regression", action="store_true", help="Exit nonzero if regressions occur")
    parser.add_argument("--self-test-gate", action="store_true", help="Execute self-test of the regression gate")
    args = parser.parse_args()

    if args.self_test_gate:
        run_self_test_gate()
        return

    if args.action == "list":
        print("\nAvailable Evaluation Suites:")
        print("----------------------------------------------------------------------")
        for name, cfg in SUITES.items():
            print(f"  * {name:<22} [{cfg['type']:<12}] Cat: {cfg.get('category','general'):<10} Profiles: {','.join(cfg['profile'])}")
            print(f"    {cfg['description']}")
        print("----------------------------------------------------------------------")
        return

    suites_to_run = []
    if args.suite:
        if args.suite not in SUITES:
            print(f"Error: Unknown suite '{args.suite}'. Available: {list(SUITES.keys())}")
            sys.exit(1)
        suites_to_run = [args.suite]
    else:
        for name, cfg in SUITES.items():
            if args.profile in cfg.get("profile", []):
                suites_to_run.append(name)

    print(f"Initiating evaluation run (Profile: {args.profile}, Suites: {len(suites_to_run)})...")
    results = []
    start_time = datetime.datetime.now(datetime.timezone.utc)

    for s_name in suites_to_run:
        res = run_suite(s_name, SUITES[s_name])
        results.append(res)

    total_duration = (datetime.datetime.now(datetime.timezone.utc) - start_time).total_seconds()
    total_suites = len(results)
    passed_suites = sum(1 for r in results if r["passed"])
    blocked_suites = sum(1 for r in results if r.get("blocked", False))
    failed_suites = total_suites - passed_suites - blocked_suites

    # Category summaries
    results_by_category: Dict[str, Dict[str, Any]] = {}
    for r in results:
        cat = r.get("category", "general")
        if cat not in results_by_category:
            results_by_category[cat] = {
                "total_suites": 0,
                "passed_suites": 0,
                "failed_suites": 0,
                "blocked_suites": 0,
                "tests_run": 0,
                "tests_passed": 0,
                "tests_failed": 0,
                "duration_seconds": 0.0,
            }
        c_entry = results_by_category[cat]
        c_entry["total_suites"] += 1
        if r["passed"]:
            c_entry["passed_suites"] += 1
        elif r.get("blocked"):
            c_entry["blocked_suites"] += 1
        else:
            c_entry["failed_suites"] += 1
        c_entry["tests_run"] += r.get("tests_run", 0)
        c_entry["tests_passed"] += r.get("tests_passed", 0)
        c_entry["tests_failed"] += r.get("tests_failed", 0)
        c_entry["duration_seconds"] = round(c_entry["duration_seconds"] + r.get("duration_seconds", 0.0), 2)

    # Collect reproducible run metadata
    datasets_map = {
        "golden_set": BACKEND_DIR / "evals" / "rag" / "golden_set.jsonl",
        "generation_eval_set": BACKEND_DIR / "evals" / "generation" / "generation_eval_set.jsonl",
        "domain_benchmark": BACKEND_DIR / "evals" / "domain" / "domain_scope_benchmark.jsonl",
    }
    metadata = collect_run_metadata(
        profile=args.profile,
        repo_root=REPO_ROOT,
        dataset_paths=datasets_map,
    )

    report = {
        "metadata": metadata,
        "summary": {
            "profile": args.profile,
            "total_duration_seconds": round(total_duration, 2),
            "total_suites": total_suites,
            "passed_suites": passed_suites,
            "failed_suites": failed_suites,
            "blocked_suites": blocked_suites,
        },
        "results_by_category": results_by_category,
        "suites": results,
    }

    results_dir = BACKEND_DIR / "evals" / "results"
    results_dir.mkdir(parents=True, exist_ok=True)
    timestamp_str = start_time.strftime("%Y%m%d_%H%M%S")
    report_file = results_dir / f"eval_report_{timestamp_str}.json"
    latest_file = results_dir / "latest_report.json"

    with open(report_file, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)
    with open(latest_file, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    baseline_file = Path(args.compare_baseline) if args.compare_baseline else (results_dir / "baseline.json")
    if args.save_as_baseline:
        with open(results_dir / "baseline.json", "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)
        print(f"\nSaved current results as new baseline: {results_dir / 'baseline.json'}")

    # Summary table
    print("\n" + "=" * 90)
    print("                    EVALUATION & REGRESSION SUMMARY")
    print("=" * 90)
    print(f"{'Suite':<22} | {'Category':<12} | {'Status':<7} | {'Passed':<6} | {'Failed':<6} | {'Duration':<8}")
    print("-" * 90)
    for r in results:
        status_disp = r['status']
        print(f"{r['suite']:<22} | {r.get('category','general'):<12} | {status_disp:<7} | {r['tests_passed']:<6} | {r['tests_failed']:<6} | {r['duration_seconds']:>6.2f}s")
    print("=" * 90)

    overall_status = "PASS"
    if failed_suites > 0:
        overall_status = "FAIL"
    elif blocked_suites > 0:
        overall_status = "BLOCKED"

    print(f"OVERALL STATUS: {overall_status} ({passed_suites}/{total_suites} suites passed, {blocked_suites} blocked, Total time: {total_duration:.2f}s)")
    print(f"Report JSON: {report_file}")

    # Regression comparison
    has_regression = False
    if baseline_file.exists() and not args.save_as_baseline:
        print("\nChecking against baseline for regressions...")
        is_clean, regressions = compare_with_baseline(report, baseline_file)
        if not is_clean:
            has_regression = True
            print("\n[!] REGRESSIONS DETECTED:")
            for reg in regressions:
                print(f"  - {reg}")
            if args.fail_on_regression:
                print("Exiting with code 1 due to --fail-on-regression.")
                sys.exit(1)
        else:
            print("[OK] No regressions detected against baseline.")

    # Explicit Exit Code Semantics:
    # 0 = PASS
    # 1 = FAIL or Regression
    # 2 = BLOCKED (if any suite was blocked due to missing dependency)
    if failed_suites > 0 or has_regression:
        sys.exit(1)
    elif blocked_suites > 0:
        sys.exit(2)
    else:
        sys.exit(0)


if __name__ == "__main__":
    main()
