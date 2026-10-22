// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.COUNT_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.START_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.collectItemsFrom;

public final class PeertubeTrendingExtractor extends KioskExtractor<StreamInfoItem> {
    public PeertubeTrendingExtractor(final StreamingService service,
                                     final ListLinkHandler handler, final String kioskId) {
        super(service, handler, kioskId);
    }

    @Override public String getName() { return getId(); }

    @Override public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        return getPage(new Page(getUrl() + "&" + START_KEY + "=0&"
                + COUNT_KEY + "=" + ITEMS_PER_PAGE));
    }

    @Override public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Page URL is required");
        }
        final Response response = getDownloader().get(page.getUrl());
        if (response == null || response.responseBody().isBlank()) {
            throw new ExtractionException("PeerTube returned no kiosk results");
        }
        final JsonObject json;
        try {
            json = JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid PeerTube kiosk response", e);
        }
        PeertubeParsingHelper.validate(json);
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        collectItemsFrom(items, json, getBaseUrl());
        return new InfoItemsPage<>(items,
                PeertubeParsingHelper.getNextPage(page.getUrl(), json.getLong("total")));
    }

    @Override public void onFetchPage(final Downloader downloader) { }
}
