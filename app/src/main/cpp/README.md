# Native C++ Inference Engine & JNI Bridge (`app/src/main/cpp/`)

This directory contains the native C++ source code, header declarations, and CMake build configuration that connect the Android Kotlin runtime to the `llama.cpp` inference engine.

---

## Directory Overview

```
app/src/main/cpp/
├── CMakeLists.txt              # CMake build rules, include paths, library link targets
├── llama_jni.cpp               # JNI bridge implementing native methods for OfflineInferenceEngine
└── include/                    # Public C/C++ headers from pinned llama.cpp commit
    ├── ggml.h                  # Core GGML tensor library definitions
    ├── ggml-alloc.h            # Memory allocator for GGML computation graphs
    ├── ggml-backend.h          # Hardware backend abstractions
    ├── ggml-cpu.h              # CPU-specific execution and SIMD operations
    ├── ggml-opt.h              # Optimization routines
    ├── gguf.h                  # GGUF file format parsing routines
    └── llama.h                 # High-level llama.cpp context, model, and sampling APIs
```

---

## Key Components

### 1. JNI Bridge (`llama_jni.cpp`)

`llama_jni.cpp` implements the JNI entry points declared in `com.offline_First.data.local.OfflineInferenceEngine`:

- **Model Initialization (`nativeInit`)**:
  - Initializes GGML backends (`ggml_backend_cpu_init`).
  - Loads GGUF model weights from disk (`llama_load_model_from_file`).
  - Allocates computation context (`llama_new_context_with_model`) with configured thread count and context window length.
  - Returns an opaque native 64-bit memory pointer handle (`jlong`) to Kotlin.

- **Streaming Generation (`nativeGenerateStream`)**:
  - Tokenizes prompt text into token IDs (`llama_tokenize`).
  - Implements **KV Cache Prefix Matching**: compares the new prompt tokens with previously evaluated tokens in the KV cache, skipping prompt evaluation for matching prefixes. This drastically reduces prompt processing time during multi-turn conversations.
  - Runs autoregressive token sampling (`llama_sampler_sample`).
  - Invokes the Kotlin callback method `onToken(String)` in real time for every generated token piece.
  - Monitors a volatile cancellation flag to abort generation immediately when requested by the UI.

- **Resource Disposal (`nativeFree`)**:
  - Safely releases `llama_context`, `llama_model`, and frees GGML backend resources.

---

### 2. Native Build Rules (`CMakeLists.txt`)

`CMakeLists.txt` configures:
- Target C++ Standard: C++17.
- Header Include Directories: `app/src/main/cpp/include`.
- Shared Libraries Imported from `app/src/main/jniLibs/${ANDROID_ABI}`:
  - `libllama.so`: The core LLaMA inference engine.
  - `libggml.so`: GGML core tensor math engine.
  - `libggml-cpu.so`: GGML CPU backend optimized with ARM NEON / AVX.
- Library Output: Compiles `llama_jni.cpp` into `libllama_jni.so`, linked with Android log (`log`) and the imported llama shared libraries.

---

## Modifying or Updating Headers

If you bump the pinned `llama.cpp` commit:
1. Run `./scripts/build_llama_prebuilt.sh` (or `.ps1` on Windows).
2. The script will automatically compile updated `.so` libraries into `jniLibs/` and copy fresh header files into `include/`.
3. Commit both the libraries and headers together.
