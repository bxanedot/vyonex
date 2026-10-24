// bxane
package com.vyonex.internal.primary

import com.vyonex.internal.primary.models.YouTubeClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class InnerTubeRetryContractTest {
    @Test
    fun retriesTransientPlayerFailuresWithBackoff() = runBlocking {
        var requests = 0
        val delays = mutableListOf<Long>()
        val client =
            HttpClient(
                MockEngine {
                    requests++
                    if (requests == 1) {
                        respond(ByteReadChannel(""), HttpStatusCode.ServiceUnavailable)
                    } else {
                        respond(
                            ByteReadChannel("{}"),
                            HttpStatusCode.OK,
                            headersOf("Content-Type", "application/json"),
                        )
                    }
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }) }
            }
        val innerTube = InnerTube(client, retryDelay = { delays += it.inWholeMilliseconds })
        try {
            innerTube.player(YouTubeClient.ANDROID, VIDEO_ID)

            assertEquals(2, requests)
            assertEquals(listOf(500L), delays)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun doesNotRetryNonTransientClientErrors() = runBlocking {
        var requests = 0
        val delays = mutableListOf<Long>()
        val client =
            HttpClient(
                MockEngine {
                    requests++
                    respond(ByteReadChannel("{}"), HttpStatusCode.BadRequest)
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }) }
            }
        val innerTube = InnerTube(client, retryDelay = { delays += it.inWholeMilliseconds })
        try {
            assertThrows(InnerTubeHttpException::class.java) {
                runBlocking { innerTube.player(YouTubeClient.ANDROID, VIDEO_ID) }
            }
            assertEquals(1, requests)
            assertEquals(emptyList<Long>(), delays)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    private companion object {
        const val VIDEO_ID = "abcdefghijk"
    }
}
