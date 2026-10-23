// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampFeaturedExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioExtractor

public object BandcampFeaturedLinkHandlerFactory : ListLinkHandlerFactory() {
    @JvmStatic
    public fun getInstance(): BandcampFeaturedLinkHandlerFactory = this

    override fun getUrl(id: String, contentFilter: List<String>, sortFilter: String): String? =
        when (id) {
            BandcampFeaturedExtractor.KIOSK_FEATURED -> BandcampFeaturedExtractor.FEATURED_API_URL
            BandcampRadioExtractor.KIOSK_RADIO -> BandcampRadioExtractor.RADIO_API_URL
            else -> null
        }

    override fun getId(url: String): String? {
        val httpsUrl = BandcampUrl.https(url)
        return when {
            BandcampExtractorHelper.isRadioUrl(httpsUrl) ||
                httpsUrl == BandcampRadioExtractor.RADIO_API_URL -> BandcampRadioExtractor.KIOSK_RADIO
            httpsUrl == BandcampFeaturedExtractor.FEATURED_API_URL -> BandcampFeaturedExtractor.KIOSK_FEATURED
            else -> null
        }
    }

    override fun onAcceptUrl(url: String): Boolean {
        val httpsUrl = BandcampUrl.https(url)
        return httpsUrl == BandcampFeaturedExtractor.FEATURED_API_URL ||
            httpsUrl == BandcampRadioExtractor.RADIO_API_URL ||
            BandcampExtractorHelper.isRadioUrl(httpsUrl)
    }
}
