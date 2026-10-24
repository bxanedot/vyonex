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
