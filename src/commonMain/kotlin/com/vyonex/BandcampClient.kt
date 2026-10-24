// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex

import com.vyonex.internal.fallback.services.bandcamp.BandcampSuggestionParser
import com.vyonex.internal.primary.bodyAsTextLimited
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Bandcamp's public autocomplete endpoint, usable with JVM and Darwin Ktor clients. */
internal class BandcampClient(private val httpClient: HttpClient) {
    suspend fun suggestions(query: String): List<String> {
        val response = httpClient.post(AUTOCOMPLETE_URL) {
            setBody(
                TextContent(
                    buildJsonObject {
                        put("fan_id", JsonNull)
                        put("full_page", false)
                        put("search_filter", "")
                        put("search_text", query)
                    }.toString(),
                    ContentType.Application.Json,
                ),
            )
        }
        if (response.status.value !in 200..299) {
            throw IllegalStateException("Bandcamp autocomplete failed with HTTP ${response.status.value}")
        }
        return BandcampSuggestionParser.parse(response.bodyAsTextLimited(MAX_RESPONSE_BYTES))
    }

    companion object {
        const val AUTOCOMPLETE_URL = "https://bandcamp.com/api/bcsearch_public_api/1/autocomplete_elastic"
        private const val MAX_RESPONSE_BYTES = 256 * 1024
    }
}
