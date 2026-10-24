import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

// bxane
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
}

group = "com.vyonex"
version = "0.1.0"

kotlin {
    jvmToolchain(27)

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_26)
            freeCompilerArgs.add("-Xjdk-release=26")
        }
    }

    val vyonexXCFramework = XCFramework("VyonEx")
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "VyonEx"
            isStatic = true
            vyonexXCFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-http:3.5.2")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
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
            implementation("com.ibm.icu:icu4j:78.3")
            implementation("org.mozilla:rhino:1.8.1")
            implementation("org.mozilla:rhino-engine:1.8.1")
        }

        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.5.2")
        }

        jvmTest.dependencies {
            implementation("io.ktor:ktor-client-mock:3.5.2")
            implementation("org.junit.jupiter:junit-jupiter:6.1.2")
            runtimeOnly("org.junit.platform:junit-platform-launcher:6.1.2")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(26)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    from(files("LICENSE", "ATTRIBUTION.md")) { into("META-INF") }
}

publishing {
    repositories {
        mavenLocal()
    }

    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("VyonEx")
            description.set("Kotlin Multiplatform media extraction library")
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
