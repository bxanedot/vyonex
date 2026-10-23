// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;

import java.util.List;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampFeaturedExtractor.FEATURED_API_URL;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampFeaturedExtractor.KIOSK_FEATURED;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioExtractor.KIOSK_RADIO;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioExtractor.RADIO_API_URL;

public final class BandcampFeaturedLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final BandcampFeaturedLinkHandlerFactory INSTANCE =
            new BandcampFeaturedLinkHandlerFactory();

    private BandcampFeaturedLinkHandlerFactory() { }

    public static BandcampFeaturedLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getUrl(final String id, final List<String> contentFilter,
                         final String sortFilter) {
        if (KIOSK_FEATURED.equals(id)) return FEATURED_API_URL;
        if (KIOSK_RADIO.equals(id)) return RADIO_API_URL;
        return null;
    }

    @Override
    public String getId(final String url) {
        final String httpsUrl = BandcampUrl.https(url);
        if (BandcampExtractorHelper.isRadioUrl(httpsUrl) || RADIO_API_URL.equals(httpsUrl)) {
            return KIOSK_RADIO;
        }
        return FEATURED_API_URL.equals(httpsUrl) ? KIOSK_FEATURED : null;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        final String httpsUrl = BandcampUrl.https(url);
        return FEATURED_API_URL.equals(httpsUrl) || RADIO_API_URL.equals(httpsUrl)
                || BandcampExtractorHelper.isRadioUrl(httpsUrl);
    }
}
