// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonObject;

import java.io.IOException;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.COUNT_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.START_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.collectItemsFrom;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.readApiObject;

public final class PeertubeTrendingExtractor extends KioskExtractor<StreamInfoItem> {
    public PeertubeTrendingExtractor(final StreamingService service,
                                    final ListLinkHandler handler, final String kioskId) {
        super(service, handler, kioskId);
    }

    @Override public String getName() { return getId(); }

    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        final String firstUrl = getUrl() + "&" + START_KEY + "=0&" + COUNT_KEY + "=" + ITEMS_PER_PAGE;
        return getPage(new Page(firstUrl));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Page URL is required");
        }
        final JsonObject data = readApiObject(getDownloader().get(page.getUrl()), "kiosk results");
        PeertubeParsingHelper.validate(data);

        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        collectItemsFrom(items, data, getBaseUrl());
        final Page next = PeertubeParsingHelper.getNextPage(page.getUrl(), data.getLong("total"));
        return new InfoItemsPage<>(items, next);
    }

    @Override public void onFetchPage(final Downloader downloader) { }
}
