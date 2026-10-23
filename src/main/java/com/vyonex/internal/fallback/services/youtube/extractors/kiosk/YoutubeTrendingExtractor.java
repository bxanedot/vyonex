// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors.kiosk;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextAtKey;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeStreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public final class YoutubeTrendingExtractor extends KioskExtractor<StreamInfoItem> {
    public static final String KIOSK_ID = "Trending";
    private static final String VIDEOS_TAB_PARAMS = "4gIOGgxtb3N0X3BvcHVsYXI%3D";

    private JsonObject response;

    public YoutubeTrendingExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler,
                                    final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final JsonObject body = prepareDesktopJsonBuilder(
                getExtractorLocalization(), getExtractorContentCountry())
                .value("browseId", "FEtrending")
                .value("params", VIDEOS_TAB_PARAMS)
                .done();
        response = getJsonPostResponse("browse",
                JsonWriter.string(body).getBytes(StandardCharsets.UTF_8), getExtractorLocalization());
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        final JsonObject header = response.getObject("header");
        String name = getTextAtKey(header.getObject("feedTabbedHeaderRenderer"), "title");
        if (name == null || name.isBlank()) {
            name = getTextAtKey(header.getObject("c4TabbedHeaderRenderer"), "title");
        }
        if (name == null || name.isBlank()) {
            name = getTextAtKey(header.getObject("pageHeaderRenderer"), "pageTitle");
        }
        if (name == null || name.isBlank()) {
            throw new ParsingException("Could not get Trending name");
        }
        return name;
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws ParsingException {
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        final TimeAgoParser dates = getTimeAgoParser();
        final JsonObject tab = getSelectedTab();
        final JsonObject content = tab.getObject("content");
        final boolean videosTab = VIDEOS_TAB_PARAMS.equals(tab.getObject("endpoint")
                .getObject("browseEndpoint").getString("params"));

        if (content.has("richGridRenderer")) {
            addRichGridVideos(content.getObject("richGridRenderer").getArray("contents"),
                    dates, items);
        } else if (content.has("sectionListRenderer")) {
            addShelfVideos(content.getObject("sectionListRenderer").getArray("contents"),
                    videosTab, dates, items);
        }
        return new InfoItemsPage<>(items, null);
    }

    private JsonObject getSelectedTab() throws ParsingException {
        final JsonArray tabs = response.getObject("contents")
                .getObject("twoColumnBrowseResultsRenderer").getArray("tabs");
        for (final JsonObject tab : tabs.streamAsJsonObjects().toList()) {
            final JsonObject renderer = tab.getObject("tabRenderer");
            if (renderer.getBoolean("selected") && renderer.has("content")) return renderer;
        }
        throw new ParsingException("Could not find the selected Trending tab");
    }

    private static void addRichGridVideos(final JsonArray contents,
                                          final TimeAgoParser dates,
                                          final StreamInfoItemsCollector items) {
        for (final JsonObject entry : contents.streamAsJsonObjects().toList()) {
            if (!entry.has("richItemRenderer")) continue;
            final JsonObject video = entry.getObject("richItemRenderer")
                    .getObject("content").getObject("videoRenderer");
            items.commit(new YoutubeStreamInfoItemExtractor(video, dates));
        }
    }

    private static void addShelfVideos(final JsonArray sections,
                                       final boolean videosTab,
                                       final TimeAgoParser dates,
                                       final StreamInfoItemsCollector items) {
        final List<JsonObject> shelves = new ArrayList<>();
        for (final JsonObject section : sections.streamAsJsonObjects().toList()) {
            for (final JsonObject content : section.getObject("itemSectionRenderer")
                    .getArray("contents").streamAsJsonObjects().toList()) {
                final JsonObject shelf = content.getObject("shelfRenderer");
                if (videosTab ? shelves.isEmpty() : !shelf.has("title")) shelves.add(shelf);
            }
            if (videosTab && !shelves.isEmpty()) break;
        }

        for (final JsonObject shelf : shelves) {
            final JsonArray videos = shelf.getObject("content")
                    .getObject("expandedShelfContentsRenderer").getArray("items");
            for (final JsonObject item : videos.streamAsJsonObjects().toList()) {
                items.commit(new YoutubeStreamInfoItemExtractor(
                        item.getObject("videoRenderer"), dates));
            }
        }
    }
}
