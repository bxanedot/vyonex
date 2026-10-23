// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import com.vyonex.internal.fallback.ServiceList
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory

public class PeertubeStreamLinkHandlerFactory private constructor() : LinkHandlerFactory() {
    @Throws(ParsingException::class)
    override fun getUrl(id: String): String = getUrl(id, ServiceList.PeerTube.getBaseUrl())

    override fun getUrl(id: String, baseUrl: String): String = "$baseUrl/videos/watch/$id"

    @Throws(ParsingException::class)
    override fun getId(url: String): String {
        val path = PeertubeUrl.segments(url)
        if (path.size == 2 && (path[0] == "w" || path[0] == "videos")) return path[1]
        if (path.size == 3 && path[0] == "videos"
            && (path[1] == "watch" || path[1] == "embed")
        ) return path[2]
        throw ParsingException("URL does not identify a PeerTube video")
    }

    override fun onAcceptUrl(url: String): Boolean = try {
        getId(url)
        true
    } catch (_: ParsingException) {
        false
    }

    public companion object {
        public const val VIDEO_API_ENDPOINT = "/api/v1/videos/"
        private val INSTANCE = PeertubeStreamLinkHandlerFactory()

        @JvmStatic
        public fun getInstance(): PeertubeStreamLinkHandlerFactory = INSTANCE
    }
}
