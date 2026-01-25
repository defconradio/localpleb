plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    //implementation("fr.acinq.bitcoin:bitcoin-kmp-jvm:0.25.0")
    implementation(project(":crypto"))
    implementation(project(":nostr")) // <-- Add this line to use nostr module
    implementation(project(":data"))
    implementation("fr.acinq.secp256k1:secp256k1-kmp-jvm:0.18.0")
    implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm:0.18.0")
    implementation("fr.acinq.bitcoin:bitcoin-kmp:0.25.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.ktor:ktor-client-core:2.3.10")
    implementation("io.ktor:ktor-client-cio:2.3.10")
    implementation("io.ktor:ktor-client-websockets:2.3.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

application {
    mainClass.set("cryptoTestKt") // Entry point for crypto tests
}

tasks.register<JavaExec>("nostr") {
    group = "application"
    description = "Run nostrTestKt main()"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("NostrTestKt")
}

tasks.register<JavaExec>("crypto") {
    group = "application"
    description = "Run cryptoTestKt main()"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("CryptoTestKt")
}
tasks.register<JavaExec>("nip44") {
    group = "application"
    description = "Run nip44TestKt main()"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("nip44.Nip44TestKt")
}
tasks.register<JavaExec>("nip17") {
    group = "application"
    description = "Run nip17TestKt main()"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Nip17TestKt") // Correct main class name, no package
}

tasks.register<JavaExec>("nip13") {
    group = "application"
    description = "Run nip13TestKt main()"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Nip13TestKt")
}
