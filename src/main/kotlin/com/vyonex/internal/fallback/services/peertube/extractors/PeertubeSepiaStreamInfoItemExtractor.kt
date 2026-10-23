// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.json.JsonObject
import java.net.URI

public class PeertubeSepiaStreamInfoItemExtractor(item: JsonObject, baseUrl: String) :
    PeertubeStreamInfoItemExtractor(item, baseUrl) {
    private val embedUrlError = adjustHost(item)

    @Throws(ParsingException::class)
    override fun getUrl(): String {
        embedUrlError?.let { throw ParsingException("Invalid Sepia video URL", it) }
        return super.getUrl()
    }

    private fun adjustHost(item: JsonObject): IllegalArgumentException? {
        val embed = embedUrl ?: return null
        return try {
            val location = URI.create(embed)
            val scheme = location.scheme
            val authority = location.rawAuthority
            if (scheme != null && authority != null) {
                val origin = "$scheme://$authority"
                val embedPath = item.getString("embedPath")
                val path = location.rawPath
                val pathStart = if (embedPath.isNullOrBlank() || path == null) -1
                else path.indexOf(embedPath)
                setBaseUrl(if (pathStart >= 0) origin + path.substring(0, pathStart) else origin)
            }
            null
        } catch (error: IllegalArgumentException) {
            error
        }
    }
}
