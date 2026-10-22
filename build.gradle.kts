// bxane
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-library")
}

group = "com.vyon"
version = "0.1.0"

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api("com.squareup.okhttp3:okhttp:5.3.2")
    api("com.github.MetrolistGroup.innertubex:innertubex:v0.7.0")
    compileOnly("com.github.TeamNewPipe:NewPipeExtractor:v0.26.3")

    implementation("io.ktor:ktor-client-core:3.5.2")
    implementation("io.ktor:ktor-client-okhttp:3.5.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
// bxane
