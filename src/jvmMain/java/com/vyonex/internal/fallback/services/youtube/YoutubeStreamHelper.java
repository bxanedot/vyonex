// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import static com.vyonex.internal.fallback.VyonEx.getDownloader;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_EMBEDDED_CLIENT_ID;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_EMBEDDED_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.CONTENT_CHECK_OK;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.CPN;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.RACY_CHECK_OK;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.VIDEO_ID;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.generateTParameter;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getAndroidUserAgent;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getClientHeaders;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getClientVersion;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getIosUserAgent;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getOriginReferrerHeaders;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getVisionOsUserAgent;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getYouTubeHeaders;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareJsonBuilder;

import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.utils.JsonUtils;
import com.vyonex.internal.json.JsonBuilder;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Builds and sends YouTube player requests for supported client profiles. */
public final class YoutubeStreamHelper {
    private static final String PLAYER = "player";
    private static final String SERVICE_INTEGRITY_DIMENSIONS = "serviceIntegrityDimensions";
    private static final String PO_TOKEN = "poToken";
    private static final String DESKTOP_WATCH_URL = "https://www.youtube.com/watch?v=";

    private YoutubeStreamHelper() { }

    @Nonnull
    public static JsonObject getWebMetadataPlayerResponse(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo client = InnertubeClientRequestInfo.ofWebClient();
        client.clientInfo.clientVersion = getClientVersion();
        final Map<String, List<String>> headers = getYouTubeHeaders();
        attachVisitorData(client, localization, contentCountry, headers, YOUTUBEI_V1_URL, null);

        final JsonBuilder<JsonObject> body = prepareJsonBuilder(
                localization, contentCountry, client, null);
        addVideoIdCpnAndOkChecks(body, videoId, null);
        final String url = YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER
                + "&$fields=microformat,videoDetails.thumbnail.thumbnails,videoDetails.videoId";
        return postJson(url, headers, body, localization);
    }

    @Nonnull
    public static JsonObject getWebEmbeddedPlayerResponse(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId,
            @Nonnull final String cpn,
            @Nullable final PoTokenResult webEmbeddedPoTokenResult,
            final int signatureTimestamp) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo client = InnertubeClientRequestInfo.ofWebEmbeddedPlayerClient();
        final Map<String, List<String>> headers = new HashMap<>(
                getClientHeaders(WEB_EMBEDDED_CLIENT_ID, WEB_EMBEDDED_CLIENT_VERSION));
        headers.putAll(getOriginReferrerHeaders("https://www.youtube.com"));
        final String embedUrl = DESKTOP_WATCH_URL + videoId;
        if (webEmbeddedPoTokenResult == null) {
            attachVisitorData(client, localization, contentCountry, headers, YOUTUBEI_V1_URL,
                    embedUrl);
        } else {
            client.clientInfo.visitorData = webEmbeddedPoTokenResult.visitorData;
        }

