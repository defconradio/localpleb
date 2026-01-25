plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

kotlin {
    // Define the Android target for KMP
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }

    // Define the JVM target
    jvm()

    // Apply the default source set hierarchy (e.g., androidMain dependsOn commonMain)
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":crypto"))
                implementation(project(":nostr"))
                implementation("fr.acinq.bitcoin:bitcoin-kmp:0.25.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // Adjusted version for compatibility
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
                implementation("androidx.datastore:datastore-preferences:1.0.0")
            }
        }
        val jvmMain by getting {
            dependencies {
                // JVM-specific dependencies can be added here
            }
        }
    }

    // Suppress expect/actual class beta warning
    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}

android {
    namespace = "com.example.data"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // The KMP plugin now handles source set configuration.
    // The conflicting manual sourceSets block has been removed.
}
