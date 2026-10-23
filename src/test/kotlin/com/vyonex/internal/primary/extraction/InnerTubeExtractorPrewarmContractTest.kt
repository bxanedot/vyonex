// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.InnerTube
import com.vyonex.internal.primary.extraction.strategy.ContentAwareFallbackStrategy
import com.vyonex.internal.primary.extraction.strategy.PoTokenProviderKind
import com.vyonex.internal.primary.models.response.PlayerResponse.StreamingData.Format
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InnerTubeExtractorPrewarmContractTest {
    @Test
    fun cachesBothConfigVariantsAndWarmsTheAuthenticatedPlayer() = runBlocking {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val innerTube = InnerTube(client).apply { cookie = "SID=session; SAPISID=credential" }
        val parser =
            RecordingConfigParser { useLoginCookies ->
                if (useLoginCookies) AUTHENTICATED_CONFIG else DEFAULT_CONFIG
            }
        val cipher = RecordingCipherService()
        try {
            val extractor = extractor(parser, cipher, innerTube)

            extractor.prewarm()
            extractor.prewarm()

            assertEquals(listOf(false, true), parser.loginCookieModes)
            assertEquals(2, cipher.initializeCount)
            assertEquals(listOf(AUTHENTICATED_CONFIG.playerUrl, AUTHENTICATED_CONFIG.playerUrl), cipher.preloadedPlayerUrls)
            assertEquals(0, cipher.fallbackPrewarmCount)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun fallsBackToCipherWarmupWhenTheDefaultConfigCannotBeLoaded() = runBlocking {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val innerTube = InnerTube(client)
        val parser = RecordingConfigParser { error("config unavailable") }
        val cipher = RecordingCipherService()
        try {
            extractor(parser, cipher, innerTube).prewarm()

            assertEquals(listOf(false), parser.loginCookieModes)
            assertEquals(1, cipher.initializeCount)
            assertEquals(1, cipher.fallbackPrewarmCount)
            assertEquals(emptyList<String>(), cipher.preloadedPlayerUrls)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun keepsTheDefaultConfigWhenAuthenticatedPrewarmFailsAndWarmsAnAvailableTokenProvider() = runBlocking {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val innerTube =
            InnerTube(client).apply {
                cookie = "SID=session; SAPISID=credential"
                visitorData = "session-visitor"
            }
        val parser =
            RecordingConfigParser { useLoginCookies ->
                if (useLoginCookies) error("authenticated config unavailable") else DEFAULT_CONFIG.copy(visitorData = "config-visitor")
            }
        val cipher = RecordingCipherService()
        val tokenProvider = RecordingTokenProvider()
        try {
            extractor(parser, cipher, innerTube, tokenProvider).prewarm()

            assertEquals(listOf(false, true), parser.loginCookieModes)
            assertEquals(listOf(DEFAULT_CONFIG.playerUrl), cipher.preloadedPlayerUrls)
            assertEquals(listOf("dQw4w9WgXcQ" to "session-visitor"), tokenProvider.requests)
            assertEquals(0, cipher.fallbackPrewarmCount)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    private fun extractor(
        parser: RecordingConfigParser,
        cipher: RecordingCipherService,
        innerTube: InnerTube,
        tokenProvider: TokenProvider = UnavailableTokenProvider,
    ) =
        InnerTubeExtractor(
            configParser = parser,
            clientDirector =
                PlayerClientDirector(
                    innerTube = innerTube,
                    fallbackStrategy = ContentAwareFallbackStrategy(),
                    tokenProvider = tokenProvider,
                ),
            cipherService = cipher,
            innerTube = innerTube,
            tokenProvider = tokenProvider,
        )

    private class RecordingConfigParser(
        private val configFor: (Boolean) -> PlayerConfig,
    ) : YtConfigParser {
        val loginCookieModes = mutableListOf<Boolean>()

        override suspend fun fetchConfig(videoId: String, useLoginCookies: Boolean): PlayerConfig {
            loginCookieModes += useLoginCookies
            return configFor(useLoginCookies)
        }
    }

    private class RecordingCipherService : ExtractionCipherService {
        var initializeCount = 0
        var fallbackPrewarmCount = 0
        val preloadedPlayerUrls = mutableListOf<String>()

        override suspend fun initialize() {
            initializeCount++
        }

        override suspend fun preloadPlayerCode(playerUrl: String) {
            preloadedPlayerUrls += playerUrl
        }

        override suspend fun prewarmPlayerCipher() {
            fallbackPrewarmCount++
        }

        override suspend fun processFormats(playerUrl: String, formats: List<Format>): List<Format> = formats
    }

    private class RecordingTokenProvider : TokenProvider {
        override val capabilities = TokenProviderCapabilities(setOf(PoTokenProviderKind.EXTERNAL))
        val requests = mutableListOf<Pair<String, String>>()

        override suspend fun getPoToken(videoId: String, visitorData: String, cookie: String?): PoTokenResult? {
            requests += videoId to visitorData
            return null
        }
    }

    private companion object {
        val DEFAULT_CONFIG = PlayerConfig("https://www.youtube.com/s/player/default/base.js", 1, null, null)
        val AUTHENTICATED_CONFIG = PlayerConfig("https://www.youtube.com/s/player/auth/base.js", 2, null, null)
    }
}
