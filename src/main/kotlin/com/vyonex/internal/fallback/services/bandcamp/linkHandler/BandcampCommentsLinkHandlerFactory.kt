// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper

public object BandcampCommentsLinkHandlerFactory : ListLinkHandlerFactory() {
    @JvmStatic
    public fun getInstance(): BandcampCommentsLinkHandlerFactory = this

    override fun getId(url: String): String = BandcampUrl.https(url)

    override fun getUrl(id: String, contentFilter: List<String>, sortFilter: String): String =
        BandcampUrl.https(id)

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String): Boolean =
        BandcampExtractorHelper.isRadioUrl(url) ||
            BandcampUrl.isArtistContent(url, "track") ||
            BandcampUrl.isArtistContent(url, "album")
}
