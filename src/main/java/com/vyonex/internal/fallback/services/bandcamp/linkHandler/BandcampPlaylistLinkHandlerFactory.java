// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import java.util.List;

public final class BandcampPlaylistLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final BandcampPlaylistLinkHandlerFactory INSTANCE =
            new BandcampPlaylistLinkHandlerFactory();

    private BandcampPlaylistLinkHandlerFactory() { }

    public static BandcampPlaylistLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getId(final String url) { return BandcampUrl.https(url); }

    @Override
    public String getUrl(final String id, final List<String> contentFilter,
                         final String sortFilter) {
        return BandcampUrl.https(id);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return BandcampUrl.isArtistContent(url, "album");
    }
}
