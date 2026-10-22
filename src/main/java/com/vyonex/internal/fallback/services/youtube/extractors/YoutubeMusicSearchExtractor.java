// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

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
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
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
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.utils.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class YoutubeMusicSearchExtractor extends SearchExtractor {
    private JsonObject initialData;

    private List<JsonObject> cachedItemSectionRendererContents;

    public YoutubeMusicSearchExtractor(final StreamingService service,
                                       final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String url = "https://music.youtube.com/youtubei/v1/search?"
                + DISABLE_PRETTY_PRINT_PARAMETER;

        final String params;

        switch (getLinkHandler().getContentFilters().get(0)) {
            case MUSIC_SONGS:
                params = "Eg-KAQwIARAAGAAgACgAMABqChAEEAUQAxAKEAk%3D";
                break;
            case MUSIC_VIDEOS:
                params = "Eg-KAQwIABABGAAgACgAMABqChAEEAUQAxAKEAk%3D";
                break;
            case MUSIC_ALBUMS:
                params = "Eg-KAQwIABAAGAEgACgAMABqChAEEAUQAxAKEAk%3D";
                break;
            case MUSIC_PLAYLISTS:
                params = "Eg-KAQwIABAAGAAgACgBMABqChAEEAUQAxAKEAk%3D";
                break;
            case MUSIC_ARTISTS:
                params = "Eg-KAQwIABAAGAAgASgAMABqChAEEAUQAxAKEAk%3D";
                break;
            default:
                params = null;
                break;
        }

        // @formatter:off
        final byte[] json = JsonWriter.string()
            .object()
                .object("context")
                    .object("client")
                        .value("clientName", "WEB_REMIX")
                        .value("clientVersion", getYoutubeMusicClientVersion())
                        .value("hl", "en-GB")
                        .value("gl", getExtractorContentCountry().getCountryCode())
                        .value("platform", "DESKTOP")
                        .value("utcOffsetMinutes", 0)
                    .end()
                    .object("request")
                        .array("internalExperimentFlags")
                        .end()
                        .value("useSsl", true)
                    .end()
                    .object("user")
                        // TODO: provide a way to enable restricted mode with:
                        //  .value("enableSafetyMode", boolean)
                        .value("lockedSafetyMode", false)
                    .end()
                .end()
                .value("query", getSearchString())
                .value("params", params)
            .end().done().getBytes(StandardCharsets.UTF_8);
        // @formatter:on

        final String responseBody = getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, getYoutubeMusicHeaders(), json));

        try {
            initialData = JsonParser.object().from(responseBody);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse JSON", e);
        }
    }

    private List<JsonObject> getItemSectionRendererContents() {
        if (cachedItemSectionRendererContents != null) {
            return cachedItemSectionRendererContents;
        }

        cachedItemSectionRendererContents = initialData
                .getObject("contents")
                .getObject("tabbedSearchResultsRenderer")
                .getArray("tabs")
                .getObject(0)
                .getObject("tabRenderer")
                .getObject("content")
                .getObject("sectionListRenderer")
                .getArray("contents")
                .streamAsJsonObjects()
                .map(c -> c.getObject("itemSectionRenderer"))
                .filter(isr -> !isr.isEmpty())
                .map(isr -> isr
                        .getArray("contents")
                        .getObject(0))
                .collect(Collectors.toList());
        return cachedItemSectionRendererContents;
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() throws ParsingException {
        for (final JsonObject obj : getItemSectionRendererContents()) {
            final JsonObject didYouMeanRenderer = obj
                    .getObject("didYouMeanRenderer");

            if (!didYouMeanRenderer.isEmpty()) {
                return getTextFromObject(didYouMeanRenderer.getObject("correctedQuery"));
            }

            // NOTE: As of 2025-07 "showing results for ..." doesn't seem to be returned by
            // the backend anymore, however the code is still present in the JS frontend.
            final JsonObject showingResultsForRenderer = obj
                .getObject("showingResultsForRenderer");
            if (!showingResultsForRenderer.isEmpty()) {
                return JsonUtils.getString(showingResultsForRenderer,
                        "correctedQueryEndpoint.searchEndpoint.query");
            }
        }

        return "";
    }

    @Override
    public boolean isCorrectedSearch() throws ParsingException {
        return getItemSectionRendererContents()
                .stream()
                .anyMatch(obj -> obj.has("showingResultsForRenderer"));
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

        final JsonArray contents = JsonUtils.getArray(JsonUtils.getArray(initialData,
                "contents.tabbedSearchResultsRenderer.tabs").getObject(0),
                "tabRenderer.content.sectionListRenderer.contents");

        Page nextPage = null;

        for (final Object content : contents) {
            if (((JsonObject) content).has("musicShelfRenderer")) {
                final JsonObject musicShelfRenderer = ((JsonObject) content)
                        .getObject("musicShelfRenderer");

                collectMusicStreamsFrom(collector, musicShelfRenderer.getArray("contents"));

                nextPage = getNextPageFrom(musicShelfRenderer.getArray("continuations"));
            }
        }

        return new InfoItemsPage<>(collector, nextPage);
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }

        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

        // @formatter:off
        final byte[] json = JsonWriter.string()
            .object()
                .object("context")
                    .object("client")
                        .value("clientName", "WEB_REMIX")
                        .value("clientVersion", getYoutubeMusicClientVersion())
                        .value("hl", "en-GB")
                        .value("gl", getExtractorContentCountry().getCountryCode())
                        .value("platform", "DESKTOP")
                        .value("utcOffsetMinutes", 0)
                    .end()
                    .object("request")
                        .array("internalExperimentFlags")
                        .end()
                        .value("useSsl", true)
                    .end()
                    .object("user")
                        // TODO: provide a way to enable restricted mode with:
                        //  .value("enableSafetyMode", boolean)
                        .value("lockedSafetyMode", false)
                    .end()
                .end()
            .end().done().getBytes(StandardCharsets.UTF_8);
        // @formatter:on

        final String responseBody = getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(
                        page.getUrl(), getYoutubeMusicHeaders(), json));

        final JsonObject ajaxJson;
        try {
            ajaxJson = JsonParser.object().from(responseBody);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse JSON", e);
        }

        final JsonObject musicShelfContinuation = ajaxJson.getObject("continuationContents")
                .getObject("musicShelfContinuation");

        collectMusicStreamsFrom(collector, musicShelfContinuation.getArray("contents"));
        final JsonArray continuations = musicShelfContinuation.getArray("continuations");

        return new InfoItemsPage<>(collector, getNextPageFrom(continuations));
    }

    private void collectMusicStreamsFrom(final MultiInfoItemsCollector collector,
                                         @Nonnull final JsonArray videos) {
        final String searchType = getLinkHandler().getContentFilters().get(0);
        videos.streamAsJsonObjects()
                .map(item -> item.getObject("musicResponsiveListItemRenderer", null))
                .filter(Objects::nonNull)
                .forEachOrdered(infoItem -> {
                    final String displayPolicy = infoItem.getString(
                            "musicItemRendererDisplayPolicy", "");
                    if (displayPolicy.equals("MUSIC_ITEM_RENDERER_DISPLAY_POLICY_GREY_OUT")) {
                        // No info about URL available
                        return;
                    }

                    final JsonArray descriptionElements = infoItem.getArray("flexColumns")
                            .getObject(1)
                            .getObject("musicResponsiveListItemFlexColumnRenderer")
                            .getObject("text")
                            .getArray("runs");

                    switch (searchType) {
                        case MUSIC_SONGS:
                        case MUSIC_VIDEOS:
                            collector.commit(new YoutubeMusicSongOrVideoInfoItemExtractor(
                                    infoItem, descriptionElements, searchType));
                            break;
                        case MUSIC_ARTISTS:
                            collector.commit(new YoutubeMusicArtistInfoItemExtractor(infoItem));
                            break;
                        case MUSIC_ALBUMS:
                        case MUSIC_PLAYLISTS:
                            collector.commit(new YoutubeMusicAlbumOrPlaylistInfoItemExtractor(
                                    infoItem, descriptionElements, searchType));
                            break;
                    }
                });
    }

    @Nullable
    private Page getNextPageFrom(final JsonArray continuations) {
        if (isNullOrEmpty(continuations)) {
            return null;
        }

        final JsonObject nextContinuationData = continuations.getObject(0)
                .getObject("nextContinuationData");
        final String continuation = nextContinuationData.getString("continuation");

        return new Page("https://music.youtube.com/youtubei/v1/search?ctoken=" + continuation
                + "&continuation=" + continuation + "&" + DISABLE_PRETTY_PRINT_PARAMETER);
    }
}
