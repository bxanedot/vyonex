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
        val parsed = PeertubeUrl.parse(url) ?: throw ParsingException("Invalid PeerTube URL: $url")
        return PeertubeUrl.videoId(parsed) ?: throw ParsingException("URL does not identify a PeerTube video")
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
