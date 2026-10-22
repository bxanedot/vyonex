// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.FoundAdException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public final class PeertubeCommentsLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final PeertubeCommentsLinkHandlerFactory INSTANCE
            = new PeertubeCommentsLinkHandlerFactory();
    private static final String COMMENTS_ENDPOINT = "/api/v1/videos/%s/comment-threads";

    private PeertubeCommentsLinkHandlerFactory() {
    }

    public static PeertubeCommentsLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return PeertubeStreamLinkHandlerFactory.getInstance().getId(url); // the same id is needed
    }

    @Override
    public boolean onAcceptUrl(final String url) throws FoundAdException {
        try {
            new URL(url);
            return url.contains("/videos/") || url.contains("/w/");
        } catch (final MalformedURLException e) {
            return false;
        }
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        return getUrl(id, contentFilter, sortFilter, ServiceList.PeerTube.getBaseUrl());
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter,
                         final String baseUrl)
            throws ParsingException, UnsupportedOperationException {
        return baseUrl + String.format(COMMENTS_ENDPOINT, id);
    }

}
