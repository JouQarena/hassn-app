// App-level build configuration for Focus Redirect
// Targets Android 7+ (API 24) with Jetpack Compose and Material 3.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hassn.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hassn.app"
        minSdk = 24      // Android 7.0+ — covers 99% of active devices
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // ── Signing configuration ──────────────────────────────────────────
    signingConfigs {
        val releaseStoreFile = project.findProperty("release.storeFile") as? String
        val releaseStorePassword = project.findProperty("release.storePassword") as? String
        val releaseKeyAlias = project.findProperty("release.keyAlias") as? String
        val releaseKeyPassword = project.findProperty("release.keyPassword") as? String

        create("release") {
            storeFile = if (releaseStoreFile != null) {
                rootProject.file(releaseStoreFile)
            } else {
                file(
                    System.getProperty("user.home") +
                        "/.android/debug.keystore"
                )
            }
            storePassword = releaseStorePassword ?: "android"
            keyAlias = releaseKeyAlias ?: "androiddebugkey"
            keyPassword = releaseKeyPassword ?: "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ── Dependencies ───────────────────────────────────────────────────────
dependencies {
    // Core AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // DataStore for persisting settings
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
