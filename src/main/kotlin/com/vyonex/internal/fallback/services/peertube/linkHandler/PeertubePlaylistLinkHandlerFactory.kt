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
        val path = PeertubeUrl.segments(url)
        if (path.size == 3 && path[0] == "w" && path[1] == "p") return path[2]
        if (path.size == 4 && path[0] == "videos" && path[1] == "watch"
            && path[2] == "playlist"
        ) return path[3]
        if (path.size == 4 && path[0] == "api" && path[1] == "v1"
            && path[2] == "video-playlists"
        ) return path[3]
        throw ParsingException("URL does not identify a PeerTube playlist")
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
