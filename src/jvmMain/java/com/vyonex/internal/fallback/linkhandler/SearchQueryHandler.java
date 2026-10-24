// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.linkhandler;

import java.util.List;

public class SearchQueryHandler extends ListLinkHandler {

    public SearchQueryHandler(final String originalUrl,
                              final String url,
                              final String searchString,
                              final List<String> contentFilters,
                              final String sortFilter) {
        super(originalUrl, url, searchString, contentFilters, sortFilter);
    }

    public SearchQueryHandler(final ListLinkHandler handler) {
        this(handler.getOriginalUrl(),
                handler.getUrl(),
                handler.getId(),
                handler.getContentFilters(),
                handler.getSortFilter());
    }

    public String getSearchString() {
        return getId();
    }
}
