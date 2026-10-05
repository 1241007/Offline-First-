#!/usr/bin/env bash
# =============================================================================
# build_llama_prebuilt.sh
#
# Builds libllama.so and libggml.so for Android from the pinned llama.cpp
# commit used by this project.
#
# Prerequisites:
#   - Android NDK installed (set ANDROID_NDK_HOME or NDK_HOME env var)
#   - CMake 3.22+ on PATH
#   - curl + tar available
#
# Usage:
#   chmod +x scripts/build_llama_prebuilt.sh
#   ./scripts/build_llama_prebuilt.sh
#
# Output:
#   app/src/main/jniLibs/<abi>/libllama.so
#   app/src/main/jniLibs/<abi>/libggml.so
#   app/src/main/cpp/include/  (llama.h, ggml.h, etc.)
# =============================================================================

set -euo pipefail

# ── Pinned llama.cpp commit (keep in sync with CMakeLists.txt) ────────────────
LLAMA_COMMIT="2bc563573479d53b30b8793039485887bc0fdda8"
LLAMA_URL="https://github.com/ggml-org/llama.cpp/archive/${LLAMA_COMMIT}.tar.gz"

# ── ABIs to build ─────────────────────────────────────────────────────────────
ABIS=("arm64-v8a" "x86_64")

# ── Resolve NDK ───────────────────────────────────────────────────────────────
NDK="${ANDROID_NDK_HOME:-${NDK_HOME:-}}"
if [[ -z "$NDK" ]]; then
    # Common Android Studio locations
    for candidate in \
        "$HOME/Library/Android/sdk/ndk/27.2.12479018" \
        "$HOME/Android/Sdk/ndk/27.2.12479018" \
        "$LOCALAPPDATA/Android/sdk/ndk/27.2.12479018"
    do
        if [[ -d "$candidate" ]]; then NDK="$candidate"; break; fi
    done
fi
if [[ -z "$NDK" || ! -d "$NDK" ]]; then
    echo "ERROR: Android NDK not found. Set ANDROID_NDK_HOME to your NDK directory."
    exit 1
fi
echo "Using NDK: $NDK"

TOOLCHAIN="$NDK/build/cmake/android.toolchain.cmake"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

WORK_DIR="$REPO_ROOT/.llama_build"
SRC_DIR="$WORK_DIR/src"
INCLUDE_OUT="$REPO_ROOT/app/src/main/cpp/include"

mkdir -p "$WORK_DIR" "$INCLUDE_OUT"

# ── Download source (once) ────────────────────────────────────────────────────
ARCHIVE="$WORK_DIR/llama_cpp.tar.gz"
if [[ ! -f "$ARCHIVE" ]]; then
    echo "Downloading llama.cpp @ $LLAMA_COMMIT..."
    curl -fsSL "$LLAMA_URL" -o "$ARCHIVE"
fi

if [[ ! -d "$SRC_DIR" ]]; then
    echo "Extracting..."
    mkdir -p "$SRC_DIR"
    tar -xzf "$ARCHIVE" -C "$SRC_DIR" --strip-components=1
fi

# ── Copy public headers ───────────────────────────────────────────────────────
echo "Copying public headers..."
cp "$SRC_DIR/include/llama.h"      "$INCLUDE_OUT/"
cp "$SRC_DIR/ggml/include/ggml.h"  "$INCLUDE_OUT/"

# ── Build per ABI ─────────────────────────────────────────────────────────────
for ABI in "${ABIS[@]}"; do
    echo ""
    echo "============================================================"
    echo " Building ABI: $ABI"
    echo "============================================================"

    BUILD_DIR="$WORK_DIR/build/$ABI"
    LIB_OUT="$REPO_ROOT/app/src/main/jniLibs/$ABI"
    mkdir -p "$BUILD_DIR" "$LIB_OUT"

    cmake -S "$SRC_DIR" -B "$BUILD_DIR" \
        -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" \
        -DANDROID_ABI="$ABI" \
        -DANDROID_PLATFORM=android-24 \
        -DANDROID_STL=c++_shared \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=ON \
        -DLLAMA_BUILD_TESTS=OFF \
        -DLLAMA_BUILD_EXAMPLES=OFF \
        -DLLAMA_BUILD_SERVER=OFF \
        -DGGML_NATIVE=OFF \
        -DGGML_OPENMP=OFF \
        -DGGML_VULKAN=OFF \
        -DCMAKE_ANDROID_ARCH_ABI="$ABI" \
        -G Ninja

    cmake --build "$BUILD_DIR" --target llama ggml --parallel "$(nproc 2>/dev/null || sysctl -n hw.logicalcpu 2>/dev/null || echo 4)"

    echo "Copying .so files for $ABI..."
    find "$BUILD_DIR" -name "libllama.so" -exec cp {} "$LIB_OUT/" \;
    find "$BUILD_DIR" -name "libggml.so"  -exec cp {} "$LIB_OUT/" \;
done

echo ""
echo "Done. Prebuilt libraries are in app/src/main/jniLibs/"
echo "Headers are in app/src/main/cpp/include/"
echo ""
echo "Next step: run a Gradle build — CMakeLists.txt will use the prebuilts."
