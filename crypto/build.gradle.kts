@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization") version "1.9.0" // Use your actual Kotlin version
}

kotlin {
    androidTarget()
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("fr.acinq.bitcoin:bitcoin-kmp:0.25.0")
                implementation("io.ktor:ktor-utils:2.3.9") // Ktor multiplatform Base64
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-android:0.18.0")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm:0.18.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.example.crypto"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // Use latest version if needed
}

