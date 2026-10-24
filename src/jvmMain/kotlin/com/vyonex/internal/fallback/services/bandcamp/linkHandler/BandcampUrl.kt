// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
import java.util.Locale

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
        if (parsed.host.equals("daily.bandcamp.com", ignoreCase = true)) return false
        val path = parsed.segments
        if (path.size > 1 || (path.size == 1 && path[0].lowercase(Locale.ROOT) !in
                setOf("releases", "music", "album", "track"))
        ) return false
        return BandcampExtractorHelper.isArtistDomain(url)
    }

    @JvmStatic
    @Throws(ParsingException::class)
    fun isArtistContent(url: String, kind: String): Boolean {
        val path = parse(url).path.lowercase(Locale.ROOT)
        val prefix = "/$kind/"
        return path.startsWith(prefix) && path.length > prefix.length &&
            BandcampExtractorHelper.isArtistDomain(url)
    }
}
