// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory

public class PeertubeChannelTabLinkHandlerFactory private constructor() : ListLinkHandlerFactory() {
    @Throws(ParsingException::class)
    override fun getId(url: String): String =
        PeertubeChannelLinkHandlerFactory.getInstance().getId(url)

    @Throws(ParsingException::class)
    override fun getUrl(id: String, filters: List<String>, sort: String): String =
        getUrl(id, filters, sort, null)

    @Throws(ParsingException::class)
    override fun getUrl(id: String, filters: List<String>, sort: String, baseUrl: String?): String {
        if (filters.isEmpty()) throw ParsingException("PeerTube channel tab is missing")
        val channels = PeertubeChannelLinkHandlerFactory.getInstance()
        val root = if (baseUrl == null) channels.getUrl(id)
        else channels.getUrl(id, filters, sort, baseUrl)
        return root + getUrlSuffix(filters[0])
    }

    override fun onAcceptUrl(url: String): Boolean =
        PeertubeChannelLinkHandlerFactory.getInstance().onAcceptUrl(url)

    override fun getAvailableContentFilter(): Array<String> =
        arrayOf(ChannelTabs.VIDEOS, ChannelTabs.CHANNELS, ChannelTabs.PLAYLISTS)

    public companion object {
        private val INSTANCE = PeertubeChannelTabLinkHandlerFactory()

        @JvmStatic
        public fun getInstance(): PeertubeChannelTabLinkHandlerFactory = INSTANCE

        @JvmStatic
        @Throws(UnsupportedTabException::class)
        public fun getUrlSuffix(tab: String): String = when (tab) {
            ChannelTabs.VIDEOS -> "/videos"
            ChannelTabs.CHANNELS -> "/video-channels"
            ChannelTabs.PLAYLISTS -> "/video-playlists"
            else -> throw UnsupportedTabException(tab)
        }
    }
}