        final JsonBuilder<JsonObject> body = prepareJsonBuilder(
                localization, contentCountry, client, embedUrl);
        addVideoIdCpnAndOkChecks(body, videoId, cpn);
        addPlaybackContext(body, embedUrl, signatureTimestamp);
        if (webEmbeddedPoTokenResult != null) {
            addPoToken(body, webEmbeddedPoTokenResult.playerRequestPoToken);
        }
        return postJson(YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER,
                headers, body, localization);
    }

    @Nonnull
    public static JsonObject getAndroidPlayerResponse(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId,
            @Nonnull final String cpn,
            @Nonnull final PoTokenResult androidPoTokenResult)
            throws IOException, ExtractionException {
        final InnertubeClientRequestInfo client = InnertubeClientRequestInfo.ofAndroidClient();
        client.clientInfo.visitorData = androidPoTokenResult.visitorData;
        final Map<String, List<String>> headers =
                getMobileClientHeaders(getAndroidUserAgent(localization));
        final JsonBuilder<JsonObject> body = prepareJsonBuilder(
                localization, contentCountry, client, null);
        addVideoIdCpnAndOkChecks(body, videoId, cpn);
        addPoToken(body, androidPoTokenResult.playerRequestPoToken);
        final String url = YOUTUBEI_V1_GAPIS_URL + PLAYER + "?"
                + DISABLE_PRETTY_PRINT_PARAMETER + "&t=" + generateTParameter() + "&id=" + videoId;
        return postJson(url, headers, body, localization);
    }

    @Nonnull
    public static JsonObject getAndroidReelPlayerResponse(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId,
            @Nonnull final String cpn) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo client = InnertubeClientRequestInfo.ofAndroidClient();
        final Map<String, List<String>> headers =
                getMobileClientHeaders(getAndroidUserAgent(localization));
        attachVisitorData(client, localization, contentCountry, headers, YOUTUBEI_V1_GAPIS_URL,
                null);

        final JsonBuilder<JsonObject> body = prepareJsonBuilder(
                localization, contentCountry, client, null);
        body.object("playerRequest");
        addVideoIdCpnAndOkChecks(body, videoId, cpn);
        body.end().value("disablePlayerResponse", false);
        final String url = YOUTUBEI_V1_GAPIS_URL + "reel/reel_item_watch?"
                + DISABLE_PRETTY_PRINT_PARAMETER + "&t=" + generateTParameter() + "&id=" + videoId
                + "&$fields=playerResponse";
        return postJson(url, headers, body, localization).getObject("playerResponse");
    }

    @Nonnull
    public static JsonObject getIosPlayerResponse(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId,
            @Nonnull final String cpn,
            @Nullable final PoTokenResult iosPoTokenResult)
            throws IOException, ExtractionException {
        final InnertubeClientRequestInfo client = InnertubeClientRequestInfo.ofIosClient();
        final Map<String, List<String>> headers = getMobileClientHeaders(getIosUserAgent(localization));
        if (iosPoTokenResult == null) {
            attachVisitorData(client, localization, contentCountry, headers, YOUTUBEI_V1_URL, null);
        } else {
            client.clientInfo.visitorData = iosPoTokenResult.visitorData;
        }

        final JsonBuilder<JsonObject> body = prepareJsonBuilder(
                localization, contentCountry, client, null);
        addVideoIdCpnAndOkChecks(body, videoId, cpn);
        if (iosPoTokenResult != null) addPoToken(body, iosPoTokenResult.playerRequestPoToken);
        final String url = YOUTUBEI_V1_GAPIS_URL + PLAYER + "?"
                + DISABLE_PRETTY_PRINT_PARAMETER + "&t=" + generateTParameter() + "&id=" + videoId;
        return postJson(url, headers, body, localization);
    }

    @Nonnull
    public static JsonObject getVisionOsPlayerResponse(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId,
            @Nonnull final String cpn) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo client = InnertubeClientRequestInfo.ofVisionOsClient();
        final Map<String, List<String>> headers =
                getMobileClientHeaders(getVisionOsUserAgent(localization));
        attachVisitorData(client, localization, contentCountry, headers, YOUTUBEI_V1_URL, null);

        final JsonBuilder<JsonObject> body = prepareJsonBuilder(
                localization, contentCountry, client, null);
        addVideoIdCpnAndOkChecks(body, videoId, cpn);
        final String url = YOUTUBEI_V1_GAPIS_URL + PLAYER + "?"
                + DISABLE_PRETTY_PRINT_PARAMETER + "&t=" + generateTParameter() + "&id=" + videoId;
        return postJson(url, headers, body, localization);
    }

    private static void attachVisitorData(
            final InnertubeClientRequestInfo client,
            final Localization localization,
            final ContentCountry contentCountry,
            final Map<String, List<String>> headers,
            final String serviceEndpoint,
            @Nullable final String embedUrl) throws IOException, ExtractionException {
        client.clientInfo.visitorData = YoutubeParsingHelper.getVisitorDataFromInnertube(
                client, localization, contentCountry, headers, serviceEndpoint, embedUrl, false);
    }

    private static JsonObject postJson(final String url,
                                       final Map<String, List<String>> headers,
                                       final JsonBuilder<JsonObject> body,
                                       final Localization localization)
            throws IOException, ExtractionException {
        final byte[] bytes = JsonWriter.string(body.done()).getBytes(StandardCharsets.UTF_8);
        final Response response = getDownloader().postWithContentTypeJson(
                url, headers, bytes, localization);
        return JsonUtils.toJsonObject(getValidJsonResponseBody(response));
    }

    private static void addVideoIdCpnAndOkChecks(final JsonBuilder<JsonObject> body,
                                                 final String videoId,
                                                 @Nullable final String cpn) {
        body.value(VIDEO_ID, videoId);
        if (cpn != null) body.value(CPN, cpn);
        body.value(CONTENT_CHECK_OK, true).value(RACY_CHECK_OK, true);
    }

    private static void addPlaybackContext(final JsonBuilder<JsonObject> body,
                                           final String referer,
                                           final int signatureTimestamp) {
        body.object("playbackContext").object("contentPlaybackContext")
                .value("signatureTimestamp", signatureTimestamp)
                .value("referer", referer).end().end();
    }

    private static void addPoToken(final JsonBuilder<JsonObject> body, final String poToken) {
        body.object(SERVICE_INTEGRITY_DIMENSIONS).value(PO_TOKEN, poToken).end();
    }

    private static Map<String, List<String>> getMobileClientHeaders(final String userAgent) {
        return Map.of("User-Agent", List.of(userAgent),
                "X-Goog-Api-Format-Version", List.of("2"));
    }
}
