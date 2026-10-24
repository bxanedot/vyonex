# VyonEx

VyonEx is one Kotlin Multiplatform project for media extraction under the `com.vyonex` namespace.

## Install locally

The artifact is not published to a public Maven repository yet. Build and install it into your local Maven cache:

```text
./gradlew publishToMavenLocal
```

Then add `mavenLocal()` to the consuming project's repositories:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.vyonex:vyonex:0.1.0")
}
```

## Compatibility

- Build and run Gradle with JDK 27.
- Published JVM bytecode targets Java 26 and requires JDK 26 or newer at runtime.
- Kotlin compiler: 2.4.21.
- JVM and iOS device/simulator targets are configured.
- The shared stream, audio, session, logging, and config types are in `commonMain`.
- The extractor and host adapters remain JVM-only while their platform services are migrated; iOS extraction is not ready yet.
- Runtime dependencies are declared by VyonEx.

## Build

```text
./gradlew clean jvmTest jvmJar publishToMavenLocal
```

On macOS, build the iOS XCFramework with:

```text
./gradlew assembleVyonExDebugXCFramework
```

## License

VyonEx is distributed under GPL-3.0-only. Third-party code and data sources are listed in [ATTRIBUTION.md](ATTRIBUTION.md), with applicable notices retained alongside included material.

## Contributors

1. bxane — project owner and maintainer.
