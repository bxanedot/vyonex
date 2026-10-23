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

    private fun extractor(innerTube: InnerTube, parser: RecordingConfigParser, logger: InnerTubeLogger) =
        InnerTubeExtractor(
            configParser = parser,
            clientDirector =
                PlayerClientDirector(
                    innerTube = innerTube,
                    fallbackStrategy = TEST_CLIENT,
                    tokenProvider = UnavailableTokenProvider,
                    logger = logger,
                ),
            cipherService = RecordingCipherService(),
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

        override suspend fun fetchConfig(videoId: String, useLoginCookies: Boolean): PlayerConfig {
            watchConfigVideoId = videoId
            return WATCH_CONFIG
        }
    }

    private class RecordingCipherService : ExtractionCipherService {
        override suspend fun initialize() = Unit

        override suspend fun preloadPlayerCode(playerUrl: String) = Unit

        override suspend fun prewarmPlayerCipher() = Unit

        override suspend fun processFormats(playerUrl: String, formats: List<Format>): List<Format> = formats
    }

    private companion object {
        const val VIDEO_ID = "abcdefghijk"
        const val CLIENT_NONCE = "ABCDEFGHIJKLMNOP"
        const val HLS_URL = "https://rr1.googlevideo.com/manifest/hls_variant/live"
        const val HLS_RESPONSE =
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"hlsManifestUrl":"$HLS_URL","adaptiveFormats":[]},"videoDetails":{"videoId":"abcdefghijk","isLiveContent":true}}"""
        val WATCH_CONFIG = PlayerConfig("", null, null, null)
        const val PLAYER_RESPONSE =
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"formats":[{"itag":140,"url":"https://rr1.googlevideo.com/videoplayback?expire=2000000000&itag=140","mimeType":"audio/mp4; codecs=\"mp4a.40.2\"","bitrate":128000,"contentLength":456789,"audioSampleRate":44100,"audioChannels":2}],"adaptiveFormats":[]},"videoDetails":{"videoId":"abcdefghijk","title":"Song title","author":"Artist name","channelId":"channel-1","lengthSeconds":"123","isLiveContent":false},"playbackTracking":{"videostatsPlaybackUrl":{"baseUrl":"https://youtube.com/ptracking?cpn=old"}}}"""
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
