// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper

internal object BandcampUrl {
    @JvmStatic
    @Throws(ParsingException::class)
    fun parse(value: String?): BandcampUrlParser.Parsed = BandcampUrlParser.parse(value)
        ?: throw ParsingException("Invalid Bandcamp URL: $value")

    @JvmStatic
    fun https(url: String): String =
        if (url.startsWith("http://")) "https://${url.substring(7)}" else url

    @JvmStatic
    @Throws(ParsingException::class)
    fun isArtistPage(url: String): Boolean {
        val parsed = parse(url)
        return BandcampUrlParser.isArtistPage(parsed) && BandcampExtractorHelper.isArtistDomain(url)
    }

    @JvmStatic
    @Throws(ParsingException::class)
    fun isArtistContent(url: String, kind: String): Boolean =
        BandcampUrlParser.isArtistContent(parse(url), kind) && BandcampExtractorHelper.isArtistDomain(url)
}
