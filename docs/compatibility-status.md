# VyonEx compatibility and provenance

## Public API compatibility matrix

The stable entry points are the six `com.vyonex` types named below. The host interface is platform-specific; value and storage contracts are shared.

| API | JVM | iOS / Swift | Contract |
| --- | --- | --- | --- |
| `VyonEx` | Available | Available | JVM exposes `init`; iOS exposes `initialize`. Both expose `initializeFallback`, `audioCandidates`, `bandcampSuggestions`, `extract`, `headersFor`, `onRefused`, `exclude`, and `onSessionChanged`. `initializeFallback` is currently a no-op. |
| `VyonExHost` | Available | Available | Both inherit session, config, time and logging services. JVM additionally receives a token provider, files directory and OkHttp client; iOS receives its token provider and uses Darwin transport. |
| `VyonExStream` | Shared | Shared | Video ID, URL, bitrate, MIME type, loudness, client/profile identifiers and request headers. |
| `VyonExAudioCandidate` | Shared | Shared | URL, bitrate, MIME type and normalized audio format. |
| `VyonExAudioFormat` | Shared | Shared | `M4A`, `WEBM_OPUS`, `OTHER`. |
| `VyonExConfigStore` | Shared | Shared | Enable flag and string/long persistence; `VyonExSession` and `VyonExLogLevel` are companion shared values. |

`src/jvmTest/.../VyonExJvmApiTest.kt` is an offline compile/reflection contract for the JVM facade and all six public types. `src/iosTest/.../VyonExIosApiTest.kt` constructs the iOS facade without JVM transport types. These checks do not prove that every service behaves identically on each target.

## Service and platform status

| Service | JVM implementation | Shared/iOS status |
| --- | --- | --- |
| YouTube | Primary extraction and progressive audio candidates | Primary extraction and watch-page fallback are exposed to Swift; QuickJS/cipher and host behavior are covered by shared tests. |
| Bandcamp | JVM fallback service and extractors; `VyonEx.bandcampSuggestions` | Autocomplete is available through the shared facade and host transport. Search, track, playlist, and radio extraction remain JVM-only. |
| MediaCCC | JVM fallback service and extractors | Several response mappers/parsers and tests are shared; service extraction is not exposed through the stable facade on iOS. |
| PeerTube | JVM fallback service and extractors | Several URL/response parsers and tests are shared; service extraction is not exposed through the stable facade on iOS. |
| SoundCloud | JVM fallback service and extractors | Parsing and payload mappers are shared; service extraction is not exposed through the stable facade on iOS. |

The non-YouTube service classes remain under the JVM fallback implementation. Shared parsers are not equivalent to a complete cross-platform service API. The current implementation therefore does not claim full iOS parity for Bandcamp, MediaCCC, PeerTube or SoundCloud.

## Artifacts and dependencies

- Independent library version: `0.1.0` (Vyon's app version is separate).
- Gradle coordinates: `com.vyonex:vyonex:0.1.0`.
- Publication: `mavenLocal()` only; no public Maven artifact or Git tag is present in this checkout's configured release history.
- Build toolchain: JDK 27; JVM bytecode target 26. The JAR converts successfully with Android D8 37.0.0, but a complete Vyon Android consumer build and runtime API-level verification against this artifact have not been performed.
- Main dependencies include Ktor, kotlinx.coroutines, kotlinx.serialization, QuickJS-KT, and (on JVM) OkHttp, Jsoup, protobuf-javalite, ICU4J and Rhino. See `build.gradle.kts` for exact versions and scopes.

## Verification evidence

- Local Windows baseline before this API test: `jvmTest` succeeded.
- Local Windows API-contract slice: `jvmTest jvmJar` succeeded with `VyonExJvmApiTest`.
- The latest GitHub Actions run for main before this change, [run 40101956216](https://github.com/bxanedot/vyonex/actions/runs/40101956216), passed both JVM and macOS jobs. The macOS job runs JVM tests, iOS simulator tests, links device/simulator frameworks, assembles `VyonEx.xcframework`, and type-checks `ci/VyonExApiCheck.swift`. A new CI run is still needed for this commit.
- Windows cannot execute Kotlin/Native simulator tests or link Apple frameworks; local Windows results do not replace the macOS check.

## Clean-room and upstream attribution log

This source tree records adapted code from [InnerTubeX](https://github.com/MetrolistGroup/innertubex) (GPL-3.0) and [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) (GPL-3.0-or-later) in `ATTRIBUTION.md`. The current YouTube primary path and JVM fallback implementations still include upstream-derived material. Common parser ports have offline tests, but this is not a completed clean-room replacement: no source-isolation claim is made, and no upstream notice should be removed. New extraction work must follow `AGENTS.md` and use XERJ references for architecture/behavior only.
