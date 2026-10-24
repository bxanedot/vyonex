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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HttpResponseBodyContractTest {
    @Test
    fun readsBodyUpToTheLimit() = runTest {
        val client = HttpClient(MockEngine { respond(ByteReadChannel("body"), HttpStatusCode.OK) })
        try {
            val response = client.get(TEST_URL)
            assertEquals("body", response.bodyAsTextLimited(4))
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsAnUnknownLengthBodyWhenStreamingPastTheLimit() = runTest {
        val client = HttpClient(MockEngine { respond(ByteReadChannel("too long"), HttpStatusCode.OK) })
        try {
            val response = client.get(TEST_URL)
            assertNull(response.contentLength())
            assertFailsWithSuspend<IllegalStateException> { response.bodyAsTextLimited(4) }
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsAnOversizedDeclaredBody() = runTest {
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
            assertFailsWithSuspend<IllegalStateException> { response.bodyAsTextLimited(4) }
        } finally {
            client.close()
        }
    }

    @Test
    fun requiresAPositiveLimit() = runTest {
        val client = HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) })
        try {
            val response = client.get(TEST_URL)
            assertFailsWithSuspend<IllegalArgumentException> { response.bodyAsTextLimited(0) }
        } finally {
            client.close()
        }
    }

    @Test
    fun measuresTheLimitInUtf8Bytes() = runTest {
        val client = HttpClient(MockEngine { respond(ByteReadChannel("é"), HttpStatusCode.OK) })
        try {
            val response = client.get(TEST_URL)
            assertEquals("é", response.bodyAsTextLimited(2))
            assertFailsWithSuspend<IllegalStateException> { response.bodyAsTextLimited(1) }
        } finally {
            client.close()
        }
    }

    private companion object {
        const val TEST_URL = "https://example.test/"
    }
}

private suspend inline fun <reified T : Throwable> assertFailsWithSuspend(crossinline block: suspend () -> Unit) {
    var failure: Throwable? = null
    try {
        block()
    } catch (error: Throwable) {
        failure = error
    }
    assertIs<T>(failure)
}
