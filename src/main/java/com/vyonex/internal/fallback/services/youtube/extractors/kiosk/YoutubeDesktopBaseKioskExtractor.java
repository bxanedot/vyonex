// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors.kiosk;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getClientVersion;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.services.youtube.YoutubeChannelHelper;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeStreamInfoItemExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeStreamInfoItemLockupExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class YoutubeDesktopBaseKioskExtractor extends KioskExtractor<StreamInfoItem> {
    private static final String VIDEO_TAB_PARAMS = "4gIOGgxtb3N0X3BvcHVsYXI%3D";

    private final String browseId;
    private final String params;
    private YoutubeChannelHelper.ChannelResponseData response;

    public YoutubeDesktopBaseKioskExtractor(final StreamingService service,
                                            final ListLinkHandler linkHandler,
                                            final String kioskId,
                                            final String browseId,
                                            final String params) {
        super(service, linkHandler, kioskId);
        this.browseId = browseId;
        this.params = params;
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        response = YoutubeChannelHelper.getChannelResponse(browseId, params,
                getExtractorLocalization(), getExtractorContentCountry());
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return YoutubeChannelHelper.getChannelName(
                YoutubeChannelHelper.getChannelHeader(response.jsonResponse),
                null, response.jsonResponse);
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        final JsonObject tab = selectedTab();
        final JsonObject content = tab.getObject("content");
        final String tabParams = tab.getObject("endpoint")
                .getObject("browseEndpoint").getString("params", "");
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        final TimeAgoParser dates = getTimeAgoParser();

        if (content.has("richGridRenderer")) {
            return collect(content.getObject("richGridRenderer").getArray("contents"),
                    response.jsonResponse.getObject("responseContext").getString("visitorData"),
                    dates, items);
        }
        if (content.has("sectionListRenderer")) {
            final List<JsonObject> shelves = findShelves(
                    content.getObject("sectionListRenderer").getArray("contents"),
                    VIDEO_TAB_PARAMS.equals(tabParams));
            final JsonArray entries = new JsonArray();
            for (final JsonObject shelf : shelves) {
                for (final JsonObject entry : shelfItems(shelf).streamAsJsonObjects().toList()) {
                    entries.add(entry);
                }
            }
            return collect(entries,
                    response.jsonResponse.getObject("responseContext").getString("visitorData"),
                    dates, items);
        }
        return new InfoItemsPage<>(items, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getBody() == null) {
            throw new IllegalArgumentException("Continuation page must contain a request body");
        }
        final JsonObject result = getJsonPostResponse("browse", page.getBody(),
                getExtractorLocalization());
        JsonArray entries = new JsonArray();
        for (final JsonObject action : result.getArray("onResponseReceivedActions")
                .streamAsJsonObjects().toList()) {
            if (action.has("appendContinuationItemsAction")) {
                entries = action.getObject("appendContinuationItemsAction")
                        .getArray("continuationItems");
                break;
            }
        }
        return collect(entries, page.getId(), getTimeAgoParser(),
                new StreamInfoItemsCollector(getServiceId()));
    }

    private JsonObject selectedTab() throws ParsingException {
        JsonObject firstWithContent = null;
        for (final JsonObject tab : response.jsonResponse.getObject("contents")
                .getObject("twoColumnBrowseResultsRenderer").getArray("tabs")
                .streamAsJsonObjects().toList()) {
            final JsonObject renderer = tab.getObject("tabRenderer");
            if (!renderer.has("content")) continue;
            if (firstWithContent == null) firstWithContent = renderer;
            if (renderer.getBoolean("selected")) return renderer;
        }
        if (firstWithContent != null) return firstWithContent;
        throw new ParsingException("YouTube kiosk response has no tab content");
    }

    private static List<JsonObject> findShelves(final JsonArray sections,
                                                final boolean videoTab) {
        final List<JsonObject> found = new ArrayList<>();
        for (final JsonObject section : sections.streamAsJsonObjects().toList()) {
            final JsonArray sectionItems = section.getObject("itemSectionRenderer").getArray("contents");
            for (final JsonObject item : sectionItems.streamAsJsonObjects().toList()) {
                if (!item.has("shelfRenderer")) continue;
                final JsonObject shelf = item.getObject("shelfRenderer");
                final JsonArray videos = shelfItems(shelf);
                if (videos.isEmpty()) continue;
                if (videoTab) {
                    found.add(shelf);
                    return found;
                }
                if (!shelf.has("title")) found.add(shelf);
            }
        }
        return found;
    }

    private static JsonArray shelfItems(final JsonObject shelf) {
        final JsonObject content = shelf.getObject("content");
        if (content.has("gridRenderer")) return content.getObject("gridRenderer").getArray("items");
        if (content.has("expandedShelfContentsRenderer")) {
            return content.getObject("expandedShelfContentsRenderer").getArray("items");
        }
        return new JsonArray();
    }

    private InfoItemsPage<StreamInfoItem> collect(final JsonArray entries,
                                                  @Nullable final String visitorData,
                                                  final TimeAgoParser dates,
                                                  final StreamInfoItemsCollector collector)
            throws IOException, ExtractionException {
        for (final JsonObject entry : entries.streamAsJsonObjects().toList()) {
            if (entry.has("gridVideoRenderer")) {
                collector.commit(new YoutubeStreamInfoItemExtractor(
                        entry.getObject("gridVideoRenderer"), dates));
            } else if (entry.has("videoRenderer")) {
                collector.commit(new YoutubeStreamInfoItemExtractor(
                        entry.getObject("videoRenderer"), dates));
            } else if (entry.has("richItemRenderer")) {
                final JsonObject video = entry.getObject("richItemRenderer")
                        .getObject("content").getObject("videoRenderer");
                if (!video.isEmpty()) collector.commit(new YoutubeStreamInfoItemExtractor(video, dates));
            } else if (entry.has("lockupViewModel")) {
                final JsonObject lockup = entry.getObject("lockupViewModel");
                if ("LOCKUP_CONTENT_TYPE_VIDEO".equals(lockup.getString("contentType"))) {
                    collector.commit(new YoutubeStreamInfoItemLockupExtractor(lockup, dates));
                }
            }
        }
        final Page next = nextPage(entries, visitorData);
        return new InfoItemsPage<>(collector, next);
    }

    @Nullable
    private Page nextPage(final JsonArray entries, @Nullable final String visitorData)
            throws IOException, ExtractionException {
        if (entries.isEmpty()) return null;
        final JsonObject last = entries.getObject(entries.size() - 1);
        if (!last.has("continuationItemRenderer")) return null;
        final String token = last.getObject("continuationItemRenderer")
                .getObject("continuationEndpoint").getObject("continuationCommand")
                .getString("token");
        if (token == null || token.isBlank()) return null;

        final JsonObject client = new JsonObject();
        client.put("clientName", "WEB");
        client.put("clientVersion", getClientVersion());
        client.put("clientScreen", "WATCH");
        client.put("platform", "DESKTOP");
        client.put("hl", getExtractorLocalization().getLocalizationCode());
        client.put("gl", getExtractorContentCountry().getCountryCode());
        client.put("utcOffsetMinutes", 0);
        if (visitorData != null) client.put("visitorData", visitorData);

        final JsonObject context = new JsonObject();
        context.put("client", client);
        final JsonObject request = new JsonObject();
        request.put("internalExperimentFlags", new JsonArray());
        request.put("useSsl", true);
        final JsonObject user = new JsonObject();
        user.put("lockedSafetyMode", false);
        context.put("request", request);
        context.put("user", user);
        final JsonObject body = new JsonObject();
        body.put("context", context);
        body.put("continuation", token);
        return new Page(YOUTUBEI_V1_URL + "browse?" + DISABLE_PRETTY_PRINT_PARAMETER,
                visitorData, null, null, JsonWriter.string(body).getBytes(StandardCharsets.UTF_8));
    }
}
