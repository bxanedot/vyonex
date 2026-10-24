// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory

public object BandcampChannelTabLinkHandlerFactory : ListLinkHandlerFactory() {
    @JvmStatic
    public fun getInstance(): BandcampChannelTabLinkHandlerFactory = this

    @JvmStatic
    @Throws(UnsupportedTabException::class)
    public fun getUrlSuffix(tab: String): String = when (tab) {
        ChannelTabs.TRACKS -> "/track"
        ChannelTabs.ALBUMS -> "/album"
        else -> throw UnsupportedTabException(tab)
    }

    @Throws(ParsingException::class)
    override fun getId(url: String): String = BandcampChannelLinkHandlerFactory.getInstance().getId(url)

    @Throws(ParsingException::class)
    override fun getUrl(id: String, contentFilter: List<String>, sortFilter: String): String =
        BandcampChannelLinkHandlerFactory.getInstance().getUrl(id) + getUrlSuffix(contentFilter[0])

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String): Boolean =
        BandcampChannelLinkHandlerFactory.getInstance().onAcceptUrl(url)

    override fun getAvailableContentFilter(): Array<String> =
        arrayOf(ChannelTabs.TRACKS, ChannelTabs.ALBUMS)
}
