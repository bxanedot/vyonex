// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.comments.CommentsExtractor;
import com.vyonex.internal.fallback.comments.CommentsInfoItem;
import com.vyonex.internal.fallback.comments.CommentsInfoItemsCollector;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;

public final class SoundcloudCommentsExtractor extends CommentsExtractor {
    public SoundcloudCommentsExtractor(final StreamingService service, final ListLinkHandler handler) {
        super(service, handler);
    }

    @Override public InfoItemsPage<CommentsInfoItem> getInitialPage()
            throws ExtractionException, IOException {
        return readPage(getUrl());
    }

    @Override public InfoItemsPage<CommentsInfoItem> getPage(final Page page)
            throws ExtractionException, IOException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            return InfoItemsPage.emptyPage();
        }
        return readPage(page.getUrl());
    }

    @Override public void onFetchPage(final com.vyonex.internal.fallback.downloader.Downloader downloader) { }

    private InfoItemsPage<CommentsInfoItem> readPage(final String url)
            throws IOException, ExtractionException {
        final Response response = VyonEx.getDownloader().get(url);
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud comments request failed with HTTP "
                    + response.responseCode());
        }
        final JsonObject data;
        try {
            data = JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid SoundCloud comments response", e);
        }
        final CommentsInfoItemsCollector items = new CommentsInfoItemsCollector(getServiceId());
        for (final Object value : data.getArray("collection")) {
            if (value instanceof JsonObject) {
                items.commit(new SoundcloudCommentsInfoItemExtractor((JsonObject) value, getUrl()));
            }
        }
        final String next = data.getString("next_href");
        return new InfoItemsPage<>(items, next == null || next.isBlank() ? null : new Page(next));
    }
}
