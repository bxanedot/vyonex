// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

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
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SoundcloudSearchExtractor extends SearchExtractor {
    private JsonObject results;

    public SoundcloudSearchExtractor(final StreamingService service, final SearchQueryHandler handler) {
        super(service, handler);
    }

    @Override public String getSearchSuggestion() { return ""; }
    @Override public boolean isCorrectedSearch() { return false; }
    @Override public List<MetaInfo> getMetaInfo() { return List.of(); }

    @Override public void onFetchPage(final Downloader downloader)
            throws IOException, ExtractionException {
        results = fetch(downloader, getUrl());
        if (results.getArray("collection").isEmpty()) {
            throw new SearchExtractor.NothingFoundException("No SoundCloud results");
        }
    }

    @Override public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return page(results);
    }

    @Override public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Search page URL is required");
        }
        return page(fetch(getDownloader(), page.getUrl()));
    }

    private JsonObject fetch(final Downloader downloader, final String url)
            throws IOException, ExtractionException {
        final Response response = downloader.get(url, getExtractorLocalization());
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud search failed with HTTP " + response.responseCode());
        }
        try {
            return JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid SoundCloud search response", e);
        }
    }

    private InfoItemsPage<InfoItem> page(final JsonObject data) {
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        final JsonArray collection = data.getArray("collection");
        for (final Object value : collection) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject result = (JsonObject) value;
            switch (result.getString("kind", "")) {
                case "user" -> items.commit(new SoundcloudChannelInfoItemExtractor(result));
                case "track" -> items.commit(new SoundcloudStreamInfoItemExtractor(result));
                case "playlist" -> items.commit(new SoundcloudPlaylistInfoItemExtractor(result));
                default -> { }
            }
        }
        final String next = data.getString("next_href");
        return new InfoItemsPage<>(items, next == null || next.isBlank() ? null : new Page(next));
    }
}
