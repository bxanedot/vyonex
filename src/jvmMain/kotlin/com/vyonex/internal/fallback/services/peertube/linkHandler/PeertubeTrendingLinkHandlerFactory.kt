// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import com.vyonex.internal.fallback.ServiceList
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Collections

public class PeertubeTrendingLinkHandlerFactory private constructor() : ListLinkHandlerFactory() {
    @Throws(ParsingException::class)
    override fun getUrl(id: String, filters: List<String>, sort: String): String =
        getUrl(id, filters, sort, ServiceList.PeerTube.getBaseUrl())

    @Throws(ParsingException::class)
    override fun getUrl(id: String, filters: List<String>, sort: String, baseUrl: String): String {
        val template = KIOSK_MAP[id] ?: throw ParsingException("Unknown PeerTube kiosk: $id")
        return String.format(template, baseUrl)
    }

    @Throws(ParsingException::class)
    override fun getId(value: String): String {
        val url = PeertubeUrl.parse(value)
        val path = url.rawPath.orEmpty()
        aliasFor(path)?.let { return it }
        if (!path.endsWith("/api/v1/videos")) {
            throw ParsingException("URL does not identify a PeerTube kiosk")
        }
        val query = parseQuery(url.rawQuery)
        return when (query) {
            mapOf("sort" to "-trending") -> KIOSK_TRENDING
            mapOf("sort" to "-likes") -> KIOSK_MOST_LIKED
            mapOf("sort" to "-publishedAt") -> KIOSK_RECENT
            mapOf("sort" to "-publishedAt", "isLocal" to "true") -> KIOSK_LOCAL
            else -> throw ParsingException("URL does not identify a PeerTube kiosk")
        }
    }

    override fun onAcceptUrl(url: String): Boolean = try {
        getId(url)
        true
    } catch (_: ParsingException) {
        false
    }

    private fun aliasFor(path: String): String? = when {
        path.endsWith("/videos/trending") -> KIOSK_TRENDING
        path.endsWith("/videos/most-liked") -> KIOSK_MOST_LIKED
        path.endsWith("/videos/recently-added") -> KIOSK_RECENT
        path.endsWith("/videos/local") -> KIOSK_LOCAL
        else -> null
    }

    private fun parseQuery(query: String?): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        if (query.isNullOrEmpty()) return values
        try {
            for (entry in query.split('&').dropLastWhile(String::isEmpty)) {
                val separator = entry.indexOf('=')
                val key = if (separator < 0) entry else entry.substring(0, separator)
                val value = if (separator < 0) "" else entry.substring(separator + 1)
                values[URLDecoder.decode(key, StandardCharsets.UTF_8)] =
                    URLDecoder.decode(value, StandardCharsets.UTF_8)
            }
        } catch (error: IllegalArgumentException) {
            throw ParsingException("Invalid PeerTube kiosk query", error)
        }
        return values
    }

    public companion object {
        public const val KIOSK_TRENDING = "Trending"
        public const val KIOSK_MOST_LIKED = "Most liked"
        public const val KIOSK_RECENT = "Recently added"
        public const val KIOSK_LOCAL = "Local"
        @JvmField
        public val KIOSK_MAP: Map<String, String> = Collections.unmodifiableMap(
            linkedMapOf(
                KIOSK_TRENDING to "%s/api/v1/videos?sort=-trending",
                KIOSK_MOST_LIKED to "%s/api/v1/videos?sort=-likes",
                KIOSK_RECENT to "%s/api/v1/videos?sort=-publishedAt",
                KIOSK_LOCAL to "%s/api/v1/videos?sort=-publishedAt&isLocal=true",
            ),
        )
        private val INSTANCE = PeertubeTrendingLinkHandlerFactory()

        @JvmStatic
        public fun getInstance(): PeertubeTrendingLinkHandlerFactory = INSTANCE
    }
}
