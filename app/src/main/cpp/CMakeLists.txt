cmake_minimum_required(VERSION 3.22.1)
project(edunova_llama LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Support 16 KB page alignment for Android 15+ / 16 KB devices
add_link_options("-Wl,-z,max-page-size=16384")

# =============================================================================
# Prebuilt llama.cpp libraries
#
# Built once via scripts/build_llama_prebuilt.ps1 and committed to
# app/src/main/jniLibs/<abi>/.  Headers live in app/src/main/cpp/include/.
#
# llama.cpp 0.5.0-dev uses a split shared lib architecture:
#   libc++_shared.so  — NDK C++ stdlib (required by all ggml libs)
#   libggml-base.so   — core ggml tensors/backend
#   libggml-cpu.so    — CPU compute backend
#   libggml.so        — ggml umbrella
#   libllama.so       — llama inference API
# =============================================================================

set(PREBUILT_LIB_DIR "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")

add_library(libcpp_shared  SHARED IMPORTED)
add_library(ggml_base      SHARED IMPORTED)
add_library(ggml_cpu       SHARED IMPORTED)
add_library(ggml           SHARED IMPORTED)
add_library(llama          SHARED IMPORTED)

set_target_properties(libcpp_shared PROPERTIES IMPORTED_LOCATION "${PREBUILT_LIB_DIR}/libc++_shared.so")
set_target_properties(ggml_base     PROPERTIES IMPORTED_LOCATION "${PREBUILT_LIB_DIR}/libggml-base.so")
set_target_properties(ggml_cpu      PROPERTIES IMPORTED_LOCATION "${PREBUILT_LIB_DIR}/libggml-cpu.so")
set_target_properties(ggml          PROPERTIES IMPORTED_LOCATION "${PREBUILT_LIB_DIR}/libggml.so")
set_target_properties(llama         PROPERTIES IMPORTED_LOCATION "${PREBUILT_LIB_DIR}/libllama.so")

# ── Our JNI bridge (the only file compiled during a normal Gradle build) ──────
add_library(edunova_llama SHARED llama_jni.cpp)

target_include_directories(edunova_llama PRIVATE
    ${CMAKE_SOURCE_DIR}/include
)

target_link_libraries(edunova_llama PRIVATE
    llama
    ggml
    ggml_base
    ggml_cpu
    libcpp_shared
    android
    log
)
