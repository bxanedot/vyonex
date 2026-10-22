// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.extractCookieValue;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistId;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getYouTubeHeaders;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;
import static com.vyonex.internal.fallback.utils.Utils.getQueryValue;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;
import static com.vyonex.internal.fallback.utils.Utils.stringToURL;

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonBuilder;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@link YoutubePlaylistExtractor} for a mix (auto-generated playlist).
 * It handles URLs in the format of
 * {@code youtube.com/watch?v=videoId&list=playlistId}
 */
public class YoutubeMixPlaylistExtractor extends PlaylistExtractor {
    private static final List<ImageSuffix> IMAGE_URL_SUFFIXES_AND_RESOLUTIONS = List.of(
            // sqdefault and maxresdefault image resolutions are not available on all
            // videos, so don't add them in the list of available resolutions
            new ImageSuffix("default.jpg", 90, 120, ResolutionLevel.LOW),
            new ImageSuffix("mqdefault.jpg", 180, 320, ResolutionLevel.MEDIUM),
            new ImageSuffix("hqdefault.jpg", 360, 480, ResolutionLevel.MEDIUM));

    /**
     * YouTube identifies mixes based on this cookie. With this information it can generate
     * continuations without duplicates.
     */
    public static final String COOKIE_NAME = "VISITOR_INFO1_LIVE";

    private JsonObject initialData;
    private JsonObject playlistData;
    private String cookieValue;

    public YoutubeMixPlaylistExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final Localization localization = getExtractorLocalization();
        final URL url = stringToURL(getUrl());
        final String mixPlaylistId = getId();
        final String videoId = getQueryValue(url, "v");
        final String playlistIndexString = getQueryValue(url, "index");

        final JsonBuilder<JsonObject> jsonBody = prepareDesktopJsonBuilder(localization,
                getExtractorContentCountry()).value("playlistId", mixPlaylistId);
        if (videoId != null) {
            jsonBody.value("videoId", videoId);
        }
        if (playlistIndexString != null) {
            jsonBody.value("playlistIndex", Integer.parseInt(playlistIndexString));
        }

        final byte[] body = JsonWriter.string(jsonBody.done()).getBytes(StandardCharsets.UTF_8);

        // Cookie is required due to consent
        final var headers = getYouTubeHeaders();

        final Response response = getDownloader().postWithContentTypeJson(
                YOUTUBEI_V1_URL + "next?" + DISABLE_PRETTY_PRINT_PARAMETER, headers, body,
                localization);

