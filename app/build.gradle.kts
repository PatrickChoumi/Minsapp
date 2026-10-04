import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// CI build number, so every new APK installs over the previous one.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "app.minsapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.minsapp"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    signingConfigs {
        // A fixed key (instead of each machine's random debug key) so updates install in place.
        // Personal sideloaded app: not meant for the Play Store.
        getByName("debug") {
            storeFile = file("minsapp-debug.keystore")
            storePassword = "android"
            keyAlias = "minsapp"
            keyPassword = "android"
        }
    }

    sourceSets {
        // The rules live in core/ so they can be unit-tested without the Android SDK.
        getByName("main").java.srcDir("../core/src/main/kotlin")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
