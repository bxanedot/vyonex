// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.search;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class SearchExtractor extends ListExtractor<InfoItem> {
    public static class NothingFoundException extends ExtractionException {
        public NothingFoundException(final String message) { super(message); }
    }

    public SearchExtractor(final StreamingService service,
                           final SearchQueryHandler queryHandler) {
        super(service, queryHandler);
    }

    public String getSearchString() { return getLinkHandler().getSearchString(); }

    @Nonnull
    public abstract String getSearchSuggestion() throws ParsingException;

    @Nonnull
    @Override
    public SearchQueryHandler getLinkHandler() {
        return (SearchQueryHandler) super.getLinkHandler();
    }

    @Nonnull
    @Override
    public String getName() { return getLinkHandler().getSearchString(); }

    public abstract boolean isCorrectedSearch() throws ParsingException;

    @Nonnull
    public abstract List<MetaInfo> getMetaInfo() throws ParsingException;
}
