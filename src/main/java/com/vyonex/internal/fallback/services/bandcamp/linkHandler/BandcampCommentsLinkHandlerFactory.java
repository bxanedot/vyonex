// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;

import java.util.List;

public final class BandcampCommentsLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final BandcampCommentsLinkHandlerFactory INSTANCE =
            new BandcampCommentsLinkHandlerFactory();

    private BandcampCommentsLinkHandlerFactory() { }

    public static BandcampCommentsLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getId(final String url) { return BandcampUrl.https(url); }

    @Override
    public String getUrl(final String id, final List<String> contentFilter,
                         final String sortFilter) {
        return BandcampUrl.https(id);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return BandcampExtractorHelper.isRadioUrl(url)
                || BandcampUrl.isArtistContent(url, "track")
                || BandcampUrl.isArtistContent(url, "album");
    }
}
