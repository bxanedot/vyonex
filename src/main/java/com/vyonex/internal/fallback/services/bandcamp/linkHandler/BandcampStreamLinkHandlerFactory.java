// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL;

public final class BandcampStreamLinkHandlerFactory extends LinkHandlerFactory {
    private static final BandcampStreamLinkHandlerFactory INSTANCE =
            new BandcampStreamLinkHandlerFactory();

    private BandcampStreamLinkHandlerFactory() { }

    public static BandcampStreamLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getId(final String url) throws ParsingException {
        if (BandcampExtractorHelper.isRadioUrl(url)) {
            return BandcampUrl.parse(url).getRawQuery().substring("show=".length());
        }
        return getUrl(url);
    }

    @Override
    public String getUrl(final String id) {
        return id.matches("\\d+") ? BASE_URL + "/?show=" + id : BandcampUrl.https(id);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return BandcampExtractorHelper.isRadioUrl(url)
                || BandcampUrl.isArtistContent(url, "track");
    }
}
