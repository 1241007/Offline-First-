#include <jni.h>
#include <android/log.h>

#include "llama.h"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

namespace {

struct ModelHandle {
    llama_model * model = nullptr;
    llama_context * context = nullptr;
    const llama_vocab * vocab = nullptr;
    llama_sampler * sampler = nullptr;
    std::atomic_bool cancelled{false};
    std::mutex inference_mutex;
    std::vector<llama_token> cached_tokens;
    std::string instance_id;
};

std::once_flag backend_init_flag;

void throw_java(JNIEnv * env, const char * class_name, const std::string & message) {
    jclass exception_class = env->FindClass(class_name);
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message.c_str());
        env->DeleteLocalRef(exception_class);
    }
}

void batch_set_tokens(
    llama_batch_ext * batch,
    const llama_token * tokens,
    int32_t count,
    llama_pos start_position
) {
    llama_batch_ext_clear(batch);
    for (int32_t i = 0; i < count; ++i) {
        const int32_t index = llama_batch_ext_add_token(batch, 0, tokens[i]);
        const llama_pos position = start_position + i;
        llama_batch_ext_set_pos(batch, index, &position);
    }
    llama_batch_ext_set_output_logits(batch, count - 1, true);
}

jlong native_load_model(JNIEnv * env, jobject, jstring model_path) {
    const char * path = env->GetStringUTFChars(model_path, nullptr);
    if (path == nullptr) {
        return 0;
    }

    std::call_once(backend_init_flag, [] { llama_backend_init(); });
    auto handle = std::make_unique<ModelHandle>();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    handle->model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (handle->model == nullptr) {
        throw_java(env, "java/lang/IllegalStateException", "llama.cpp could not load the GGUF model.");
        return 0;
    }

    handle->vocab = llama_model_get_vocab(handle->model);
    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = 2048;
    context_params.n_batch = 512;
    context_params.n_ubatch = 512;
    context_params.no_perf = true;
    handle->context = llama_init_from_model(handle->model, context_params);

    if (handle->context == nullptr) {
        llama_model_free(handle->model);
        throw_java(env, "java/lang/IllegalStateException", "llama.cpp could not initialize the model context.");
        return 0;
    }

    const int32_t threads = std::max(1, std::min(4, static_cast<int32_t>(std::thread::hardware_concurrency())));
    llama_set_n_threads(handle->context, threads, threads);
    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    handle->sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(handle->sampler, llama_sampler_init_greedy());

    handle->instance_id = std::to_string(reinterpret_cast<uintptr_t>(handle.get()));
    handle->cached_tokens.clear();

    return reinterpret_cast<jlong>(handle.release());
}

