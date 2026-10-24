// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class InnerTubeVisitorDataContractTest {
    @Test
    fun getsVisitorDataFromTheServiceWorkerResponse() = runTest {
        val client = mockClient(workerResponse("worker-token"))
        val innerTube = InnerTube(client)
        try {
            assertEquals("worker-token", innerTube.fetchFreshVisitorData())
            assertEquals("worker-token", innerTube.visitorData)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun rejectsMalformedServiceWorkerDataAndUsesHomepageValue() = runTest {
        val homepage = "<script>{\"visitorData\":\"home-token\"}</script>"
        val client = mockClient(workerResponse("bad\r\nX-Injected: yes"), homepage)
        val innerTube = InnerTube(client)
        try {
            assertEquals("home-token", innerTube.fetchFreshVisitorData())
            assertEquals("home-token", innerTube.visitorData)
        } finally {
            innerTube.close()
            client.close()
        }
    }

    private fun mockClient(
        workerBody: String,
        homepageBody: String? = null,
    ) = HttpClient(
        MockEngine { request ->
            val body =
                when {
                    request.url.host == "www.youtube.com" && request.url.encodedPath == "/sw.js_data" -> workerBody
                    request.url.host == "music.youtube.com" ->
                        requireNotNull(homepageBody) { "Unexpected homepage request" }
                    else -> error("Unexpected request: ${request.url}")
                }
            respond(
                ByteReadChannel(body),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    )

    private fun workerResponse(visitorData: String): String {
        val visitorDataEntry = JsonArray(List(13) { JsonNull } + JsonPrimitive(visitorData))
        val visitorDataNode = JsonArray(listOf(visitorDataEntry))
        val responseContext = JsonArray(listOf(visitorDataNode))
        val payload = JsonArray(listOf(JsonArray(listOf(JsonNull, JsonNull, responseContext))))
        return ")]}'\n$payload"
    }
}
