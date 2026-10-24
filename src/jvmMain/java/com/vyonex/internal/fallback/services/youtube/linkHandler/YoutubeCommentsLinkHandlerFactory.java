// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.exceptions.FoundAdException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class YoutubeCommentsLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final YoutubeCommentsLinkHandlerFactory INSTANCE =
            new YoutubeCommentsLinkHandlerFactory();

    private YoutubeCommentsLinkHandlerFactory() { }

    public static YoutubeCommentsLinkHandlerFactory getInstance() { return INSTANCE; }
    @Override public String getUrl(final String id) { return "https://www.youtube.com/watch?v=" + id; }
    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return getUrl(id);
    }
    @Override public String getId(final String url) throws ParsingException {
        return YoutubeStreamLinkHandlerFactory.getInstance().getId(url);
    }
    @Override public boolean onAcceptUrl(final String url) throws FoundAdException {
        try {
            getId(url);
            return true;
        } catch (final FoundAdException e) {
            throw e;
        } catch (final ParsingException e) {
            return false;
        }
    }
}
