// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.ALL;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.CHANNELS;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.PLAYLISTS;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.VIDEOS;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.getSearchParameter;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.youtube.YoutubeContinuationTokenParser;
import com.vyonex.internal.fallback.services.youtube.YoutubeMetaInfoHelper;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonBuilder;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class YoutubeSearchExtractor extends SearchExtractor {
    @Nullable
    private final String searchType;
    private final boolean extractVideoResults;
    private final boolean extractChannelResults;
    private final boolean extractPlaylistResults;
    private JsonObject response;

    public YoutubeSearchExtractor(final StreamingService service,
                                  final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
        final List<String> filters = linkHandler.getContentFilters();
        searchType = isNullOrEmpty(filters) ? null : filters.get(0);
        extractVideoResults = searchType == null || ALL.equals(searchType)
                || VIDEOS.equals(searchType);
        extractChannelResults = searchType == null || ALL.equals(searchType)
                || CHANNELS.equals(searchType);
        extractPlaylistResults = searchType == null || ALL.equals(searchType)
                || PLAYLISTS.equals(searchType);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final Localization localization = getExtractorLocalization();
        final JsonBuilder<JsonObject> body = prepareDesktopJsonBuilder(
                localization, getExtractorContentCountry()).value("query", getSearchString());
        final String params = getSearchParameter(searchType);
        if (!isNullOrEmpty(params)) body.value("params", params);
        response = getJsonPostResponse("search",
                JsonWriter.string(body.done()).getBytes(StandardCharsets.UTF_8), localization);
    }

    @Nonnull
    @Override
    public String getUrl() throws ParsingException {
        return super.getUrl() + "&gl=" + getExtractorContentCountry().getCountryCode();
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() throws ParsingException {
        final JsonObject first = firstSectionItems().getObject(0);
        final String suggestion = first.getObject("didYouMeanRenderer")
                .getObject("correctedQueryEndpoint").getObject("searchEndpoint")
                .getString("query");
        if (suggestion != null) return suggestion;
        final String corrected = getTextFromObject(first.getObject("showingResultsForRenderer")
                .getObject("correctedQuery"));
        return corrected == null ? "" : corrected;
    }

    @Override
    public boolean isCorrectedSearch() throws ParsingException {
        return !firstSectionItems().getObject(0)
                .getObject("showingResultsForRenderer").isEmpty();
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() throws ParsingException {
        return YoutubeMetaInfoHelper.getMetaInfo(searchSections());
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        Page next = null;
        for (final JsonObject section : searchSections().streamAsJsonObjects().toList()) {
            if (section.has("itemSectionRenderer")) {
                collectItems(items, section.getObject("itemSectionRenderer").getArray("contents"));
            } else if (section.has("continuationItemRenderer")) {
                next = nextPage(section.getObject("continuationItemRenderer"));
            }
        }
        return new InfoItemsPage<>(items, next);
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain a URL");
        }
        final Localization localization = getExtractorLocalization();
        final JsonBuilder<JsonObject> body = prepareDesktopJsonBuilder(
                localization, getExtractorContentCountry()).value("continuation", page.getId());
        final JsonObject nextResponse = getJsonPostResponse("search",
                JsonWriter.string(body.done()).getBytes(StandardCharsets.UTF_8), localization);
        final JsonArray continuationItems = nextResponse.getArray("onResponseReceivedCommands")
                .getObject(0).getObject("appendContinuationItemsAction")
                .getArray("continuationItems");
        final JsonArray contents = continuationItems.getObject(0)
                .getObject("itemSectionRenderer").getArray("contents");
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        collectItems(items, contents);
        return new InfoItemsPage<>(items,
                nextPage(continuationItems.getObject(1).getObject("continuationItemRenderer")));
    }

    private JsonArray searchSections() {
        return response.getObject("contents").getObject("twoColumnSearchResultsRenderer")
                .getObject("primaryContents").getObject("sectionListRenderer")
                .getArray("contents");
    }

    private JsonArray firstSectionItems() {
        return searchSections().getObject(0).getObject("itemSectionRenderer")
                .getArray("contents");
    }

    private void collectItems(final MultiInfoItemsCollector items, final JsonArray contents)
            throws NothingFoundException {
        final TimeAgoParser dates = getTimeAgoParser();
        for (final JsonObject entry : contents.streamAsJsonObjects().toList()) {
            if (entry.has("backgroundPromoRenderer")) {
                throw new NothingFoundException(getTextFromObject(
                        entry.getObject("backgroundPromoRenderer").getObject("bodyText")));
            }
            if (entry.has("videoRenderer") && extractVideoResults) {
                items.commit(new YoutubeStreamInfoItemExtractor(
                        entry.getObject("videoRenderer"), dates));
            } else if (entry.has("channelRenderer") && extractChannelResults) {
                items.commit(new YoutubeChannelInfoItemExtractor(entry.getObject("channelRenderer")));
            } else if (entry.has("playlistRenderer") && extractPlaylistResults) {
                items.commit(new YoutubePlaylistInfoItemExtractor(
                        entry.getObject("playlistRenderer")));
            } else if (entry.has("showRenderer") && extractPlaylistResults) {
                items.commit(new YoutubeShowRendererInfoItemExtractor(entry.getObject("showRenderer")));
            } else if (entry.has("lockupViewModel")) {
                collectLockup(items, entry.getObject("lockupViewModel"), dates);
            }
        }
    }

    private void collectLockup(final MultiInfoItemsCollector items,
                               final JsonObject lockup,
                               final TimeAgoParser dates) {
        final String type = lockup.getString("contentType", "");
        if (("LOCKUP_CONTENT_TYPE_PLAYLIST".equals(type)
                || "LOCKUP_CONTENT_TYPE_PODCAST".equals(type)) && extractPlaylistResults) {
            items.commit(new YoutubeMixOrPlaylistLockupInfoItemExtractor(lockup));
        } else if ("LOCKUP_CONTENT_TYPE_VIDEO".equals(type) && extractVideoResults) {
            items.commit(new YoutubeStreamInfoItemLockupExtractor(lockup, dates));
        }
    }

    @Nullable
    private Page nextPage(final JsonObject continuation) {
        if (continuation.isEmpty()) return null;
        final String token = YoutubeContinuationTokenParser.INSTANCE.parse(JsonWriter.string(continuation));
        if (isNullOrEmpty(token)) return null;
        return new Page(YOUTUBEI_V1_URL + "search?" + DISABLE_PRETTY_PRINT_PARAMETER, token);
    }
}
