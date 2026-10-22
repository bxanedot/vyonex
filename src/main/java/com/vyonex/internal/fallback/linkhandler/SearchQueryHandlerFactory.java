// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.linkhandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;

import java.util.Collections;
import java.util.List;

public abstract class SearchQueryHandlerFactory extends ListLinkHandlerFactory {

    @Override
    public abstract String getUrl(String query, List<String> contentFilter, String sortFilter)
            throws ParsingException, UnsupportedOperationException;

    public String getSearchString(final String url) {
        return "";
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return getSearchString(url);
    }

    @Override
    public SearchQueryHandler fromQuery(final String query,
                                        final List<String> contentFilter,
                                        final String sortFilter) throws ParsingException {
        return new SearchQueryHandler(super.fromQuery(query, contentFilter, sortFilter));
    }

    public SearchQueryHandler fromQuery(final String query) throws ParsingException {
        return fromQuery(query, Collections.emptyList(), "");
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        return false;
    }
}
