// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BandcampClientTest {
    @Test
    fun fetchesTrackMetadataOnlyForHttpsBandcampTrackPagesWithoutFollowingRedirects() = runTest {
        var requestUrl: String? = null
        val client = HttpClient(MockEngine { request ->
            requestUrl = request.url.toString()
            respond(
                ByteReadChannel("""{"url":"https://artist.bandcamp.com/track/song","artist":"Artist","trackinfo":[{"file":{"mp3-128":"https://audio.example/song.mp3"},"duration":42}],"current":{"title":"Song"}}"""),
                HttpStatusCode.OK,
            )
        })
        try {
            val track = BandcampClient(client).track("http://artist.bandcamp.com/track/song")
            assertEquals("https://artist.bandcamp.com/track/song", requestUrl)
            assertEquals("Song", track.title)
            assertEquals("Artist", track.artist)
            assertEquals("https://audio.example/song.mp3", track.audioUrl)
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsUntrustedBandcampTrackUrlsBeforeNetworkRequest() = runTest {
        var requestSeen = false
        val client = HttpClient(MockEngine {
            requestSeen = true
            respond(ByteReadChannel("{}"), HttpStatusCode.OK)
        })
        try {
            assertFailsWith<IllegalArgumentException> {
                BandcampClient(client).track("https://bandcamp.com.evil.example/track/song")
            }
            assertFailsWith<IllegalArgumentException> {
                BandcampClient(client).track("https://artist.bandcamp.com/album/song")
            }
            assertEquals(false, requestSeen)
        } finally {
            client.close()
        }
    }

    @Test
    fun redirectsAreRejectedAndPaidTracksRemainUnavailable() = runTest {
        var redirectRequests = 0
        val redirectClient = HttpClient(MockEngine {
            redirectRequests++
            respond(ByteReadChannel(""), HttpStatusCode.Found, headers = io.ktor.http.headersOf("Location", "https://evil.example/"))
        })
        try {
            assertFailsWith<IllegalStateException> { BandcampClient(redirectClient).track("https://artist.bandcamp.com/track/song") }
            assertEquals(1, redirectRequests)
        } finally {
            redirectClient.close()
        }

        val paidClient = HttpClient(MockEngine {
            respond(ByteReadChannel("""{"url":"https://artist.bandcamp.com/track/song","trackinfo":[{"file":null}],"current":{"title":"Song"}}"""), HttpStatusCode.OK)
        })
        try {
            assertFailsWith<IllegalStateException> { BandcampClient(paidClient).track("https://artist.bandcamp.com/track/song") }
        } finally {
            paidClient.close()
        }
    }

    @Test
    fun malformedTrackPageAndHttpFailureAreReported() = runTest {
        val malformedClient = HttpClient(MockEngine {
            respond(ByteReadChannel("not-json"), HttpStatusCode.OK)
        })
        try {
            assertFailsWith<IllegalArgumentException> {
                BandcampClient(malformedClient).track("https://artist.bandcamp.com/track/song")
            }
        } finally {
            malformedClient.close()
        }

        val failedClient = HttpClient(MockEngine {
            respond(ByteReadChannel("blocked"), HttpStatusCode.ServiceUnavailable)
        })
        try {
            val error = assertFailsWith<IllegalStateException> {
                BandcampClient(failedClient).track("https://artist.bandcamp.com/track/song")
            }
            assertEquals("Bandcamp track page failed with HTTP 503", error.message)
        } finally {
            failedClient.close()
        }
    }

    @Test
    fun rejectsNonHttpsAndCredentialBearingAudioUrls() = runTest {
        val client = HttpClient(MockEngine { request ->
            val audioUrl = if (request.url.encodedPath.endsWith("file")) "file:///private/song.mp3" else "https://user:pass@audio.example/song.mp3"
            respond(
                ByteReadChannel("""{"trackinfo":[{"file":{"mp3-128":"$audioUrl"}}]}"""),
                HttpStatusCode.OK,
            )
        })
        try {
            assertFailsWith<IllegalStateException> {
                BandcampClient(client).track("https://artist.bandcamp.com/track/file")
            }
            assertFailsWith<IllegalStateException> {
                BandcampClient(client).track("https://artist.bandcamp.com/track/credentials")
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun postsAutocompleteRequestAndParsesSuggestions() = runTest {
        var requestSeen = false
        val client = HttpClient(MockEngine { request ->
            requestSeen = true
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(BandcampClient.AUTOCOMPLETE_URL, request.url.toString())
            val outgoingBody = request.body as TextContent
            assertEquals(ContentType.Application.Json, outgoingBody.contentType)
            val body = outgoingBody.text
            assertEquals(
                Json.parseToJsonElement("""{"fan_id":null,"full_page":false,"search_filter":"","search_text":"ambient & jazz"}"""),
                Json.parseToJsonElement(body),
            )
            respond(
                ByteReadChannel("""{"auto":{"results":[{"name":"Ambient"},{"name":"Ambient"},{"name":"Jazz"}]}}"""),
                HttpStatusCode.OK,
            )
        })
        try {
            assertEquals(listOf("Ambient", "Jazz"), BandcampClient(client).suggestions("ambient & jazz"))
            assertTrue(requestSeen)
        } finally {
            client.close()
        }
    }

    @Test
    fun malformedSuccessBodyReturnsNoSuggestions() = runTest {
        val client = HttpClient(MockEngine {
            respond(ByteReadChannel("not-json"), HttpStatusCode.OK)
        })
        try {
            assertEquals(emptyList(), BandcampClient(client).suggestions("ambient"))
        } finally {
            client.close()
        }
    }

    @Test
    fun nonSuccessStatusFailsInsteadOfLookingLikeNoSuggestions() = runTest {
        val client = HttpClient(MockEngine {
            respond(ByteReadChannel("blocked"), HttpStatusCode.ServiceUnavailable)
        })
        try {
            val error = assertFailsWith<IllegalStateException> {
                BandcampClient(client).suggestions("ambient")
            }
            assertEquals("Bandcamp autocomplete failed with HTTP 503", error.message)
        } finally {
            client.close()
        }
    }
}
