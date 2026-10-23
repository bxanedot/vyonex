// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.utils.JsonUtils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import com.vyonex.internal.json.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getYoutubeMusicClientVersion;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getYoutubeMusicHeaders;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS;

/** Fetches and maps typed YouTube Music search results. */
public class YoutubeMusicSearchExtractor extends SearchExtractor {
    private static final String SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?"
            + DISABLE_PRETTY_PRINT_PARAMETER;
    private JsonObject searchData;

    public YoutubeMusicSearchExtractor(final StreamingService service,
                                       final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final JsonObject body = requestContext();
        body.put("query", getSearchString());
        final String params = paramsFor(getLinkHandler().getContentFilters().get(0));
        if (params != null) body.put("params", params);
        searchData = parseResponse(getValidJsonResponseBody(getDownloader().postWithContentTypeJson(
                SEARCH_URL, getYoutubeMusicHeaders(), JsonWriter.string(body)
                        .getBytes(StandardCharsets.UTF_8))));
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() throws ParsingException {
        for (final JsonObject section : itemSections()) {
            final JsonObject correction = section.getObject("didYouMeanRenderer");
            if (!correction.isEmpty()) {
                final String suggestion = getTextFromObject(correction.getObject("correctedQuery"));
                return suggestion == null ? "" : suggestion;
            }
            final JsonObject showing = section.getObject("showingResultsForRenderer");
            if (!showing.isEmpty()) {
                return JsonUtils.getString(showing,
                        "correctedQueryEndpoint.searchEndpoint.query");
            }
        }
        return "";
    }

    @Override
    public boolean isCorrectedSearch() throws ParsingException {
        return itemSections().stream().anyMatch(section -> section.has("showingResultsForRenderer"));
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() {
        return List.of();
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        final JsonArray sections = JsonUtils.getArray(searchData,
                "contents.tabbedSearchResultsRenderer.tabs").getObject(0)
                .getObject("tabRenderer").getObject("content").getObject("sectionListRenderer")
                .getArray("contents");
        Page nextPage = null;
        for (final JsonObject section : sections.streamAsJsonObjects().toList()) {
            final JsonObject shelf = section.getObject("musicShelfRenderer");
            if (shelf.isEmpty()) continue;
            collectItems(collector, shelf.getArray("contents"));
            nextPage = nextPageFrom(shelf.getArray("continuations"));
        }
        return new InfoItemsPage<>(collector, nextPage);
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Music continuation URL is missing");
        }
        final JsonObject response = parseResponse(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(page.getUrl(), getYoutubeMusicHeaders(),
                        JsonWriter.string(requestContext()).getBytes(StandardCharsets.UTF_8))));
        final JsonObject shelf = response.getObject("continuationContents")
                .getObject("musicShelfContinuation");
        if (shelf.isEmpty()) throw new ParsingException("Music continuation data is missing");

        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        collectItems(collector, shelf.getArray("contents"));
        return new InfoItemsPage<>(collector, nextPageFrom(shelf.getArray("continuations")));
    }

    private void collectItems(@Nonnull final MultiInfoItemsCollector collector,
                              @Nonnull final JsonArray results) {
        final String searchType = getLinkHandler().getContentFilters().get(0);
        for (final JsonObject result : results.streamAsJsonObjects().toList()) {
            final JsonObject item = result.getObject("musicResponsiveListItemRenderer", null);
            if (item == null || "MUSIC_ITEM_RENDERER_DISPLAY_POLICY_GREY_OUT"
                    .equals(item.getString("musicItemRendererDisplayPolicy"))) continue;

            final StreamInfoItemExtractor stream = streamItem(item, searchType);
            if (stream != null) {
                collector.commit(stream);
            } else if (MUSIC_ARTISTS.equals(searchType)) {
                collector.commit(new YoutubeMusicArtistInfoItemExtractor(item));
            } else if (MUSIC_ALBUMS.equals(searchType) || MUSIC_PLAYLISTS.equals(searchType)) {
                final JsonArray details = item.getArray("flexColumns").getObject(1)
                        .getObject("musicResponsiveListItemFlexColumnRenderer")
                        .getObject("text").getArray("runs");
                collector.commit(new YoutubeMusicAlbumOrPlaylistInfoItemExtractor(
                        item, details, searchType));
            }
        }
    }

