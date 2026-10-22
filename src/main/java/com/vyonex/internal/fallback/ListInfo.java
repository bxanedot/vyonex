// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

import java.util.List;

public abstract class ListInfo<T extends InfoItem> extends Info {
    private List<T> relatedItems;
    private Page nextPage;
    private final List<String> contentFilters;
    private final String sortFilter;

    public ListInfo(final int serviceId, final String id, final String url,
                    final String originalUrl, final String name,
                    final List<String> contentFilters, final String sortFilter) {
        super(serviceId, id, url, originalUrl, name);
        this.contentFilters = contentFilters;
        this.sortFilter = sortFilter;
    }

    public ListInfo(final int serviceId, final ListLinkHandler handler, final String name) {
        super(serviceId, handler, name);
        contentFilters = handler.getContentFilters();
        sortFilter = handler.getSortFilter();
    }

    public List<T> getRelatedItems() { return relatedItems; }
    public void setRelatedItems(final List<T> items) { relatedItems = items; }
    public boolean hasNextPage() { return Page.isValid(nextPage); }
    public Page getNextPage() { return nextPage; }
    public void setNextPage(final Page page) { nextPage = page; }
    public List<String> getContentFilters() { return contentFilters; }
    public String getSortFilter() { return sortFilter; }
}
