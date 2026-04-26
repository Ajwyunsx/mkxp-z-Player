plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.mkxpz.engine"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-Wall", "-Wextra")
            }
        }
    }

    ndkVersion = "25.1.8937393"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        // SDL's Android glue supports optional Bluetooth, microphone, and haptics paths.
        // Those permissions are feature-dependent and guarded at runtime by SDL/native code.
        disable += "MissingPermission"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.intuit.sdp:sdp-android:1.1.0")
}
