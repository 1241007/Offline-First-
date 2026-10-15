# Native Prebuilt Setup

## One-time setup (do this once per llama.cpp version bump)

Run this from the repo root:

```bash
chmod +x scripts/build_llama_prebuilt.sh
./scripts/build_llama_prebuilt.sh
```

**What it does:**
- Downloads the pinned llama.cpp commit (same one as before)
- Compiles `libllama.so` + `libggml.so` for `arm64-v8a` and `x86_64`
- Drops them into `app/src/main/jniLibs/<abi>/`
- Copies public headers into `app/src/main/cpp/include/`

**After running the script**, commit the generated `.so` files and headers:
```bash
git add app/src/main/jniLibs/ app/src/main/cpp/include/
git commit -m "chore: add prebuilt llama.cpp native libs"
```

**CI:** Run the script in your CI pipeline only when `LLAMA_COMMIT` in the script changes.
Normal app builds will never recompile llama.cpp.

## Updating llama.cpp

1. Update `LLAMA_COMMIT` in `build_llama_prebuilt.sh`
2. Re-run the script
3. Commit the new `.so` files
