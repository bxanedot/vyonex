// bxane
package com.vyonex.internal.primary.cipher

import com.vyonex.internal.primary.models.response.PlayerResponse.StreamingData.Format
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RemotePlayerConfigStoreTest {
    @Test
    fun loadsTheDefaultRemotePlayerConfigFromItsAllowedSource() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { request ->
                    requestedUrls += request.url.toString()
                    respond(
                        content = ByteReadChannel(PLAYER_CONFIG_JSON),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            )
        try {
            val store = RemotePlayerConfigStore(client, TestPlayerConfigRepository())

            val config = store.getConfig(PLAYER_URL)

            assertEquals(12345, config?.signatureTimestamp)
            assertEquals(listOf(CONFIG_URL), requestedUrls)
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsPlayerConfigUrlsOutsideTheDefaultSource() {
        val client = HttpClient(MockEngine { error("No request expected") })
        try {
            val store = RemotePlayerConfigStore(client, TestPlayerConfigRepository())

            assertNull(store.validatedSourceUrlOrNull("https://raw.githubusercontent.com/attacker/repo/main/player_configs.json"))
        } finally {
            client.close()
        }
    }

    @Test
    fun usesThePlayerScriptSolverWhenTheRemoteConfigIsUnavailable() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { request ->
                    val url = request.url.toString()
                    requestedUrls += url
                    when (url) {
                        CONFIG_URL -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
                        PLAYER_URL ->
                            respond(
                                content = ByteReadChannel(PLAYER_SCRIPT),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/javascript"),
                            )

                        else -> error("Unexpected request: $url")
                    }
                },
            )
        val service = YouTubeCipherService(client, RemotePlayerConfigStore(client, TestPlayerConfigRepository()))
        try {
            val mediaUrl = "https://r.googlevideo.com/videoplayback?itag=140"
            val encodedMediaUrl = java.net.URLEncoder.encode(mediaUrl, Charsets.UTF_8)
            val formats =
                service.processFormats(
                    PLAYER_URL,
                    listOf(Format(itag = 140, mimeType = "audio/mp4", signatureCipher = "url=$encodedMediaUrl&s=abcd&sp=sig")),
                )

            assertEquals("$mediaUrl&sig=dcba", formats.single().url)
            assertNull(formats.single().signatureCipher)
            assertEquals(listOf(CONFIG_URL, PLAYER_URL), requestedUrls)
        } finally {
            service.dispose()
            client.close()
        }
    }

    private class TestPlayerConfigRepository : PlayerConfigRepository {
        override val enabled = true
        override val sourceUrl = CONFIG_URL
        override var cachedJson = ""
        override var cachedAtMs = 0L
        override var cachedSourceUrl = ""
        override var cachedEtag = ""
    }

    private companion object {
        const val CONFIG_URL =
            "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"
        const val PLAYER_URL = "https://www.youtube.com/s/player/1234abcd/player_ias.vflset/en_US/base.js"
        const val PLAYER_CONFIG_JSON =
            """{"schemaVersion":1,"players":{"1234abcd":{"sig":"x(1,2,INPUT)","nClass":"A","sts":12345}}}"""
        const val PLAYER_SCRIPT =
            """var a=function(a){a=a.split("");a.reverse();return a.join("")};var b=function(a){a=a.split("");a.reverse();return a.join("")};var broken = ;"""
    }
}
