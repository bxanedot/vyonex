// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.InnerTube
import com.vyonex.internal.primary.extraction.strategy.ClientFallbackStrategy
import com.vyonex.internal.primary.models.YouTubeClient
import com.vyonex.internal.primary.models.response.PlayerResponse.StreamingData.Format
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InnerTubeExtractorRoutingContractTest {
    @Test
    fun usesAuthenticatedWatchConfigFirstForExplicitContent() = runBlocking {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val innerTube = InnerTube(client).apply { cookie = "SID=session; SAPISID=credential" }
        val parser = RecordingConfigParser()
        try {
            val failure = failToExtract(innerTube, parser, ContentHints(isExplicit = true))

            assertEquals(StreamResolveException.Reason.EXPLICIT_UNSUPPORTED, failure.reason)
            assertEquals(listOf(true, false), parser.watchCookieModes)
            assertEquals(1, parser.embeddedFetches)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun triesSignedOutThenAuthenticatedWatchConfigForOrdinaryContent() = runBlocking {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val innerTube = InnerTube(client).apply { cookie = "SID=session; SAPISID=credential" }
        val parser = RecordingConfigParser()
        try {
            val failure = failToExtract(innerTube, parser, ContentHints())

            assertEquals(StreamResolveException.Reason.NO_PLAYABLE_STREAM, failure.reason)
            assertEquals(listOf(false, true), parser.watchCookieModes)
            assertEquals(1, parser.embeddedFetches)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun embeddedClientOverrideUsesOnlyEmbeddedConfiguration() = runBlocking {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        try {
            val failure =
                failToExtract(
                    innerTube,
                    parser,
                    ContentHints(playbackClientOverrideId = "WEB_EMBEDDED_PLAYER"),
                )

            assertEquals(StreamResolveException.Reason.NO_PLAYABLE_STREAM, failure.reason)
            assertEquals(emptyList<Boolean>(), parser.watchCookieModes)
            assertEquals(1, parser.embeddedFetches)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun uploadedContentDoesNotUseTheEmbeddedFallback() = runBlocking {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser()
        try {
            val failure = failToExtract(innerTube, parser, ContentHints(isUploaded = true))

            assertEquals(StreamResolveException.Reason.NO_PLAYABLE_STREAM, failure.reason)
            assertEquals(listOf(false), parser.watchCookieModes)
            assertEquals(0, parser.embeddedFetches)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun rejectsEmbeddedConfigWithoutEncryptedHostFlags() = runBlocking {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser().apply {
            embeddedConfig = EMBEDDED_CONFIG.copy(encryptedHostFlags = null)
        }
        try {
            val failure =
                failToExtract(
                    innerTube,
                    parser,
                    ContentHints(playbackClientOverrideId = "WEB_EMBEDDED_PLAYER"),
                )

            assertEquals(StreamResolveException.Reason.NO_PLAYABLE_STREAM, failure.reason)
            assertEquals(emptyList<Boolean>(), parser.watchCookieModes)
            assertEquals(1, parser.embeddedFetches)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun reportsEmbeddedConfigFetchFailureAsNetworkFailure() = runBlocking {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser().apply {
            embeddedFailure = IllegalStateException("offline config parser")
        }
        try {
            val failure =
                failToExtract(
                    innerTube,
                    parser,
                    ContentHints(playbackClientOverrideId = "WEB_EMBEDDED_PLAYER"),
                )

            assertEquals(StreamResolveException.Reason.NETWORK, failure.reason)
            assertEquals(emptyList<Boolean>(), parser.watchCookieModes)
            assertEquals(1, parser.embeddedFetches)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    private suspend fun failToExtract(
        innerTube: InnerTube,
        parser: RecordingConfigParser,
        hints: ContentHints,
    ): StreamResolveException =
        try {
            extractor(innerTube, parser).extract("abcdefghijk", hints)
            error("Expected extraction to fail without playback clients")
        } catch (failure: StreamResolveException) {
            failure
        }

    private fun extractor(innerTube: InnerTube, parser: RecordingConfigParser) =
        InnerTubeExtractor(
            configParser = parser,
            clientDirector =
                PlayerClientDirector(
                    innerTube = innerTube,
                    fallbackStrategy = NO_PLAYBACK_CLIENTS,
                    tokenProvider = UnavailableTokenProvider,
                ),
            cipherService = RecordingCipherService(),
            innerTube = innerTube,
        )

    private class RecordingConfigParser : YtConfigParser {
        val watchCookieModes = mutableListOf<Boolean>()
        var embeddedFetches = 0
        var embeddedConfig = EMBEDDED_CONFIG
        var embeddedFailure: Exception? = null

        override suspend fun fetchConfig(videoId: String, useLoginCookies: Boolean): PlayerConfig {
            watchCookieModes += useLoginCookies
            return WATCH_CONFIG
        }

        override suspend fun fetchEmbeddedConfig(videoId: String, useLoginCookies: Boolean): PlayerConfig {
            embeddedFetches++
            embeddedFailure?.let { throw it }
            return embeddedConfig
        }
    }

    private class RecordingCipherService : ExtractionCipherService {
        override suspend fun initialize() = Unit

        override suspend fun preloadPlayerCode(playerUrl: String) = Unit

        override suspend fun prewarmPlayerCipher() = Unit

        override suspend fun processFormats(playerUrl: String, formats: List<Format>): List<Format> = formats
    }

    private companion object {
        val NO_PLAYBACK_CLIENTS =
            object : ClientFallbackStrategy {
                override fun resolveClients(hints: ContentHints): List<YouTubeClient> = emptyList()
            }
        val WATCH_CONFIG = PlayerConfig("https://www.youtube.com/s/player/watch/base.js", 1, null, null)
        val EMBEDDED_CONFIG = PlayerConfig("https://www.youtube.com/s/player/embed/base.js", 2, null, null, "encrypted-flags")
    }
}
