# Precompiled Native Shared Libraries (`app/src/main/jniLibs/`)

This directory houses precompiled 64-bit native shared libraries (`.so`) for the `llama.cpp` inference engine and its underlying `ggml` tensor framework.

---

## Directory Structure & ABI Architecture

```
app/src/main/jniLibs/
├── arm64-v8a/                  # 64-bit ARM architecture (physical Android devices)
│   ├── libc++_shared.so        # LLVM C++ standard library runtime
│   ├── libggml-base.so         # GGML core memory and base mathematical operations
│   ├── libggml-cpu.so          # Optimized CPU kernels (ARM NEON SIMD)
│   ├── libggml.so              # GGML high-level tensor computation engine
│   └── libllama.so             # LLaMA transformer model architecture & sampling runtime
└── x86_64/                     # 64-bit x86 architecture (Android Studio emulators)
    ├── libc++_shared.so        # LLVM C++ standard library runtime
    ├── libggml-base.so         # GGML core memory and base mathematical operations
    ├── libggml-cpu.so          # Optimized CPU kernels (AVX / AVX2 SIMD)
    ├── libggml.so              # GGML high-level tensor computation engine
    └── libllama.so             # LLaMA transformer model architecture & sampling runtime
```

---

## Why Prebuilt Binaries?

Compiling `llama.cpp` from source during standard Android Studio Gradle builds adds 8–15 minutes of build overhead and requires complete CMake toolchain synchronization across developer workstations. 

Precompiling these libraries and checking them into version control ensures:
- **Instant Clean Builds**: Standard Gradle builds complete in seconds rather than minutes.
- **Deterministic Native Behavior**: All developers and CI runners execute identical, validated binaries with identical compiler flags.
- **Reduced Toolchain Flakiness**: Removes the requirement for every developer to configure a custom NDK cross-compilation environment.

---

## Rebuilding or Upgrading Libraries

When bumping the `llama.cpp` commit or changing compiler flags, execute the root automation script:

```powershell
# Windows
.\scripts\build_llama_prebuilt.ps1

# Linux / macOS
chmod +x scripts/build_llama_prebuilt.sh
./scripts/build_llama_prebuilt.sh
```

This will automatically recompile all `.so` binaries for both ABIs and update the C++ headers in `app/src/main/cpp/include/`.
