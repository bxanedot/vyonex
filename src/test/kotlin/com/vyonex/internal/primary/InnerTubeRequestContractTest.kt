// bxane
package com.vyonex.internal.primary

import com.vyonex.internal.primary.models.YouTubeClient
import com.vyonex.internal.primary.models.YouTubeLocale
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InnerTubeRequestContractTest {
    @Test
    fun nativePlaybackUsesPublicEndpointAndDoesNotSendAccountCookies() = runBlocking {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val client = mockClient { requests += it }
        val innerTube = InnerTube(client).apply {
            locale = YouTubeLocale(gl = "CH", hl = "fr")
            cookie = "SID=private; SAPISID=private"
        }
        try {
            innerTube.player(YouTubeClient.ANDROID, VIDEO_ID)

            val request = requests.single()
            assertEquals("www.youtube.com", request.url.host)
            assertEquals("/youtubei/v1/player", request.url.encodedPath)
            assertEquals("false", request.url.parameters["prettyPrint"])
            assertEquals("3", request.headers["X-YouTube-Client-Name"])
            assertEquals("com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip", request.headers[HttpHeaders.UserAgent])
            assertEquals("fr-CH,fr;q=0.9", request.headers[HttpHeaders.AcceptLanguage])
            assertNull(request.headers[HttpHeaders.Cookie])
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun musicPlaybackUsesMusicEndpointAndOptedInAccountHeaders() = runBlocking {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val client = mockClient { requests += it }
        val innerTube = InnerTube(client).apply {
            locale = YouTubeLocale(gl = "CH", hl = "fr")
            cookie = "SID=session; SAPISID=credential"
        }
        try {
            innerTube.player(YouTubeClient.WEB_REMIX, VIDEO_ID)

            val request = requests.single()
            assertEquals("music.youtube.com", request.url.host)
            assertEquals("SID=session; SAPISID=credential; PREF=hl=fr&gl=CH", request.headers[HttpHeaders.Cookie])
            assertEquals("0", request.headers["X-Goog-AuthUser"])
            assertTrue(request.headers[HttpHeaders.Authorization].orEmpty().startsWith("SAPISIDHASH "))
        } finally {
            innerTube.close()
            client.close()
        }
    }

    private fun mockClient(capture: (io.ktor.client.request.HttpRequestData) -> Unit) =
        HttpClient(
            MockEngine { request ->
                capture(request)
                respond(
                    ByteReadChannel("{}"),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }) }
        }

    private companion object {
        const val VIDEO_ID = "abcdefghijk"
    }
}
