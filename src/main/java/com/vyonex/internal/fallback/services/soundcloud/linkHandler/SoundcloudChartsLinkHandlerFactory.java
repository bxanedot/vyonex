// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.utils.Parser;

import java.util.List;

public final class SoundcloudChartsLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final SoundcloudChartsLinkHandlerFactory INSTANCE =
            new SoundcloudChartsLinkHandlerFactory();

    private static final String URL_PATTERN =
            "^https?://(www\\.|m\\.)?soundcloud.com/charts(/top|/new)?/?([#?].*)?$";

    private SoundcloudChartsLinkHandlerFactory() {
    }

    public static SoundcloudChartsLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return "New & hot";
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        return "https://soundcloud.com/charts/new";
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        return Parser.isMatch(URL_PATTERN, url.toLowerCase());
    }
}
