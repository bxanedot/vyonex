// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.comments.CommentsExtractor;
import com.vyonex.internal.fallback.comments.CommentsInfoItem;
import com.vyonex.internal.fallback.comments.CommentsInfoItemsCollector;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.COUNT_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.START_KEY;

public final class PeertubeCommentsExtractor extends CommentsExtractor {
    private static final String CHILDREN = "children";
    private Boolean reply;

    public PeertubeCommentsExtractor(final StreamingService service, final ListLinkHandler handler) {
        super(service, handler);
    }

    @Override public InfoItemsPage<CommentsInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final String url = isReply() ? getUrl()
                : getUrl() + "?" + START_KEY + "=0&" + COUNT_KEY + "=" + ITEMS_PER_PAGE;
        return getPage(new Page(url));
    }

    @Override public InfoItemsPage<CommentsInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Page URL is required");
        }

        final JsonObject json;
        final boolean embedded = page.getBody() != null;
        if (embedded) {
            try {
                json = JsonParser.object().from(new String(page.getBody(), StandardCharsets.UTF_8));
            } catch (final JsonParserException e) {
                throw new ParsingException("Invalid embedded comment replies", e);
            }
        } else {
            final Response response = getDownloader().get(page.getUrl());
            if (response == null || response.responseBody().isBlank()) {
                throw new ExtractionException("PeerTube returned no comment data");
            }
            try {
                json = JsonParser.object().from(response.responseBody());
            } catch (final JsonParserException e) {
                throw new ParsingException("Invalid comment response", e);
            }
        }

        PeertubeParsingHelper.validate(json);
        final CommentsInfoItemsCollector collector = new CommentsInfoItemsCollector(getServiceId());
        final boolean replies = embedded || json.has(CHILDREN) || isReply();
        final long total;
        if (replies) {
            final JsonArray entries = json.getArray(CHILDREN);
            collectReplies(collector, entries);
            total = entries.size();
        } else {
            final JsonArray entries = json.getArray("data", null);
            if (entries == null) throw new ParsingException("Comment response has no data array");
            collectComments(collector, entries);
            total = json.getLong("total", entries.size());
        }

        final Page next = replies ? null : PeertubeParsingHelper.getNextPage(page.getUrl(), total);
        return new InfoItemsPage<>(collector, next);
    }

    private void collectComments(final CommentsInfoItemsCollector collector,
                                 final JsonArray entries) throws ParsingException {
        for (final Object value : entries) {
            if (value instanceof JsonObject) {
                final JsonObject item = (JsonObject) value;
                if (!item.getBoolean("isDeleted")) {
                    collector.commit(new PeertubeCommentsInfoItemExtractor(
                            item, null, getUrl(), getBaseUrl(), false));
                }
            }
        }
    }

    private void collectReplies(final CommentsInfoItemsCollector collector,
                                final JsonArray entries) throws ParsingException {
        reply = true;
        for (final Object value : entries) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject entry = (JsonObject) value;
            final JsonObject item = entry.getObject("comment", null);
            if (item != null && !item.getBoolean("isDeleted")) {
                collector.commit(new PeertubeCommentsInfoItemExtractor(item,
                        entry.getArray(CHILDREN), getUrl(), getBaseUrl(), true));
            }
        }
    }

    private boolean isReply() throws ParsingException {
        if (reply == null) reply = getOriginalUrl().contains("/comment-threads/");
        return reply;
    }

    @Override public void onFetchPage(final Downloader downloader) { }
}
