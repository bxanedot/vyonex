// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItem;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemsCollector;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import com.vyonex.internal.json.JsonWriter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL;

public class BandcampFeaturedExtractor extends KioskExtractor<PlaylistInfoItem> {
    public static final String KIOSK_FEATURED = "Featured";
    public static final String FEATURED_API_URL = BASE_API_URL + "/mobile/24/bootstrap_data";
    public static final String MORE_FEATURED_API_URL =
            BASE_API_URL + "/mobile/24/feed_older_logged_out";

    private JsonObject response;

    public BandcampFeaturedExtractor(final StreamingService service,
                                     final ListLinkHandler linkHandler,
                                     final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final byte[] body = JsonWriter.string().object().value("platform", "")
                .value("version", 0).end().done().getBytes(StandardCharsets.UTF_8);
        try {
            response = JsonParser.object().from(downloader.postWithContentTypeJson(
                    FEATURED_API_URL, Map.of(), body).responseBody());
        } catch (final JsonParserException e) {
            throw new ParsingException("Bandcamp featured response is invalid", e);
        }
    }

    @Nonnull
    @Override
    public String getName() { return KIOSK_FEATURED; }

    @Nonnull
    @Override
    public InfoItemsPage<PlaylistInfoItem> getInitialPage() {
        return extract(response.getObject("feed_content").getObject("stories")
                .getArray("featured"));
    }

    @Override
    public InfoItemsPage<PlaylistInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        try {
            response = JsonParser.object().from(getDownloader().get(page.getUrl()).responseBody());
        } catch (final JsonParserException e) {
            throw new ParsingException("Bandcamp featured page is invalid", e);
        }
        return extract(response.getObject("stories").getArray("featured"));
    }

    private InfoItemsPage<PlaylistInfoItem> extract(final JsonArray stories) {
        final PlaylistInfoItemsCollector items = new PlaylistInfoItemsCollector(getServiceId());
        JsonObject last = null;
        for (final Object value : stories) {
            if (!(value instanceof JsonObject story)) continue;
            last = story;
            if (story.getString("album_title") != null) {
                items.commit(new BandcampPlaylistInfoItemFeaturedExtractor(story));
            }
        }
        return new InfoItemsPage<>(items, last == null ? null : nextPage(last));
    }

    private Page nextPage(final JsonObject story) {
        final String type = story.getString("story_type");
        if (type == null || !story.isNumber("story_date") || !story.isNumber("ntid")) return null;
        final String cursor = "featured:" + story.getLong("story_date") + ':' + type + ':'
                + story.getLong("ntid");
        return new Page(MORE_FEATURED_API_URL + "?story_groups=" + cursor);
    }
}
