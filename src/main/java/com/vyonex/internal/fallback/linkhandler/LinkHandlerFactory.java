// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.linkhandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.utils.Utils;

import java.util.Objects;

public abstract class LinkHandlerFactory {
    public abstract String getId(String url) throws ParsingException, UnsupportedOperationException;

    public abstract String getUrl(String id) throws ParsingException, UnsupportedOperationException;

    public abstract boolean onAcceptUrl(String url) throws ParsingException;

    public String getUrl(final String id, final String baseUrl)
            throws ParsingException, UnsupportedOperationException {
        return getUrl(id);
    }

    public LinkHandler fromUrl(final String url) throws ParsingException {
        if (Utils.isNullOrEmpty(url)) {
            throw new IllegalArgumentException("The url is null or empty");
        }
        final String polishedUrl = Utils.followGoogleRedirectIfNeeded(url);
        return fromUrl(polishedUrl, Utils.getBaseUrl(polishedUrl));
    }

    public LinkHandler fromUrl(final String url, final String baseUrl) throws ParsingException {
        Objects.requireNonNull(url, "URL cannot be null");
        if (!acceptUrl(url)) throw new ParsingException("URL not accepted: " + url);
        final String id = getId(url);
        return new LinkHandler(url, getUrl(id, baseUrl), id);
    }

    public LinkHandler fromId(final String id) throws ParsingException {
        Objects.requireNonNull(id, "ID cannot be null");
        final String url = getUrl(id);
        return new LinkHandler(url, url, id);
    }

    public LinkHandler fromId(final String id, final String baseUrl) throws ParsingException {
        Objects.requireNonNull(id, "ID cannot be null");
        final String url = getUrl(id, baseUrl);
        return new LinkHandler(url, url, id);
    }

    public boolean acceptUrl(final String url) throws ParsingException {
        return onAcceptUrl(url);
    }
}
