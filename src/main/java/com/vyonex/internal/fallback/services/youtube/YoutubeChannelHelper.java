// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.defaultAlertsCheck;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;

import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Maps shared channel response data used by channel pages, tabs, and kiosk pages. */
public final class YoutubeChannelHelper {
    private static final int MAX_REDIRECTS = 3;

    private YoutubeChannelHelper() { }

    @Nonnull
    public static String resolveChannelId(@Nonnull final String idOrPath)
            throws ExtractionException, IOException {
        final String[] path = pathSegments(idOrPath);
        if (path.length > 0 && path[0].startsWith("UC")) return path[0];
        if (path.length > 0 && "channel".equals(path[0])) {
            if (path.length < 2 || path[1].isEmpty()) {
                throw new ExtractionException("Channel path has no id: " + idOrPath);
            }
            return path[1];
        }

        String url = idOrPath.startsWith("http://") || idOrPath.startsWith("https://")
                ? idOrPath : "https://www.youtube.com/" + trimLeadingSlashes(idOrPath);
        JsonObject endpoint = new JsonObject();
        String pageType = "";
        for (int attempt = 0; attempt < MAX_REDIRECTS && url != null; attempt++) {
            final JsonObject request = prepareDesktopJsonBuilder(
                    Localization.DEFAULT, ContentCountry.DEFAULT).value("url", url).done();
            final JsonObject response = getJsonPostResponse("navigation/resolve_url",
                    JsonWriter.string(request).getBytes(StandardCharsets.UTF_8), Localization.DEFAULT);
            checkIfChannelResponseIsValid(response);
            endpoint = response.getObject("endpoint");
            pageType = endpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata").getString("webPageType", "");
            url = "WEB_PAGE_TYPE_UNKNOWN".equals(pageType)
                    ? endpoint.getObject("urlEndpoint").getString("url") : null;
        }

        final String browseId = endpoint.getObject("browseEndpoint").getString("browseId", "");
        if (isChannelDestination(pageType, browseId)) {
            if (!browseId.startsWith("UC")) {
                throw new ExtractionException("Resolved destination is not a channel");
            }
            return browseId;
        }
        if (path.length > 1 && !path[1].isEmpty()) return path[1];
        throw new ExtractionException("Could not resolve channel id for " + idOrPath);
    }

    private static String[] pathSegments(final String value) {
        return trimLeadingSlashes(value).split("/");
    }

    private static String trimLeadingSlashes(final String value) {
        int first = 0;
        while (first < value.length() && value.charAt(first) == '/') first++;
        return value.substring(first);
    }

    private static boolean isChannelDestination(final String pageType, final String browseId) {
        return ("WEB_PAGE_TYPE_BROWSE".equalsIgnoreCase(pageType)
                || "WEB_PAGE_TYPE_CHANNEL".equalsIgnoreCase(pageType))
                && browseId != null && !browseId.isEmpty();
    }

    public static final class ChannelResponseData {
        @Nonnull public final JsonObject jsonResponse;
        @Nonnull public final String channelId;

        private ChannelResponseData(@Nonnull final JsonObject jsonResponse,
                                    @Nonnull final String channelId) {
            this.jsonResponse = jsonResponse;
            this.channelId = channelId;
        }
    }

    @Nonnull
    public static ChannelResponseData getChannelResponse(@Nonnull final String channelId,
                                                         @Nonnull final String parameters,
                                                         @Nonnull final Localization localization,
                                                         @Nonnull final ContentCountry country)
            throws ExtractionException, IOException {
        String id = channelId;
        for (int attempt = 0; attempt < MAX_REDIRECTS; attempt++) {
            final JsonObject body = prepareDesktopJsonBuilder(localization, country)
                    .value("browseId", id)
                    .value("params", parameters)
                    .done();
            final byte[] bytes = JsonWriter.string(body).getBytes(StandardCharsets.UTF_8);
            final JsonObject response = getJsonPostResponse("browse", bytes, localization);
            checkIfChannelResponseIsValid(response);

            final JsonObject endpoint = navigationEndpoint(response);
            final String pageType = endpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata").getString("webPageType");
            final String redirectedId = endpoint.getObject("browseEndpoint").getString("browseId");
            if (isChannelDestination(pageType, redirectedId)) {
                if (!redirectedId.startsWith("UC")) {
                    throw new ExtractionException("Redirect destination is not a channel");
                }
                id = redirectedId;
                continue;
            }

            defaultAlertsCheck(response);
            return new ChannelResponseData(response, id);
        }
        throw new ExtractionException("Channel redirects exceeded the limit of three requests");
    }

    private static JsonObject navigationEndpoint(final JsonObject response) {
        for (final JsonObject action : response.getArray("onResponseReceivedActions")
                .streamAsJsonObjects().toList()) {
            final JsonObject endpoint = action.getObject("navigateAction").getObject("endpoint");
            if (!endpoint.isEmpty()) return endpoint;
        }
        return new JsonObject();
    }

    private static void checkIfChannelResponseIsValid(@Nonnull final JsonObject response)
            throws ContentNotAvailableException {
        final JsonObject error = response.getObject("error");
        if (error.isEmpty()) return;
        if (error.getInt("code") == 404) {
            throw new ContentNotAvailableException("Channel not found");
        }
        throw new ContentNotAvailableException("Channel request failed: "
                + error.getString("status", "") + " " + error.getString("message", ""));
    }

    public static final class ChannelHeader implements Serializable {
        public enum HeaderType { C4_TABBED, INTERACTIVE_TABBED, CAROUSEL, PAGE }

