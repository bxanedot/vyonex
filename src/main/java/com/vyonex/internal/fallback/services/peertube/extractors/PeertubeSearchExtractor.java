// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.COUNT_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.START_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.collectItemsFrom;

public final class PeertubeSearchExtractor extends SearchExtractor {
    private final boolean sepia;

    public PeertubeSearchExtractor(final StreamingService service,
                                   final SearchQueryHandler handler, final boolean sepia) {
        super(service, handler);
        this.sepia = sepia;
    }

    @Override public String getSearchSuggestion() { return ""; }
    @Override public boolean isCorrectedSearch() { return false; }
    @Override public List<MetaInfo> getMetaInfo() { return List.of(); }

    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final String firstUrl = getUrl() + "&" + START_KEY + "=0&" + COUNT_KEY + "=" + ITEMS_PER_PAGE;
        return getPage(new Page(firstUrl));
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Page URL is required");
        }
        final Response response = getDownloader().get(page.getUrl());
        if (response == null || response.responseBody().isBlank()) {
            throw new ExtractionException("PeerTube returned no search results");
        }

        final JsonObject data;
        try {
            data = JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException error) {
            throw new ParsingException("Invalid PeerTube search response", error);
        }
        PeertubeParsingHelper.validate(data);
        if (response.responseCode() < 200 || response.responseCode() >= 300) {
            throw new ExtractionException("PeerTube search returned HTTP "
                    + response.responseCode());
        }

        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        collectItemsFrom(items, data, getBaseUrl(), sepia);
        final Page next = PeertubeParsingHelper.getNextPage(page.getUrl(), data.getLong("total"));
        return new InfoItemsPage<>(items, next);
    }

    @Override public void onFetchPage(final Downloader downloader) { }
}
