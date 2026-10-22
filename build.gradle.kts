// bxane
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("java-library")
    id("com.google.protobuf")
    id("maven-publish")
}

group = "com.vyonex"
version = "0.1.0"

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll("-Xmulti-platform", "-Xexpect-actual-classes")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api("com.squareup.okhttp3:okhttp:5.3.2")
    api("io.ktor:ktor-client-core:3.5.2")
    api("io.ktor:ktor-client-okhttp:3.5.2")
    api("io.ktor:ktor-client-content-negotiation:3.5.2")
    api("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("io.github.dokar3:quickjs-kt:1.0.14")
    implementation("org.jsoup:jsoup:1.22.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.protobuf:protobuf-javalite:4.35.0")
    implementation("org.mozilla:rhino:1.8.1")
    implementation("org.mozilla:rhino-engine:1.8.1")
    testImplementation("io.ktor:ktor-client-mock:3.5.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.2")
}

tasks.test { useJUnitPlatform() }

tasks.jar {
    from(files("LICENSE", "LICENSE-APACHE", "ATTRIBUTION.md")) { into("META-INF") }
}

publishing {
    publications {
        create<MavenPublication>("vyonex") {
            from(components["java"])
            pom {
                name.set("VyonEx")
                description.set("Kotlin/JVM media extraction library")
                url.set("https://github.com/bxanedot/vyonex")
                licenses {
                    license {
                        name.set("GNU General Public License, Version 3.0 only")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("bxane")
                        name.set("bxane")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/bxanedot/vyonex.git")
                    developerConnection.set("scm:git:ssh://git@github.com/bxanedot/vyonex.git")
                    url.set("https://github.com/bxanedot/vyonex")
                }
            }
        }
    }
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.35.0" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins { named("java") { option("lite") } }
        }
    }
}
// bxane
