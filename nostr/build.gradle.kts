plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.0"
    id("com.android.library") // Required for androidTarget()
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.atomicfu)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                api(project(":crypto"))
            }
        }
        val androidMain by getting // Android source set
        val jvmMain by getting // JVM source set
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    jvmToolchain(21)
}

android {
    namespace = "com.example.nostr"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Optionally, configure publishing, etc.
