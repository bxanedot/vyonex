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
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudSearchPagePayload;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudSearchPageParser;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudSearchResult;

import java.io.IOException;
import java.util.List;

public final class SoundcloudSearchExtractor extends SearchExtractor {
    private InfoItemsPage<InfoItem> initialPage;

    public SoundcloudSearchExtractor(final StreamingService service, final SearchQueryHandler handler) {
        super(service, handler);
    }

    @Override public String getSearchSuggestion() { return ""; }
    @Override public boolean isCorrectedSearch() { return false; }
    @Override public List<MetaInfo> getMetaInfo() { return List.of(); }

    @Override public void onFetchPage(final Downloader downloader)
            throws IOException, ExtractionException {
        final SoundcloudSearchPagePayload results = fetch(downloader, getUrl());
        if (results.getCollectionSize() == 0) {
            throw new SearchExtractor.NothingFoundException("No SoundCloud results");
        }
        initialPage = page(results);
    }

    @Override public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return initialPage;
    }

    @Override public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Search page URL is required");
        }
        return page(fetch(getDownloader(), page.getUrl()));
    }

    private SoundcloudSearchPagePayload fetch(final Downloader downloader, final String url)
            throws IOException, ExtractionException {
        final Response response = downloader.get(url, getExtractorLocalization());
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud search failed with HTTP " + response.responseCode());
        }
        try {
            return SoundcloudSearchPageParser.INSTANCE.parse(response.responseBody());
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("Invalid SoundCloud search response", e);
        }
    }

    private InfoItemsPage<InfoItem> page(final SoundcloudSearchPagePayload data) {
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        for (final SoundcloudSearchResult result : data.getResults()) {
            if (result instanceof SoundcloudSearchResult.User user) {
                items.commit(new SoundcloudChannelInfoItemExtractor(user));
            } else if (result instanceof SoundcloudSearchResult.Track track) {
                items.commit(new SoundcloudStreamInfoItemExtractor(track));
            } else if (result instanceof SoundcloudSearchResult.Playlist playlist) {
                items.commit(new SoundcloudPlaylistInfoItemExtractor(playlist));
            }
        }
        final String next = SoundcloudParsingHelper.getNextPageUrl(data.getNextPageUrl());
        return new InfoItemsPage<>(items, next.isBlank() ? null : new Page(next));
    }
}
