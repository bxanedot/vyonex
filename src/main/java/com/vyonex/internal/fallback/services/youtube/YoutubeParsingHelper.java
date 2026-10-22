/*
 * Created by Christian Schabesberger on 02.03.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeParsingHelper.java is part of VyonEx.
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
package com.vyonex.internal.fallback.services.youtube;

import static com.vyonex.internal.fallback.VyonEx.getDownloader;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.ANDROID_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.DESKTOP_CLIENT_PLATFORM;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.IOS_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.IOS_DEVICE_MODEL;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.IOS_USER_AGENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.VISIONOS_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.VISIONOS_DEVICE_MODEL;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.VISIONOS_USER_AGENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_CLIENT_ID;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_CLIENT_NAME;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_HARDCODED_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_REMIX_CLIENT_ID;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_REMIX_CLIENT_NAME;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_REMIX_HARDCODED_CLIENT_VERSION;
import static com.vyonex.internal.fallback.utils.Utils.HTTP;
import static com.vyonex.internal.fallback.utils.Utils.HTTPS;
import static com.vyonex.internal.fallback.utils.Utils.getStringResultFromRegexArray;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonBuilder;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import com.vyonex.internal.json.JsonWriter;

import org.jsoup.nodes.Entities;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Image.ResolutionLevel;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.AccountTerminatedException;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.fallback.stream.AudioTrackType;
import com.vyonex.internal.fallback.utils.JsonUtils;
import com.vyonex.internal.fallback.utils.Parser;
import com.vyonex.internal.fallback.utils.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class YoutubeParsingHelper {

    private YoutubeParsingHelper() {
    }

    /**
     * The base URL of requests of the {@code WEB} clients to the InnerTube internal API.
     */
    public static final String YOUTUBEI_V1_URL = "https://www.youtube.com/youtubei/v1/";

    /**
     * The base URL of requests of non-web clients to the InnerTube internal API.
     */
    public static final String YOUTUBEI_V1_GAPIS_URL =
            "https://youtubei.googleapis.com/youtubei/v1/";

    /**
     * The base URL of YouTube Music.
     */
    private static final String YOUTUBE_MUSIC_URL = "https://music.youtube.com";

    /**
     * A parameter to disable pretty-printed response of InnerTube requests, to reduce response
     * sizes.
     *
     * <p>
     * Sent in query parameters of the requests.
     * </p>
     **/
    public static final String DISABLE_PRETTY_PRINT_PARAMETER = "prettyPrint=false";

    /**
     * A parameter sent by official clients named {@code contentPlaybackNonce}.
     *
     * <p>
     * It is sent by official clients on videoplayback requests and InnerTube player requests in
     * most cases.
     * </p>
     *
     * <p>
     * It is composed of 16 characters which are generated from
     * {@link #CONTENT_PLAYBACK_NONCE_ALPHABET this alphabet}, with the use of strong random
     * values.
     * </p>
     *
     * @see #generateContentPlaybackNonce()
     */
    public static final String CPN = "cpn";
    public static final String VIDEO_ID = "videoId";

    /**
     * A parameter sent by official clients named {@code contentCheckOk}.
     *
     * <p>
     * Setting it to {@code true} allows us to get streaming data on videos with a warning about
     * what the sensible content they contain.
     * </p>
     */
    public static final String CONTENT_CHECK_OK = "contentCheckOk";

    /**
     * A parameter which may be sent by official clients named {@code racyCheckOk}.
     *
     * <p>
     * What this parameter does is not really known, but it seems to be linked to sensitive
     * contents such as age-restricted content.
     * </p>
     */
    public static final String RACY_CHECK_OK = "racyCheckOk";

    private static String clientVersion;
    private static String youtubeMusicClientVersion;
    private static boolean clientVersionExtracted;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Boolean> hardcodedClientVersionValid = Optional.empty();

    private static final String[] INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES =
            {"INNERTUBE_CONTEXT_CLIENT_VERSION\":\"([0-9\\.]+?)\"",
                    "innertube_context_client_version\":\"([0-9\\.]+?)\"",
                    "client.version=([0-9\\.]+)"};
    private static final String[] INITIAL_DATA_REGEXES =
            {"window\\[\"ytInitialData\"\\]\\s*=\\s*(\\{.*?\\});",
                    "var\\s*ytInitialData\\s*=\\s*(\\{.*?\\});"};

    private static final String CONTENT_PLAYBACK_NONCE_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private static Random numberGenerator = new Random();

    private static final String FEED_BASE_CHANNEL_ID =
            "https://www.youtube.com/feeds/videos.xml?channel_id=";
    private static final String FEED_BASE_USER = "https://www.youtube.com/feeds/videos.xml?user=";
    private static boolean consentAccepted = false;

    public static boolean isGoogleURL(final String url) {
        final String cachedUrl = extractCachedUrlIfNeeded(url);
        if (cachedUrl == null) return false;
        try {
            final String host = new URI(cachedUrl).getHost();
            if (host == null) return false;
            String domain = host.toLowerCase(Locale.ROOT);
            if (domain.startsWith("www.")) domain = domain.substring(4);
            if (domain.startsWith("m.")) domain = domain.substring(2);
            return domain.startsWith("google.");
        } catch (final URISyntaxException | IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isYoutubeURL(@Nonnull final URL url) {
        final String host = url.getHost().toLowerCase(Locale.ROOT);
        return host.equals("youtube.com") || host.equals("www.youtube.com")
                || host.equals("m.youtube.com") || host.equals("music.youtube.com");
    }

    public static boolean isYoutubeServiceURL(@Nonnull final URL url) {
        final String host = url.getHost();
        return host.equalsIgnoreCase("www.youtube-nocookie.com")
                || host.equalsIgnoreCase("youtu.be");
    }

    public static boolean isHooktubeURL(@Nonnull final URL url) {
        return url.getHost().equalsIgnoreCase("hooktube.com");
    }

    public static boolean isInvidiousURL(@Nonnull final URL url) {
        final String host = url.getHost().toLowerCase(Locale.ROOT);
        return host.contains("invidious") || host.contains("piped")
                || host.equals("yewtu.be") || host.endsWith(".yewtu.be");
    }

    public static boolean isY2ubeURL(@Nonnull final URL url) {
        return url.getHost().equalsIgnoreCase("y2u.be");
    }

    /**
     * Parses the duration string of the video expecting ":" or "." as separators
     *
     * @return the duration in seconds
     * @throws ParsingException when more than 3 separators are found
     */
    public static int parseDurationString(@Nonnull final String input)
            throws ParsingException, NumberFormatException {
        if (input == null) {
            throw new ParsingException("Duration is missing");
        }
        final String value = input.trim();
        if (value.equalsIgnoreCase("SHORTS")) {
            return 0;
        }
        if (value.isEmpty() || (value.contains(":") && value.contains("."))) {
            throw new ParsingException("Unsupported duration: " + input);
        }

        final String[] parts = value.split(value.contains(":") ? ":" : "\\.", -1);
        if (parts.length == 0 || parts.length > 4) {
            throw new ParsingException("Unsupported duration: " + input);
        }

        final long[] multipliers = parts.length == 4
                ? new long[]{24L * 60 * 60, 60L * 60, 60, 1}
                : parts.length == 3
                ? new long[]{60L * 60, 60, 1}
                : parts.length == 2 ? new long[]{60, 1} : new long[]{1};
        long seconds = 0;
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            if (!part.matches("\\d+")) {
                throw new ParsingException("Invalid duration component: " + part);
            }
            final long component = Long.parseLong(part);
            if (component > Integer.MAX_VALUE) {
                throw new ParsingException("Duration component is too large: " + part);
            }
            seconds += component * multipliers[i];
        }
        if (seconds > Integer.MAX_VALUE) {
            throw new ParsingException("Duration is too large: " + input);
        }
        return (int) seconds;
    }

    @Nonnull
    public static String getFeedUrlFrom(@Nonnull final String channelIdOrUser) {
        if (channelIdOrUser.startsWith("user/")) {
            return FEED_BASE_USER + channelIdOrUser.substring("user/".length());
        } else if (channelIdOrUser.startsWith("channel/")) {
            return FEED_BASE_CHANNEL_ID + channelIdOrUser.substring("channel/".length());
        } else {
            return FEED_BASE_CHANNEL_ID + channelIdOrUser;
        }
    }

    /**
     * Checks if the given playlist id is a YouTube Mix (auto-generated playlist)
     * Ids from a YouTube Mix start with "RD"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube Mix
     */
    public static boolean isYoutubeMixId(@Nonnull final String playlistId) {
        return playlistId != null && playlistId.startsWith("RD");
    }

    /**
     * Checks if the given playlist id is a YouTube My Mix (auto-generated playlist)
     * Ids from a YouTube My Mix start with "RDMM"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube My Mix
     */
    public static boolean isYoutubeMyMixId(@Nonnull final String playlistId) {
        return playlistId != null && playlistId.startsWith("RDMM");
    }

    /**
     * Checks if the given playlist id is a YouTube Music Mix (auto-generated playlist)
     * Ids from a YouTube Music Mix start with "RDAMVM" or "RDCLAK"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube Music Mix
     */
    public static boolean isYoutubeMusicMixId(@Nonnull final String playlistId) {
        return playlistId != null && (playlistId.startsWith("RDAMVM")
                || playlistId.startsWith("RDCLAK"));
    }

    /**
     * Checks if the given playlist id is a YouTube Genre Mix (auto-generated playlist)
     * Ids from a YouTube Genre Mix start with "RDGMEM"
     *
     * @return Whether given id belongs to a YouTube Genre Mix
     */
    public static boolean isYoutubeGenreMixId(@Nonnull final String playlistId) {
        return playlistId != null && playlistId.startsWith("RDGMEM");
    }

    /**
     * @param playlistId the playlist id to parse
     * @return the {@link PlaylistInfo.PlaylistType} extracted from the playlistId (mix playlist
     *         types included)
     * @throws ParsingException if the playlistId is null or empty, if the playlistId is not a mix,
     *                          if it is a mix but it's not based on a specific stream (this is the
     *                          case for channel or genre mixes)
     */
    @Nonnull
    public static String extractVideoIdFromMixId(final String playlistId)
            throws ParsingException {
        if (playlistId == null || playlistId.isEmpty()) {
            throw new ParsingException("A video cannot be derived from an empty playlist id");
        }
        if (isYoutubeMyMixId(playlistId)) {
            return playlistId.substring(4);
        }
        if (isYoutubeMusicMixId(playlistId)) {
            return playlistId.substring(6);
        }
        if (isYoutubeGenreMixId(playlistId)) {
            throw new ParsingException("A genre mix does not identify a video");
        }
        if (isYoutubeMixId(playlistId) && playlistId.length() == 13) {
            return playlistId.substring(2);
        }
        throw new ParsingException("A video cannot be derived from playlist id: " + playlistId);
    }

    /**
     * @param playlistId the playlist id to parse
     * @return the {@link PlaylistInfo.PlaylistType} extracted from the playlistId (mix playlist
     *         types included)
     * @throws ParsingException if the playlistId is null or empty
     */
    @Nonnull
    public static PlaylistInfo.PlaylistType extractPlaylistTypeFromPlaylistId(
            final String playlistId) throws ParsingException {
        if (playlistId == null || playlistId.isEmpty()) {
            throw new ParsingException("Could not extract playlist type from empty playlist id");
        }
        if (isYoutubeMusicMixId(playlistId)) {
            return PlaylistInfo.PlaylistType.MIX_MUSIC;
        }
        if (isYoutubeGenreMixId(playlistId)) {
            return PlaylistInfo.PlaylistType.MIX_GENRE;
        }
        if (isYoutubeMixId(playlistId)) {
            return PlaylistInfo.PlaylistType.MIX_STREAM;
        }
        return PlaylistInfo.PlaylistType.NORMAL;
    }

    /**
     * @param playlistUrl the playlist url to parse
     * @return the {@link PlaylistInfo.PlaylistType} extracted from the playlistUrl's list param
     *         (mix playlist types included)
     * @throws ParsingException if the playlistUrl is malformed, if has no list param or if the list
     *                          param is empty
     */
    public static PlaylistInfo.PlaylistType extractPlaylistTypeFromPlaylistUrl(
            final String playlistUrl) throws ParsingException {
        try {
            return extractPlaylistTypeFromPlaylistId(
                    Utils.getQueryValue(Utils.stringToURL(playlistUrl), "list"));
        } catch (final MalformedURLException e) {
            throw new ParsingException("Could not extract playlist type from malformed url", e);
        }
    }

    private static JsonObject getInitialData(final String html) throws ParsingException {
        try {
            return JsonParser.object().from(getStringResultFromRegexArray(html,
                    INITIAL_DATA_REGEXES, 1));
        } catch (final JsonParserException | Parser.RegexException e) {
            throw new ParsingException("Could not get ytInitialData", e);
        }
    }

    public static boolean isHardcodedClientVersionValid()
            throws IOException, ExtractionException {
        if (hardcodedClientVersionValid.isPresent()) {
            return hardcodedClientVersionValid.get();
        }
        // @formatter:off
        final byte[] body = JsonWriter.string()
            .object()
                .object("context")
                    .object("client")
                        .value("hl", "en-GB")
                        .value("gl", "GB")
                        .value("clientName", WEB_CLIENT_NAME)
                        .value("clientVersion", WEB_HARDCODED_CLIENT_VERSION)
                        .value("platform", DESKTOP_CLIENT_PLATFORM)
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
                .value("fetchLiveState", true)
            .end().done().getBytes(StandardCharsets.UTF_8);
        // @formatter:on

        final var headers = getClientHeaders(WEB_CLIENT_ID, WEB_HARDCODED_CLIENT_VERSION);

        // This endpoint is fetched by the YouTube website to get the items of its main menu and is
        // pretty lightweight (around 30kB)
        final Response response = getDownloader().postWithContentTypeJson(
                YOUTUBEI_V1_URL + "guide?" + DISABLE_PRETTY_PRINT_PARAMETER,
                headers, body);
        final String responseBody = response.responseBody();
        final int responseCode = response.responseCode();

        hardcodedClientVersionValid = Optional.of(responseBody.length() > 5000
                && responseCode == 200); // Ensure to have a valid response
        return hardcodedClientVersionValid.get();
    }


    private static void extractClientVersionFromSwJs()
            throws IOException, ExtractionException {
        if (clientVersionExtracted) {
            return;
        }
        final String response = getDownloader().get("https://www.youtube.com/sw.js",
                getOriginReferrerHeaders("https://www.youtube.com")).responseBody();
        try {
            clientVersion = getStringResultFromRegexArray(response,
                    INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1);
        } catch (final Parser.RegexException e) {
            throw new ParsingException("Could not extract YouTube client version from sw.js", e);
        }
        clientVersionExtracted = true;
    }

    private static void extractClientVersionFromHtmlSearchResultsPage()
            throws IOException, ExtractionException {
        // Don't extract the InnerTube client version if it has been already extracted
        if (clientVersionExtracted) {
            return;
        }

        // Don't provide a search term in order to have a smaller response
        final String url = "https://www.youtube.com/results?search_query=&ucbcb=1";
        final String html = getDownloader().get(url, getCookieHeader()).responseBody();
        final JsonObject initialData = getInitialData(html);
        final JsonArray serviceTrackingParams = initialData.getObject("responseContext")
                .getArray("serviceTrackingParams");

        // Try to get version from initial data first
        clientVersion = getClientVersionFromServiceTrackingParam(
                serviceTrackingParams, "CSI", "cver");

        if (clientVersion == null) {
            try {
                clientVersion = getStringResultFromRegexArray(html,
                        INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1);
            } catch (final Parser.RegexException ignored) {
            }
        }

        // Fallback to get a shortened client version which does not contain the last two
        // digits
        if (isNullOrEmpty(clientVersion)) {
            clientVersion = getClientVersionFromServiceTrackingParam(
                    serviceTrackingParams, "ECATCHER", "client.version");
        }

        if (isNullOrEmpty(clientVersion)) {
            throw new ParsingException("Could not extract YouTube client version from HTML");
        }

        clientVersionExtracted = true;
    }

    @Nullable
    private static String getClientVersionFromServiceTrackingParam(
            @Nonnull final JsonArray serviceTrackingParams,
            @Nonnull final String serviceName,
            @Nonnull final String clientVersionKey) {
        return serviceTrackingParams.streamAsJsonObjects().filter(serviceTrackingParam ->
                        serviceTrackingParam.getString("service", "")
                                .equals(serviceName))
                .flatMap(serviceTrackingParam -> serviceTrackingParam.getArray("params")
                        .streamAsJsonObjects())
                .filter(param -> param.getString("key", "")
                        .equals(clientVersionKey))
                .map(param -> param.getString("value"))
                .filter(paramValue -> !isNullOrEmpty(paramValue))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the client version used by YouTube website on InnerTube requests.
     */
    public static String getClientVersion() throws IOException, ExtractionException {
        if (!isNullOrEmpty(clientVersion)) {
            return clientVersion;
        }

        try {
            extractClientVersionFromSwJs();
        } catch (final IOException | ExtractionException ignored) {
            try {
                extractClientVersionFromHtmlSearchResultsPage();
            } catch (final IOException | ExtractionException htmlFailure) {
                if (isHardcodedClientVersionValid()) {
                    clientVersion = WEB_HARDCODED_CLIENT_VERSION;
                    clientVersionExtracted = true;
                    return clientVersion;
                }
                throw new ExtractionException("Could not get YouTube client version",
                        htmlFailure);
            }
        }

        return clientVersion;
    }

    /**
     * <p>
     * <b>Only used in tests.</b>
     * </p>
     *
     * <p>
     * Quick-and-dirty solution to reset global state in between test classes.
     * </p>
     * <p>
     * This is needed for the mocks because in order to reach that state a network request has to
     * be made. If the global state is not reset and the RecordingDownloader is used,
     * then only the first test class has that request recorded. Meaning running the other
     * tests with mocks will fail, because the mock is missing.
     * </p>
     */
    public static void resetClientVersion() {
        clientVersion = null;
        youtubeMusicClientVersion = null;
        clientVersionExtracted = false;
        hardcodedClientVersionValid = Optional.empty();
    }

    /**
     * <p>
     * <b>Only used in tests.</b>
     * </p>
     */
    public static void setNumberGenerator(final Random random) {
        numberGenerator = random;
    }

    public static boolean isHardcodedYoutubeMusicClientVersionValid() throws IOException,
            ReCaptchaException {
        final JsonObject client = new JsonObject();
        client.put("clientName", WEB_REMIX_CLIENT_NAME);
        client.put("clientVersion", WEB_REMIX_HARDCODED_CLIENT_VERSION);
        client.put("hl", "en-GB");
        client.put("gl", "GB");
        client.put("platform", DESKTOP_CLIENT_PLATFORM);
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
        final JsonObject payload = new JsonObject();
        payload.put("context", context);
        payload.put("input", "");

        final Map<String, List<String>> headers = new HashMap<>(
                getOriginReferrerHeaders(YOUTUBE_MUSIC_URL));
        headers.putAll(getClientHeaders(WEB_REMIX_CLIENT_ID, WEB_HARDCODED_CLIENT_VERSION));
        final String endpoint = YOUTUBE_MUSIC_URL
                + "/youtubei/v1/music/get_search_suggestions?"
                + DISABLE_PRETTY_PRINT_PARAMETER;
        final Response response = getDownloader().postWithContentTypeJson(endpoint, headers,
                JsonWriter.string(payload).getBytes(StandardCharsets.UTF_8));
        return response.responseCode() == 200 && response.responseBody().length() > 500;
    }

    public static String getYoutubeMusicClientVersion()
            throws IOException, ReCaptchaException, Parser.RegexException {
        if (!isNullOrEmpty(youtubeMusicClientVersion)) {
            return youtubeMusicClientVersion;
        }
        if (isHardcodedYoutubeMusicClientVersionValid()) {
            youtubeMusicClientVersion = WEB_REMIX_HARDCODED_CLIENT_VERSION;
            return youtubeMusicClientVersion;
        }

        try {
            youtubeMusicClientVersion = getMusicClientVersion(
                    YOUTUBE_MUSIC_URL + "/sw.js", getOriginReferrerHeaders(YOUTUBE_MUSIC_URL));
        } catch (final IOException | ReCaptchaException | Parser.RegexException ignored) {
            youtubeMusicClientVersion = getMusicClientVersion(
                    YOUTUBE_MUSIC_URL + "/?ucbcb=1", getCookieHeader());
        }
        return youtubeMusicClientVersion;
    }

    private static String getMusicClientVersion(final String url,
            final Map<String, List<String>> headers)
            throws IOException, ReCaptchaException, Parser.RegexException {
        final String response = getDownloader().get(url, headers).responseBody();
        return getStringResultFromRegexArray(response, INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1);
    }

    @Nullable
    public static String getUrlFromNavigationEndpoint(
            @Nonnull final JsonObject navigationEndpoint) {
        if (navigationEndpoint.has("urlEndpoint")) {
            final String value = navigationEndpoint.getObject("urlEndpoint").getString("url");
            if (value != null) {
                try {
                    final URI uri = new URI(value);
                    if ("/redirect".equals(uri.getPath())) {
                        final String target = queryParameter(uri.getRawQuery(), "q");
                        if (target != null) return target;
                    } else if (isHttpUrl(uri)) {
                        return uri.toASCIIString();
                    } else if (value.startsWith("/channel") || value.startsWith("/user")
                            || value.startsWith("/watch")) {
                        return "https://www.youtube.com" + value;
                    }
                } catch (final URISyntaxException ignored) {
                    return null;
                }
            }
        }

        if (navigationEndpoint.has("browseEndpoint")) {
            final JsonObject browseEndpoint = navigationEndpoint.getObject("browseEndpoint");
            final String canonicalBaseUrl = browseEndpoint.getString("canonicalBaseUrl");
            final String browseId = browseEndpoint.getString("browseId");
            if (browseId != null) {
                if (browseId.startsWith("UC")) {
                    return "https://www.youtube.com/channel/" + browseId;
                }
                if (browseId.startsWith("VL")) {
                    return "https://www.youtube.com/playlist?list=" + browseId.substring(2);
                }
            }
            if (canonicalBaseUrl != null && !canonicalBaseUrl.isEmpty()) {
                return canonicalBaseUrl.startsWith("http") ? canonicalBaseUrl
                        : canonicalBaseUrl.startsWith("/")
                        ? "https://www.youtube.com" + canonicalBaseUrl : null;
            }
        }

        if (navigationEndpoint.has("watchEndpoint")) {
            final JsonObject watch = navigationEndpoint.getObject("watchEndpoint");
            final String videoId = watch.getString(VIDEO_ID);
            if (videoId == null || videoId.isEmpty()) return null;
            final StringBuilder result = new StringBuilder(
                    "https://www.youtube.com/watch?v=").append(videoId);
            final String playlistId = watch.getString("playlistId");
            if (playlistId != null) result.append("&list=").append(playlistId);
            if (watch.has("startTimeSeconds")) {
                result.append("&t=").append(watch.getInt("startTimeSeconds"));
            }
            return result.toString();
        }

        if (navigationEndpoint.has("watchPlaylistEndpoint")) {
            final String playlistId = navigationEndpoint.getObject("watchPlaylistEndpoint")
                    .getString("playlistId");
            return playlistId == null ? null
                    : "https://www.youtube.com/playlist?list=" + playlistId;
        }

        if (navigationEndpoint.has("showDialogCommand")) {
            final JsonArray items = navigationEndpoint.getObject("showDialogCommand")
                    .getObject("panelLoadingStrategy").getObject("inlineContent")
                    .getObject("dialogViewModel").getObject("customContent")
                    .getObject("listViewModel").getArray("listItems");
            if (!items.isEmpty()) {
                final JsonObject command = items.getObject(0).getObject("listItemViewModel")
                        .getObject("rendererContext").getObject("commandContext")
                        .getObject("onTap").getObject("innertubeCommand");
                final String url = getUrlFromNavigationEndpoint(command);
                if (url != null) return url;
            }
        }

        if (navigationEndpoint.has("commandMetadata")) {
            final JsonObject metadata = navigationEndpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata");
            final String value = metadata.getString("url");
            if (value != null) {
                if (value.startsWith("http://") || value.startsWith("https://")) return value;
                if (value.startsWith("/")) return "https://www.youtube.com" + value;
            }
        }
        return null;
    }

    private static boolean isHttpUrl(final URI uri) {
        final String scheme = uri.getScheme();
        return uri.getHost() != null && scheme != null
                && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
    }

    private static String queryParameter(final String query, final String key) {
        if (query == null) return null;
        for (final String parameter : query.split("&")) {
            final int separator = parameter.indexOf('=');
            if (separator >= 0 && key.equals(URLDecoder.decode(
                    parameter.substring(0, separator), StandardCharsets.UTF_8))) {
                return URLDecoder.decode(parameter.substring(separator + 1),
                        StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Get the text from a JSON object that has either a {@code simpleText} or a {@code runs}
     * array.
     *
     * @param textObject JSON object to get the text from
     * @param html       whether to return HTML, by parsing the {@code navigationEndpoint}
     * @return text in the JSON object or {@code null}
     */
    @Nullable
    public static String getTextFromObject(final JsonObject textObject, final boolean html) {
        if (textObject == null || textObject.isEmpty()) return null;
        if (textObject.has("simpleText")) {
            final String text = textObject.getString("simpleText");
            return text == null || !html ? text : htmlText(text);
        }

        final JsonArray runs = textObject.getArray("runs");
        if (runs.isEmpty()) return null;
        final StringBuilder result = new StringBuilder();
        for (final JsonObject run : runs.streamAsJsonObjects().toList()) {
            final String text = run.getString("text");
            if (text == null) continue;
            if (!html) {
                result.append(text);
                continue;
            }

            final String url = run.has("navigationEndpoint")
                    ? safeHtmlUrl(getUrlFromNavigationEndpoint(
                    run.getObject("navigationEndpoint"))) : null;
            if (url != null) result.append("<a href=\"").append(Entities.escape(url)).append("\">");
            if (run.getBoolean("bold", false)) result.append("<b>");
            if (run.getBoolean("italics", false)) result.append("<i>");
            if (run.getBoolean("strikethrough", false)) result.append("<s>");
            result.append(Entities.escape(text));
            if (run.getBoolean("strikethrough", false)) result.append("</s>");
            if (run.getBoolean("italics", false)) result.append("</i>");
            if (run.getBoolean("bold", false)) result.append("</b>");
            if (url != null) result.append("</a>");
        }
        final String text = result.toString();
        return html ? htmlSpacing(text) : text;
    }

    @Nonnull
    public static String getTextFromObjectOrThrow(final JsonObject textObject, final String error)
            throws ParsingException {
        final String result = getTextFromObject(textObject);
        if (result == null) {
            throw new ParsingException("Could not extract text: " + error);
        }
        return result;
    }

    @Nullable
    public static String getTextFromObject(final JsonObject textObject) {
        return getTextFromObject(textObject, false);
    }

    @Nullable
    public static String getUrlFromObject(final JsonObject textObject) {
        if (textObject == null) return null;
        for (final JsonObject textPart : textObject.getArray("runs").streamAsJsonObjects().toList()) {
            final String url = getUrlFromNavigationEndpoint(textPart.getObject("navigationEndpoint"));
            if (url != null && !url.isEmpty()) return url;
        }
        return null;
    }

    private static String safeHtmlUrl(final String value) {
        if (value == null) return null;
        try {
            final URI uri = new URI(value);
            return isHttpUrl(uri) ? uri.toASCIIString() : null;
        } catch (final URISyntaxException e) {
            return null;
        }
    }

    private static String htmlText(final String value) {
        return htmlSpacing(Entities.escape(value));
    }

    private static String htmlSpacing(final String value) {
        return value.replace("\n", "<br>").replace("  ", " &nbsp;");
    }

    @Nullable
    public static String getTextAtKey(@Nonnull final JsonObject jsonObject, final String theKey) {
        return jsonObject.isString(theKey) ? jsonObject.getString(theKey)
                : getTextFromObject(jsonObject.getObject(theKey));
    }

    public static String fixThumbnailUrl(@Nonnull final String thumbnailUrl) {
        final String value = thumbnailUrl.startsWith("//")
                ? thumbnailUrl.substring(2) : thumbnailUrl;
        return value.startsWith(HTTP) ? Utils.replaceHttpWithHttps(value)
                : value.startsWith(HTTPS) ? value : "https://" + value;
    }

    /**
     * Get thumbnails from a {@link JsonObject} representing a YouTube
     * {@link com.vyonex.internal.fallback.InfoItem InfoItem}.
     *
     * <p>
     * Thumbnails are got from the {@code thumbnails} {@link JsonArray} inside the {@code thumbnail}
     * {@link JsonObject} of the YouTube {@link com.vyonex.internal.fallback.InfoItem InfoItem},
     * using {@link #getImagesFromThumbnailsArray(JsonArray)}.
     * </p>
     *
     * @param infoItem a YouTube {@link com.vyonex.internal.fallback.InfoItem InfoItem}
     * @return an unmodifiable list of {@link Image}s found in the {@code thumbnails}
     * {@link JsonArray}
     * @throws ParsingException if an exception occurs when
     *                          {@link #getImagesFromThumbnailsArray(JsonArray)} is executed
     */
    @Nonnull
    public static List<Image> getThumbnailsFromInfoItem(@Nonnull final JsonObject infoItem)
            throws ParsingException {
        return getImagesFromThumbnailsArray(infoItem.getObject("thumbnail")
                .getArray("thumbnails"));
    }

    /**
     * Get images from a YouTube {@code thumbnails} {@link JsonArray}.
     *
     * <p>
     * The properties of the {@link Image}s created will be set using the corresponding ones of
     * thumbnail items.
     * </p>
     *
     * @param thumbnails a YouTube {@code thumbnails} {@link JsonArray}
     * @return an unmodifiable list of {@link Image}s extracted from the given {@link JsonArray}
     */
    @Nonnull
    public static List<Image> getImagesFromThumbnailsArray(
            @Nonnull final JsonArray thumbnails) {
        final List<Image> images = new java.util.ArrayList<>(thumbnails.size());
        for (final JsonObject thumbnail : thumbnails.streamAsJsonObjects().toList()) {
            final String url = thumbnail.getString("url");
            if (isNullOrEmpty(url)) continue;
            final int height = thumbnail.getInt("height", Image.HEIGHT_UNKNOWN);
            images.add(new Image(fixThumbnailUrl(url), height,
                    thumbnail.getInt("width", Image.WIDTH_UNKNOWN),
                    ResolutionLevel.fromHeight(height)));
        }
        return List.copyOf(images);
    }

    @Nonnull
    public static String getValidJsonResponseBody(@Nonnull final Response response)
            throws ParsingException, MalformedURLException {
        if (response.responseCode() == 404) {
            throw new ContentNotAvailableException("YouTube returned "
                    + response.responseCode() + " " + response.responseMessage());
        }

        final String responseBody = response.responseBody();
        if (responseBody.length() < 50) {
            throw new ParsingException("YouTube JSON response is too short");
        }

        final URL finalUrl = new URL(response.latestUrl());
        if ("www.youtube.com".equalsIgnoreCase(finalUrl.getHost())) {
            final String path = finalUrl.getPath();
            if (path.equalsIgnoreCase("/oops") || path.equalsIgnoreCase("/error")) {
                throw new ContentNotAvailableException("YouTube redirected to an error page");
            }
        }

        final String contentType = response.getHeader("Content-Type");
        if (contentType != null
                && contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
            throw new ParsingException("YouTube returned HTML instead of JSON at "
                    + response.latestUrl());
        }
        return responseBody;
    }

    public static JsonObject getJsonPostResponse(@Nonnull final String endpoint,
                                                 final byte[] body,
                                                 @Nonnull final Localization localization)
            throws IOException, ExtractionException {
        return postJson(endpoint, "", body, localization);
    }

    public static JsonObject getJsonPostResponse(@Nonnull final String endpoint,
                                                 @Nonnull final List<String> queryParameters,
                                                 final byte[] body,
                                                 @Nonnull final Localization localization)
            throws IOException, ExtractionException {
        final String query = queryParameters.isEmpty() ? ""
                : String.join("&", queryParameters) + "&";
        return postJson(endpoint, query, body, localization);
    }

    private static JsonObject postJson(final String endpoint, final String query,
                                       final byte[] body, final Localization localization)
            throws IOException, ExtractionException {
        final String url = YOUTUBEI_V1_URL + endpoint + "?" + query
                + DISABLE_PRETTY_PRINT_PARAMETER;
        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, getYouTubeHeaders(), body,
                        localization)));
    }

    @Nonnull
    public static JsonBuilder<JsonObject> prepareDesktopJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry) throws IOException, ExtractionException {
        final JsonObject client = new JsonObject();
        client.put("hl", localization.getLocalizationCode());
        client.put("gl", contentCountry.getCountryCode());
        client.put("clientName", WEB_CLIENT_NAME);
        client.put("clientVersion", getClientVersion());
        client.put("originalUrl", "https://www.youtube.com");
        client.put("platform", DESKTOP_CLIENT_PLATFORM);
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
        return JsonObject.builder().value("context", context);
    }

    /**
     * Get the user-agent string used as the user-agent for InnerTube requests with the Android
     * client.
     *
     * <p>
     * If the {@link Localization} provided is {@code null}, fallbacks to
     * {@link Localization#DEFAULT the default one}.
     * </p>
     *
     * @param localization the {@link Localization} to set in the user-agent
     * @return the Android user-agent used for InnerTube requests with the Android client,
     * depending on the {@link Localization} provided
     */
    @Nonnull
    public static String getAndroidUserAgent(@Nullable final Localization localization) {
        final Localization language = localization == null ? Localization.DEFAULT : localization;
        return "com.google.android.youtube/" + ANDROID_CLIENT_VERSION
                + " (Linux; U; Android 15; "
                + language.getCountryCode() + ") gzip";
    }

    /**
     * Get the user-agent string used as the user-agent for InnerTube requests with the iOS
     * client.
     *
     * <p>
     * If the {@link Localization} provided is {@code null}, fallbacks to
     * {@link Localization#DEFAULT the default one}.
     * </p>
     *
     * @param localization the {@link Localization} to set in the user-agent
     * @return the iOS user-agent used for InnerTube requests with the iOS client, depending on the
     * {@link Localization} provided
     */
    @Nonnull
    public static String getIosUserAgent(@Nullable final Localization localization) {
        final Localization language = localization == null ? Localization.DEFAULT : localization;
        return "com.google.ios.youtube/" + IOS_CLIENT_VERSION + "(" + IOS_DEVICE_MODEL
                + "; U; CPU iOS " + IOS_USER_AGENT_VERSION + " like Mac OS X; "
                + language.getCountryCode() + ")";
    }

    /**
     * Get the user-agent string used as the user-agent for InnerTube requests with the visionOS
     * client.
     *
     * <p>
     * If the {@link Localization} provided is {@code null}, fallbacks to
     * {@link Localization#DEFAULT the default one}.
     * </p>
     *
     * @param localization the {@link Localization} to set in the user-agent
     * @return the visionOS user-agent used for InnerTube requests with the visionOS client,
     * depending on the {@link Localization} provided
     */
    @Nonnull
    public static String getVisionOsUserAgent(@Nullable final Localization localization) {
        final Localization language = localization == null ? Localization.DEFAULT : localization;
        return "com.google.visionos.youtube/" + VISIONOS_CLIENT_VERSION + "("
                + VISIONOS_DEVICE_MODEL + "; U; CPU visionOS " + VISIONOS_USER_AGENT_VERSION
                + " like Mac OS X; " + language.getCountryCode() + ")";
    }

    /**
     * Returns a {@link Map} containing the required YouTube Music headers.
     */
    @Nonnull
    public static Map<String, List<String>> getYoutubeMusicHeaders() {
        final Map<String, List<String>> headers = new HashMap<>(
                getOriginReferrerHeaders(YOUTUBE_MUSIC_URL));
        headers.putAll(getClientHeaders(WEB_REMIX_CLIENT_ID, youtubeMusicClientVersion));
        return headers;
    }

    /**
     * Returns a {@link Map} containing the required YouTube headers, including the
     * <code>CONSENT</code> cookie to prevent redirects to <code>consent.youtube.com</code>
     */
    public static Map<String, List<String>> getYouTubeHeaders()
            throws ExtractionException, IOException {
        final Map<String, List<String>> headers = getClientInfoHeaders();
        headers.put("Cookie", List.of(generateConsentCookie()));
        return headers;
    }

    /**
     * Returns a {@link Map} containing the {@code X-YouTube-Client-Name},
     * {@code X-YouTube-Client-Version}, {@code Origin}, and {@code Referer} headers.
     */
    public static Map<String, List<String>> getClientInfoHeaders()
            throws ExtractionException, IOException {
        final Map<String, List<String>> headers = new HashMap<>(
                getOriginReferrerHeaders("https://www.youtube.com"));
        headers.putAll(getClientHeaders(WEB_CLIENT_ID, getClientVersion()));
        return headers;
    }

    /**
     * Returns an unmodifiable {@link Map} containing the {@code Origin} and {@code Referer}
     * headers set to the given URL.
     *
     * @param url The URL to be set as the origin and referrer.
     */
    public static Map<String, List<String>> getOriginReferrerHeaders(@Nonnull final String url) {
        final List<String> value = List.of(url);
        return Map.of("Origin", value, "Referer", value);
    }

    /**
     * Returns an unmodifiable {@link Map} containing the {@code X-YouTube-Client-Name} and
     * {@code X-YouTube-Client-Version} headers.
     *
     * @param name The X-YouTube-Client-Name value.
     * @param version X-YouTube-Client-Version value.
     */
    public static Map<String, List<String>> getClientHeaders(@Nonnull final String name,
                                                             @Nonnull final String version) {
        return Map.of("X-YouTube-Client-Name", List.of(name),
                "X-YouTube-Client-Version", List.of(version));
    }

    /**
     * Create a map with the required cookie header.
     * @return A singleton map containing the header.
     */
    public static Map<String, List<String>> getCookieHeader() {
        return Map.of("Cookie", List.of(generateConsentCookie()));
    }

    @Nonnull
    public static String generateConsentCookie() {
        return "SOCS=" + (consentAccepted ? "CAISAiAD" : "CAE=");
    }

    public static String extractCookieValue(final String cookieName,
                                            @Nonnull final Response response) {
        String result = "";
        for (final Map.Entry<String, List<String>> header : response.responseHeaders().entrySet()) {
            if (header.getKey() == null || !header.getKey().equalsIgnoreCase("Set-Cookie")) {
                continue;
            }
            for (final String cookie : header.getValue()) {
                final int equals = cookie.indexOf('=');
                if (equals < 0 || !cookieName.equals(cookie.substring(0, equals).trim())) continue;
                final int end = cookie.indexOf(';', equals + 1);
                result = cookie.substring(equals + 1,
                        end < 0 ? cookie.length() : end).trim();
            }
        }
        return result;
    }

    /**
     * Shared alert detection function, multiple endpoints return the error similarly structured.
     * <p>
     * Will check if the object has an alert of the type "ERROR".
     * </p>
     *
     * @param initialData the object which will be checked if an alert is present
     * @throws ContentNotAvailableException if an alert is detected
     */
    public static void defaultAlertsCheck(@Nonnull final JsonObject initialData)
            throws ParsingException {
        final JsonArray alerts = initialData.getArray("alerts");
        if (alerts.isEmpty()) return;
        final JsonObject alert = alerts.getObject(0).getObject("alertRenderer");
        if (!"ERROR".equalsIgnoreCase(alert.getString("type", ""))) return;

        final String text = getTextFromObject(alert.getObject("text"));
        if (text != null && (text.contains("This account has been terminated")
                || text.contains("This channel was removed"))) {
            final String lower = text.toLowerCase(Locale.ROOT);
            if (lower.contains("violated") || lower.contains("violation")
                    || lower.contains("violating") || lower.contains("infringement")) {
                throw new AccountTerminatedException(text,
                        AccountTerminatedException.Reason.VIOLATION);
            }
            throw new AccountTerminatedException(text);
        }
        throw new ContentNotAvailableException("YouTube returned an error: " + text);
    }

    /**
     * Sometimes, YouTube provides URLs which use Google's cache. They look like
     * {@code https://webcache.googleusercontent.com/search?q=cache:CACHED_URL}
     *
     * @param url the URL which might refer to the Google's webcache
     * @return the URL which is referring to the original site
     */
    public static String extractCachedUrlIfNeeded(final String url) {
        if (url == null) {
            return null;
        }
        try {
            final URI uri = new URI(url);
            if (!"webcache.googleusercontent.com".equalsIgnoreCase(uri.getHost())
                    || uri.getRawQuery() == null) {
                return url;
            }
            for (final String parameter : uri.getRawQuery().split("&")) {
                final int separator = parameter.indexOf('=');
                if (separator < 0 || !"q".equals(URLDecoder.decode(
                        parameter.substring(0, separator), StandardCharsets.UTF_8))) {
                    continue;
                }
                final String query = URLDecoder.decode(parameter.substring(separator + 1),
                        StandardCharsets.UTF_8);
                final int cache = query.indexOf("cache:");
                if (cache < 0) return url;
                final String cached = query.substring(cache + "cache:".length());
                final int scheme = Math.max(cached.indexOf("https://"), cached.indexOf("http://"));
                return scheme < 0 ? cached : cached.substring(scheme);
            }
        } catch (final URISyntaxException | IllegalArgumentException e) {
            return url;
        }
        return url;
    }

    public static boolean isVerified(final JsonArray badges) {
        for (final JsonObject badge : badges.streamAsJsonObjects().toList()) {
            final String style = badge.getObject("metadataBadgeRenderer").getString("style");
            if (style != null && (style.equals("BADGE_STYLE_TYPE_VERIFIED")
                    || style.equals("BADGE_STYLE_TYPE_VERIFIED_ARTIST"))) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasArtistOrVerifiedIconBadgeAttachment(
            @Nonnull final JsonArray attachmentRuns) {
        for (final JsonObject run : attachmentRuns.streamAsJsonObjects().toList()) {
            final JsonArray sources = run.getObject("element").getObject("type")
                    .getObject("imageType").getObject("image").getArray("sources");
            for (final JsonObject source : sources.streamAsJsonObjects().toList()) {
                final String name = source.getObject("clientResource").getString("imageName");
                if ("CHECK_CIRCLE_FILLED".equals(name) || "AUDIO_BADGE".equals(name)
                        || "MUSIC_FILLED".equals(name)) return true;
            }
        }
        return false;
    }

    /**
     * Generate a content playback nonce (also called {@code cpn}), sent by YouTube clients in
     * playback requests (and also for some clients, in the player request body).
     *
     * @return a content playback nonce string
     */
    @Nonnull
    public static String generateContentPlaybackNonce() {
        return randomClientToken(16);
    }

    /**
     * Try to generate a {@code t} parameter, sent by mobile clients as a query of the player
     * request.
     *
     * <p>
     * Some researches needs to be done to know how this parameter, unique at each request, is
     * generated.
     * </p>
     *
     * @return a 12 characters string to try to reproduce the {@code} parameter
     */
    @Nonnull
    public static String generateTParameter() {
        return randomClientToken(12);
    }

    /**
     * Check if the streaming URL is from the YouTube {@code WEB} client.
     *
     * @param url the streaming URL to be checked.
     * @return true if it's a {@code WEB} streaming URL, false otherwise
     */
    public static boolean isWebStreamingUrl(@Nonnull final String url) {
        return hasClientParameter(url, "WEB");
    }

    /**
     * Check if the streaming URL is from the YouTube {@code WEB_EMBEDDED_PLAYER} client.
     *
     * @param url the streaming URL to be checked.
     * @return true if it's a {@code WEB_EMBEDDED_PLAYER} streaming URL, false otherwise
     */
    public static boolean isWebEmbeddedPlayerStreamingUrl(@Nonnull final String url) {
        return hasClientParameter(url, "WEB_EMBEDDED_PLAYER");
    }

    /**
     * Check if the streaming URL is a URL from the YouTube {@code ANDROID} client.
     *
     * @param url the streaming URL to be checked.
     * @return true if it's a {@code ANDROID} streaming URL, false otherwise
     */
    public static boolean isAndroidStreamingUrl(@Nonnull final String url) {
        return hasClientParameter(url, "ANDROID");
    }

    /**
     * Check if the streaming URL is a URL from the YouTube {@code IOS} client.
     *
     * @param url the streaming URL on which check if it's a {@code IOS} streaming URL.
     * @return true if it's a {@code IOS} streaming URL, false otherwise
     */
    public static boolean isIosStreamingUrl(@Nonnull final String url) {
        return hasClientParameter(url, "IOS");
    }

    /**
     * Check if the streaming URL is a URL from the YouTube {@code VISIONOS} client.
     *
     * @param url the streaming URL on which check if it's a {@code VISIONOS} streaming URL.
     * @return true if it's a {@code VISIONOS} streaming URL, false otherwise
     */
    public static boolean isVisionOsStreamingUrl(@Nonnull final String url) {
        return hasClientParameter(url, "VISIONOS");
    }

    private static String randomClientToken(final int length) {
        final StringBuilder token = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            token.append(CONTENT_PLAYBACK_NONCE_ALPHABET.charAt(
                    numberGenerator.nextInt(CONTENT_PLAYBACK_NONCE_ALPHABET.length())));
        }
        return token.toString();
    }

    private static boolean hasClientParameter(final String url, final String clientName) {
        if (url == null) return false;
        try {
            final String query = new URI(url).getRawQuery();
            if (query == null) return false;
            for (final String parameter : query.split("&")) {
                final int separator = parameter.indexOf('=');
                if (separator < 0) continue;
                if ("c".equals(URLDecoder.decode(parameter.substring(0, separator),
                        StandardCharsets.UTF_8))
                        && clientName.equals(URLDecoder.decode(parameter.substring(separator + 1),
                        StandardCharsets.UTF_8))) return true;
            }
        } catch (final URISyntaxException | IllegalArgumentException e) {
            return false;
        }
        return false;
    }

    /**
     * Determines how the consent cookie that is required for YouTube, {@code SOCS}, will be
     * generated.
     *
     * <ul>
     *   <li>{@code false} (the default value) will use {@code CAE=};</li>
     *   <li>{@code true} will use {@code CAISAiAD}.</li>
     * </ul>
     *
     * <p>
     * Setting this value to {@code true} is needed to extract mixes and some YouTube Music
     * playlists in some countries such as the EU ones.
     * </p>
     */
    public static void setConsentAccepted(final boolean accepted) {
        consentAccepted = accepted;
    }

    /**
     * Get the value of the consent's acceptance.
     *
     * @see #setConsentAccepted(boolean)
     * @return the consent's acceptance value
     */
    public static boolean isConsentAccepted() {
        return consentAccepted;
    }

    /**
     * Extract the audio track type from the formats XTags.
     * <p>
     * Example: {@code acont=original, lang=en}.
     * </p>
     * @param xtags XTags of the audio track
     * @return {@link AudioTrackType} or {@code null} if no track type was found
     */
    @Nullable
    public static AudioTrackType extractAudioTrackType(@Nullable final String xtags) {
        if (xtags == null) {
            return null;
        }
        try {
            final CodedInputStream tags = CodedInputStream.newInstance(
                    Base64.getUrlDecoder().decode(xtags));
            boolean foundAudioType = false;
            String audioType = null;
            while (!tags.isAtEnd()) {
                final int tag = tags.readTag();
                if (tag == 0) break;
                if (WireFormat.getTagFieldNumber(tag) == 1
                        && WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    final CodedInputStream pair = CodedInputStream.newInstance(tags.readByteArray());
                    String key = null;
                    String value = null;
                    while (!pair.isAtEnd()) {
                        final int pairTag = pair.readTag();
                        if (pairTag == 0) break;
                        if (WireFormat.getTagFieldNumber(pairTag) == 1
                                && WireFormat.getTagWireType(pairTag)
                                == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                            key = pair.readStringRequireUtf8();
                        } else if (WireFormat.getTagFieldNumber(pairTag) == 2
                                && WireFormat.getTagWireType(pairTag)
                                == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                            value = pair.readStringRequireUtf8();
                        } else if (!pair.skipField(pairTag)) {
                            break;
                        }
                    }
                    if (!foundAudioType && "acont".equals(key)) {
                        foundAudioType = true;
                        audioType = value;
                    }
                } else if (!tags.skipField(tag)) {
                    break;
                }
            }
            if (!foundAudioType) return null;
            if (audioType == null) return null;
            switch (audioType) {
                case "original": return AudioTrackType.ORIGINAL;
                case "dubbed":
                case "dubbed-auto": return AudioTrackType.DUBBED;
                case "descriptive": return AudioTrackType.DESCRIPTIVE;
                case "secondary": return AudioTrackType.SECONDARY;
                default: return null;
            }
        } catch (final IOException | IllegalArgumentException ignored) {
            return null;
        }
    }

    @Nonnull
    public static String getVisitorDataFromInnertube(
            @Nonnull final InnertubeClientRequestInfo innertubeClientRequestInfo,
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Map<String, List<String>> httpHeaders,
            @Nonnull final String innertubeDomainAndVersionEndpoint,
            @Nullable final String embedUrl,
            final boolean useGuideEndpoint) throws IOException, ExtractionException {
        final JsonObject request = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, embedUrl).done();
        final byte[] requestBody = JsonWriter.string(request).getBytes(StandardCharsets.UTF_8);
        final String endpoint = innertubeDomainAndVersionEndpoint
                + (useGuideEndpoint ? "guide" : "visitor_id")
                + "?" + DISABLE_PRETTY_PRINT_PARAMETER;
        final Response response = getDownloader().postWithContentTypeJson(
                endpoint, httpHeaders, requestBody);
        final JsonObject responseJson = JsonUtils.toJsonObject(
                getValidJsonResponseBody(response));
        final String visitorData = responseJson.getObject("responseContext")
                .getString("visitorData");
        if (isNullOrEmpty(visitorData)) {
            throw new ParsingException("Could not get visitorData");
        }
        return visitorData;
    }

    @Nonnull
    public static JsonBuilder<JsonObject> prepareJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final InnertubeClientRequestInfo innertubeClientRequestInfo,
            @Nullable final String embedUrl) {
        final JsonObject client = new JsonObject();
        client.put("clientName", innertubeClientRequestInfo.clientInfo.clientName);
        client.put("clientVersion", innertubeClientRequestInfo.clientInfo.clientVersion);
        putIfPresent(client, "clientScreen", innertubeClientRequestInfo.clientInfo.clientScreen);
        putIfPresent(client, "platform", innertubeClientRequestInfo.deviceInfo.platform);
        putIfPresent(client, "visitorData", innertubeClientRequestInfo.clientInfo.visitorData);
        putIfPresent(client, "deviceMake", innertubeClientRequestInfo.deviceInfo.deviceMake);
        putIfPresent(client, "deviceModel", innertubeClientRequestInfo.deviceInfo.deviceModel);
        putIfPresent(client, "osName", innertubeClientRequestInfo.deviceInfo.osName);
        putIfPresent(client, "osVersion", innertubeClientRequestInfo.deviceInfo.osVersion);
        if (innertubeClientRequestInfo.deviceInfo.androidSdkVersion > 0) {
            client.put("androidSdkVersion", innertubeClientRequestInfo.deviceInfo.androidSdkVersion);
        }
        client.put("hl", localization.getLocalizationCode());
        client.put("gl", contentCountry.getCountryCode());
        client.put("utcOffsetMinutes", 0);

        final JsonObject context = new JsonObject();
        context.put("client", client);
        if (embedUrl != null) {
            final JsonObject thirdParty = new JsonObject();
            thirdParty.put("embedUrl", embedUrl);
            context.put("thirdParty", thirdParty);
        }
        final JsonObject request = new JsonObject();
        request.put("internalExperimentFlags", new JsonArray());
        request.put("useSsl", true);
        context.put("request", request);
        final JsonObject user = new JsonObject();
        user.put("lockedSafetyMode", false);
        context.put("user", user);

        return JsonObject.builder().value("context", context);
    }

    private static void putIfPresent(final JsonObject target, final String key,
                                     @Nullable final Object value) {
        if (value != null) target.put(key, value);
    }

    /**
     * Gets the first collaborator, which is the channel that owns the video,
     * i.e. the video is displayed on their channel page.
     *
     * @param navigationEndpoint JSON object for the navigationEndpoint
     * @return The first collaborator in the JSON object or {@code null}
     */
    @Nullable
    public static JsonObject getFirstCollaborator(final JsonObject navigationEndpoint)
            throws ParsingException {
        final JsonArray items = navigationEndpoint.getObject("showDialogCommand")
                .getObject("panelLoadingStrategy").getObject("inlineContent")
                .getObject("dialogViewModel").getObject("customContent")
                .getObject("listViewModel").getArray("listItems");
        if (items.isEmpty()) return null;
        final JsonObject collaborator = items.getObject(0).getObject("listItemViewModel");
        return collaborator.isEmpty() ? null : collaborator;
    }
}
