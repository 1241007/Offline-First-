cmake_minimum_required(VERSION 3.22.1)
project(edunova_llama LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)

include(FetchContent)
FetchContent_Declare(
    llama_cpp
    URL https://github.com/ggml-org/llama.cpp/archive/2bc563573479d53b30b8793039485887bc0fdda8.tar.gz
)
FetchContent_MakeAvailable(llama_cpp)

add_library(edunova_llama SHARED llama_jni.cpp)
target_link_libraries(edunova_llama PRIVATE llama android log)
target_include_directories(edunova_llama PRIVATE
    ${llama_cpp_SOURCE_DIR}/include
)
