// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.feed.FeedExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.io.IOException;

public class YoutubeFeedExtractor extends FeedExtractor {
    private static final String CHANNEL_URL = "https://www.youtube.com/channel/";
    private Document document;

    public YoutubeFeedExtractor(final StreamingService service, final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String feedUrl = YoutubeParsingHelper.getFeedUrlFrom(getLinkHandler().getId());
        final Response response = downloader.get(feedUrl);
        if (response.responseCode() == 404) {
            throw new ContentNotAvailableException("Could not get feed: 404 - not found");
        }
        document = Jsoup.parse(response.responseBody());
    }

    @Nonnull @Override public InfoItemsPage<StreamInfoItem> getInitialPage() {
        if (document == null) throw new IllegalStateException("Feed was not fetched");
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        for (final Element entry : document.select("feed > entry")) {
            items.commit(new YoutubeFeedInfoItemExtractor(entry));
        }
        return new InfoItemsPage<>(items, null);
    }

    @Nonnull @Override public String getId() { return getUrl().replace(CHANNEL_URL, ""); }

    @Nonnull @Override public String getUrl() {
        final Element authorUri = document.selectFirst("feed > author > uri");
        if (authorUri != null && !authorUri.text().isEmpty()) return authorUri.text();
        final Element alternate = document.selectFirst("feed > link[rel*=alternate]");
        return alternate == null ? "" : alternate.attr("href");
    }

    @Nonnull @Override public String getName() {
        final Element name = document.selectFirst("feed > author > name");
        return name == null ? "" : name.text();
    }

    @Override public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }
}
