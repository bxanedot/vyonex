// bxane
package com.vyonex.internal.primary

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HttpResponseBodyContractTest {
    @Test
    fun readsBodyUpToTheLimit() = runBlocking {
        val client = HttpClient(MockEngine { respond(ByteReadChannel("body"), HttpStatusCode.OK) })
        try {
            val response = client.get(TEST_URL)
            assertEquals("body", response.bodyAsTextLimited(4))
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsAnUnknownLengthBodyWhenStreamingPastTheLimit() = runBlocking {
        val client = HttpClient(MockEngine { respond(ByteReadChannel("too long"), HttpStatusCode.OK) })
        try {
            val response = client.get(TEST_URL)
            assertNull(response.contentLength())
            assertThrows(IllegalStateException::class.java) { runBlocking { response.bodyAsTextLimited(4) } }
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsAnOversizedDeclaredBody() = runBlocking {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        ByteReadChannel("12345"),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentLength, "5"),
                    )
                },
            )
        try {
            val response = client.get(TEST_URL)
            assertEquals(5, response.contentLength())
            assertThrows(IllegalStateException::class.java) { runBlocking { response.bodyAsTextLimited(4) } }
        } finally {
            client.close()
        }
    }

    @Test
    fun requiresAPositiveLimit() = runBlocking {
        val client = HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) })
        try {
            val response = client.get(TEST_URL)
            assertThrows(IllegalArgumentException::class.java) { runBlocking { response.bodyAsTextLimited(0) } }
        } finally {
            client.close()
        }
    }

    private companion object {
        const val TEST_URL = "https://example.test/"
    }
}
