// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.VyonEx
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.exceptions.ReCaptchaException
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
import com.vyonex.internal.json.JsonWriter
import java.io.IOException
import java.nio.charset.StandardCharsets

public class BandcampSuggestionExtractor(service: StreamingService) : SuggestionExtractor(service) {
    @Throws(IOException::class, ExtractionException::class)
    override fun suggestionList(query: String): List<String> {
        val downloader = VyonEx.getDownloader()
            ?: throw ParsingException("VyonEx has not been initialized")
        val body = JsonWriter.string().`object`()
            .value("fan_id", null as String?)
            .value("full_page", false)
            .value("search_filter", "")
            .value("search_text", query)
            .end().done()
            .toByteArray(StandardCharsets.UTF_8)

        val response = try {
            JsonParser.`object`().from(
                downloader.postWithContentTypeJson(AUTOCOMPLETE_URL, emptyMap(), body)
                    .responseBody(),
            )
        } catch (_: JsonParserException) {
            return emptyList()
        } catch (error: ReCaptchaException) {
            throw ParsingException("Bandcamp blocked autocomplete", error)
        }
        val results = response.getObject("auto")?.getArray("results") ?: return emptyList()
        val names = LinkedHashSet<String>()
        for (item in results) {
            val name = (item as? JsonObject)?.getString("name", "") ?: continue
            if (name.isNotBlank()) names.add(name)
        }
        return names.toList()
    }

    private companion object {
        const val AUTOCOMPLETE_URL = "${BandcampExtractorHelper.BASE_API_URL}/bcsearch_public_api/1/autocomplete_elastic"
    }
}
