// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class PeertubeCommentsLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final PeertubeCommentsLinkHandlerFactory INSTANCE =
            new PeertubeCommentsLinkHandlerFactory();

    private PeertubeCommentsLinkHandlerFactory() { }
    public static PeertubeCommentsLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getId(final String url) throws ParsingException {
        return PeertubeStreamLinkHandlerFactory.getInstance().getId(url);
    }
    @Override public boolean onAcceptUrl(final String url) {
        return PeertubeStreamLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }
    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return getUrl(id, filters, sort, ServiceList.PeerTube.getBaseUrl());
    }
    @Override public String getUrl(final String id, final List<String> filters, final String sort,
                                   final String baseUrl) {
        return baseUrl + "/api/v1/videos/" + id + "/comment-threads";
    }
}
