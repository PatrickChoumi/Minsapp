// Pure-Kotlin rules, built and tested on their own (no Android SDK needed).
// The Android app compiles these same sources (see app/build.gradle.kts).
rootProject.name = "minsapp-core"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
