// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

public object BandcampSearchQueryHandlerFactory : SearchQueryHandlerFactory() {
    @JvmStatic
    public fun getInstance(): BandcampSearchQueryHandlerFactory = this

    @Throws(ParsingException::class)
    override fun getUrl(query: String, contentFilter: List<String>, sortFilter: String): String =
        "${BandcampExtractorHelper.BASE_URL}/search?q=${URLEncoder.encode(query, StandardCharsets.UTF_8)}&page=1"
}
