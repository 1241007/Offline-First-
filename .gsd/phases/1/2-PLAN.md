---
phase: 1
plan: 2
wave: 1
---

# Plan 1.2: Offline On-Device Generation & Prompt Formatting Evaluator

## Objective
Evaluate the on-device model and Android inference path independently of retrieval, covering GGUF model artifact integrity, tokenization and truncation limits, prompt formatting, and strict refusal to fabricate missing evidence.

## Context
- .gsd/SPEC.md
- .gsd/ROADMAP.md
- docs/evaluation/EVALUATION_INVENTORY.md
- app/src/main/java/com/offline_First/data/local/LocalAIRepository.kt
- app/src/main/java/com/offline_First/data/local/OfflineInferenceEngine.kt
- app/src/main/java/com/offline_First/data/local/OfflineModelManager.kt
- app/src/main/java/com/offline_First/data/provider/OfflineLlamaProvider.kt

## Tasks

<task type="auto">
  <name>Build Host-Side Offline Generation Evaluation Suite</name>
  <files>backend/evals/generation/test_offline_generation_eval.py</files>
  <action>
    Create `backend/evals/generation/test_offline_generation_eval.py` to evaluate offline inference contracts:
    - GGUF artifact validation: magic header bytes `0x47475546`, versions 2-3, tensor and metadata count assertions.
    - Tokenization and context truncation logic: maximum input sequence handling without crash.
    - System prompt injection formatting: ensuring Teacher/General mode prompt template aligns with Qwen tokenizer expectations.
    - Tripartite execution reporting: distinguish Host-side tests, Emulator tests, and Physical-device tests.
    - Clear BLOCKED / SKIPPED status reporting when hardware or binary artifacts are not present.
  </action>
  <verify>$env:PYTHONPATH="backend"; .\backend\.venv\Scripts\python.exe -m pytest backend/evals/generation/test_offline_generation_eval.py -v</verify>
  <done>Host-side offline generation evaluator executes cleanly with explicit artifact integrity verification.</done>
</task>

<task type="auto">
  <name>Verify Offline Evidence Grounding and Abstention Invariants</name>
  <files>app/src/test/java/com/offline_First/data/local/OfflineGenerationQualityTest.kt</files>
  <action>
    Create Android unit test `OfflineGenerationQualityTest.kt` verifying:
    - Injected evidence formatting: retrieved curriculum passages are properly structured into the prompt.
    - Strict refusal invariant: when evidence is MISSING, the offline path strictly emits the standardized curriculum abstention and NEVER invokes ungrounded inference.
    - Zero citation hallucination: offline messages verify that `citations` remains empty when no valid curriculum passages were retrieved.
    - End-of-sequence and termination handling.
  </action>
  <verify>$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest --tests "com.offline_First.data.local.OfflineGenerationQualityTest"</verify>
  <done>Offline generation quality and abstention invariants verified on Android unit test harness.</done>
</task>

## Success Criteria
- [ ] Offline model artifact and GGUF header contracts verified with real binary assertions.
- [ ] Abstention invariant proven: empty evidence halts inference and outputs curriculum disclaimer.
- [ ] Tripartite reporting separates host-side, emulator, and physical device test outcomes.
