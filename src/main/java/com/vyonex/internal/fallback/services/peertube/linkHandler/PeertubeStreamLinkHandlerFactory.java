// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.FoundAdException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.utils.Parser;

import java.net.MalformedURLException;
import java.net.URL;

public final class PeertubeStreamLinkHandlerFactory extends LinkHandlerFactory {

    private static final PeertubeStreamLinkHandlerFactory INSTANCE
            = new PeertubeStreamLinkHandlerFactory();
    private static final String ID_PATTERN = "(/w/|(/videos/(watch/|embed/)?))(?!p/)([^/?&#]*)";
    // we exclude p/ because /w/p/ is playlist, not video
    public static final String VIDEO_API_ENDPOINT = "/api/v1/videos/";

    // From PeerTube 3.3.0, the default path is /w/.
    // We still use /videos/watch/ for compatibility reasons:
    // /videos/watch/ is still accepted by >=3.3.0 but /w/ isn't by <3.3.0
    private static final String VIDEO_PATH = "/videos/watch/";

    private PeertubeStreamLinkHandlerFactory() {
    }

    public static PeertubeStreamLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id) throws ParsingException, UnsupportedOperationException {
        return getUrl(id, ServiceList.PeerTube.getBaseUrl());
    }

    @Override
    public String getUrl(final String id, final String baseUrl) {
        return baseUrl + VIDEO_PATH + id;
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return Parser.matchGroup(ID_PATTERN, url, 4);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws FoundAdException {
        if (url.contains("/playlist/")) {
            return false;
        }
        try {
            new URL(url);
            getId(url);
            return true;
        } catch (final ParsingException | MalformedURLException e) {
            return false;
        }
    }
}
