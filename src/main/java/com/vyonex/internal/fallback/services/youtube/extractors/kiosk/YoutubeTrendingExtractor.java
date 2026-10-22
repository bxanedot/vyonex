/*
 * Created by Christian Schabesberger on 12.08.17.
 *
 * Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeTrendingExtractor.java is part of VyonEx.
 *
 * VyonEx is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VyonEx is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VyonEx. If not, see <https://www.gnu.org/licenses/>.
 */

// bxane
package com.vyonex.internal.fallback.services.youtube.extractors.kiosk;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextAtKey;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public class YoutubeTrendingExtractor extends KioskExtractor<StreamInfoItem> {

    public static final String KIOSK_ID = "Trending";

    private JsonObject initialData;

    private static final String VIDEOS_TAB_PARAMS = "4gIOGgxtb3N0X3BvcHVsYXI%3D";

    public YoutubeTrendingExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler,
                                    final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        // @formatter:off
        final byte[] body = JsonWriter.string(prepareDesktopJsonBuilder(getExtractorLocalization(),
                getExtractorContentCountry())
                .value("browseId", "FEtrending")
                .value("params", VIDEOS_TAB_PARAMS)
                .done())
                .getBytes(StandardCharsets.UTF_8);
        // @formatter:on

        initialData = getJsonPostResponse("browse", body, getExtractorLocalization());
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        final JsonObject header = initialData.getObject("header");
        String name = null;
        if (header.has("feedTabbedHeaderRenderer")) {
            name = getTextAtKey(header.getObject("feedTabbedHeaderRenderer"), "title");
        } else if (header.has("c4TabbedHeaderRenderer")) {
            name = getTextAtKey(header.getObject("c4TabbedHeaderRenderer"), "title");
        } else if (header.has("pageHeaderRenderer")) {
            name = getTextAtKey(header.getObject("pageHeaderRenderer"), "pageTitle");
        }

        if (isNullOrEmpty(name)) {
            throw new ParsingException("Could not get Trending name");
        }
        return name;
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws ParsingException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        final TimeAgoParser timeAgoParser = getTimeAgoParser();
        final JsonObject tab = getTrendingTab();
        final JsonObject tabContent = tab.getObject("content");
        final boolean isVideoTab = tab.getObject("endpoint").getObject("browseEndpoint")
                .getString("params", "").equals(VIDEOS_TAB_PARAMS);

        if (tabContent.has("richGridRenderer")) {
            tabContent.getObject("richGridRenderer")
                    .getArray("contents")
                    .streamAsJsonObjects()
                    // Filter Trending shorts and Recently trending sections
                    .filter(content -> content.has("richItemRenderer"))
                    .map(content -> content.getObject("richItemRenderer")
                            .getObject("content")
                            .getObject("videoRenderer"))
                    .forEachOrdered(videoRenderer -> collector.commit(
                            new YoutubeStreamInfoItemExtractor(videoRenderer, timeAgoParser)));
        } else if (tabContent.has("sectionListRenderer")) {
            final Stream<JsonObject> shelves = tabContent.getObject("sectionListRenderer")
                    .getArray("contents")
                    .streamAsJsonObjects()
                    .flatMap(content -> content.getObject("itemSectionRenderer")
                            .getArray("contents")
                            .streamAsJsonObjects())
                    .map(content -> content.getObject("shelfRenderer"));

            final Stream<JsonObject> items;
            if (isVideoTab) {
                // The first shelf of the Videos tab contains the normal trends
                items = shelves.findFirst().stream();
            } else {
                // Filter Trending shorts and Recently trending sections which have a title,
                // contrary to normal trends
                items = shelves.filter(shelfRenderer -> !shelfRenderer.has("title"));
            }

            items.flatMap(shelfRenderer -> shelfRenderer.getObject("content")
                            .getObject("expandedShelfContentsRenderer")
                            .getArray("items")
                            .streamAsJsonObjects())
                    .map(item -> item.getObject("videoRenderer"))
                    .forEachOrdered(videoRenderer -> collector.commit(
                            new YoutubeStreamInfoItemExtractor(videoRenderer, timeAgoParser)));
        }

        return new InfoItemsPage<>(collector, null);
    }

    private JsonObject getTrendingTab() throws ParsingException {
        return initialData.getObject("contents")
                .getObject("twoColumnBrowseResultsRenderer")
                .getArray("tabs")
                .streamAsJsonObjects()
                .map(tab -> tab.getObject("tabRenderer"))
                .filter(tabRenderer -> tabRenderer.getBoolean("selected"))
                .filter(tabRenderer -> tabRenderer.has("content"))
                // There should be at most one tab selected
                .findFirst()
                .orElseThrow(() ->
                        new ParsingException("Could not get \"Now\" or \"Videos\" trending tab"));
    }
}
