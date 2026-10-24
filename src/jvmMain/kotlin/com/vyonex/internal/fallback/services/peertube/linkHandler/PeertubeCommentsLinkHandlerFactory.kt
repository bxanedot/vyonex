// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import com.vyonex.internal.fallback.ServiceList
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory

public class PeertubeCommentsLinkHandlerFactory private constructor() : ListLinkHandlerFactory() {
    @Throws(ParsingException::class)
    override fun getId(url: String): String = PeertubeStreamLinkHandlerFactory.getInstance().getId(url)

    override fun onAcceptUrl(url: String): Boolean =
        PeertubeStreamLinkHandlerFactory.getInstance().onAcceptUrl(url)

    override fun getUrl(id: String, filters: List<String>, sort: String): String =
        getUrl(id, filters, sort, ServiceList.PeerTube.getBaseUrl())

    override fun getUrl(id: String, filters: List<String>, sort: String, baseUrl: String): String =
        "$baseUrl/api/v1/videos/$id/comment-threads"

    public companion object {
        private val INSTANCE = PeertubeCommentsLinkHandlerFactory()

        @JvmStatic
        public fun getInstance(): PeertubeCommentsLinkHandlerFactory = INSTANCE
    }
}
