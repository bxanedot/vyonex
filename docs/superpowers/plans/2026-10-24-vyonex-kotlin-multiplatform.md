# VyonEx Kotlin Multiplatform Implementation Plan

> **For agentic workers:** Use the native execution path; implement each task in order with test-first changes.

**Goal:** Keep VyonEx's JVM extractor usable and expose the same supported extraction API to Swift/iOS from one Kotlin Multiplatform project.

**Architecture:** Convert the existing Gradle project to KMP, share the public value types, extraction orchestration, QuickJS runtime, player URL-cipher parser, configured player cipher solver, and challenge orchestration. Keep JVM-only HTTP adapters and the legacy Rhino parser in `jvmMain`; use Darwin for iOS HTTP while the public extraction facade and host are ported.

**Tech Stack:** Kotlin Multiplatform 2.4.21, Gradle 9.8.2, JDK 27 for builds, JVM bytecode 26, Ktor 3.5.2, Darwin, Kotlin/Native iOS Arm64 and simulator targets.

**Spec:** `docs/superpowers/specs/2026-10-24-vyonex-kotlin-multiplatform-design.md`

## Global Constraints

- Preserve `VyonEx`, `VyonExHost`, `VyonExStream`, `VyonExAudioCandidate`, `VyonExAudioFormat`, and `VyonExConfigStore` under `com.vyonex`.
- Keep the current extractor features, service URLs, ordinary Maven dependencies, GPL license, and Git history.
- Replace copied source and bundled data clean-room; retain attribution until each derived slice has passing compatibility tests.
- Build JVM code with JDK 27 and target Java 26; build `iosArm64`, `iosSimulatorArm64`, and `iosX64` frameworks on macOS.

## Review Focus

- JVM client source compatibility: compile a fixture implementing all six public APIs on the JVM.
- Swift visibility: inspect generated framework headers for the six API names and an async extraction call.
- HTTP failures and malformed service payloads: retain typed errors and existing fallbacks in offline tests.
- Missing host storage or QuickJS runtime support: extraction must fail or fall back without crashing.
- iOS target drift: compile both device and simulator frameworks in macOS CI for every shared API change.

---

### Task 1: KMP foundation and shared value API

**Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `src/main/**`, `src/jvmMain/**`, `src/commonMain/kotlin/com/vyonex/**`, `src/test/**`, `src/jvmTest/**`, and a macOS workflow.

**Interfaces:** Keep the six public names; move platform-neutral values, extraction orchestration, QuickJS execution, player URL-cipher parser, configured player cipher solver, and challenge orchestration to `commonMain`. Keep Java, filesystem, OkHttp, protobuf, ICU, and the legacy Rhino parser in `jvmMain`. Add iOS device and simulator frameworks using shared source sets.

- [ ] Add JVM API compatibility tests and assertions that shared models are available from common code.
- [ ] Convert the project to the KMP plugin, relocate sources to the standard source sets, configure JVM and iOS targets, and declare Darwin in `iosMain`.
- [ ] Configure JVM publication as `com.vyonex:vyonex` and KMP publication metadata without changing its coordinates.
- [ ] Run focused JVM tests and ensure Gradle configures all iOS targets; add macOS CI to link the device/simulator frameworks and XCFramework.

### Task 2: Common host services and HTTP boundary

**Files:** `VyonExHost.kt`, `PrimaryExtractorBackend.kt`, `FallbackExtractorBackend.kt`, platform host adapters, and API compatibility tests.

**Interfaces:** Common host services supply the Ktor `HttpClient`, storage access, time, session, logging, config, and token provider without exposing JVM types. JVM adapters retain the existing OkHttp integration; iOS uses Darwin and app-provided storage.

- [ ] Add JVM and common/iOS host contract tests for session, config defaults, storage absence, and HTTP error mapping.
- [ ] Move the host contract to `commonMain`; implement platform HTTP/storage services for JVM and iOS.
- [ ] Run JVM tests and both framework link jobs; verify Kotlin suspend functions export safely to Swift.

### Task 3: YouTube primary extractor

**Files:** `PrimaryExtractorBackend.kt`, its helpers, cipher/token/session services, and existing YouTube tests.

**Interfaces:** `VyonEx.extract`, `headersFor`, `onRefused`, `exclude`, and `onSessionChanged` remain available on JVM and iOS.

- [ ] Add offline request/response fixtures for player success, ciphered URL, token rejection, session changes, and exclusions.
- [ ] Move portable extraction logic to `commonMain`; implement platform script execution and storage services.
- [ ] Run focused JVM tests and macOS framework tests before retiring any derived notices.

### Task 4: Non-YouTube services

**Files:** service parsing/extractor packages and service-specific tests.

**Interfaces:** Preserve current public stream output and supported service URLs.

- [ ] Add offline compatibility fixtures for each non-YouTube service's URL parsing, successful payload, malformed payload, and HTTP failure.
- [ ] Replace platform-specific networking/parsing boundaries with shared Ktor/common code in small service groups.
- [ ] Run the service test slice and full JVM suite; compile both iOS frameworks.

### Task 5: YouTube fallback and bundled assets

**Files:** fallback extraction, player resources, scripts, and related tests.

**Interfaces:** Preserve `initializeFallback` and `audioCandidates` behavior for JVM and iOS.

- [ ] Add offline fixtures for fallback initialization, format selection, script-backed extraction, and failure fallback.
- [ ] Implement iOS-compatible script execution and replace bundled derived assets only when fixtures pass on both platform families.
- [ ] Run full JVM and macOS native verification, inventory source/data, then remove only notices made obsolete by the clean-room replacements.

### Task 6: API, artifacts, and provenance audit

**Files:** `README.md`, publication metadata, `ATTRIBUTION.md`, CI, and tests.

**Interfaces:** JVM and iOS artifacts expose the six named APIs; the JVM coordinate remains `com.vyonex:vyonex`.

- [ ] Add JVM compile fixture and Swift-facing header/API checks.
- [ ] Run `clean test jvmJar` plus both iOS framework builds; verify generated publication metadata and attribution inventory.
- [ ] Update compatibility documentation and commit the independently verified result.
