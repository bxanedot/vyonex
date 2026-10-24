// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import com.vyonex.internal.fallback.ServiceList
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory

public class PeertubeChannelLinkHandlerFactory private constructor() : ListLinkHandlerFactory() {
    @Throws(ParsingException::class)
    override fun getId(url: String): String {
        val path = PeertubeUrl.segments(url) ?: throw ParsingException("Invalid PeerTube URL: $url")
        val apiRoute = path.size > 2 && path[0] == "api" && path[1] == "v1"
        val start = if (apiRoute) 2 else 0
        if (path.size <= start + 1) throw ParsingException("URL has no channel ID")
        val type = when (path[start]) {
            "accounts", "a" -> "accounts"
            "video-channels", "c" -> "video-channels"
            else -> throw ParsingException("URL does not identify a PeerTube channel")
        }
        return "$type/${path[start + 1]}"
    }

    override fun getUrl(id: String, filters: List<String>, sort: String): String =
        getUrl(id, filters, sort, ServiceList.PeerTube.getBaseUrl())

    @Throws(ParsingException::class)
    override fun getUrl(id: String, filters: List<String>, sort: String, baseUrl: String): String =
        "$baseUrl/${canonicalId(id)}"

    override fun onAcceptUrl(url: String): Boolean = try {
        getId(url)
        true
    } catch (_: ParsingException) {
        false
    }

    private fun canonicalId(id: String): String {
        val parts = id.split('/', ignoreCase = false, limit = Int.MAX_VALUE)
        if (parts.size == 1 && id.isNotBlank()) return "accounts/$id"
        if (parts.size == 2 && parts[1].isNotBlank()) {
            return when (parts[0]) {
                "accounts", "a" -> "accounts/${parts[1]}"
                "video-channels", "c" -> "video-channels/${parts[1]}"
                else -> throw ParsingException("Unsupported PeerTube channel ID: $id")
            }
        }
        throw ParsingException("Invalid PeerTube channel ID: $id")
    }

    public companion object {
        public const val API_ENDPOINT = "/api/v1/"
        private val INSTANCE = PeertubeChannelLinkHandlerFactory()

        @JvmStatic
        public fun getInstance(): PeertubeChannelLinkHandlerFactory = INSTANCE
    }
}
