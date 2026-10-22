// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.COUNT_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.START_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.collectItemsFrom;
import static com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory.getUrlSuffix;

public final class PeertubeChannelTabExtractor extends ChannelTabExtractor {
    private final String baseUrl;

    public PeertubeChannelTabExtractor(final StreamingService service,
                                       final ListLinkHandler handler) throws ParsingException {
        super(service, handler);
        baseUrl = getBaseUrl();
    }

    @Override public void onFetchPage(final Downloader downloader) { }

    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final String url = baseUrl + PeertubeChannelLinkHandlerFactory.API_ENDPOINT + getId()
                + getUrlSuffix(getName()) + "?" + START_KEY + "=0&"
                + COUNT_KEY + "=" + ITEMS_PER_PAGE;
        return getPage(new Page(url));
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Page URL is required");
        }
        final Response response = getDownloader().get(page.getUrl());
        if (response == null || response.responseBody().isBlank()) {
            throw new ExtractionException("PeerTube returned no channel tab data");
        }
        final JsonObject data;
        try {
            data = JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException error) {
            throw new ParsingException("Invalid PeerTube channel tab response", error);
        }
        PeertubeParsingHelper.validate(data);

        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        collectItemsFrom(items, data, baseUrl);
        final Page next = PeertubeParsingHelper.getNextPage(page.getUrl(), data.getLong("total"));
        return new InfoItemsPage<>(items, next);
    }
}
