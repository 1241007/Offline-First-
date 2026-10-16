# AI Assistant Adapters (`adapters/`)

The `adapters/` directory contains optional model-specific configuration profiles and behavioral guidelines for various Large Language Model coding agents working on the EduNova codebase.

---

## Purpose & Architectural Role

Different AI assistants and reasoning models have varying context window limits, prompt sensitivities, and execution behaviors. The files in this folder calibrate agent behaviors to maximize token efficiency, accuracy, and adherence to project rules.

> [!NOTE]
> All guidelines in this directory are non-binding enhancements. The single source of truth for repository engineering standards, architectural boundaries, and constraints is [PROJECT_RULES.md](../PROJECT_RULES.md).

---

## Files in this Directory

| File | Target Model / Agent | Core Instructions & Guidelines |
|---|---|---|
| [`CLAUDE.md`](file:///e:/Offline%20First/adapters/CLAUDE.md) | Anthropic Claude models (Claude 3.5 Sonnet / Haiku / Opus) | Compact context patterns, command shortcuts, test suites, and strict diff constraints. |
| [`GEMINI.md`](file:///e:/Offline%20First/adapters/GEMINI.md) | Google Gemini models (Gemini 1.5/2.5/3.8 Flash & Pro) | Large context window handling, search-first optimization, XML context structuring, and grounding boundaries. |
| [`GPT_OSS.md`](file:///e:/Offline%20First/adapters/GPT_OSS.md) | OpenAI / Open Source LLMs (GPT-4o, Llama 3, Qwen 2.5) | Step-by-step thinking protocols, instruction-following constraints, and deterministic tool usage. |

---

## When to Consult These Files

- **Before initiating large architectural refactors**: Consult the adapter corresponding to your active LLM to ensure optimal prompt structuring and token budgeting.
- **When switching agents**: Keep instructions unified across Claude, Gemini, and open-source models by honoring the common constraints defined in `PROJECT_RULES.md`.