jstring native_generate_stream(
    JNIEnv * env,
    jobject,
    jlong raw_handle,
    jstring prompt,
    jint max_tokens,
    jobject callback,
    jlongArray out_metrics
) {
    auto * handle = reinterpret_cast<ModelHandle *>(raw_handle);
    if (handle == nullptr || handle->model == nullptr || handle->context == nullptr) {
        throw_java(env, "java/lang/IllegalStateException", "The offline model is not loaded.");
        return nullptr;
    }

    const char * prompt_chars = env->GetStringUTFChars(prompt, nullptr);
    if (prompt_chars == nullptr) {
        return nullptr;
    }
    const std::string prompt_text(prompt_chars);
    env->ReleaseStringUTFChars(prompt, prompt_chars);

    jmethodID on_token_method = nullptr;
    if (callback != nullptr) {
        jclass callback_class = env->GetObjectClass(callback);
        if (callback_class != nullptr) {
            on_token_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)Z");
            env->DeleteLocalRef(callback_class);
        }
    }

    std::lock_guard<std::mutex> inference_lock(handle->inference_mutex);

    using clock = std::chrono::steady_clock;

    const int32_t token_count = -llama_tokenize(
        handle->vocab,
        prompt_text.c_str(),
        static_cast<int32_t>(prompt_text.size()),
        nullptr,
        0,
        true,
        true
    );
    if (token_count <= 0) {
        throw_java(env, "java/lang/IllegalStateException", "Could not tokenize the offline prompt.");
        return nullptr;
    }

    std::vector<llama_token> tokens(static_cast<size_t>(token_count));
    if (llama_tokenize(
            handle->vocab,
            prompt_text.c_str(),
            static_cast<int32_t>(prompt_text.size()),
            tokens.data(),
            token_count,
            true,
            true
        ) < 0) {
        throw_java(env, "java/lang/IllegalStateException", "Could not tokenize the offline prompt.");
        return nullptr;
    }

    constexpr int32_t context_size = 2048;
    const int32_t prediction_limit = std::clamp(static_cast<int32_t>(max_tokens), 1, 512);
    if (token_count >= context_size - prediction_limit) {
        throw_java(env, "java/lang/IllegalArgumentException", "The conversation is too long for the local model context.");
        return nullptr;
    }

    // ── KV Cache Prefix Matching ──────────────────────────────────────────────
    size_t common_prefix = 0;
    while (common_prefix < handle->cached_tokens.size() &&
           common_prefix < tokens.size() &&
           handle->cached_tokens[common_prefix] == tokens[common_prefix]) {
        common_prefix++;
    }

    if (common_prefix < handle->cached_tokens.size()) {
        if (common_prefix == 0) {
            llama_memory_clear(llama_get_memory(handle->context), true);
            handle->cached_tokens.clear();
        } else {
            llama_memory_seq_rm(llama_get_memory(handle->context), 0, static_cast<llama_pos>(common_prefix), -1);
            handle->cached_tokens.resize(common_prefix);
        }
    }

    std::unique_ptr<llama_batch_ext, decltype(&llama_batch_ext_free)> batch(
        llama_batch_ext_init(handle->context),
        llama_batch_ext_free
    );
    if (!batch) {
        throw_java(env, "java/lang/IllegalStateException", "Could not allocate the offline inference batch.");
        return nullptr;
    }

    llama_sampler_reset(handle->sampler);

    auto t_prompt_eval_start = clock::now();
    int32_t position = static_cast<int32_t>(common_prefix);
    const int32_t evaluated_tokens = token_count - static_cast<int32_t>(common_prefix);

    for (int32_t offset = static_cast<int32_t>(common_prefix); offset < token_count;) {
        const int32_t chunk_size = std::min(512, token_count - offset);
        batch_set_tokens(batch.get(), tokens.data() + offset, chunk_size, position);
        if (llama_process(handle->context, LLAMA_PROCESS_TYPE_DECODE, batch.get()) != 0) {
            llama_memory_clear(llama_get_memory(handle->context), true);
            handle->cached_tokens.clear();
            throw_java(env, "java/lang/IllegalStateException", "llama.cpp failed while evaluating the prompt.");
            return nullptr;
        }
        offset += chunk_size;
        position += chunk_size;
    }

    auto t_prompt_eval_end = clock::now();
    int64_t prompt_eval_ms = std::chrono::duration_cast<std::chrono::milliseconds>(t_prompt_eval_end - t_prompt_eval_start).count();

    for (size_t i = common_prefix; i < tokens.size(); ++i) {
        handle->cached_tokens.push_back(tokens[i]);
    }

    // ── Token Generation & Streaming ──────────────────────────────────────────
    auto t_gen_start = clock::now();
    std::string generated_text;
    int32_t generated_tokens_count = 0;

    for (int32_t generated = 0; generated < prediction_limit; ++generated) {
        if (handle->cancelled.load()) {
            break;
        }

        const llama_token token = llama_sampler_sample(handle->sampler, handle->context, -1);
        if (llama_vocab_is_eog(handle->vocab, token)) {
            break;
        }
        llama_sampler_accept(handle->sampler, token);

        char piece_buffer[256];
        int32_t piece_size = llama_token_to_piece(
            handle->vocab,
            token,
            piece_buffer,
            sizeof(piece_buffer),
            0,
            true
        );
        std::string piece_str;
        if (piece_size < 0) {
            std::vector<char> expanded_buffer(static_cast<size_t>(-piece_size));
            piece_size = llama_token_to_piece(
                handle->vocab,
                token,
                expanded_buffer.data(),
                static_cast<int32_t>(expanded_buffer.size()),
                0,
                true
            );
            if (piece_size < 0) {
                throw_java(env, "java/lang/IllegalStateException", "Could not decode a generated model token.");
                return nullptr;
            }
            piece_str.assign(expanded_buffer.data(), static_cast<size_t>(piece_size));
        } else {
            piece_str.assign(piece_buffer, static_cast<size_t>(piece_size));
        }

        generated_text.append(piece_str);
        handle->cached_tokens.push_back(token);
        generated_tokens_count++;

        if (callback != nullptr && on_token_method != nullptr) {
            jstring piece_jstr = env->NewStringUTF(piece_str.c_str());
            jboolean keep_going = env->CallBooleanMethod(callback, on_token_method, piece_jstr);
            env->DeleteLocalRef(piece_jstr);
            if (!keep_going) {
                break;
            }
        }

        batch_set_tokens(batch.get(), &token, 1, position++);
        if (llama_process(handle->context, LLAMA_PROCESS_TYPE_DECODE, batch.get()) != 0) {
            throw_java(env, "java/lang/IllegalStateException", "llama.cpp failed during text generation.");
            return nullptr;
        }
    }

    auto t_gen_end = clock::now();
    int64_t gen_ms = std::chrono::duration_cast<std::chrono::milliseconds>(t_gen_end - t_gen_start).count();

    if (out_metrics != nullptr) {
        jlong metrics[6] = {
            static_cast<jlong>(token_count),
            static_cast<jlong>(common_prefix),
            static_cast<jlong>(evaluated_tokens),
            static_cast<jlong>(generated_tokens_count),
            static_cast<jlong>(prompt_eval_ms),
            static_cast<jlong>(gen_ms)
        };
        env->SetLongArrayRegion(out_metrics, 0, 6, metrics);
    }

    __android_log_print(ANDROID_LOG_INFO, "OFFLINE_LATENCY",
        "OFFLINE_NATIVE: prompt_tokens=%d, cached_prefix=%zu, evaluated=%d, prompt_eval_ms=%lld, gen_tokens=%d, gen_ms=%lld, tok_per_sec=%.2f",
        token_count, common_prefix, evaluated_tokens, (long long)prompt_eval_ms,
        generated_tokens_count, (long long)gen_ms,
        gen_ms > 0 ? (generated_tokens_count * 1000.0 / gen_ms) : 0.0);

    return env->NewStringUTF(generated_text.c_str());
}

