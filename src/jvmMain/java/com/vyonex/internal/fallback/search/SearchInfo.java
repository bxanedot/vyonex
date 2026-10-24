// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.search;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.ListInfo;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.utils.ExtractorHelper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class SearchInfo extends ListInfo<InfoItem> {
    private final String searchString;
    private String searchSuggestion;
    private boolean isCorrectedSearch;
    private List<MetaInfo> metaInfo = List.of();

    public SearchInfo(final int serviceId,
                      final SearchQueryHandler queryHandler,
                      final String searchString) {
        super(serviceId, queryHandler, "Search");
        this.searchString = searchString;
    }

    public static SearchInfo getInfo(final StreamingService service,
                                     final SearchQueryHandler queryHandler)
            throws ExtractionException, IOException {
        final SearchExtractor extractor = service.getSearchExtractor(queryHandler);
        extractor.fetchPage();
        return getInfo(extractor);
    }

    public static SearchInfo getInfo(final SearchExtractor extractor)
            throws ExtractionException, IOException {
        final SearchInfo info = new SearchInfo(
                extractor.getServiceId(), extractor.getLinkHandler(), extractor.getSearchString());

        try {
            info.setOriginalUrl(extractor.getOriginalUrl());
        } catch (final Exception error) {
            info.addError(error);
        }
        try {
            info.setSearchSuggestion(extractor.getSearchSuggestion());
        } catch (final Exception error) {
            info.addError(error);
        }
        try {
            info.setIsCorrectedSearch(extractor.isCorrectedSearch());
        } catch (final Exception error) {
            info.addError(error);
        }
        try {
            info.setMetaInfo(extractor.getMetaInfo());
        } catch (final Exception error) {
            info.addError(error);
        }

        final ListExtractor.InfoItemsPage<InfoItem> firstPage =
                ExtractorHelper.getItemsPageOrLogError(info, extractor);
        info.setRelatedItems(firstPage.getItems());
        info.setNextPage(firstPage.getNextPage());
        return info;
    }

    public static ListExtractor.InfoItemsPage<InfoItem> getMoreItems(
            final StreamingService service, final SearchQueryHandler queryHandler,
            final Page page) throws IOException, ExtractionException {
        return service.getSearchExtractor(queryHandler).getPage(page);
    }

    public String getSearchString() { return searchString; }
    public String getSearchSuggestion() { return searchSuggestion; }
    public boolean isCorrectedSearch() { return isCorrectedSearch; }
    public void setIsCorrectedSearch(final boolean correctedSearch) {
        this.isCorrectedSearch = correctedSearch;
    }
    public void setSearchSuggestion(final String searchSuggestion) {
        this.searchSuggestion = searchSuggestion;
    }

    @Nonnull
    public List<MetaInfo> getMetaInfo() { return metaInfo; }
    public void setMetaInfo(@Nonnull final List<MetaInfo> metaInfo) {
        this.metaInfo = metaInfo;
    }
}
