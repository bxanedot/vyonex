// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;
import com.vyonex.internal.fallback.utils.Utils;

import java.util.List;

/**
 * Like in {@link BandcampStreamLinkHandlerFactory}, tracks have no meaningful IDs except for
 * their URLs
 */
public final class BandcampCommentsLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final BandcampCommentsLinkHandlerFactory INSTANCE
            = new BandcampCommentsLinkHandlerFactory();

    private BandcampCommentsLinkHandlerFactory() {
    }

    public static BandcampCommentsLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return Utils.replaceHttpWithHttps(url);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        if (BandcampExtractorHelper.isRadioUrl(url)) {
            return true;
        }

        // Don't accept URLs that don't point to a track
        if (!url.toLowerCase().matches("https?://.+\\..+/(track|album)/.+")) {
            return false;
        }

        // Test whether domain is supported
        return BandcampExtractorHelper.isArtistDomain(url);
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        return Utils.replaceHttpWithHttps(id);
    }
}
