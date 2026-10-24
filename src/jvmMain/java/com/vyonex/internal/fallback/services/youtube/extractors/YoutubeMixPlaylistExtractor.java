// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Image.ResolutionLevel;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.fallback.utils.ImageSuffix;
import com.vyonex.internal.fallback.utils.JsonUtils;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonBuilder;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.extractCookieValue;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistId;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getYouTubeHeaders;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;

/** Loads and paginates YouTube-generated mix playlists. */
public class YoutubeMixPlaylistExtractor extends PlaylistExtractor {
    private static final List<ImageSuffix> THUMBNAILS = List.of(
            new ImageSuffix("default.jpg", 90, 120, ResolutionLevel.LOW),
            new ImageSuffix("mqdefault.jpg", 180, 320, ResolutionLevel.MEDIUM),
            new ImageSuffix("hqdefault.jpg", 360, 480, ResolutionLevel.MEDIUM));
    public static final String COOKIE_NAME = "VISITOR_INFO1_LIVE";

    private JsonObject initialData;
    private JsonObject playlistData;
    private String visitorCookie = "";

    public YoutubeMixPlaylistExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final Localization localization = getExtractorLocalization();
        final URL requestUrl = Utils.stringToURL(getUrl());
        final JsonBuilder<JsonObject> body = prepareDesktopJsonBuilder(localization,
                getExtractorContentCountry()).value("playlistId", getId());

        final String videoId = Utils.getQueryValue(requestUrl, "v");
        if (videoId != null && !videoId.isBlank()) body.value("videoId", videoId);
        final String index = Utils.getQueryValue(requestUrl, "index");
        if (index != null) {
            try {
                final int value = Integer.parseInt(index);
                if (value < 0) throw new NumberFormatException("index is negative");
                body.value("playlistIndex", value);
            } catch (final NumberFormatException error) {
                throw new ParsingException("Playlist index is invalid", error);
            }
        }

