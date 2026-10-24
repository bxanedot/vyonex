// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import com.vyonex.internal.fallback.ServiceList
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory

public class PeertubePlaylistLinkHandlerFactory private constructor() : ListLinkHandlerFactory() {
    override fun getUrl(id: String, filters: List<String>, sort: String): String =
        getUrl(id, filters, sort, ServiceList.PeerTube.getBaseUrl())

    override fun getUrl(id: String, filters: List<String>, sort: String, baseUrl: String): String =
        "$baseUrl/api/v1/video-playlists/$id"

    @Throws(ParsingException::class)
    override fun getId(url: String): String {
        val parsed = PeertubeUrl.parse(url) ?: throw ParsingException("Invalid PeerTube URL: $url")
        return PeertubeUrl.playlistId(parsed) ?: throw ParsingException("URL does not identify a PeerTube playlist")
    }

    override fun onAcceptUrl(url: String): Boolean = try {
        getId(url)
        true
    } catch (_: ParsingException) {
        false
    }

    public companion object {
        private val INSTANCE = PeertubePlaylistLinkHandlerFactory()

        @JvmStatic
        public fun getInstance(): PeertubePlaylistLinkHandlerFactory = INSTANCE
    }
}
