plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// --- Load .env file at build time (top-level scope) ---
// providers.environmentVariable() only reads OS-level env vars; .env files
// are never automatically sourced by Gradle. This block parses the .env file
// at the top level where all standard Kotlin/Java APIs are available.
fun loadDotEnv(): Map<String, String> {
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) return emptyMap()
    return envFile.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
        .associate { line ->
            val idx = line.indexOf('=')
            line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        }
}

val dotEnv = loadDotEnv()

val smallOfflineModelUrl: String =
    (System.getenv("OFFLINE_MODEL_1_5B_URL")
        ?: dotEnv["OFFLINE_MODEL_1_5B_URL"]
        ?: providers.gradleProperty("offlineModelSmallUrl").orNull
        ?: "https://huggingface.co/buckets/PatilKrish/Qwen_Models2/resolve/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf?download=true")

val largeOfflineModelUrl: String =
    (System.getenv("OFFLINE_MODEL_3B_URL")
        ?: dotEnv["OFFLINE_MODEL_3B_URL"]
        ?: providers.gradleProperty("offlineModelLargeUrl").orNull
        ?: "https://huggingface.co/buckets/PatilKrish/Qwen_Models2/resolve/Qwen2.5-3B-Instruct-Q4_K_M.gguf?download=true")

val backendBaseUrl: String =
    (System.getenv("BACKEND_BASE_URL")
        ?: dotEnv["BACKEND_BASE_URL"]
        ?: providers.gradleProperty("backendBaseUrl").orNull
        ?: "https://edunova-backend-9waj.onrender.com")

android {
    namespace = "com.offline_First"
    ndkVersion = "27.2.12479018"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.offline_First"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OFFLINE_MODEL_SMALL_URL", "\"$smallOfflineModelUrl\"")
        buildConfigField("String", "OFFLINE_MODEL_LARGE_URL", "\"$largeOfflineModelUrl\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }

    // Package the prebuilt llama.cpp .so files alongside our JNI bridge
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs(setOf(file("src/main/jniLibs")))
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}