// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.linkhandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.utils.Utils;

import java.util.List;
import java.util.Objects;

public abstract class ListLinkHandlerFactory extends LinkHandlerFactory {

    public abstract String getUrl(String id, List<String> contentFilter, String sortFilter)
            throws ParsingException, UnsupportedOperationException;

    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter,
                         final String baseUrl)
            throws ParsingException, UnsupportedOperationException {
        return getUrl(id, contentFilter, sortFilter);
    }

    @Override
    public ListLinkHandler fromUrl(final String url) throws ParsingException {
        final String resolvedUrl = Utils.followGoogleRedirectIfNeeded(url);
        return fromUrl(resolvedUrl, Utils.getBaseUrl(resolvedUrl));
    }

    @Override
    public ListLinkHandler fromUrl(final String url, final String baseUrl) throws ParsingException {
        Objects.requireNonNull(url, "URL may not be null");
        return new ListLinkHandler(super.fromUrl(url, baseUrl));
    }

    @Override
    public ListLinkHandler fromId(final String id) throws ParsingException {
        return new ListLinkHandler(super.fromId(id));
    }

    @Override
    public ListLinkHandler fromId(final String id, final String baseUrl) throws ParsingException {
        return new ListLinkHandler(super.fromId(id, baseUrl));
    }

    public ListLinkHandler fromQuery(final String id,
                                     final List<String> contentFilters,
                                     final String sortFilter) throws ParsingException {
        final String url = getUrl(id, contentFilters, sortFilter);
        return new ListLinkHandler(url, url, id, contentFilters, sortFilter);
    }

    public ListLinkHandler fromQuery(final String id,
                                     final List<String> contentFilters,
                                     final String sortFilter,
                                     final String baseUrl) throws ParsingException {
        final String url = getUrl(id, contentFilters, sortFilter, baseUrl);
        return new ListLinkHandler(url, url, id, contentFilters, sortFilter);
    }


    public String getUrl(final String id) throws ParsingException, UnsupportedOperationException {
        return getUrl(id, List.of(), "");
    }

    @Override
    public String getUrl(final String id, final String baseUrl) throws ParsingException {
        return getUrl(id, List.of(), "", baseUrl);
    }

    public String[] getAvailableContentFilter() {
        return new String[0];
    }

    public String[] getAvailableSortFilter() {
        return new String[0];
    }
}
