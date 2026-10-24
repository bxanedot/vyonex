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
- The YouTube primary extractor and watch-page fallback are available to Swift/iOS through the `VyonEx` host/extract API. iOS uses Ktor Darwin and keeps prepared-player scripts in memory.
- The JVM facade and fallback behavior remain unchanged.
- Runtime dependencies are declared by VyonEx.

On iOS, implement `VyonExHost` and `VyonExConfigStore`, construct `VyonEx(host:)`, call `initialize()`, then call the suspend `extract(videoId:maxKbps:skipClients:requireM4a:)` method from Swift with `async`. Extraction tries the primary YouTube clients first, then reads the watch-page player response if primary extraction fails.

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
