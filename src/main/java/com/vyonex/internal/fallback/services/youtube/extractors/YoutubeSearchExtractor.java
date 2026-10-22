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

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonBuilder;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

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
import com.vyonex.internal.fallback.services.youtube.YoutubeMetaInfoHelper;
import com.vyonex.internal.fallback.utils.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
 * Created by Christian Schabesberger on 22.07.2018
 *
 * Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeSearchExtractor.java is part of VyonEx.
 *
 * VyonEx is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VyonEx is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VyonEx.  If not, see <http://www.gnu.org/licenses/>.
 */

public class YoutubeSearchExtractor extends SearchExtractor {

    @Nullable
    private final String searchType;
    private final boolean extractVideoResults;
    private final boolean extractChannelResults;
    private final boolean extractPlaylistResults;

    private JsonObject initialData;

    public YoutubeSearchExtractor(final StreamingService service,
                                  final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
        final List<String> contentFilters = linkHandler.getContentFilters();
        searchType = isNullOrEmpty(contentFilters) ? null : contentFilters.get(0);
        // Save whether we should extract video, channel and playlist results depending on the
        // requested search type, as YouTube returns sometimes videos inside channel search results
        // If no search type is provided or ALL filter is requested, extract everything
        extractVideoResults = searchType == null || ALL.equals(searchType)
                || VIDEOS.equals(searchType);
        extractChannelResults = searchType == null || ALL.equals(searchType)
                || CHANNELS.equals(searchType);
        extractPlaylistResults = searchType == null || ALL.equals(searchType)
                || PLAYLISTS.equals(searchType);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws IOException,
            ExtractionException {
        final String query = super.getSearchString();
        final Localization localization = getExtractorLocalization();
        final String params = getSearchParameter(searchType);

        final JsonBuilder<JsonObject> jsonBody = prepareDesktopJsonBuilder(localization,
                getExtractorContentCountry())
                .value("query", query);
        if (!isNullOrEmpty(params)) {
            jsonBody.value("params", params);
        }

        final byte[] body = JsonWriter.string(jsonBody.done()).getBytes(StandardCharsets.UTF_8);

        initialData = getJsonPostResponse("search", body, localization);
    }

    @Nonnull
    @Override
    public String getUrl() throws ParsingException {
        return super.getUrl() + "&gl=" + getExtractorContentCountry().getCountryCode();
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() throws ParsingException {
        final JsonObject itemSectionRenderer = initialData.getObject("contents")
                .getObject("twoColumnSearchResultsRenderer")
                .getObject("primaryContents")
                .getObject("sectionListRenderer")
                .getArray("contents")
                .getObject(0)
                .getObject("itemSectionRenderer");
        final JsonObject didYouMeanRenderer = itemSectionRenderer.getArray("contents")
                .getObject(0)
                .getObject("didYouMeanRenderer");

        if (!didYouMeanRenderer.isEmpty()) {
            return JsonUtils.getString(didYouMeanRenderer,
                    "correctedQueryEndpoint.searchEndpoint.query");
        }

        return Objects.requireNonNullElse(
                getTextFromObject(itemSectionRenderer.getArray("contents")
                        .getObject(0)
                        .getObject("showingResultsForRenderer")
                        .getObject("correctedQuery")), "");
    }

    @Override
    public boolean isCorrectedSearch() {
        final JsonObject showingResultsForRenderer = initialData.getObject("contents")
                .getObject("twoColumnSearchResultsRenderer").getObject("primaryContents")
                .getObject("sectionListRenderer").getArray("contents").getObject(0)
                .getObject("itemSectionRenderer").getArray("contents").getObject(0)
                .getObject("showingResultsForRenderer");
        return !showingResultsForRenderer.isEmpty();
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() throws ParsingException {
        return YoutubeMetaInfoHelper.getMetaInfo(
                initialData.getObject("contents")
                        .getObject("twoColumnSearchResultsRenderer")
                        .getObject("primaryContents")
                        .getObject("sectionListRenderer")
                        .getArray("contents"));
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

        final JsonArray sections = initialData.getObject("contents")
                .getObject("twoColumnSearchResultsRenderer")
                .getObject("primaryContents")
                .getObject("sectionListRenderer")
                .getArray("contents");

        Page nextPage = null;

        for (final Object section : sections) {
            final JsonObject sectionJsonObject = (JsonObject) section;
            if (sectionJsonObject.has("itemSectionRenderer")) {
                final JsonObject itemSectionRenderer =
                        sectionJsonObject.getObject("itemSectionRenderer");

                collectStreamsFrom(collector, itemSectionRenderer.getArray("contents"));
            } else if (sectionJsonObject.has("continuationItemRenderer")) {
                nextPage = getNextPageFrom(
                        sectionJsonObject.getObject("continuationItemRenderer"));
            }
        }

        return new InfoItemsPage<>(collector, nextPage);
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page) throws IOException,
            ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }

        final Localization localization = getExtractorLocalization();
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

        // @formatter:off
        final byte[] json = JsonWriter.string(prepareDesktopJsonBuilder(localization,
                getExtractorContentCountry())
                .value("continuation", page.getId())
                .done())
                .getBytes(StandardCharsets.UTF_8);
        // @formatter:on

        final JsonObject ajaxJson = getJsonPostResponse("search", json, localization);

        final JsonArray continuationItems = ajaxJson.getArray("onResponseReceivedCommands")
                .getObject(0)
                .getObject("appendContinuationItemsAction")
                .getArray("continuationItems");

        final JsonArray contents = continuationItems.getObject(0)
                .getObject("itemSectionRenderer")
                .getArray("contents");
        collectStreamsFrom(collector, contents);

        return new InfoItemsPage<>(collector, getNextPageFrom(continuationItems.getObject(1)
                .getObject("continuationItemRenderer")));
    }

    private void collectStreamsFrom(final MultiInfoItemsCollector collector,
                                    @Nonnull final JsonArray contents)
            throws NothingFoundException {
        final TimeAgoParser timeAgoParser = getTimeAgoParser();

        for (final Object content : contents) {
            final JsonObject item = (JsonObject) content;
            if (item.has("backgroundPromoRenderer")) {
                throw new NothingFoundException(
                        getTextFromObject(item.getObject("backgroundPromoRenderer")
                                .getObject("bodyText")));
            } else if (item.has("videoRenderer") && extractVideoResults) {
                collector.commit(new YoutubeStreamInfoItemExtractor(
                        item.getObject("videoRenderer"), timeAgoParser));
            } else if (item.has("channelRenderer") && extractChannelResults) {
                collector.commit(new YoutubeChannelInfoItemExtractor(
                        item.getObject("channelRenderer")));
            } else if (item.has("playlistRenderer") && extractPlaylistResults) {
                collector.commit(new YoutubePlaylistInfoItemExtractor(
                        item.getObject("playlistRenderer")));
            } else if (item.has("showRenderer") && extractPlaylistResults) {
                collector.commit(new YoutubeShowRendererInfoItemExtractor(
                        item.getObject("showRenderer")));
            } else if (item.has("lockupViewModel")) {
                final JsonObject lockupViewModel = item.getObject("lockupViewModel");
                final String contentType = lockupViewModel.getString("contentType");
                if (("LOCKUP_CONTENT_TYPE_PLAYLIST".equals(contentType)
                        || "LOCKUP_CONTENT_TYPE_PODCAST".equals(contentType))
                        && extractPlaylistResults) {
                    collector.commit(
                            new YoutubeMixOrPlaylistLockupInfoItemExtractor(lockupViewModel));
                } else if ("LOCKUP_CONTENT_TYPE_VIDEO".equals(contentType)
                        && extractVideoResults) {
                    collector.commit(new YoutubeStreamInfoItemLockupExtractor(
                            lockupViewModel, timeAgoParser));
                }
            }
        }
    }

    @Nullable
    private Page getNextPageFrom(final JsonObject continuationItemRenderer) {
        if (isNullOrEmpty(continuationItemRenderer)) {
            return null;
        }

        final String token = continuationItemRenderer.getObject("continuationEndpoint")
                .getObject("continuationCommand")
                .getString("token");

        final String url = YOUTUBEI_V1_URL + "search?" + DISABLE_PRETTY_PRINT_PARAMETER;

        return new Page(url, token);
    }
}