    private static StreamInfoItemExtractor streamItem(final JsonObject item,
                                                       final String searchType) {
        if (!MUSIC_SONGS.equals(searchType) && !MUSIC_VIDEOS.equals(searchType)) return null;
        final JsonArray columns = item.getArray("flexColumns");
        final JsonArray details = columns.size() < 2 ? new JsonArray()
                : columns.getObject(1).getObject("musicResponsiveListItemFlexColumnRenderer")
                        .getObject("text").getArray("runs");
        return new YoutubeMusicSongOrVideoInfoItemExtractor(item, details, searchType);
    }

    @Nonnull
    private JsonObject requestContext() throws IOException, ExtractionException {
        final JsonObject client = new JsonObject();
        client.put("clientName", "WEB_REMIX");
        client.put("clientVersion", getYoutubeMusicClientVersion());
        client.put("hl", "en-GB");
        client.put("gl", getExtractorContentCountry().getCountryCode());
        client.put("platform", "DESKTOP");
        client.put("utcOffsetMinutes", 0);
        final JsonObject request = new JsonObject();
        request.put("internalExperimentFlags", new JsonArray());
        request.put("useSsl", true);
        final JsonObject user = new JsonObject();
        user.put("lockedSafetyMode", false);
        final JsonObject context = new JsonObject();
        context.put("client", client);
        context.put("request", request);
        context.put("user", user);
        final JsonObject body = new JsonObject();
        body.put("context", context);
        return body;
    }

    @Nullable
    private Page nextPageFrom(final JsonArray continuations) {
        for (final JsonObject continuation : continuations.streamAsJsonObjects().toList()) {
            final String token = continuation.getObject("nextContinuationData")
                    .getString("continuation");
            if (token != null && !token.isBlank()) {
                return new Page("https://music.youtube.com/youtubei/v1/search?ctoken=" + token
                        + "&continuation=" + token + "&" + DISABLE_PRETTY_PRINT_PARAMETER);
            }
        }
        return null;
    }

    private List<JsonObject> itemSections() throws ParsingException {
        final JsonArray contents = JsonUtils.getArray(searchData,
                "contents.tabbedSearchResultsRenderer.tabs").getObject(0)
                .getObject("tabRenderer").getObject("content").getObject("sectionListRenderer")
                .getArray("contents");
        final List<JsonObject> result = new ArrayList<>();
        for (final JsonObject content : contents.streamAsJsonObjects().toList()) {
            final JsonObject section = content.getObject("itemSectionRenderer");
            if (section.isEmpty()) continue;
            final JsonObject first = section.getArray("contents").getObject(0);
            if (!first.isEmpty()) result.add(first);
        }
        return result;
    }

    private static JsonObject parseResponse(final String body) throws ParsingException {
        try {
            return JsonParser.object().from(body);
        } catch (final JsonParserException error) {
            throw new ParsingException("Could not parse Music search response", error);
        }
    }

    @Nullable
    private static String paramsFor(final String filter) {
        return switch (filter) {
            case MUSIC_SONGS -> "Eg-KAQwIARAAGAAgACgAMABqChAEEAUQAxAKEAk%3D";
            case MUSIC_VIDEOS -> "Eg-KAQwIABABGAAgACgAMABqChAEEAUQAxAKEAk%3D";
            case MUSIC_ALBUMS -> "Eg-KAQwIABAAGAEgACgAMABqChAEEAUQAxAKEAk%3D";
            case MUSIC_PLAYLISTS -> "Eg-KAQwIABAAGAAgACgBMABqChAEEAUQAxAKEAk%3D";
            case MUSIC_ARTISTS -> "Eg-KAQwIABAAGAAgASgAMABqChAEEAUQAxAKEAk%3D";
            default -> null;
        };
    }
}