        initialData = JsonUtils.toJsonObject(getValidJsonResponseBody(response));
        playlistData = initialData
                .getObject("contents")
                .getObject("twoColumnWatchNextResults")
                .getObject("playlist")
                .getObject("playlist");
        if (isNullOrEmpty(playlistData)) {
            final ExtractionException ex = new ExtractionException("Could not get playlistData");
            if (!YoutubeParsingHelper.isConsentAccepted()) {
                throw new ContentNotAvailableException(
                        "Consent is required in some countries to view Mix playlists",
                        ex);
            }
            throw ex;
        }
        cookieValue = extractCookieValue(COOKIE_NAME, response);
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        final String name = YoutubeParsingHelper.getTextAtKey(playlistData, "title");
        if (isNullOrEmpty(name)) {
            throw new ParsingException("Could not get playlist name");
        }
        return name;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        try {
            return getThumbnailsFromPlaylistId(playlistData.getString("playlistId"));
        } catch (final Exception e) {
            try {
                // Fallback to thumbnail of current video. Always the case for channel mixes
                return getThumbnailsFromVideoId(initialData.getObject("currentVideoEndpoint")
                        .getObject("watchEndpoint").getString("videoId"));
            } catch (final Exception ignored) {
            }

            throw new ParsingException("Could not get playlist thumbnails", e);
        }
    }

    @Override
    public String getUploaderUrl() {
        // YouTube mixes are auto-generated by YouTube
        return "";
    }

    @Override
    public String getUploaderName() {
        // YouTube mixes are auto-generated by YouTube
        return "YouTube";
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        // YouTube mixes are auto-generated by YouTube
        return List.of();
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() {
        // Auto-generated playlists always start with 25 videos and are endless
        return ListExtractor.ITEM_COUNT_INFINITE;
    }

    @Nonnull
    @Override
    public Description getDescription() throws ParsingException {
        return Description.EMPTY_DESCRIPTION;
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        collectStreamsFrom(collector, playlistData.getArray("contents"));

        final Map<String, String> cookies = new HashMap<>();
        cookies.put(COOKIE_NAME, cookieValue);

        return new InfoItemsPage<>(collector, getNextPageFrom(playlistData, cookies));
    }

    @Nonnull
    private Page getNextPageFrom(@Nonnull final JsonObject playlistJson,
                                 final Map<String, String> cookies)
            throws IOException, ExtractionException {
        final JsonObject lastStream = ((JsonObject) playlistJson.getArray("contents")
                .get(playlistJson.getArray("contents").size() - 1));
        if (lastStream == null || lastStream.getObject("playlistPanelVideoRenderer") == null) {
            throw new ExtractionException("Could not extract next page url");
        }

        final JsonObject watchEndpoint = lastStream.getObject("playlistPanelVideoRenderer")
                .getObject("navigationEndpoint").getObject("watchEndpoint");
        final String playlistId = watchEndpoint.getString("playlistId");
        final String videoId = watchEndpoint.getString("videoId");
        final int index = watchEndpoint.getInt("index");
        final String params = watchEndpoint.getString("params");
        final byte[] body = JsonWriter.string(prepareDesktopJsonBuilder(getExtractorLocalization(),
                getExtractorContentCountry())
                .value("videoId", videoId)
                .value("playlistId", playlistId)
                .value("playlistIndex", index)
                .value("params", params)
                .done())
                .getBytes(StandardCharsets.UTF_8);

        return new Page(YOUTUBEI_V1_URL + "next?" + DISABLE_PRETTY_PRINT_PARAMETER, null, null,
                cookies, body);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) throws IOException,
            ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }
        if (!page.getCookies().containsKey(COOKIE_NAME)) {
            throw new IllegalArgumentException("Cookie '" + COOKIE_NAME + "' is missing");
        }

        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        // Cookie is required due to consent
        final var headers = getYouTubeHeaders();

        final Response response = getDownloader().postWithContentTypeJson(page.getUrl(), headers,
                page.getBody(), getExtractorLocalization());
        final JsonObject ajaxJson = JsonUtils.toJsonObject(getValidJsonResponseBody(response));
        final JsonObject playlistJson = ajaxJson.getObject("contents")
                .getObject("twoColumnWatchNextResults").getObject("playlist").getObject("playlist");
        final JsonArray allStreams = playlistJson.getArray("contents");
        // Sublist because YouTube returns up to 24 previous streams in the mix
        // +1 because the stream of "currentIndex" was already extracted in previous request
        final List<Object> newStreams =
                allStreams.subList(playlistJson.getInt("currentIndex") + 1, allStreams.size());

        collectStreamsFrom(collector, newStreams);
        return new InfoItemsPage<>(collector, getNextPageFrom(playlistJson, page.getCookies()));
    }

    private void collectStreamsFrom(@Nonnull final StreamInfoItemsCollector collector,
                                    @Nullable final List<Object> streams) {
        if (streams == null) {
            return;
        }

        final TimeAgoParser timeAgoParser = getTimeAgoParser();

        new JsonArray(streams).streamAsJsonObjects()
                .map(stream -> {
                    final var renderer = stream.getObject("playlistPanelVideoRenderer");
                    return new YoutubeStreamInfoItemExtractor(renderer, timeAgoParser);
                })
                .forEachOrdered(collector::commit);
    }

    @Nonnull
    private List<Image> getThumbnailsFromPlaylistId(@Nonnull final String playlistId)
            throws ParsingException {
        return getThumbnailsFromVideoId(YoutubeParsingHelper.extractVideoIdFromMixId(playlistId));
    }

    @Nonnull
    private List<Image> getThumbnailsFromVideoId(@Nonnull final String videoId) {
        final String baseUrl = "https://i.ytimg.com/vi/" + videoId + "/";
        return IMAGE_URL_SUFFIXES_AND_RESOLUTIONS.stream()
                .map(imageSuffix -> new Image(baseUrl + imageSuffix.getSuffix(),
                        imageSuffix.getHeight(), imageSuffix.getWidth(),
                        imageSuffix.getResolutionLevel()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Nonnull
    @Override
    public PlaylistInfo.PlaylistType getPlaylistType() throws ParsingException {
        return extractPlaylistTypeFromPlaylistId(playlistData.getString("playlistId"));
    }
}
