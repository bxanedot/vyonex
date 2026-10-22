// bxane
package com.vyonex.internal.fallback.services.youtube;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.defaultAlertsCheck;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonWriter;

import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shared functions for extracting YouTube channel pages and tabs.
 */
public final class YoutubeChannelHelper {

    private static final String BROWSE_ENDPOINT = "browseEndpoint";
    private static final String BROWSE_ID = "browseId";
    private static final String CAROUSEL_HEADER_RENDERER = "carouselHeaderRenderer";
    private static final String C4_TABBED_HEADER_RENDERER = "c4TabbedHeaderRenderer";
    private static final String CONTENT = "content";
    private static final String CONTENTS = "contents";
    private static final String HEADER = "header";
    private static final String PAGE_HEADER_VIEW_MODEL = "pageHeaderViewModel";
    private static final String TAB_RENDERER = "tabRenderer";
    private static final String TITLE = "title";
    private static final String TOPIC_CHANNEL_DETAILS_RENDERER = "topicChannelDetailsRenderer";

    private YoutubeChannelHelper() {
    }

    /**
     * Take a YouTube channel ID or URL path, resolve it if necessary and return a channel ID.
     *
     * @param idOrPath a YouTube channel ID or URL path
     * @return a YouTube channel ID
     * @throws IOException if a channel resolve request failed
     * @throws ExtractionException if a channel resolve request response could not be parsed or is
     * invalid
     */
    @Nonnull
    public static String resolveChannelId(@Nonnull final String idOrPath)
            throws ExtractionException, IOException {
        final String[] path = idOrPath.split("/");
        if (path[0].startsWith("UC")) return path[0];
        if ("channel".equals(path[0])) {
            if (path.length < 2 || path[1].isEmpty()) {
                throw new ExtractionException("Channel path has no id: " + idOrPath);
            }
            return path[1];
        }

        String url = idOrPath.startsWith("http://") || idOrPath.startsWith("https://")
                ? idOrPath : "https://www.youtube.com/" + idOrPath.replaceFirst("^/+", "");
        JsonObject endpoint = new JsonObject();
        String pageType = "";
        for (int attempt = 0; attempt < 3 && url != null; attempt++) {
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

        final String browseId = endpoint.getObject(BROWSE_ENDPOINT).getString(BROWSE_ID, "");
        if (("WEB_PAGE_TYPE_BROWSE".equalsIgnoreCase(pageType)
                || "WEB_PAGE_TYPE_CHANNEL".equalsIgnoreCase(pageType))
                && !browseId.isEmpty()) {
            if (!browseId.startsWith("UC")) {
                throw new ExtractionException("Resolved destination is not a channel");
            }
            return browseId;
        }
        if (path.length > 1 && !path[1].isEmpty()) return path[1];
        throw new ExtractionException("Could not resolve channel id for " + idOrPath);
    }

    /**
     * Response data object for {@link #getChannelResponse(String, String, Localization,
     * ContentCountry)}, after any redirection in the allowed redirects count ({@code 3}).
     */
    public static final class ChannelResponseData {

        /**
         * The channel response as a JSON object, after all redirects.
         */
        @Nonnull
        public final JsonObject jsonResponse;

        /**
         * The channel ID after all redirects.
         */
        @Nonnull
        public final String channelId;

        private ChannelResponseData(@Nonnull final JsonObject jsonResponse,
                                    @Nonnull final String channelId) {
            this.jsonResponse = jsonResponse;
            this.channelId = channelId;
        }
    }

    /**
     * Fetch a YouTube channel tab response, using the given channel ID and tab parameters.
     *
     * <p>
     * Redirections to other channels are supported to up to 3 redirects, which could happen for
     * instance for localized channels or for auto-generated ones. For instance, there are three IDs
     * of the auto-generated "Movies and Shows" channel, i.e. {@code UCuJcl0Ju-gPDoksRjK1ya-w},
     * {@code UChBfWrfBXL9wS6tQtgjt_OQ} and {@code UCok7UTQQEP1Rsctxiv3gwSQ}, and they all redirect
     * to the {@code UClgRkhTL3_hImCAmdLfDE4g} one.
     * </p>
     *
     * @param channelId    a valid YouTube channel ID
     * @param parameters   the parameters to specify the YouTube channel tab; if invalid ones are
     *                     specified, YouTube should return the {@code Home} tab
     * @param localization the {@link Localization} to use
     * @param country      the {@link ContentCountry} to use
     * @return a {@link ChannelResponseData channel response data}
     * @throws IOException if a channel request failed
     * @throws ExtractionException if a channel request response could not be parsed or is invalid
     */
    @Nonnull
    public static ChannelResponseData getChannelResponse(@Nonnull final String channelId,
                                                         @Nonnull final String parameters,
                                                         @Nonnull final Localization localization,
                                                         @Nonnull final ContentCountry country)
            throws ExtractionException, IOException {
        String id = channelId;
        for (int attempt = 0; attempt < 3; attempt++) {
            final byte[] body = JsonWriter.string(prepareDesktopJsonBuilder(
                                    localization, country)
                            .value(BROWSE_ID, id)
                            .value("params", parameters)
                            .done())
                    .getBytes(StandardCharsets.UTF_8);

            final JsonObject jsonResponse = getJsonPostResponse(
                    "browse", body, localization);
            checkIfChannelResponseIsValid(jsonResponse);
            final JsonArray actions = jsonResponse.getArray("onResponseReceivedActions");
            final JsonObject endpoint = actions.isEmpty() ? new JsonObject()
                    : actions.getObject(0).getObject("navigateAction").getObject("endpoint");
            final String webPageType = endpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata")
                    .getString("webPageType");
            final String browseId = endpoint.getObject(BROWSE_ENDPOINT)
                    .getString(BROWSE_ID, "");
            if (("WEB_PAGE_TYPE_BROWSE".equalsIgnoreCase(webPageType)
                    || "WEB_PAGE_TYPE_CHANNEL".equalsIgnoreCase(webPageType))
                    && !browseId.isEmpty()) {
                if (!browseId.startsWith("UC")) {
                    throw new ExtractionException("Redirect destination is not a channel");
                }
                id = browseId;
            } else {
                defaultAlertsCheck(jsonResponse);
                return new ChannelResponseData(jsonResponse, id);
            }
        }
        throw new ExtractionException("Channel redirects exceeded the limit of three requests");
    }

    /**
     * Assert that a channel JSON response does not contain an {@code error} JSON object.
     *
     * @param jsonResponse a channel JSON response
     * @throws ContentNotAvailableException if the channel was not found
     */
    private static void checkIfChannelResponseIsValid(@Nonnull final JsonObject jsonResponse)
            throws ContentNotAvailableException {
        final JsonObject error = jsonResponse.getObject("error");
        if (error.isEmpty()) return;
        if (error.getInt("code") == 404) {
            throw new ContentNotAvailableException("Channel not found");
        }
        throw new ContentNotAvailableException("Channel request failed: "
                + error.getString("status", "") + " " + error.getString("message", ""));
    }

    /**
     * A channel header response.
     *
     * <p>
     * This class allows the distinction between a classic header and a carousel one, used for
     * auto-generated ones like the gaming or music topic channels and for big events such as the
     * Coachella music festival, which have a different data structure and do not return the same
     * properties.
     * </p>
     */
    public static final class ChannelHeader implements Serializable {

        /**
         * Types of supported YouTube channel headers.
         */
        public enum HeaderType {

            /**
             * A {@code c4TabbedHeaderRenderer} channel header type.
             *
             * <p>
             * This header is returned on the majority of channels and contains the channel's name,
             * its banner and its avatar and its subscriber count in most cases.
             * </p>
             */
            C4_TABBED,

            /**
             * An {@code interactiveTabbedHeaderRenderer} channel header type.
             *
             * <p>
             * This header is returned for gaming topic channels, and only contains the channel's
             * name, its banner and a poster as its "avatar".
             * </p>
             */
            INTERACTIVE_TABBED,

            /**
             * A {@code carouselHeaderRenderer} channel header type.
             *
             * <p>
             * This header returns only the channel's name, its avatar and its subscriber count.
             * </p>
             */
            CAROUSEL,

            /**
             * A {@code pageHeaderRenderer} channel header type.
             *
             * <p>
             * This header returns only the channel's name and its avatar for system channels.
             * </p>
             */
            PAGE
        }

        /**
         * The channel header JSON response.
         */
        @Nonnull
        public final JsonObject json;

        /**
         * The type of the channel header.
         *
         * <p>
         * See the documentation of the {@link HeaderType} class for more details.
         * </p>
         */
        public final HeaderType headerType;

        public ChannelHeader(@Nonnull final JsonObject json, final HeaderType headerType) {
            this.json = json;
            this.headerType = headerType;
        }
    }

    /**
     * Get a channel header it if exists.
     *
     * @param channelResponse a full channel JSON response
     * @return a {@link ChannelHeader} or {@code null} if no supported header has been found
     */
    @Nullable
    public static ChannelHeader getChannelHeader(
            @Nonnull final JsonObject channelResponse) {
        final JsonObject header = channelResponse.getObject(HEADER);
        if (header.has(C4_TABBED_HEADER_RENDERER)) {
            return new ChannelHeader(header.getObject(C4_TABBED_HEADER_RENDERER),
                    ChannelHeader.HeaderType.C4_TABBED);
        }
        if (header.has(CAROUSEL_HEADER_RENDERER)) {
            for (final JsonObject item : header.getObject(CAROUSEL_HEADER_RENDERER)
                    .getArray(CONTENTS).streamAsJsonObjects().toList()) {
                if (item.has(TOPIC_CHANNEL_DETAILS_RENDERER)) {
                    return new ChannelHeader(item.getObject(TOPIC_CHANNEL_DETAILS_RENDERER),
                            ChannelHeader.HeaderType.CAROUSEL);
                }
            }
        }
        if (header.has("pageHeaderRenderer")) {
            return new ChannelHeader(header.getObject("pageHeaderRenderer"),
                    ChannelHeader.HeaderType.PAGE);
        }
        if (header.has("interactiveTabbedHeaderRenderer")) {
            return new ChannelHeader(header.getObject("interactiveTabbedHeaderRenderer"),
                    ChannelHeader.HeaderType.INTERACTIVE_TABBED);
        }
        return null;
    }

    /**
     * Check if a channel is verified by using its header.
     *
     * <p>
     * The header is mandatory, so the verified status of age-restricted channels with a
     * {@code channelAgeGateRenderer} cannot be checked.
     * </p>
     *
     * @param channelHeader the {@link ChannelHeader} of a non age-restricted channel
     * @return whether the channel is verified
     */
    public static boolean isChannelVerified(@Nonnull final ChannelHeader channelHeader) {
        switch (channelHeader.headerType) {
            case CAROUSEL:
                return true;
            case PAGE:
                final JsonObject page = channelHeader.json.getObject(CONTENT)
                        .getObject(PAGE_HEADER_VIEW_MODEL);
                final boolean hasBadge = hasArtistOrVerifiedIconBadgeAttachment(
                        page.getObject(TITLE)
                                .getObject("dynamicTextViewModel")
                                .getObject("text")
                                .getArray("attachmentRuns"));
                return hasBadge || page.getObject("image").has("contentPreviewImageViewModel");
            case INTERACTIVE_TABBED:
                return channelHeader.json.has("autoGenerated");
            default:
                return YoutubeParsingHelper.isVerified(channelHeader.json.getArray("badges"));
        }
    }

    /**
     * Get the ID of a channel from its response.
     *
     * <p>
     * For {@link ChannelHeader.HeaderType#C4_TABBED c4TabbedHeaderRenderer} and
     * {@link ChannelHeader.HeaderType#CAROUSEL carouselHeaderRenderer} channel headers, the ID is
     * get from the header.
     * </p>
     *
     * <p>
     * For other headers or if it cannot be got, the ID from the {@code channelMetadataRenderer}
     * in the channel response is used.
     * </p>
     *
     * <p>
     * If the ID cannot still be get, the fallback channel ID, if provided, will be used.
     * </p>
     *
     * @param channelHeader     the channel header
     * @param fallbackChannelId the fallback channel ID, which can be null
     * @return the ID of the channel
     * @throws ParsingException if the channel ID cannot be got from the channel header, the
     * channel response and the fallback channel ID
     */
    @Nonnull
    public static String getChannelId(
            @Nullable final ChannelHeader channelHeader,
            @Nonnull final JsonObject jsonResponse,
            @Nullable final String fallbackChannelId) throws ParsingException {
        if (channelHeader != null) {
            switch (channelHeader.headerType) {
                case C4_TABBED:
                case CAROUSEL:
                    String channelId = channelHeader.headerType
                            == ChannelHeader.HeaderType.C4_TABBED
                            ? channelHeader.json.getString("channelId") : null;
                    if (channelId == null || channelId.isEmpty()) {
                        channelId = channelHeader.json.getObject("navigationEndpoint")
                                .getObject(BROWSE_ENDPOINT).getString(BROWSE_ID);
                    }
                    if (channelId != null && !channelId.isEmpty()) return channelId;
                    break;
                default:
                    break;
            }
        }

        final String externalChannelId = jsonResponse.getObject("metadata")
                .getObject("channelMetadataRenderer")
                .getString("externalChannelId");
        if (externalChannelId != null && !externalChannelId.isEmpty()) return externalChannelId;
        if (fallbackChannelId != null && !fallbackChannelId.isEmpty()) return fallbackChannelId;
        throw new ParsingException("Could not get channel ID");
    }

    @Nonnull
    public static String getChannelName(@Nullable final ChannelHeader channelHeader,
                                        @Nullable final JsonObject channelAgeGateRenderer,
                                        @Nonnull final JsonObject jsonResponse)
            throws ParsingException {
        if (channelAgeGateRenderer != null) {
            final String title = channelAgeGateRenderer.getString("channelTitle");
            if (title == null || title.isEmpty()) {
                throw new ParsingException("Could not get channel name");
            }
            return title;
        }

        final String metadataTitle = jsonResponse.getObject("metadata")
                .getObject("channelMetadataRenderer")
                .getString(TITLE);
        if (metadataTitle != null && !metadataTitle.isEmpty()) return metadataTitle;

        String name = null;
        if (channelHeader != null) {
            final JsonObject header = channelHeader.json;
            switch (channelHeader.headerType) {
                case PAGE:
                    name = header.getObject(CONTENT).getObject(PAGE_HEADER_VIEW_MODEL)
                            .getObject(TITLE).getObject("dynamicTextViewModel")
                            .getObject("text").getString(CONTENT, header.getString("pageTitle"));
                    break;
                case CAROUSEL:
                case INTERACTIVE_TABBED:
                    name = getTextFromObject(header.getObject(TITLE));
                    break;
                case C4_TABBED:
                    name = header.getString(TITLE);
                    break;
                default:
                    break;
            }
        }
        if (name != null && !name.isEmpty()) return name;
        final String microformatTitle = jsonResponse.getObject("microformat")
                .getObject("microformatDataRenderer").getString(TITLE);
        if (microformatTitle != null && !microformatTitle.isEmpty()) return microformatTitle;
        throw new ParsingException("Could not get channel name");
    }

    @Nullable
    public static JsonObject getChannelAgeGateRenderer(@Nonnull final JsonObject jsonResponse) {
        final JsonArray tabs = jsonResponse.getObject(CONTENTS)
                .getObject("twoColumnBrowseResultsRenderer")
                .getArray("tabs");
        for (final JsonObject tab : tabs.streamAsJsonObjects().toList()) {
            final JsonArray contents = tab.getObject(TAB_RENDERER).getObject(CONTENT)
                    .getObject("sectionListRenderer").getArray(CONTENTS);
            for (final JsonObject item : contents.streamAsJsonObjects().toList()) {
                if (item.has("channelAgeGateRenderer")) {
                    return item.getObject("channelAgeGateRenderer");
                }
            }
        }
        return null;
    }
}
