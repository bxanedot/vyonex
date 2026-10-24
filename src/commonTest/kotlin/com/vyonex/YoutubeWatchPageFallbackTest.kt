// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YoutubeWatchPageFallbackTest {
    @Test
    fun mapsAnOfflineWatchPageToTheRequestedMp4AudioStream() = runTest {
        val client =
            HttpClient(
                MockEngine { request ->
                    assertEquals("/watch", request.url.encodedPath)
                    respond(
                        ByteReadChannel(WATCH_PAGE),
                        HttpStatusCode.OK,
                        headers { append(HttpHeaders.ContentType, "text/html") },
                    )
                },
            )
        try {
            val backend =
                PrimaryExtractorBackend(
                    host = TestHost(),
                    platform =
                        PlatformExtractorDependencies(
                            httpClient = client,
                            backgroundDispatcher = Dispatchers.Unconfined,
                            tokenProvider = null,
                            readPlayer = { null },
                            writePlayer = { _, _ -> },
                        ),
                )

            val stream =
                backend.extractWatchPageFallback(
                    videoId = "abcdefghijk",
                    maxKbps = 256,
                    requireM4a = true,
                )

            assertNotNull(stream)
            assertEquals("https://r1.googlevideo.com/videoplayback?itag=140", stream.url)
            assertEquals(128, stream.kbps)
            assertEquals("audio/mp4; codecs=\"mp4a.40.2\"", stream.mimeType)
            assertEquals("WEB_WATCH_PAGE", stream.profileId)
            assertEquals("https://www.youtube.com/", stream.headers["Referer"])
        } finally {
            client.close()
        }
    }

    @Test
    fun usesTheWatchPageAfterPrimaryPlaybackFails() = runTest {
        var watchRequests = 0
        val client =
            HttpClient(
                MockEngine { request ->
                    if (request.url.encodedPath == "/watch") {
                        watchRequests++
                        if (watchRequests > 1) {
                            respond(
                                ByteReadChannel(WATCH_PAGE),
                                HttpStatusCode.OK,
                                headers { append(HttpHeaders.ContentType, "text/html") },
                            )
                        } else {
                            respond(ByteReadChannel(""), HttpStatusCode.Forbidden)
                        }
                    } else {
                        respond(ByteReadChannel("{}"), HttpStatusCode.Forbidden)
                    }
                },
            )
        try {
            val backend =
                PrimaryExtractorBackend(
                    host = TestHost(),
                    platform =
                        PlatformExtractorDependencies(
                            httpClient = client,
                            backgroundDispatcher = Dispatchers.Unconfined,
                            tokenProvider = null,
                            readPlayer = { null },
                            writePlayer = { _, _ -> },
                        ),
                )

            val stream = backend.extractWithWatchPageFallback("abcdefghijk", maxKbps = 256, requireM4a = true)

            assertNotNull(stream)
            assertEquals("https://r1.googlevideo.com/videoplayback?itag=140", stream.url)
            assertTrue(watchRequests >= 2)
        } finally {
            client.close()
        }
    }

    @Test
    fun honorsTheWebClientSkipBeforeRequestingTheWatchPage() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine {
            requests++
            respond(ByteReadChannel(""), HttpStatusCode.Forbidden)
        })
        try {
            val backend =
                PrimaryExtractorBackend(
                    host = TestHost(),
                    platform =
                        PlatformExtractorDependencies(
                            httpClient = client,
                            backgroundDispatcher = Dispatchers.Unconfined,
                            tokenProvider = null,
                            readPlayer = { null },
                            writePlayer = { _, _ -> },
                        ),
                )

            assertNull(backend.extractWatchPageFallback("abcdefghijk", maxKbps = 256, skipClients = setOf("WEB")))
            assertEquals(0, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun usesAuthenticatedCookiesWhenPersistenceIsDisabled() = runTest {
        val cookie = "SID=session; SAPISID=auth-token"
        var requestCookie: String? = null
        val client =
            HttpClient(
                MockEngine { request ->
                    requestCookie = request.headers[HttpHeaders.Cookie]
                    respond(
                        ByteReadChannel(WATCH_PAGE),
                        HttpStatusCode.OK,
                        headers { append(HttpHeaders.ContentType, "text/html") },
                    )
                },
            )
        try {
            val backend =
                PrimaryExtractorBackend(
                    host = TestHost(cookie),
                    platform =
                        PlatformExtractorDependencies(
                            httpClient = client,
                            backgroundDispatcher = Dispatchers.Unconfined,
                            tokenProvider = null,
                            readPlayer = { null },
                            writePlayer = { _, _ -> },
                        ),
                )

            val stream = backend.extractWatchPageFallback("abcdefghijk", maxKbps = 256, requireM4a = true)

            assertNotNull(stream)
            assertEquals(cookie, requestCookie)
            assertEquals(cookie, stream.headers[HttpHeaders.Cookie])
        } finally {
            client.close()
        }
    }

    private class TestHost(private val cookie: String? = null) : VyonExHostServices {
        override val configStore =
            object : VyonExConfigStore {
                override val enabled = false
                override fun getString(key: String): String? = null
                override fun getLong(key: String): Long? = null
                override fun putString(key: String, value: String) = Unit
                override fun putLong(key: String, value: Long) = Unit
            }

        override suspend fun session() = VyonExSession(cookie, null, "US", "en")
        override fun nowMs() = 0L
        override fun log(level: VyonExLogLevel, message: String, trackId: String?) = Unit
    }

    private companion object {
        const val WATCH_PAGE =
            """<script>{"PLAYER_JS_URL":"/s/player/abc123/player_ias.vflset/en_US/base.js"}</script><script>ytInitialPlayerResponse = {"playabilityStatus":{"status":"OK"},"videoDetails":{"videoId":"abcdefghijk"},"streamingData":{"adaptiveFormats":[{"itag":251,"mimeType":"audio/webm; codecs=\"opus\"","bitrate":192000,"url":"https://r1.googlevideo.com/videoplayback?itag=251"},{"itag":140,"mimeType":"audio/mp4; codecs=\"mp4a.40.2\"","bitrate":128000,"url":"https://r1.googlevideo.com/videoplayback?itag=140"}]}};</script>"""
    }
}