jstring native_generate(JNIEnv * env, jobject thiz, jlong raw_handle, jstring prompt, jint max_tokens) {
    return native_generate_stream(env, thiz, raw_handle, prompt, max_tokens, nullptr, nullptr);
}

jstring native_get_instance_id(JNIEnv * env, jobject, jlong raw_handle) {
    auto * handle = reinterpret_cast<ModelHandle *>(raw_handle);
    if (handle == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(handle->instance_id.c_str());
}

void native_clear_cache(JNIEnv *, jobject, jlong raw_handle) {
    auto * handle = reinterpret_cast<ModelHandle *>(raw_handle);
    if (handle != nullptr && handle->context != nullptr) {
        std::lock_guard<std::mutex> inference_lock(handle->inference_mutex);
        llama_memory_clear(llama_get_memory(handle->context), true);
        handle->cached_tokens.clear();
    }
}

void native_cancel(JNIEnv *, jobject, jlong raw_handle) {
    auto * handle = reinterpret_cast<ModelHandle *>(raw_handle);
    if (handle != nullptr) {
        handle->cancelled.store(true);
    }
}

void native_reset_cancellation(JNIEnv *, jobject, jlong raw_handle) {
    auto * handle = reinterpret_cast<ModelHandle *>(raw_handle);
    if (handle != nullptr) {
        handle->cancelled.store(false);
    }
}

void native_unload(JNIEnv *, jobject, jlong raw_handle) {
    auto * handle = reinterpret_cast<ModelHandle *>(raw_handle);
    if (handle == nullptr) {
        return;
    }
    handle->cancelled.store(true);
    std::lock_guard<std::mutex> inference_lock(handle->inference_mutex);
    handle->cached_tokens.clear();
    if (handle->sampler != nullptr) {
        llama_sampler_free(handle->sampler);
    }
    if (handle->context != nullptr) {
        llama_free(handle->context);
    }
    if (handle->model != nullptr) {
        llama_model_free(handle->model);
    }
    delete handle;
}

JNINativeMethod native_methods[] = {
    {const_cast<char *>("nativeLoadModel"), const_cast<char *>("(Ljava/lang/String;)J"), reinterpret_cast<void *>(native_load_model)},
    {const_cast<char *>("nativeGenerate"), const_cast<char *>("(JLjava/lang/String;I)Ljava/lang/String;"), reinterpret_cast<void *>(native_generate)},
    {const_cast<char *>("nativeGenerateStream"), const_cast<char *>("(JLjava/lang/String;ILcom/offline_First/data/local/NativeTokenCallback;[J)Ljava/lang/String;"), reinterpret_cast<void *>(native_generate_stream)},
    {const_cast<char *>("nativeGetInstanceId"), const_cast<char *>("(J)Ljava/lang/String;"), reinterpret_cast<void *>(native_get_instance_id)},
    {const_cast<char *>("nativeClearCache"), const_cast<char *>("(J)V"), reinterpret_cast<void *>(native_clear_cache)},
    {const_cast<char *>("nativeResetCancellation"), const_cast<char *>("(J)V"), reinterpret_cast<void *>(native_reset_cancellation)},
    {const_cast<char *>("nativeCancel"), const_cast<char *>("(J)V"), reinterpret_cast<void *>(native_cancel)},
    {const_cast<char *>("nativeUnload"), const_cast<char *>("(J)V"), reinterpret_cast<void *>(native_unload)},
};

} // namespace

JNIEXPORT jint JNI_OnLoad(JavaVM * vm, void *) {
    JNIEnv * env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass bridge_class = env->FindClass("com/offline_First/data/local/LlamaNativeBridge");
    if (bridge_class == nullptr) {
        return JNI_ERR;
    }
    const jint method_count = static_cast<jint>(sizeof(native_methods) / sizeof(native_methods[0]));
    const jint result = env->RegisterNatives(bridge_class, native_methods, method_count);
    env->DeleteLocalRef(bridge_class);
    return result == JNI_OK ? JNI_VERSION_1_6 : JNI_ERR;
}
