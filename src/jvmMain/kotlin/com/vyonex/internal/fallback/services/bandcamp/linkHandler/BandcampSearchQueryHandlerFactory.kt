// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory

public object BandcampSearchQueryHandlerFactory : SearchQueryHandlerFactory() {
    @JvmStatic
    public fun getInstance(): BandcampSearchQueryHandlerFactory = this

    @Throws(ParsingException::class)
    override fun getUrl(query: String, contentFilter: List<String>, sortFilter: String): String =
        BandcampUrlParser.searchUrl(query)
}
