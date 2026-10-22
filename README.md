# VyonEx

VyonEx is the reusable Kotlin/JVM YouTube audio extraction module used by Vyon. It contains the InnerTubeX adapter and the NewPipe Extractor fallback. Vyon's `StreamResolver` remains responsible for retries, URL validation, caching, bitrate and format policy, metrics, and playback/download decisions.

## Compatibility

- Kotlin/JVM 17.
- InnerTubeX `0.7.0` is the primary extractor.
- NewPipe Extractor `0.26.3` is the fallback. The Vyon Android and desktop applications provide its runtime artifact; VyonEx declares it as `compileOnly`.
- The module rejects unsupported SABR-only responses. SABR remains deferred until playback is integrated end to end.
- `com.vyon.vyonex` is the public host and stream API. Backend classes are implementation details and may change between VyonEx releases.

## Build

From this directory, run:

```text
./gradlew compileKotlin jar
```

The module is also included in Vyon's root Gradle build as `:vyonex` from `core/vyonex`.

## Dependency notices

VyonEx source is licensed under Apache-2.0; see [LICENSE](LICENSE). Its upstream dependencies retain their own terms:

| Dependency | Version | Upstream | License |
| --- | --- | --- | --- |
| InnerTubeX | 0.7.0 | [MetrolistGroup/innertubex](https://github.com/MetrolistGroup/innertubex) | GPL-3.0 |
| NewPipe Extractor | 0.26.3 | [TeamNewPipe/NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) | GPL-3.0 |
| Ktor client | 3.5.2 | [ktorio/ktor](https://github.com/ktorio/ktor) | Apache-2.0 |
| OkHttp | 5.3.2 | [square/okhttp](https://github.com/square/okhttp) | Apache-2.0 |
| kotlinx.coroutines | 1.10.2 | [Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Apache-2.0 |
| kotlinx.serialization JSON | 1.11.0 | [Kotlin/kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | Apache-2.0 |

The consuming Vyon applications retain dependency notices and the license texts required for their redistributed dependencies.
