---
phase: 8
plan: 1
wave: 4
---

# Plan 8.1: Android Low-RAM, Latency, and Resource Safety Evaluation

## Objective
Empirically benchmark and validate the on-device inference footprint, disk consumption, cold initialization latency, inference throughput, and low-memory exception handling on Android to guarantee stability on lower-end devices.

## Context
- .gsd/SPEC.md
- app/src/main/java/com/offline_First/data/local/E5EmbeddingService.kt
- app/src/main/java/com/offline_First/data/local/OfflineSyllabusRepository.kt
- backend/scripts/verify_inference_parity.py

## Tasks

<task type="auto">
  <name>Benchmark Model Artifact Footprint and Inference Latencies</name>
  <files>backend/scripts/benchmark_e5_performance.py</files>
  <action>
    Create `backend/scripts/benchmark_e5_performance.py` to evaluate:
    - Exact byte size of `model_quantized.onnx` (~113 MB) and `tokenizer.json` (~16.3 MB)
    - SHA-256 hash validation
    - Cold initialization time across 5 trials
    - Warm inference latency over typical query lengths (5 tokens, 20 tokens, 60 tokens)
    - Peak resident process memory (RSS) delta during inference
    - CPU thread scalability (single thread vs multi-threaded execution)
  </action>
  <verify>.\backend\.venv\Scripts\python.exe backend/scripts/benchmark_e5_performance.py</verify>
  <done>Performance benchmark script completes and outputs documented latency/RAM metrics.</done>
</task>

<task type="auto">
  <name>Verify Android Thread Offloading and Out-of-Memory Fallback</name>
  <files>app/src/test/java/com/offline_First/data/local/LowMemoryDegradationTest.kt</files>
  <action>
    Create `LowMemoryDegradationTest.kt` in `app/src/test/java/com/offline_First/data/local/` verifying:
    - Model initialization and inference run strictly on `Dispatchers.IO`, never blocking main thread.
    - Simulated `OutOfMemoryError` or model loading failure transitions `modelState` to `ModelState.Error` without crashing the application.
    - Graceful fallback: when vector model fails to initialize, keyword search remains available to answer curriculum queries.
  </action>
  <verify>.\gradlew.bat testDebugUnitTest --tests "com.offline_First.data.local.LowMemoryDegradationTest"</verify>
  <done>Thread isolation and memory pressure degradation verified under test runner.</done>
</task>

## Success Criteria
- [ ] Model asset sizes and SHA-256 hashes verified with byte-level precision.
- [ ] Inference confirmed off main thread with safe degradation on OOM.
- [ ] Latency and memory metrics recorded with empirical measurements rather than estimates.
