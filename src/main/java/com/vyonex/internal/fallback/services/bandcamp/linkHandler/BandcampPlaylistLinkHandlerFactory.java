// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;
import com.vyonex.internal.fallback.utils.Utils;

import java.util.List;

/**
 * Just as with streams, the album ids are essentially useless for us.
 */
public final class BandcampPlaylistLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final BandcampPlaylistLinkHandlerFactory INSTANCE
            = new BandcampPlaylistLinkHandlerFactory();

    private BandcampPlaylistLinkHandlerFactory() {
    }

    public static BandcampPlaylistLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return getUrl(url);
    }

    @Override
    public String getUrl(final String url,
                         final List<String> contentFilter,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        return Utils.replaceHttpWithHttps(url);
    }

    /**
     * Accepts all bandcamp URLs that contain /album/ behind their domain name.
     */
    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {

        // Exclude URLs which do not lead to an album
        if (!url.toLowerCase().matches("https?://.+\\..+/album/.+")) {
            return false;
        }

        // Test whether domain is supported
        return BandcampExtractorHelper.isArtistDomain(url);
    }
}
