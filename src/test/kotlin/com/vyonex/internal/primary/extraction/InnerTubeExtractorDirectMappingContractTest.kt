// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.InnerTube
import com.vyonex.internal.primary.InnerTubeLogEvent
import com.vyonex.internal.primary.InnerTubeLogger
import com.vyonex.internal.primary.extraction.strategy.ClientFallbackStrategy
import com.vyonex.internal.primary.models.YouTubeClient
import com.vyonex.internal.primary.models.response.PlayerResponse
import com.vyonex.internal.primary.models.response.PlayerResponse.StreamingData.Format
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class InnerTubeExtractorDirectMappingContractTest {
    @Test
    fun mapsAPlaybackReadyDirectAudioResponseWithoutFetchingAWatchPage() = runBlocking {
        assertEquals("OK", PlayerResponse.parse(PLAYER_RESPONSE).playabilityStatus.status)
        val requests = mutableListOf<String>()
        val client = playerClient(PLAYER_RESPONSE, requests)
        val logEvents = mutableListOf<InnerTubeLogEvent>()
        val logger = InnerTubeLogger { logEvents += it }
        val innerTube = InnerTube(client, logger = logger)
        val parser = RecordingConfigParser()
        try {
            val stream =
                try {
                    extractor(innerTube, parser, logger).extract(
                        videoId = VIDEO_ID,
                        hints = ContentHints(),
                        clientPlaybackNonce = CLIENT_NONCE,
                    )
                } catch (error: Exception) {
                    throw AssertionError(logEvents.joinToString("\n") { "${it.tag}: ${it.message} ${it.details}" }, error)
                }

            assertEquals(VIDEO_ID, stream?.videoId)
            assertEquals("https://rr1.googlevideo.com/videoplayback?expire=2000000000&itag=140&cpn=$CLIENT_NONCE", stream?.audioUrl)
            assertEquals(456789L, stream?.contentLengthBytes)
            assertEquals(140, stream?.itag)
            assertEquals("audio/mp4", stream?.mimeType)
            assertEquals("mp4a.40.2", stream?.codecs)
            assertEquals(128000, stream?.bitrate)
            assertEquals(44100, stream?.sampleRate)
            assertEquals("TEST_PLAYER", stream?.clientName)
            assertEquals(Instant.fromEpochSeconds(2_000_000_000), stream?.expiresAt)
            assertEquals("Song title", stream?.mediaMetadata?.title)
            assertEquals("Artist name", stream?.mediaMetadata?.author)
            assertEquals("123", stream?.mediaMetadata?.durationSeconds?.toString())
            assertEquals(CLIENT_NONCE, stream?.playbackTracking?.clientPlaybackNonce)
            assertFalse(stream?.requireBoundedRange ?: true)
            assertNull(parser.watchConfigVideoId)
            assertEquals(1, requests.size)
            assertEquals("/youtubei/v1/player", java.net.URI(requests.single()).path)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun selectsAnAllowedHlsManifestForLiveContent() = runBlocking {
        val requests = mutableListOf<String>()
        val client = playerClient(HLS_RESPONSE, requests)
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        try {
            val stream =
                extractor(innerTube, parser, InnerTubeLogger.NONE).extract(
                    videoId = VIDEO_ID,
                    hints = ContentHints(isLive = true),
                    clientPlaybackNonce = CLIENT_NONCE,
                )

            assertEquals(HLS_URL, stream?.audioUrl)
            assertEquals(HLS_URL, stream?.videoUrl)
            assertEquals("application/x-mpegURL", stream?.mimeType)
            assertEquals(96, stream?.itag)
            assertEquals("TEST_PLAYER", stream?.clientName)
            assertFalse(stream?.requireBoundedRange ?: true)
            assertEquals(VIDEO_ID, parser.watchConfigVideoId)
            assertEquals(1, requests.size)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun mapsAValidSabrAudioResponseIntoAPlaybackStream() = runBlocking {
        val requests = mutableListOf<String>()
        val client = playerClient(SABR_RESPONSE, requests)
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        val sabrClient = TEST_CLIENT.resolveClients(ContentHints()).single().copy(clientName = "TEST_SABR", useSabr = true)
        val fallbackStrategy =
            object : ClientFallbackStrategy {
                override fun resolveClients(hints: ContentHints): List<YouTubeClient> = listOf(sabrClient)
            }
        try {
            val stream =
                extractor(innerTube, parser, InnerTubeLogger.NONE, fallbackStrategy = fallbackStrategy).extract(
                    videoId = VIDEO_ID,
                    hints = ContentHints(),
                    clientPlaybackNonce = CLIENT_NONCE,
                )

            assertEquals("sabr://$VIDEO_ID", stream?.audioUrl)
            assertEquals(140, stream?.itag)
            assertEquals("audio/mp4", stream?.mimeType)
            assertEquals("TEST_SABR", stream?.clientName)
            assertEquals(
                "https://rr1.googlevideo.com/videoplayback?expire=2000000000&cpn=$CLIENT_NONCE",
                stream?.sabrBootstrap?.serverAbrStreamingUrl,
            )
            assertNull(parser.watchConfigVideoId)
            assertEquals(1, requests.size)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun skipsAnInvalidHlsManifestAndUsesTheNextAllowedClientManifest() = runBlocking {
        val requests = mutableListOf<String>()
        val invalidManifest = "https://attacker.test/manifest/hls_variant/live"
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString()
                    val response = if (requests.size == 1) invalidManifest else HLS_URL
                    respond(
                        ByteReadChannel(
                            CIPHER_RESPONSE.replace(
                                "\"streamingData\":{",
                                "\"streamingData\":{\"hlsManifestUrl\":\"$response\",",
                            ),
                        ),
                        HttpStatusCode.OK,
                        headers { append(HttpHeaders.ContentType, "application/json") },
                    )
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }) }
            }
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        val clients = TEST_CLIENT.resolveClients(ContentHints()) + TEST_CLIENT.resolveClients(ContentHints()).single().copy(clientName = "TEST_PLAYER_2", clientId = "100")
        val fallbackStrategy =
            object : ClientFallbackStrategy {
                override fun resolveClients(hints: ContentHints): List<YouTubeClient> = clients
            }
        try {
            val stream =
                extractor(innerTube, parser, InnerTubeLogger.NONE, fallbackStrategy = fallbackStrategy).extract(
                    videoId = VIDEO_ID,
                    hints = ContentHints(),
                    clientPlaybackNonce = CLIENT_NONCE,
                )

            assertEquals(HLS_URL, stream?.audioUrl)
            assertEquals("application/x-mpegURL", stream?.mimeType)
            assertNull(parser.watchConfigVideoId)
            assertEquals(2, requests.size)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun mapsAudioAfterCipherServiceResolvesItsEncryptedUrl() = runBlocking {
        val requests = mutableListOf<String>()
        val client = playerClient(CIPHER_RESPONSE, requests)
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        val cipher =
            RecordingCipherService { format ->
                format.copy(url = RESOLVED_AUDIO_URL, signatureCipher = null, cipher = null)
            }
        try {
            val stream =
                extractor(innerTube, parser, InnerTubeLogger.NONE, cipher).extract(
                    videoId = VIDEO_ID,
                    hints = ContentHints(),
                    clientPlaybackNonce = CLIENT_NONCE,
                )

            assertEquals("$RESOLVED_AUDIO_URL&cpn=$CLIENT_NONCE", stream?.audioUrl)
            assertEquals(140, stream?.itag)
            assertEquals("audio/mp4", stream?.mimeType)
            assertEquals(listOf(PLAYER_SCRIPT_URL), cipher.processedPlayerUrls)
            assertEquals(listOf(listOf(140)), cipher.processedItags)
            assertEquals(VIDEO_ID, parser.watchConfigVideoId)
            assertEquals(2, requests.size)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun reusesAPlayerConfigAfterItResolvesAPlayableStream() = runBlocking {
        val requests = mutableListOf<String>()
        val client = playerClient(CIPHER_RESPONSE, requests)
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        val cipher =
            RecordingCipherService { format ->
                format.copy(url = RESOLVED_AUDIO_URL, signatureCipher = null, cipher = null)
            }
        try {
            val extractor = extractor(innerTube, parser, InnerTubeLogger.NONE, cipher)
            val first = extractor.extract(VIDEO_ID, ContentHints(), clientPlaybackNonce = CLIENT_NONCE)
            val second = extractor.extract(VIDEO_ID, ContentHints(), clientPlaybackNonce = CLIENT_NONCE)

            assertEquals("$RESOLVED_AUDIO_URL&cpn=$CLIENT_NONCE", first?.audioUrl)
            assertEquals("$RESOLVED_AUDIO_URL&cpn=$CLIENT_NONCE", second?.audioUrl)
            assertEquals(1, parser.watchConfigFetches)
            assertEquals(4, requests.size)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun dropsCachedPlayerConfigWhenTheSessionChanges() = runBlocking {
        val requests = mutableListOf<String>()
        val client = playerClient(CIPHER_RESPONSE, requests)
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        val cipher =
            RecordingCipherService { format ->
                format.copy(url = RESOLVED_AUDIO_URL, signatureCipher = null, cipher = null)
            }
        try {
            val extractor = extractor(innerTube, parser, InnerTubeLogger.NONE, cipher)
            assertEquals(
                "$RESOLVED_AUDIO_URL&cpn=$CLIENT_NONCE",
                extractor.extract(VIDEO_ID, ContentHints(), clientPlaybackNonce = CLIENT_NONCE)?.audioUrl,
            )

            innerTube.cookie = "SID=session; SAPISID=credential"

            assertEquals(
                "$RESOLVED_AUDIO_URL&cpn=$CLIENT_NONCE",
                extractor.extract(VIDEO_ID, ContentHints(), clientPlaybackNonce = CLIENT_NONCE)?.audioUrl,
            )
            assertEquals(2, parser.watchConfigFetches)
            assertEquals(4, requests.size)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun retriesAllAudioFormatsWhenThePreferredCipherCannotBeResolved() = runBlocking {
        val requests = mutableListOf<String>()
        val client = playerClient(CIPHER_FALLBACK_RESPONSE, requests)
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        val cipher =
            RecordingCipherService { format ->
                if (format.itag == 140) {
                    format.copy(url = RESOLVED_AUDIO_URL, signatureCipher = null, cipher = null)
                } else {
                    format
                }
            }
        try {
            val stream =
                extractor(innerTube, parser, InnerTubeLogger.NONE, cipher).extract(
                    videoId = VIDEO_ID,
                    hints = ContentHints(),
                    clientPlaybackNonce = CLIENT_NONCE,
                )

            assertEquals("$RESOLVED_AUDIO_URL&cpn=$CLIENT_NONCE", stream?.audioUrl)
            assertEquals(140, stream?.itag)
            assertEquals(listOf(listOf(251), listOf(140, 251)), cipher.processedItags)
            assertEquals(2, requests.size)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun mapsVideoAndAudioAfterCipherResolution() = runBlocking {
        val requests = mutableListOf<String>()
        val client = playerClient(AUDIO_VIDEO_CIPHER_RESPONSE, requests)
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        val cipher =
            RecordingCipherService { format ->
                when (format.itag) {
                    140 -> format.copy(url = RESOLVED_AUDIO_URL, signatureCipher = null, cipher = null)
                    137 -> format.copy(url = RESOLVED_VIDEO_URL, signatureCipher = null, cipher = null)
                    else -> format
                }
            }
        try {
            val stream =
                extractor(innerTube, parser, InnerTubeLogger.NONE, cipher).extract(
                    videoId = VIDEO_ID,
                    hints = ContentHints(wantVideo = true),
                    clientPlaybackNonce = CLIENT_NONCE,
                )

            assertEquals("$RESOLVED_AUDIO_URL&cpn=$CLIENT_NONCE", stream?.audioUrl)
            assertEquals("$RESOLVED_VIDEO_URL&cpn=$CLIENT_NONCE", stream?.videoUrl)
            assertEquals(137, stream?.videoItag)
            assertEquals(1920, stream?.videoWidth)
            assertEquals(1080, stream?.videoHeight)
            assertEquals("video/mp4", stream?.videoMimeType)
            assertEquals("avc1.640028", stream?.videoCodecs)
            assertEquals(listOf(listOf(140, 137)), cipher.processedItags)
            assertEquals(listOf(1080), stream?.availableVideoHeights)
            assertEquals(VIDEO_ID, parser.watchConfigVideoId)
            assertEquals(1, requests.size)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    private fun extractor(
        innerTube: InnerTube,
        parser: RecordingConfigParser,
        logger: InnerTubeLogger,
        cipher: RecordingCipherService = RecordingCipherService(),
        fallbackStrategy: ClientFallbackStrategy = TEST_CLIENT,
    ) =
        InnerTubeExtractor(
            configParser = parser,
            clientDirector =
                PlayerClientDirector(
                    innerTube = innerTube,
                    fallbackStrategy = fallbackStrategy,
                    tokenProvider = UnavailableTokenProvider,
                    logger = logger,
                ),
            cipherService = cipher,
            innerTube = innerTube,
            logger = logger,
        )

    private fun playerClient(response: String, requests: MutableList<String>) =
        HttpClient(
            MockEngine { request ->
                requests += request.url.toString()
                respond(
                    ByteReadChannel(response),
                    HttpStatusCode.OK,
                    headers { append(HttpHeaders.ContentType, "application/json") },
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }) }
        }

    private class RecordingConfigParser : YtConfigParser {
        var watchConfigVideoId: String? = null
        var watchConfigFetches = 0

        override suspend fun fetchConfig(videoId: String, useLoginCookies: Boolean): PlayerConfig {
            watchConfigFetches++
            watchConfigVideoId = videoId
            return WATCH_CONFIG
        }
    }

    private class RecordingCipherService(
        private val transform: (Format) -> Format = { it },
    ) : ExtractionCipherService {
        val processedPlayerUrls = mutableListOf<String>()
        val processedItags = mutableListOf<List<Int>>()

        override suspend fun initialize() = Unit

        override suspend fun preloadPlayerCode(playerUrl: String) = Unit

        override suspend fun prewarmPlayerCipher() = Unit

        override suspend fun processFormats(playerUrl: String, formats: List<Format>): List<Format> {
            processedPlayerUrls += playerUrl
            processedItags += formats.map(Format::itag)
            return formats.map(transform)
        }
    }

    private companion object {
        const val VIDEO_ID = "abcdefghijk"
        const val CLIENT_NONCE = "ABCDEFGHIJKLMNOP"
        const val HLS_URL = "https://rr1.googlevideo.com/manifest/hls_variant/live"
        const val PLAYER_SCRIPT_URL = "https://www.youtube.com/s/player/watch/base.js"
        const val RESOLVED_AUDIO_URL = "https://rr1.googlevideo.com/videoplayback?expire=2000000000&itag=140"
        const val RESOLVED_VIDEO_URL = "https://rr1.googlevideo.com/videoplayback?expire=2000000000&itag=137"
        const val HLS_RESPONSE =
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"hlsManifestUrl":"$HLS_URL","adaptiveFormats":[]},"videoDetails":{"videoId":"abcdefghijk","isLiveContent":true}}"""
        const val SABR_RESPONSE =
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"serverAbrStreamingUrl":"https://rr1.googlevideo.com/videoplayback?expire=2000000000","adaptiveFormats":[{"itag":140,"mimeType":"audio/mp4; codecs=\"mp4a.40.2\"","bitrate":128000,"contentLength":456789,"approxDurationMs":"123000","audioSampleRate":44100,"audioChannels":2,"lastModified":"111"},{"itag":137,"mimeType":"video/mp4; codecs=\"avc1.640028\"","bitrate":400000,"width":640,"height":360,"lastModified":"222"}],"expiresInSeconds":3600},"videoDetails":{"videoId":"abcdefghijk","title":"Song title","author":"Artist name","lengthSeconds":"123","isLiveContent":false},"playerConfig":{"mediaCommonConfig":{"mediaUstreamerRequestConfig":{"videoPlaybackUstreamerConfig":"YWJj"}}}}"""
        val WATCH_CONFIG = PlayerConfig(PLAYER_SCRIPT_URL, 12345, null, null)
        const val PLAYER_RESPONSE =
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"formats":[{"itag":140,"url":"https://rr1.googlevideo.com/videoplayback?expire=2000000000&itag=140","mimeType":"audio/mp4; codecs=\"mp4a.40.2\"","bitrate":128000,"contentLength":456789,"audioSampleRate":44100,"audioChannels":2}],"adaptiveFormats":[]},"videoDetails":{"videoId":"abcdefghijk","title":"Song title","author":"Artist name","channelId":"channel-1","lengthSeconds":"123","isLiveContent":false},"playbackTracking":{"videostatsPlaybackUrl":{"baseUrl":"https://youtube.com/ptracking?cpn=old"}}}"""
        const val CIPHER_RESPONSE =
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"formats":[{"itag":140,"signatureCipher":"url=https%3A%2F%2Frr1.googlevideo.com%2Fvideoplayback%3Fexpire%3D2000000000%26itag%3D140&sig=signature","mimeType":"audio/mp4; codecs=\"mp4a.40.2\"","bitrate":128000,"contentLength":456789,"audioSampleRate":44100,"audioChannels":2}],"adaptiveFormats":[]},"videoDetails":{"videoId":"abcdefghijk","title":"Song title","author":"Artist name","channelId":"channel-1","lengthSeconds":"123","isLiveContent":false}}"""
        const val CIPHER_FALLBACK_RESPONSE =
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"formats":[{"itag":140,"signatureCipher":"url=https%3A%2F%2Frr1.googlevideo.com%2Fvideoplayback%3Fexpire%3D2000000000%26itag%3D140&sig=signature","mimeType":"audio/mp4; codecs=\"mp4a.40.2\"","bitrate":128000,"contentLength":456789,"audioSampleRate":44100,"audioChannels":2},{"itag":251,"signatureCipher":"url=https%3A%2F%2Frr1.googlevideo.com%2Fvideoplayback%3Fexpire%3D2000000000%26itag%3D251&sig=signature","mimeType":"audio/webm; codecs=\"opus\"","bitrate":256000,"contentLength":456789,"audioSampleRate":48000,"audioChannels":2}],"adaptiveFormats":[]},"videoDetails":{"videoId":"abcdefghijk","title":"Song title","author":"Artist name","channelId":"channel-1","lengthSeconds":"123","isLiveContent":false}}"""
        const val AUDIO_VIDEO_CIPHER_RESPONSE =
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"formats":[],"adaptiveFormats":[{"itag":140,"signatureCipher":"url=https%3A%2F%2Frr1.googlevideo.com%2Fvideoplayback%3Fexpire%3D2000000000%26itag%3D140&sig=signature","mimeType":"audio/mp4; codecs=\"mp4a.40.2\"","bitrate":128000,"contentLength":456789,"audioSampleRate":44100,"audioChannels":2},{"itag":137,"signatureCipher":"url=https%3A%2F%2Frr1.googlevideo.com%2Fvideoplayback%3Fexpire%3D2000000000%26itag%3D137&sig=signature","mimeType":"video/mp4; codecs=\"avc1.640028\"","bitrate":4000000,"width":1920,"height":1080,"contentLength":1234567}],"expiresInSeconds":3600},"videoDetails":{"videoId":"abcdefghijk","title":"Song title","author":"Artist name","channelId":"channel-1","lengthSeconds":"123","isLiveContent":false}}"""
        val TEST_CLIENT =
            object : ClientFallbackStrategy {
                private val client =
                    YouTubeClient(
                        clientName = "TEST_PLAYER",
                        clientVersion = "1.0",
                        clientId = "99",
                        userAgent = "VyonEx test client",
                    )

                override fun resolveClients(hints: ContentHints): List<YouTubeClient> = listOf(client)
            }
    }
}