        @Nonnull public final JsonObject json;
        public final HeaderType headerType;

        public ChannelHeader(@Nonnull final JsonObject json, final HeaderType headerType) {
            this.json = json;
            this.headerType = headerType;
        }
    }

    @Nullable
    public static ChannelHeader getChannelHeader(@Nonnull final JsonObject response) {
        final JsonObject header = response.getObject("header");
        JsonObject renderer = header.getObject("c4TabbedHeaderRenderer", null);
        if (renderer != null) {
            return new ChannelHeader(renderer, ChannelHeader.HeaderType.C4_TABBED);
        }

        renderer = header.getObject("carouselHeaderRenderer", null);
        if (renderer != null) {
            for (final JsonObject item : renderer.getArray("contents").streamAsJsonObjects().toList()) {
                final JsonObject topic = item.getObject("topicChannelDetailsRenderer", null);
                if (topic != null) return new ChannelHeader(topic, ChannelHeader.HeaderType.CAROUSEL);
            }
        }

        renderer = header.getObject("pageHeaderRenderer", null);
        if (renderer != null) return new ChannelHeader(renderer, ChannelHeader.HeaderType.PAGE);
        renderer = header.getObject("interactiveTabbedHeaderRenderer", null);
        return renderer == null ? null
                : new ChannelHeader(renderer, ChannelHeader.HeaderType.INTERACTIVE_TABBED);
    }

    public static boolean isChannelVerified(@Nonnull final ChannelHeader header) {
        switch (header.headerType) {
            case CAROUSEL:
                return true;
            case PAGE:
                final JsonObject page = header.json.getObject("content")
                        .getObject("pageHeaderViewModel");
                return hasArtistOrVerifiedIconBadgeAttachment(page.getObject("title")
                        .getObject("dynamicTextViewModel").getObject("text")
                        .getArray("attachmentRuns"))
                        || page.getObject("image").has("contentPreviewImageViewModel");
            case INTERACTIVE_TABBED:
                return header.json.getBoolean("autoGenerated");
            default:
                return YoutubeParsingHelper.isVerified(header.json.getArray("badges"));
        }
    }

    @Nonnull
    public static String getChannelId(@Nullable final ChannelHeader header,
                                      @Nonnull final JsonObject response,
                                      @Nullable final String fallbackChannelId)
            throws ParsingException {
        if (header != null && (header.headerType == ChannelHeader.HeaderType.C4_TABBED
                || header.headerType == ChannelHeader.HeaderType.CAROUSEL)) {
            String id = header.headerType == ChannelHeader.HeaderType.C4_TABBED
                    ? header.json.getString("channelId") : null;
            if (id == null || id.isBlank()) {
                id = header.json.getObject("navigationEndpoint")
                        .getObject("browseEndpoint").getString("browseId");
            }
            if (id != null && !id.isBlank()) return id;
        }

        final String metadataId = response.getObject("metadata")
                .getObject("channelMetadataRenderer").getString("externalChannelId");
        if (metadataId != null && !metadataId.isBlank()) return metadataId;
        if (fallbackChannelId != null && !fallbackChannelId.isBlank()) return fallbackChannelId;
        throw new ParsingException("Could not get channel ID");
    }

    @Nonnull
    public static String getChannelName(@Nullable final ChannelHeader header,
                                        @Nullable final JsonObject ageGate,
                                        @Nonnull final JsonObject response)
            throws ParsingException {
        if (ageGate != null) {
            final String title = ageGate.getString("channelTitle");
            if (title == null || title.isBlank()) throw new ParsingException("Could not get channel name");
            return title;
        }

        final String metadataTitle = response.getObject("metadata")
                .getObject("channelMetadataRenderer").getString("title");
        if (metadataTitle != null && !metadataTitle.isBlank()) return metadataTitle;

        String name = null;
        if (header != null) {
            switch (header.headerType) {
                case PAGE:
                    final JsonObject pageTitle = header.json.getObject("content")
                            .getObject("pageHeaderViewModel").getObject("title")
                            .getObject("dynamicTextViewModel").getObject("text");
                    name = pageTitle.getString("content", header.json.getString("pageTitle"));
                    break;
                case CAROUSEL:
                case INTERACTIVE_TABBED:
                    name = getTextFromObject(header.json.getObject("title"));
                    break;
                case C4_TABBED:
                    name = header.json.getString("title");
                    break;
                default:
                    break;
            }
        }
        if (name != null && !name.isBlank()) return name;

        final String microformatTitle = response.getObject("microformat")
                .getObject("microformatDataRenderer").getString("title");
        if (microformatTitle != null && !microformatTitle.isBlank()) return microformatTitle;
        throw new ParsingException("Could not get channel name");
    }

    @Nullable
    public static JsonObject getChannelAgeGateRenderer(@Nonnull final JsonObject response) {
        final JsonArray tabs = response.getObject("contents")
                .getObject("twoColumnBrowseResultsRenderer").getArray("tabs");
        for (final JsonObject tab : tabs.streamAsJsonObjects().toList()) {
            final JsonArray sections = tab.getObject("tabRenderer").getObject("content")
                    .getObject("sectionListRenderer").getArray("contents");
            for (final JsonObject section : sections.streamAsJsonObjects().toList()) {
                final JsonObject ageGate = section.getObject("channelAgeGateRenderer", null);
                if (ageGate != null) return ageGate;
            }
        }
        return null;
    }
}
