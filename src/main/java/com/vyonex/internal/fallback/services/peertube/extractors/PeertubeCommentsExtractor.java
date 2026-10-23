// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.comments.CommentsExtractor;
import com.vyonex.internal.fallback.comments.CommentsInfoItem;
import com.vyonex.internal.fallback.comments.CommentsInfoItemsCollector;
import com.vyonex.internal.fallback.downloader.Downloader;
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

    public PeertubeCommentsExtractor(final StreamingService service, final ListLinkHandler handler) {
        super(service, handler);
    }

    @Override
    public InfoItemsPage<CommentsInfoItem> getInitialPage() throws IOException, ExtractionException {
        final String url = isReply() ? getUrl()
                : getUrl() + "?" + START_KEY + "=0&" + COUNT_KEY + "=" + ITEMS_PER_PAGE;
        return getPage(new Page(url));
    }

    @Override
    public InfoItemsPage<CommentsInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Page URL is required");
        }

        final boolean embedded = page.getBody() != null;
        final JsonObject data = embedded ? parseEmbedded(page) : fetch(page.getUrl());
        PeertubeParsingHelper.validate(data);

        final CommentsInfoItemsCollector items = new CommentsInfoItemsCollector(getServiceId());
        final boolean isReplyPage = embedded || page.getId() != null
                || data.has(CHILDREN) || isReply();
        final Page next;
        if (isReplyPage) {
            final JsonArray children = data.getArray(CHILDREN);
            collectReplies(items, children);
            next = null;
        } else {
            final JsonArray comments = data.getArray("data", null);
            if (comments == null) throw new ParsingException("Comment response has no data array");
            collectComments(items, comments);
            final long total = data.getLong("total", comments.size());
            next = PeertubeParsingHelper.getNextPage(page.getUrl(), total);
        }
        return new InfoItemsPage<>(items, next);
    }

    private JsonObject fetch(final String url) throws IOException, ExtractionException {
        return PeertubeParsingHelper.readApiObject(getDownloader().get(url), "comment data");
    }

    private static JsonObject parseEmbedded(final Page page) throws ParsingException {
        try {
            return JsonParser.object().from(new String(page.getBody(), StandardCharsets.UTF_8));
        } catch (final JsonParserException error) {
            throw new ParsingException("Invalid embedded comment replies", error);
        }
    }

    private void collectComments(final CommentsInfoItemsCollector items,
                                 final JsonArray comments) throws ParsingException {
        for (final Object value : comments) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject comment = (JsonObject) value;
            if (!comment.getBoolean("isDeleted")) {
                items.commit(new PeertubeCommentsInfoItemExtractor(
                        comment, null, getUrl(), getBaseUrl(), false));
            }
        }
    }

    private void collectReplies(final CommentsInfoItemsCollector items,
                                final JsonArray replies) throws ParsingException {
        for (final Object value : replies) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject node = (JsonObject) value;
            final JsonObject comment = node.getObject("comment", null);
            if (comment != null && !comment.getBoolean("isDeleted")) {
                items.commit(new PeertubeCommentsInfoItemExtractor(
                        comment, node.getArray(CHILDREN), getUrl(), getBaseUrl(), true));
            }
        }
    }

    private boolean isReply() throws ParsingException {
        return getOriginalUrl().contains("/comment-threads/");
    }

    @Override public void onFetchPage(final Downloader downloader) { }
}