        final byte[] requestBody = JsonWriter.string(body.done()).getBytes(StandardCharsets.UTF_8);
        final Response response = getDownloader().postWithContentTypeJson(
                YOUTUBEI_V1_URL + "next?" + DISABLE_PRETTY_PRINT_PARAMETER,
                getYouTubeHeaders(), requestBody, localization);
        initialData = JsonUtils.toJsonObject(getValidJsonResponseBody(response));
        playlistData = getPlaylistData(initialData);
        if (playlistData.isEmpty()) {
            final ExtractionException cause = new ExtractionException("Mix response has no playlist data");
            if (!YoutubeParsingHelper.isConsentAccepted()) {
                throw new ContentNotAvailableException(
                        "Consent is required in some countries to view Mix playlists", cause);
            }
            throw cause;
        }
        visitorCookie = extractCookieValue(COOKIE_NAME, response);
    }

    @Override
    public String getName() throws ParsingException {
        final String title = YoutubeParsingHelper.getTextAtKey(playlistData, "title");
        if (title == null || title.isBlank()) throw new ParsingException("Mix title is missing");
        return title;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        final String playlistId = playlistData.getString("playlistId");
        try {
            if (playlistId == null || playlistId.isBlank()) {
                throw new ParsingException("Mix playlist ID is missing");
            }
            return thumbnailsForVideo(YoutubeParsingHelper.extractVideoIdFromMixId(playlistId));
        } catch (final ParsingException playlistFailure) {
            final String videoId = initialData.getObject("currentVideoEndpoint")
                    .getObject("watchEndpoint").getString("videoId");
            if (videoId != null && !videoId.isBlank()) return thumbnailsForVideo(videoId);
            throw new ParsingException("Could not read mix thumbnails", playlistFailure);
        }
    }

    @Override public String getUploaderUrl() { return ""; }
    @Override public String getUploaderName() { return "YouTube"; }
    @Nonnull @Override public List<Image> getUploaderAvatars() { return List.of(); }
    @Override public boolean isUploaderVerified() { return false; }
    @Override public long getStreamCount() { return ListExtractor.ITEM_COUNT_INFINITE; }
    @Nonnull @Override public Description getDescription() { return Description.EMPTY_DESCRIPTION; }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        collectStreams(collector, playlistData.getArray("contents"));
        return new InfoItemsPage<>(collector, nextPageFrom(playlistData,
                Map.of(COOKIE_NAME, visitorCookie)));
    }

    @Nullable
    private Page nextPageFrom(@Nonnull final JsonObject playlist,
                              @Nonnull final Map<String, String> cookies)
            throws IOException, ExtractionException {
        final JsonArray streams = playlist.getArray("contents");
        if (streams.isEmpty()) return null;
        final JsonObject lastRenderer = streams.getObject(streams.size() - 1)
                .getObject("playlistPanelVideoRenderer", null);
        if (lastRenderer == null) return null;

        final JsonObject endpoint = lastRenderer.getObject("navigationEndpoint")
                .getObject("watchEndpoint");
        final String videoId = endpoint.getString("videoId");
        final String playlistId = endpoint.getString("playlistId");
        final int index = endpoint.getInt("index", -1);
        if (videoId == null || videoId.isBlank() || playlistId == null || playlistId.isBlank()
                || index < 0) {
            throw new ExtractionException("Last mix item has no continuation route");
        }

        final JsonBuilder<JsonObject> body = prepareDesktopJsonBuilder(getExtractorLocalization(),
                getExtractorContentCountry()).value("videoId", videoId)
                .value("playlistId", playlistId).value("playlistIndex", index);
        final String params = endpoint.getString("params");
        if (params != null && !params.isBlank()) body.value("params", params);
        final byte[] bytes = JsonWriter.string(body.done()).getBytes(StandardCharsets.UTF_8);
        return new Page(YOUTUBEI_V1_URL + "next?" + DISABLE_PRETTY_PRINT_PARAMETER,
                null, null, cookies, bytes);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Mix continuation URL is missing");
        }
        final String cookie = page.getCookies() == null ? null : page.getCookies().get(COOKIE_NAME);
        if (cookie == null || cookie.isBlank()) {
            throw new IllegalArgumentException("Mix continuation cookie is missing");
        }

        final Map<String, List<String>> headers = new HashMap<>(getYouTubeHeaders());
        final List<String> existingCookies = headers.getOrDefault("Cookie", List.of());
        final String consentCookie = existingCookies.isEmpty() ? "" : existingCookies.get(0);
        headers.put("Cookie", List.of(consentCookie + "; " + COOKIE_NAME + "=" + cookie));
        final Response response = getDownloader().postWithContentTypeJson(page.getUrl(),
                headers, page.getBody(), getExtractorLocalization());
        final JsonObject data = JsonUtils.toJsonObject(getValidJsonResponseBody(response));
        final JsonObject playlist = getPlaylistData(data);
        if (playlist.isEmpty()) throw new ParsingException("Continuation has no playlist data");

        final JsonArray streams = playlist.getArray("contents");
        final int currentIndex = playlist.getInt("currentIndex", -1);
        if (currentIndex < -1 || currentIndex >= streams.size()) {
            throw new ParsingException("Mix continuation index is outside its result list");
        }
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        collectStreams(collector, new JsonArray(streams.subList(currentIndex + 1, streams.size())));
        return new InfoItemsPage<>(collector, nextPageFrom(playlist, page.getCookies()));
    }

    private void collectStreams(@Nonnull final StreamInfoItemsCollector collector,
                                @Nonnull final JsonArray streams) {
        final TimeAgoParser dates = getTimeAgoParser();
        for (final JsonObject stream : streams.streamAsJsonObjects().toList()) {
            final JsonObject renderer = stream.getObject("playlistPanelVideoRenderer", null);
            if (renderer != null) collector.commit(new YoutubeStreamInfoItemExtractor(renderer, dates));
        }
    }

    private static JsonObject getPlaylistData(@Nonnull final JsonObject response) {
        return response.getObject("contents").getObject("twoColumnWatchNextResults")
                .getObject("playlist").getObject("playlist");
    }

    @Nonnull
    private static List<Image> thumbnailsForVideo(@Nonnull final String videoId) {
        final String prefix = "https://i.ytimg.com/vi/" + videoId + "/";
        return THUMBNAILS.stream().map(suffix -> new Image(prefix + suffix.getSuffix(),
                suffix.getHeight(), suffix.getWidth(), suffix.getResolutionLevel())).toList();
    }

    @Override
    public PlaylistInfo.PlaylistType getPlaylistType() throws ParsingException {
        return extractPlaylistTypeFromPlaylistId(playlistData.getString("playlistId"));
    }
}
