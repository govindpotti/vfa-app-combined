plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.vfa.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.vfa.app"
        // Old LG hardware. 23 (Android 6.0) is as low as this can go: Compose 1.10
        // itself declares minSdk 23, so Lollipop would mean dropping Compose entirely.
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The two analysis services (see /server and /step_verifier). Leave empty to run the
        // app fully offline on its simulated checkpoints + result; set them to the deployed
        // base URLs (no trailing path) to use the real analysis.
        buildConfigField(
            "String",
            "ANALYZER_URL",
            providers.gradleProperty("ANALYZER_URL").orElse("").get().asBuildConfigString()
        )
        buildConfigField(
            "String",
            "VERIFIER_URL",
            providers.gradleProperty("VERIFIER_URL").orElse("").get().asBuildConfigString()
        )
    }

    buildTypes {
        release {
            // R8: shrink, optimise and strip unused resources. The debug build's dex
            // is ~33 MB unshrunk, most of it Compose and CameraX that never runs.
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("phone") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isJniDebuggable = false
            // Installable with adb on the physical LG while keeping release shrinking.
            optimization {
                enable = true
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
    androidResources {
        // The demo clips are already H.264 — don't let aapt try to compress them.
        noCompress += "mp4"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Live camera for the checkpoints and the two reader photos.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
