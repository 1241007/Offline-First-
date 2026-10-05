# =============================================================================
# build_llama_prebuilt.ps1
#
# Builds libllama.so and libggml.so for Android from the pinned llama.cpp
# commit. Uses the CMake and Ninja already bundled in the Android SDK.
#
# Run from the repo root:
#   .\scripts\build_llama_prebuilt.ps1
#
# Output:
#   app\src\main\jniLibs\arm64-v8a\libllama.so
#   app\src\main\jniLibs\arm64-v8a\libggml.so
#   app\src\main\jniLibs\x86_64\libllama.so
#   app\src\main\jniLibs\x86_64\libggml.so
#   app\src\main\cpp\include\llama.h
#   app\src\main\cpp\include\ggml.h
# =============================================================================

$ErrorActionPreference = "Stop"

# ── Pinned llama.cpp commit (keep in sync with CMakeLists.txt) ───────────────
$LlamaCommit = "2bc563573479d53b30b8793039485887bc0fdda8"
$LlamaUrl    = "https://github.com/ggml-org/llama.cpp/archive/$LlamaCommit.tar.gz"

# ── ABIs ─────────────────────────────────────────────────────────────────────
$Abis = @("arm64-v8a", "x86_64")

# ── Resolve Android SDK ───────────────────────────────────────────────────────
$SdkCandidates = @(
    "$env:LOCALAPPDATA\Android\Sdk",
    "$env:USERPROFILE\AppData\Local\Android\Sdk",
    "C:\Android\Sdk"
)
$AndroidSdk = $env:ANDROID_SDK_ROOT
if (-not $AndroidSdk) {
    foreach ($c in $SdkCandidates) {
        if (Test-Path $c) { $AndroidSdk = $c; break }
    }
}
if (-not $AndroidSdk -or -not (Test-Path $AndroidSdk)) {
    Write-Error "Android SDK not found. Set ANDROID_SDK_ROOT env var."
    exit 1
}
Write-Host "Using Android SDK: $AndroidSdk"

# ── Resolve NDK ───────────────────────────────────────────────────────────────
$NdkVersion = "27.2.12479018"
$NdkPath = "$AndroidSdk\ndk\$NdkVersion"
if (-not (Test-Path $NdkPath)) {
    Write-Error "NDK $NdkVersion not found at $NdkPath. Install it via Android Studio SDK Manager."
    exit 1
}
Write-Host "Using NDK: $NdkPath"
$Toolchain = "$NdkPath\build\cmake\android.toolchain.cmake"

# ── Resolve CMake and Ninja from SDK ─────────────────────────────────────────
$CmakeExe = "$AndroidSdk\cmake\3.31.6\bin\cmake.exe"
$NinjaExe = "$AndroidSdk\cmake\3.31.6\bin\ninja.exe"
if (-not (Test-Path $CmakeExe)) {
    Write-Error "CMake not found at $CmakeExe. Install CMake 3.31.6 via Android Studio SDK Manager."
    exit 1
}
Write-Host "Using CMake: $CmakeExe"
Write-Host "Using Ninja: $NinjaExe"

# ── Paths ─────────────────────────────────────────────────────────────────────
$RepoRoot   = Split-Path -Parent $PSScriptRoot
$WorkDir    = "$RepoRoot\.llama_build"
$SrcDir     = "$WorkDir\src"
$Archive    = "$WorkDir\llama_cpp.tar.gz"
$IncludeOut = "$RepoRoot\app\src\main\cpp\include"

New-Item -ItemType Directory -Force -Path $WorkDir    | Out-Null
New-Item -ItemType Directory -Force -Path $IncludeOut | Out-Null

# ── Download source (once) ───────────────────────────────────────────────────
if (-not (Test-Path $Archive)) {
    Write-Host "Downloading llama.cpp @ $LlamaCommit..."
    Invoke-WebRequest -Uri $LlamaUrl -OutFile $Archive -UseBasicParsing
}

if (-not (Test-Path $SrcDir)) {
    Write-Host "Extracting archive..."
    New-Item -ItemType Directory -Force -Path $SrcDir | Out-Null
    # Use tar (available on Windows 10+)
    tar -xzf $Archive -C $SrcDir --strip-components=1
}

# ── Copy public headers (llama.h + all headers it transitively includes) ─────
Write-Host "Copying public headers..."
Copy-Item "$SrcDir\include\llama.h"             "$IncludeOut\llama.h"         -Force
Copy-Item "$SrcDir\ggml\include\ggml.h"         "$IncludeOut\ggml.h"          -Force
Copy-Item "$SrcDir\ggml\include\ggml-cpu.h"     "$IncludeOut\ggml-cpu.h"      -Force
Copy-Item "$SrcDir\ggml\include\ggml-backend.h" "$IncludeOut\ggml-backend.h"  -Force
Copy-Item "$SrcDir\ggml\include\ggml-opt.h"     "$IncludeOut\ggml-opt.h"      -Force
Copy-Item "$SrcDir\ggml\include\gguf.h"         "$IncludeOut\gguf.h"          -Force
Copy-Item "$SrcDir\ggml\include\ggml-alloc.h"   "$IncludeOut\ggml-alloc.h"    -Force

