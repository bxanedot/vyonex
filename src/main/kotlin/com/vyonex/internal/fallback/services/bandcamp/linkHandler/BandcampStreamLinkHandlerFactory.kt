// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper

public object BandcampStreamLinkHandlerFactory : LinkHandlerFactory() {
    @JvmStatic
    public fun getInstance(): BandcampStreamLinkHandlerFactory = this

    @Throws(ParsingException::class)
    override fun getId(url: String): String {
        if (BandcampExtractorHelper.isRadioUrl(url)) {
            return BandcampUrl.parse(url).rawQuery!!.removePrefix("show=")
        }
        return getUrl(url)
    }

    override fun getUrl(id: String): String =
        if (id.matches(Regex("\\d+"))) "${BandcampExtractorHelper.BASE_URL}/?show=$id"
        else BandcampUrl.https(id)

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String): Boolean =
        BandcampExtractorHelper.isRadioUrl(url) || BandcampUrl.isArtistContent(url, "track")
}
