// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.InnerTube
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YtConfigParserContractTest {
    @Test
    fun readsPlayerConfigurationFromTheWatchPage() = runTest {
        val requests = mutableListOf<Pair<String, String?>>()
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString() to request.headers[HttpHeaders.Cookie]
                    respond(
                        ByteReadChannel(WATCH_PAGE),
                        HttpStatusCode.OK,
                        headers { append(HttpHeaders.ContentType, "text/html") },
                    )
                },
            )
        try {
            val config = YtConfigParserImpl(client, InnerTube(client)).fetchConfig(VIDEO_ID)

            assertEquals(PLAYER_URL, config.playerUrl)
            assertEquals(12345, config.signatureTimestamp)
            assertEquals("visitor-token", config.visitorData)
            assertEquals("2.20261001.00.00", config.clientVersion)
            assertNull(config.encryptedHostFlags)
            assertEquals(listOf<Pair<String, String?>>("https://www.youtube.com/watch?v=$VIDEO_ID&bpctr=9999999999&has_verified=1" to null), requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun readsTheWatchPagePlayerResponseForFallbackExtraction() = runTest {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        ByteReadChannel(WATCH_PAGE_WITH_PLAYER_RESPONSE),
                        HttpStatusCode.OK,
                        headers { append(HttpHeaders.ContentType, "text/html") },
                    )
                },
            )
        try {
            val page = YtConfigParserImpl(client, InnerTube(client)).fetchWatchPage(VIDEO_ID, useLoginCookies = false)

            assertEquals(PLAYER_URL, page?.playerUrl)
            assertEquals(VIDEO_ID, page?.playerResponse?.videoDetails?.videoId)
            assertEquals(
                "https://media.example/audio",
                page?.playerResponse?.streamingData?.adaptiveFormats?.single()?.url,
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun retriesAnAuthenticatedEmbedPageWithoutCookiesAndKeepsItsReferer() = runTest {
        val requests = mutableListOf<Pair<String?, String?>>()
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.headers[HttpHeaders.Cookie] to request.headers[HttpHeaders.Referrer]
                    if (requests.size == 1) {
                        respond(ByteReadChannel(""), HttpStatusCode.Forbidden)
                    } else {
                        respond(
                            ByteReadChannel(WATCH_PAGE),
                            HttpStatusCode.OK,
                            headers { append(HttpHeaders.ContentType, "text/html") },
                        )
                    }
                },
            )
        val innerTube = InnerTube(client).apply { cookie = "SID=session; PREF=language; SAPISID=token" }
        try {
            val config = YtConfigParserImpl(client, innerTube).fetchEmbeddedConfig(VIDEO_ID, useLoginCookies = true)

            assertEquals("secret-flag", config.encryptedHostFlags)
            assertEquals(
                listOf<Pair<String?, String?>>(
                    "SID=session; SAPISID=token" to "https://www.reddit.com/",
                    null to "https://www.reddit.com/",
                ),
                requests,
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun onlyAcceptsPlayerScriptsOnTheValidatedYouTubePath() {
        val client = HttpClient(MockEngine { error("No request expected") })
        try {
            val parser = YtConfigParserImpl(client, InnerTube(client))

            assertEquals(PLAYER_URL, parser.extractPlayerUrl("""{"PLAYER_JS_URL":"/s/player/abc123/player_ias.vflset/en_US/base.js"}"""))
            assertNull(parser.extractPlayerUrl("""{"PLAYER_JS_URL":"https://youtube.com.attacker.test/s/player/abc123/base.js"}"""))
            assertNull(parser.extractPlayerUrl("""{"PLAYER_JS_URL":"https://www.youtube.com/watch?v=abc123"}"""))
        } finally {
            client.close()
        }
    }

    @Test
    fun extractsSignatureTimestampFromPageAndPlayerScriptForms() {
        val client = HttpClient(MockEngine { error("No request expected") })
        try {
            val parser = YtConfigParserImpl(client, InnerTube(client))

            assertEquals(98765, parser.extractSignatureTimestamp("""{"signatureTimestamp":98765}"""))
            assertEquals(12345, parser.extractSignatureTimestamp("""var player={"STS":12345};"""))
            assertNull(parser.extractSignatureTimestamp("""{"signatureTimestamp":1234}"""))
        } finally {
            client.close()
        }
    }

    private companion object {
        const val VIDEO_ID = "abcdefghijk"
        const val PLAYER_URL = "https://www.youtube.com/s/player/abc123/player_ias.vflset/en_US/base.js"
        const val WATCH_PAGE =
            """<script>{"PLAYER_JS_URL":"/s/player/abc123/player_ias.vflset/en_US/base.js","signatureTimestamp":12345,"visitorData":"visitor-token","INNERTUBE_CLIENT_VERSION":"2.20261001.00.00","encryptedHostFlags":"secret-flag"}</script>"""
        const val WATCH_PAGE_WITH_PLAYER_RESPONSE =
            """<script>{"PLAYER_JS_URL":"/s/player/abc123/player_ias.vflset/en_US/base.js"}</script><script>ytInitialPlayerResponse = {"playabilityStatus":{"status":"OK"},"videoDetails":{"videoId":"abcdefghijk"},"streamingData":{"adaptiveFormats":[{"itag":140,"mimeType":"audio/mp4","bitrate":128000,"url":"https://media.example/audio"}]}};</script>"""
    }
}