# ── Build per ABI ─────────────────────────────────────────────────────────────
foreach ($Abi in $Abis) {
    Write-Host ""
    Write-Host "============================================================"
    Write-Host " Building ABI: $Abi"
    Write-Host "============================================================"

    $BuildDir = "$WorkDir\build\$Abi"
    $LibOut   = "$RepoRoot\app\src\main\jniLibs\$Abi"
    New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
    New-Item -ItemType Directory -Force -Path $LibOut   | Out-Null

    $CmakeArgs = @(
        "-S", $SrcDir,
        "-B", $BuildDir,
        "-G", "Ninja",
        "-DCMAKE_MAKE_PROGRAM=$NinjaExe",
        "-DCMAKE_TOOLCHAIN_FILE=$Toolchain",
        "-DANDROID_ABI=$Abi",
        "-DANDROID_PLATFORM=android-24",
        "-DANDROID_STL=c++_shared",
        "-DCMAKE_SHARED_LINKER_FLAGS=-lc++_shared",
        "-DCMAKE_BUILD_TYPE=Release",
        "-DBUILD_SHARED_LIBS=ON",
        "-DLLAMA_BUILD_TESTS=OFF",
        "-DLLAMA_BUILD_EXAMPLES=OFF",
        "-DLLAMA_BUILD_SERVER=OFF",
        "-DGGML_NATIVE=OFF",
        "-DGGML_OPENMP=OFF",
        "-DGGML_VULKAN=OFF"
    )

    Write-Host "Configuring..."
    $ErrorActionPreference = "Continue"
    & $CmakeExe @CmakeArgs 2>&1 | Where-Object { $_ -notmatch "Deprecation Warning|cmake_minimum_required|Compatibility with CMake" } | Write-Host
    $configExit = $LASTEXITCODE
    $ErrorActionPreference = "Stop"
    if ($configExit -ne 0) { Write-Error "CMake configure failed for $Abi"; exit 1 }

    Write-Host "Building (this takes several minutes on first run)..."
    $ErrorActionPreference = "Continue"
    & $CmakeExe --build $BuildDir --target llama ggml ggml-base ggml-cpu --parallel 2>&1 | Where-Object { $_ -match "^\[|FAILED|error:" } | Write-Host
    $buildExit = $LASTEXITCODE
    $ErrorActionPreference = "Stop"
    if ($buildExit -ne 0) { Write-Error "CMake build failed for $Abi"; exit 1 }

    Write-Host "Copying .so files for $Abi..."
    Get-ChildItem -Path $BuildDir -Filter "libllama.so"      -Recurse | Select-Object -First 1 | Copy-Item -Destination "$LibOut\libllama.so"      -Force
    Get-ChildItem -Path $BuildDir -Filter "libggml.so"       -Recurse | Select-Object -First 1 | Copy-Item -Destination "$LibOut\libggml.so"       -Force
    Get-ChildItem -Path $BuildDir -Filter "libggml-base.so"  -Recurse | Select-Object -First 1 | Copy-Item -Destination "$LibOut\libggml-base.so"  -Force
    Get-ChildItem -Path $BuildDir -Filter "libggml-cpu.so"   -Recurse | Select-Object -First 1 | Copy-Item -Destination "$LibOut\libggml-cpu.so"   -Force

    # libc++_shared.so must ship with all NDK c++_shared libs
    # NDK uses triple-style dirs: aarch64-linux-android / x86_64-linux-android
    $AbiToTriple = @{ "arm64-v8a" = "aarch64-linux-android"; "x86_64" = "x86_64-linux-android" }
    $Triple = $AbiToTriple[$Abi]
    $LibcppSrc = "$NdkPath\toolchains\llvm\prebuilt\windows-x86_64\sysroot\usr\lib\$Triple\libc++_shared.so"
    if (Test-Path $LibcppSrc) {
        Copy-Item $LibcppSrc "$LibOut\libc++_shared.so" -Force
        Write-Host "  Copied libc++_shared.so"
    } else {
        Write-Warning "  libc++_shared.so not found at $LibcppSrc"
    }

    Write-Host "Done: $Abi"
}

Write-Host ""
Write-Host "============================================================"
Write-Host " All prebuilts ready."
Write-Host " Libraries : app\src\main\jniLibs\<abi>\"
Write-Host " Headers   : app\src\main\cpp\include\"
Write-Host ""
Write-Host " Next: commit these files, then run a normal Gradle build."
Write-Host " llama.cpp will NEVER be recompiled again unless you bump"
Write-Host " the commit hash and re-run this script."
Write-Host "============================================================"
