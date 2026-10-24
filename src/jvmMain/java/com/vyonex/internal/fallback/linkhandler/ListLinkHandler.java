// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.linkhandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListLinkHandler extends LinkHandler {
    protected final List<String> contentFilters;
    protected final String sortFilter;

    public ListLinkHandler(final String originalUrl,
                           final String url,
                           final String id,
                           final List<String> contentFilters,
                           final String sortFilter) {
        super(originalUrl, url, id);
        this.contentFilters = Collections.unmodifiableList(new ArrayList<>(contentFilters));
        this.sortFilter = sortFilter;
    }

    public ListLinkHandler(final ListLinkHandler handler) {
        this(handler.getOriginalUrl(),
                handler.getUrl(),
                handler.getId(),
                handler.getContentFilters(),
                handler.getSortFilter());
    }

    public ListLinkHandler(final LinkHandler handler) {
        this(handler.getOriginalUrl(),
                handler.getUrl(),
                handler.getId(),
                Collections.emptyList(),
                "");
    }

    public List<String> getContentFilters() {
        return contentFilters;
    }

    public String getSortFilter() {
        return sortFilter;
    }
}
