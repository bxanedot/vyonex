# VyonEx

VyonEx is one Kotlin/JVM library for media extraction under the `com.vyonex` namespace.

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

- Kotlin/JVM 17.
- The com.vyonex package is the public host and stream API.
- Runtime dependencies are declared by VyonEx.

## Build

```text
./gradlew clean test jar publishToMavenLocal
```

## License

VyonEx is distributed under GPL-3.0-only. Third-party code and data sources are listed in [ATTRIBUTION.md](ATTRIBUTION.md), with applicable notices retained alongside included material.

## Contributors

1. bxane — project owner and maintainer.
