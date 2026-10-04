plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.offline_First"
    ndkVersion = "27.2.12479018"

    compileSdk {
        version = release(37)
    }

    val smallOfflineModelUrl = providers.gradleProperty("offlineModelSmallUrl")
        .orElse(providers.environmentVariable("OFFLINE_MODEL_1_5B_URL"))
        .orElse("https://huggingface.co/buckets/PatilKrish/Qwen_Models2/resolve/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf?download=true")
        .get()
    val largeOfflineModelUrl = providers.gradleProperty("offlineModelLargeUrl")
        .orElse(providers.environmentVariable("OFFLINE_MODEL_3B_URL"))
        .orElse("https://huggingface.co/buckets/PatilKrish/Qwen_Models2/resolve/Qwen2.5-3B-Instruct-Q4_K_M.gguf?download=true")
        .get()

    defaultConfig {
        applicationId = "com.offline_First"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OFFLINE_MODEL_SMALL_URL", "\"$smallOfflineModelUrl\"")
        buildConfigField("String", "OFFLINE_MODEL_LARGE_URL", "\"$largeOfflineModelUrl\"")

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
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}