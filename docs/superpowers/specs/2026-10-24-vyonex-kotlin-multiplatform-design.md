# VyonEx Kotlin Multiplatform and iOS Design

## Intent

Ship VyonEx as one Kotlin Multiplatform Gradle module that remains usable from JVM applications and can also be linked into Swift/iOS applications as an XCFramework. Keep the existing `com.vyonex` entry points: `VyonEx`, `VyonExHost`, `VyonExStream`, `VyonExAudioCandidate`, `VyonExAudioFormat`, and `VyonExConfigStore`.

## Current state and constraints

- The repository now uses Kotlin Multiplatform 2.4.21, Gradle 9.8.2, and a JDK 27 toolchain; JVM bytecode targets Java 26.
- Public adapters currently expose JVM-only `java.io.File` and OkHttp engine types.
- The source tree contains 213 Java files and 124 Kotlin files. The fallback extractor relies on JVM-only Java APIs and engines, while parts of the primary YouTube path are already Kotlin and use Ktor.
- Common code cannot import Java sources, Rhino, ICU4J, or the Ktor OkHttp engine. The existing QuickJS-KT dependency supports Kotlin/Native, so its runtime can be shared from `commonMain`; Rhino-dependent parsing still needs a platform boundary or replacement.
- This checkout runs on Windows. Kotlin/Native configures iOS targets here, but building and linking iOS frameworks requires a macOS host with Apple tooling.
- Kotlin's published compatibility table lists Gradle through 9.7.0 for plugin 2.4.21; this project uses Gradle 9.8.2 to run on JDK 27. JVM verification passes locally, and macOS CI must verify native builds.
- Preserve Git history, Maven coordinates, service URLs, the GPL license, and upstream notices until each derived source/data slice has its tested replacement.

## Approaches considered

1. **One KMP module with shared models and platform adapters (recommended).** Keep the existing JVM surface on `jvmMain`, move portable contracts and extraction logic into `commonMain`, and use Ktor Darwin for iOS HTTP while sharing the KMP QuickJS runtime. Publish JVM and iOS variants from the existing `vyonex` artifact. This keeps one dependency and avoids rewriting platform-specific code into a lowest-common-denominator implementation.
2. **Separate JVM and iOS implementations.** Add an iOS facade alongside the current library and duplicate extraction orchestration. This is quicker initially but drifts and makes feature parity harder to verify.
3. **A new iOS-only artifact.** Leave the JVM project untouched and publish another dependency. This conflicts with the single-project and single-dependency goal.

## Chosen design

Use one KMP module with `commonMain`, `jvmMain`, and the iOS hierarchy (`iosArm64`, `iosSimulatorArm64`, `iosX64`). Keep JVM binary signatures where they already exist; use platform adapters for filesystem and HTTP. Use Ktor common APIs for shared HTTP behavior, the OkHttp engine on JVM, and Darwin on iOS. The primary backend, public YouTube `extract` facade, watch-page response parser, QuickJS-KT runtime, player-wrapper parser, cipher solvers, challenge orchestration, and YouTube cipher service now live in common/iOS source sets. The Rhino-backed parser and legacy NewPipe fallback APIs remain JVM-only.

## Port status

- Shared and locally verified: extractor request orchestration, primary backend wiring, QuickJS runtime, player URL-cipher parser/solver, configured player cipher solver, challenge orchestration, and YouTube cipher service. The JVM retains the legacy parser fallback through a platform seam; iOS uses the shared parser path.
- iOS exposes `VyonExHost`, `VyonEx`, and YouTube extraction with primary clients followed by the shared watch-page fallback, plus header/session methods. The iOS Darwin client is created internally. The Rhino-backed player-function parser and legacy NewPipe fallback methods remain JVM-only; prepared-player scripts use memory-only caching on iOS.
- Windows checks compile iOS device/simulator main and test sources. Native simulator test execution and Apple framework linking must pass on macOS CI before these shared cipher slices are considered supported on iOS.

Move shared value types first, then the YouTube primary path and its platform services, then non-YouTube services, YouTube fallback, and bundled assets. The JVM target remains functional throughout the migration. A source or attribution notice is retired only after its replacement passes offline compatibility tests on the targets it serves.

## Verification

- Run focused offline compatibility tests and the full JVM suite after each slice.
- Build the iOS simulator/device framework and run shared tests on macOS CI; local Windows success alone does not prove iOS compatibility.
- Verify the six public entry points in both published JVM metadata and the generated iOS framework.
- Keep `ATTRIBUTION.md` until an inventory confirms all derived sources and bundled data have been replaced.

## Risks

- Java 26 bytecode raises the JVM consumer requirement from Java 17, as requested for the latest runtime.
- iOS cannot use JVM-only JavaScript engines, ICU4J, Java file APIs, or OkHttp engine integration; each needs an explicit native/shared replacement.
- iOS builds cannot be fully verified on this Windows host, so a macOS build gate is required before calling the platform supported.
- The current Kotlin/Gradle version pair is outside Kotlin's listed Gradle compatibility range; CI failures may require choosing between the newest JDK 27-capable Gradle and a listed-compatible older Gradle/JDK combination.
