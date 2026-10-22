// bxane
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;
import java.util.regex.Pattern;

public final class MediaCCCRecentListLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final MediaCCCRecentListLinkHandlerFactory INSTANCE =
            new MediaCCCRecentListLinkHandlerFactory();

    private static final String PATTERN = "^(https?://)?media\\.ccc\\.de/recent/?$";

    private MediaCCCRecentListLinkHandlerFactory() {
    }

    public static MediaCCCRecentListLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return "recent";
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        return Pattern.matches(PATTERN, url);
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        return "https://media.ccc.de/recent";
    }
}
