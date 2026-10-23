/*
 * Created by Christian Schabesberger on 06.08.15.
 *
 * Copyright (C) 2019 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeStreamExtractor.java is part of VyonEx.
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
package com.vyonex.internal.fallback.services.youtube.extractors;

import static com.vyonex.internal.fallback.services.youtube.YoutubeDescriptionHelper.attributedDescriptionToHtml;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.CONTENT_CHECK_OK;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.RACY_CHECK_OK;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.VIDEO_ID;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.fixThumbnailUrl;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.generateContentPlaybackNonce;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItemExtractor;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.AccountTerminatedException;
import com.vyonex.internal.fallback.exceptions.AgeRestrictedContentException;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.GeographicRestrictionException;
import com.vyonex.internal.fallback.exceptions.PaidContentException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.PrivateContentException;
import com.vyonex.internal.fallback.exceptions.YoutubeMusicPremiumContentException;
import com.vyonex.internal.fallback.exceptions.SignInConfirmNotBotException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.localization.TimeAgoPatternsManager;
import com.vyonex.internal.fallback.services.youtube.ItagItem;
import com.vyonex.internal.fallback.services.youtube.PoTokenProvider;
import com.vyonex.internal.fallback.services.youtube.PoTokenResult;
import com.vyonex.internal.fallback.services.youtube.YoutubeMetaInfoHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeStreamHelper;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.Frameset;
import com.vyonex.internal.fallback.stream.Stream;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamSegment;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.SubtitlesStream;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.fallback.utils.JsonUtils;
import com.vyonex.internal.fallback.utils.Pair;
import com.vyonex.internal.fallback.utils.Utils;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class YoutubeStreamExtractor extends StreamExtractor {

    private static final String PREMIERED = "Premiered ";
    private static final String PREMIERED_ON = "Premiered on ";
    private static final String FORMATS = "formats";
    private static final String ADAPTIVE_FORMATS = "adaptiveFormats";
    private static final String STREAMING_DATA = "streamingData";
    private static final String NEXT = "next";
    private static final String PLAYER_CAPTIONS_TRACKLIST_RENDERER
            = "playerCaptionsTracklistRenderer";
    private static final String CAPTIONS = "captions";
    private static final String PLAYABILITY_STATUS = "playabilityStatus";
    private static final String THUMBNAIL = "thumbnail";
    private static final String THUMBNAILS = "thumbnails";
    private static final String VIDEO_DETAILS = "videoDetails";
    private static final String TITLE = "title";

    @Nullable
    private static PoTokenProvider poTokenProvider;
    private static boolean fetchIosClient;

    private JsonObject playerResponse;
    private JsonObject nextResponse;

    @Nullable
    private JsonObject visionOsStreamingData;
    @Nullable
    private JsonObject iosStreamingData;
    @Nullable
    private JsonObject androidStreamingData;

    private JsonObject videoPrimaryInfoRenderer;
    private JsonObject videoSecondaryInfoRenderer;
    private JsonObject playerMicroFormatRenderer;
    private JsonObject playerCaptionsTracklistRenderer;
    private JsonArray thumbnailsArray;
    private int ageLimit = -1;
    private StreamType streamType;

    // We need to store the contentPlaybackNonces because we need to append them to videoplayback
    // URLs (with the cpn parameter).
    // Also because a nonce should be unique, it should be different between clients used, so
    // three different strings are used.
    private String visionOsCpn;
    private String iosCpn;
    private String androidCpn;

    @Nullable
    private String androidStreamingUrlsPoToken;
    @Nullable
    private String iosStreamingUrlsPoToken;

    public YoutubeStreamExtractor(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Impl
    //////////////////////////////////////////////////////////////////////////*/

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        assertPageFetched();
        return extractVideoTitle(playerResponse.getObject(VIDEO_DETAILS),
                getVideoPrimaryInfoRenderer());
    }

    static String extractVideoTitle(@Nonnull final JsonObject videoDetails,
                                    @Nonnull final JsonObject primaryInfo)
            throws ParsingException {
        String title = videoDetails.getString(TITLE);
        if (isNullOrEmpty(title)) {
            title = getTextFromObject(primaryInfo.getObject(TITLE));
        }
        if (isNullOrEmpty(title)) throw new ParsingException("Could not get name");
        return title;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return extractTextualUploadDate(playerMicroFormatRenderer,
                getVideoPrimaryInfoRenderer(), streamType);
    }

    @Nullable
    static String extractTextualUploadDate(@Nonnull final JsonObject microFormat,
                                           @Nonnull final JsonObject primaryInfo,
                                           @Nullable final StreamType streamType) {
        String timestamp = microFormat.getString("uploadDate");
        if (isNullOrEmpty(timestamp)) timestamp = microFormat.getString("publishDate");
        if (!isNullOrEmpty(timestamp)) return timestamp;

        final JsonObject liveDetails = microFormat.getObject("liveBroadcastDetails");
        timestamp = liveDetails.getString("endTimestamp");
        if (isNullOrEmpty(timestamp)) timestamp = liveDetails.getString("startTimestamp");
        if (!isNullOrEmpty(timestamp)) return timestamp;
        if (streamType == StreamType.LIVE_STREAM) return null;

        final String dateText = getTextFromObject(primaryInfo.getObject("dateText"));
        if (dateText == null) return null;
        if (dateText.startsWith(PREMIERED_ON)) return dateText.substring(PREMIERED_ON.length());
        if (dateText.startsWith(PREMIERED)) return dateText.substring(PREMIERED.length());
        return dateText;
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return parseUploadDate(getTextualUploadDate());
    }

    @Nullable
    static DateWrapper parseUploadDate(@Nullable final String dateText) throws ParsingException {
        if (dateText == null) return null;
        try {
            return DateWrapper.fromOffsetDateTime(dateText);
        } catch (final ParsingException ignored) { }

        try {
            return TimeAgoPatternsManager.getTimeAgoParserFor(new Localization("en"))
                    .parse(dateText);
        } catch (final ParsingException ignored) { }

        for (final String pattern : List.of("MMM dd, yyyy", "dd MMM yyyy")) {
            try {
                final var formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                return new DateWrapper(LocalDate.parse(dateText, formatter).atStartOfDay(), true);
            } catch (final DateTimeParseException ignored) {
                // Try the next supported date layout.
            }
        }
        throw new ParsingException("Could not parse upload date \"" + dateText + "\"");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        assertPageFetched();
        try {
            return getImagesFromThumbnailsArray(thumbnailsArray);
        } catch (final Exception e) {
            throw new ParsingException("Could not get thumbnails");
        }
    }

    @Nonnull
    @Override
    public Description getDescription() throws ParsingException {
        assertPageFetched();
        return extractDescription(playerResponse.getObject(VIDEO_DETAILS),
                getVideoSecondaryInfoRenderer(), playerMicroFormatRenderer);
    }

    static Description extractDescription(@Nonnull final JsonObject videoDetails,
                                          @Nonnull final JsonObject secondaryInfo,
                                          @Nonnull final JsonObject microFormat)
            throws ParsingException {
        final String rendered = getTextFromObject(secondaryInfo.getObject("description"), true);
        if (!isNullOrEmpty(rendered)) return new Description(rendered, Description.HTML);

        final String attributed = attributedDescriptionToHtml(
                secondaryInfo.getObject("attributedDescription"));
        if (!isNullOrEmpty(attributed)) return new Description(attributed, Description.HTML);

        String plain = videoDetails.getString("shortDescription");
        if (plain == null) plain = getTextFromObject(microFormat.getObject("description"));
        return new Description(plain, Description.PLAIN_TEXT);
    }

    @Override
    public int getAgeLimit() throws ParsingException {
        if (ageLimit != -1) {
            return ageLimit;
        }
        ageLimit = parseAgeLimit(getVideoSecondaryInfoRenderer());
        return ageLimit;
    }

    static int parseAgeLimit(@Nonnull final JsonObject secondaryInfo) {
        final JsonArray rows = secondaryInfo.getObject("metadataRowContainer")
                .getObject("metadataRowContainerRenderer").getArray("rows");
        for (int row = 0; row < rows.size(); row++) {
            final JsonArray contents = rows.getObject(row).getObject("metadataRowRenderer")
                    .getArray("contents");
            for (int content = 0; content < contents.size(); content++) {
                final JsonArray runs = contents.getObject(content).getArray("runs");
                for (int run = 0; run < runs.size(); run++) {
                    if (runs.getObject(run).getString("text", "").contains("Age-restricted")) {
                        return 18;
                    }
                }
            }
        }
        return NO_AGE_LIMIT;
    }

    @Override
    public long getLength() throws ParsingException {
        assertPageFetched();
        return extractVideoDuration(playerResponse.getObject(VIDEO_DETAILS), Arrays.asList(
                androidStreamingData, iosStreamingData));
    }

    static long extractVideoDuration(@Nonnull final JsonObject videoDetails,
                                     @Nonnull final List<JsonObject> streamingDatas)
            throws ParsingException {
        final String duration = videoDetails.getString("lengthSeconds");
        if (!isNullOrEmpty(duration)) {
            try {
                final long seconds = Long.parseLong(duration);
                if (seconds >= 0) return seconds;
            } catch (final NumberFormatException ignored) {
                // Use a client format if the page duration is absent or invalid.
            }
        }
        return getDurationFromFirstAdaptiveFormat(streamingDatas);
    }

    static long getDurationFromFirstAdaptiveFormat(@Nonnull final List<JsonObject> streamingDatas)
            throws ParsingException {
        for (final JsonObject streamingData : streamingDatas) {
            if (streamingData == null) continue;
            final JsonArray adaptiveFormats = streamingData.getArray(ADAPTIVE_FORMATS);
            for (int index = 0; index < adaptiveFormats.size(); index++) {
                final String durationMs = adaptiveFormats.getObject(index)
                        .getString("approxDurationMs");
                try {
                    final long milliseconds = Long.parseLong(durationMs);
                    if (milliseconds < 0) continue;
                    return milliseconds / 1000 + (milliseconds % 1000 >= 500 ? 1 : 0);
                } catch (final NumberFormatException ignored) {
                    // A later format or client may still include a usable duration.
                }
            }
        }

        throw new ParsingException("Could not get duration");
    }

    /**
     * Attempts to parse (and return) the offset to start playing the video from.
     *
     * @return the offset (in seconds), or 0 if no timestamp is found.
     */
    @Override
    public long getTimeStamp() throws ParsingException {
        final long timestamp =
                getTimestampSeconds("((#|&|\\?)t=\\d*h?\\d*m?\\d+s?)");

        if (timestamp == -2) {
            // Regex for timestamp was not found
            return 0;
        }
        return timestamp;
    }

    @Override
    public long getViewCount() throws ParsingException {
        return parseViewCount(getVideoPrimaryInfoRenderer(),
                playerResponse.getObject(VIDEO_DETAILS));
    }

    static long parseViewCount(@Nonnull final JsonObject primaryInfo,
                               @Nonnull final JsonObject videoDetails)
            throws ParsingException {
        String views = getTextFromObject(primaryInfo.getObject("viewCount")
                .getObject("videoViewCountRenderer").getObject("viewCount"));
        if (isNullOrEmpty(views)) views = videoDetails.getString("viewCount");
        if (isNullOrEmpty(views)) throw new ParsingException("Could not get view count");
        if (views.toLowerCase(Locale.ROOT).contains("no views")) return 0;
        try {
            return Long.parseLong(Utils.removeNonDigitCharacters(views));
        } catch (final NumberFormatException e) {
            throw new ParsingException("Could not parse view count", e);
        }
    }

    @Override
    public long getLikeCount() throws ParsingException {
        assertPageFetched();
        final JsonArray buttons = getVideoPrimaryInfoRenderer()
                .getObject("videoActions")
                .getObject("menuRenderer")
                .getArray("topLevelButtons");
        return parseLikeCount(playerResponse.getObject(VIDEO_DETAILS).getBoolean("allowRatings"),
                buttons);
    }

    static long parseLikeCount(final boolean allowRatings, @Nonnull final JsonArray buttons)
            throws ParsingException {
        if (!allowRatings) return -1;
        for (final JsonObject button : buttons.streamAsJsonObjects().toList()) {
            final String count = button.getObject("segmentedLikeDislikeButtonViewModel")
                    .getObject("likeButtonViewModel").getObject("likeButtonViewModel")
                    .getObject("toggleButtonViewModel").getObject("toggleButtonViewModel")
                    .getObject("defaultButtonViewModel").getObject("buttonViewModel")
                    .getString("accessibilityText");
            if (!isNullOrEmpty(count)) {
                try {
                    return parseLikeCountText(count);
                } catch (final ParsingException ignored) {
                    // Older button data can still provide a usable accessibility label.
                }
            }
        }
        for (final JsonObject button : buttons.streamAsJsonObjects().toList()) {
            final JsonObject renderer = button.getObject("segmentedLikeDislikeButtonRenderer")
                    .getObject("likeButton").getObject("toggleButtonRenderer");
            String count = renderer.getObject("accessibilityData")
                    .getObject("accessibilityData").getString("label");
            if (count == null) count = renderer.getObject("accessibility").getString("label");
            if (count == null) {
                count = renderer.getObject("defaultText").getObject("accessibility")
                        .getObject("accessibilityData").getString("label");
            }
            if (!isNullOrEmpty(count)) {
                if (count.toLowerCase(Locale.ROOT).contains("no likes")) return 0;
                try {
                    return parseLikeCountText(count);
                } catch (final ParsingException ignored) {
                    // Continue through the available button variants.
                }
            }
        }
        throw new ParsingException("Could not get like count");
    }

    private static long parseLikeCountText(final String text) throws ParsingException {
        try {
            return Long.parseLong(Utils.removeNonDigitCharacters(text));
        } catch (final NumberFormatException e) {
            throw new ParsingException("Could not parse like count: " + text, e);
        }
    }

    @Nonnull
    @Override
    public String getUploaderUrl() throws ParsingException {
        assertPageFetched();
        return extractUploaderUrl(playerResponse.getObject(VIDEO_DETAILS));
    }

    static String extractUploaderUrl(@Nonnull final JsonObject videoDetails)
            throws ParsingException {
        final String channelId = videoDetails.getString("channelId");
        if (isNullOrEmpty(channelId)) throw new ParsingException("Could not get uploader url");
        return YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/" + channelId);
    }

    @Nonnull
    @Override
    public String getUploaderName() throws ParsingException {
        assertPageFetched();
        return extractUploaderName(playerResponse.getObject(VIDEO_DETAILS));
    }

    static String extractUploaderName(@Nonnull final JsonObject videoDetails)
            throws ParsingException {
        final String name = videoDetails.getString("author");
        if (isNullOrEmpty(name)) throw new ParsingException("Could not get uploader name");
        return name;
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return isUploaderVerified(getVideoSecondaryInfoRenderer().getObject("owner")
                .getObject("videoOwnerRenderer"));
    }

    static boolean isUploaderVerified(@Nonnull final JsonObject owner) throws ParsingException {
        if (owner.has("badges")) return YoutubeParsingHelper.isVerified(owner.getArray("badges"));
        final JsonObject collaborator = YoutubeParsingHelper.getFirstCollaborator(
                owner.getObject("navigationEndpoint"));
        return collaborator != null && YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment(
                collaborator.getObject(TITLE).getArray("attachmentRuns"));
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() throws ParsingException {
        assertPageFetched();
        return extractUploaderAvatars(getVideoSecondaryInfoRenderer().getObject("owner")
                .getObject("videoOwnerRenderer"), ageLimit);
    }

    static List<Image> extractUploaderAvatars(@Nonnull final JsonObject owner,
                                              final int ageLimit) throws ParsingException {
        final List<Image> imageList;
        if (owner.has("avatarStack")) {
            imageList = getImagesFromThumbnailsArray(
                    owner.getObject("avatarStack").getObject("avatarStackViewModel")
                            .getArray("avatars").getObject(0).getObject("avatarViewModel")
                            .getObject("image").getArray("sources"));
        } else {
            imageList = getImagesFromThumbnailsArray(owner.getObject(THUMBNAIL)
                    .getArray(THUMBNAILS));
        }
        if (imageList.isEmpty() && ageLimit == NO_AGE_LIMIT) {
            throw new ParsingException("Could not get uploader avatars");
        }
        return imageList;
    }

    @Override
    public long getUploaderSubscriberCount() throws ParsingException {
        return extractUploaderSubscriberCount(videoSecondaryInfoRenderer.getObject("owner")
                .getObject("videoOwnerRenderer"));
    }

    static long extractUploaderSubscriberCount(@Nonnull final JsonObject owner)
            throws ParsingException {
        String countText = getTextFromObject(owner.getObject("subscriberCountText"));
        if (isNullOrEmpty(countText)) {
            final JsonObject collaborator = YoutubeParsingHelper.getFirstCollaborator(
                    owner.getObject("navigationEndpoint"));
            if (collaborator != null) {
                final String subtitle = collaborator.getObject("subtitle").getString("content");
                if (subtitle != null) {
                    final int separator = subtitle.indexOf('•');
                    if (separator >= 0) countText = subtitle.substring(separator + 1).trim();
                }
            }
        }
        if (isNullOrEmpty(countText)) return UNKNOWN_SUBSCRIBER_COUNT;
        try {
            return Utils.mixedNumberWordToLong(countText);
        } catch (final NumberFormatException e) {
            throw new ParsingException("Could not get uploader subscriber count", e);
        }
    }

    @Nonnull
    @Override
    public String getDashMpdUrl() throws ParsingException {
        assertPageFetched();

        // There is no DASH manifest available with the iOS and visionOS clients
        return getManifestUrl(
                "dash",
                List.of(new ManifestSource(androidStreamingData, androidStreamingUrlsPoToken)),
                // Return version 7 of the DASH manifest, which is the latest one, reducing
                // manifest size and allowing playback with some DASH players
                "mpd_version=7");
    }

    @Nonnull
    @Override
    public String getHlsUrl() throws ParsingException {
        assertPageFetched();

        // Return HLS manifest of an Apple client first because on livestreams, the HLS manifest
        // returned has separated audio and video streams
        // Also, on videos, non-Apple clients don't have an HLS manifest URL in their player
        // response
        return getManifestUrl(
                "hls",
                List.of(new ManifestSource(visionOsStreamingData, null),
                        new ManifestSource(iosStreamingData, iosStreamingUrlsPoToken),
                        new ManifestSource(androidStreamingData, androidStreamingUrlsPoToken)),
                "");
    }

    record ManifestSource(@Nullable JsonObject streamingData, @Nullable String poToken) { }

    @Nonnull
    static String getManifestUrl(
            @Nonnull final String manifestType,
            @Nonnull final List<ManifestSource> sources,
            @Nonnull final String partToAppendToManifestUrlEnd) {
        final String manifestKey = manifestType + "ManifestUrl";

        for (final ManifestSource source : sources) {
            if (source.streamingData() == null) continue;
            final String manifestUrl = source.streamingData().getString(manifestKey);
            if (isNullOrEmpty(manifestUrl)) continue;
            return appendManifestParameters(manifestUrl, source.poToken(),
                    partToAppendToManifestUrlEnd);
        }

        return "";
    }

    static String appendManifestParameters(@Nonnull final String manifestUrl,
                                           @Nullable final String poToken,
                                           @Nullable final String additionalParameters) {
        final StringBuilder parameters = new StringBuilder();
        if (!isNullOrEmpty(poToken)) {
            parameters.append("pot=").append(URLEncoder.encode(poToken, StandardCharsets.UTF_8));
        }
        if (!isNullOrEmpty(additionalParameters)) {
            if (!parameters.isEmpty()) parameters.append('&');
            parameters.append(additionalParameters);
        }
        if (parameters.isEmpty()) return manifestUrl;

        final int fragmentStart = manifestUrl.indexOf('#');
        final String base = fragmentStart < 0 ? manifestUrl : manifestUrl.substring(0, fragmentStart);
        final String fragment = fragmentStart < 0 ? "" : manifestUrl.substring(fragmentStart);
        final String separator = !base.contains("?") ? "?"
                : base.endsWith("?") || base.endsWith("&") ? "" : "&";
        return base + separator + parameters + fragment;
    }

    @Override
    public List<AudioStream> getAudioStreams() throws ExtractionException {
        assertPageFetched();
        return getItags(ADAPTIVE_FORMATS, ItagItem.ItagType.AUDIO,
                info -> YoutubeStreamFormatMapper.audioStream(info, streamType), "audio");
    }

    @Override
    public List<VideoStream> getVideoStreams() throws ExtractionException {
        assertPageFetched();
        return getItags(FORMATS, ItagItem.ItagType.VIDEO,
                info -> YoutubeStreamFormatMapper.videoStream(info, streamType, false), "video");
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() throws ExtractionException {
        assertPageFetched();
        return getItags(ADAPTIVE_FORMATS, ItagItem.ItagType.VIDEO_ONLY,
                info -> YoutubeStreamFormatMapper.videoStream(info, streamType, true), "video-only");
    }

    @Override
    @Nonnull
    public List<SubtitlesStream> getSubtitlesDefault() throws ParsingException {
        return getSubtitles(MediaFormat.TTML);
    }

    @Override
    @Nonnull
    public List<SubtitlesStream> getSubtitles(final MediaFormat format) throws ParsingException {
        assertPageFetched();
        return parseSubtitles(playerCaptionsTracklistRenderer, format);
    }

    static List<SubtitlesStream> parseSubtitles(@Nonnull final JsonObject captionTracks,
                                               @Nonnull final MediaFormat format)
            throws ParsingException {
        final List<SubtitlesStream> subtitlesToReturn = new ArrayList<>();
        final JsonArray tracks = captionTracks.getArray("captionTracks");
        for (int i = 0; i < tracks.size(); i++) {
            final JsonObject track = tracks.getObject(i);
            final String languageCode = track.getString("languageCode");
            final String baseUrl = track.getString("baseUrl");
            final String vssId = track.getString("vssId");
            if (languageCode != null && baseUrl != null && vssId != null) {
                subtitlesToReturn.add(new SubtitlesStream.Builder()
                        .setContent(withSubtitleFormat(baseUrl, format.getSuffix()), true)
                        .setMediaFormat(format)
                        .setLanguageCode(languageCode)
                        .setAutoGenerated(vssId.startsWith("a."))
                        .build());
            }
        }
        return subtitlesToReturn;
    }

    private static String withSubtitleFormat(final String baseUrl, final String suffix) {
        final int fragmentStart = baseUrl.indexOf('#');
        final String head = fragmentStart < 0 ? baseUrl : baseUrl.substring(0, fragmentStart);
        final String fragment = fragmentStart < 0 ? "" : baseUrl.substring(fragmentStart);
        final int queryStart = head.indexOf('?');
        final String path = queryStart < 0 ? head : head.substring(0, queryStart);
        final List<String> parameters = new ArrayList<>();
        if (queryStart >= 0) {
            for (final String parameter : head.substring(queryStart + 1).split("&")) {
                final int separator = parameter.indexOf('=');
                final String key = separator < 0 ? parameter : parameter.substring(0, separator);
                final String decodedKey;
                try {
                    decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
                } catch (final IllegalArgumentException ignored) {
                    parameters.add(parameter);
                    continue;
                }
                if (!"fmt".equals(decodedKey) && !"tlang".equals(decodedKey)) {
                    parameters.add(parameter);
                }
            }
        }
        parameters.add("fmt=" + URLEncoder.encode(suffix, StandardCharsets.UTF_8));
        return path + "?" + String.join("&", parameters) + fragment;
    }

    @Override
    public StreamType getStreamType() {
        assertPageFetched();

        return streamType;
    }

    private void setStreamType() {
        streamType = parseStreamType(playerResponse);
    }

    static StreamType parseStreamType(@Nonnull final JsonObject response) {
        if (response.getObject(PLAYABILITY_STATUS).has("liveStreamability")) {
            return StreamType.LIVE_STREAM;
        }
        return response.getObject(VIDEO_DETAILS).getBoolean("isPostLiveDvr", false)
                ? StreamType.POST_LIVE_STREAM : StreamType.VIDEO_STREAM;
    }

    @Nullable
    @Override
    public MultiInfoItemsCollector getRelatedItems() throws ExtractionException {
        assertPageFetched();

        if (getAgeLimit() != NO_AGE_LIMIT) {
            return null;
        }

        try {
            final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

            final JsonArray results = nextResponse
                    .getObject("contents")
                    .getObject("twoColumnWatchNextResults")
                    .getObject("secondaryResults")
                    .getObject("secondaryResults")
                    .getArray("results");

            final TimeAgoParser timeAgoParser = getTimeAgoParser();
            results.streamAsJsonObjects()
                    .map(result -> parseRelatedItem(result, timeAgoParser))
                    .filter(Objects::nonNull)
                    .forEach(collector::commit);

            return collector;
        } catch (final Exception e) {
            throw new ParsingException("Could not get related videos", e);
        }
    }

    @Nullable
    static InfoItemExtractor parseRelatedItem(@Nonnull final JsonObject result,
                                              @Nullable final TimeAgoParser timeAgoParser) {
        if (result.has("compactVideoRenderer")) {
            return new YoutubeStreamInfoItemExtractor(
                    result.getObject("compactVideoRenderer"), timeAgoParser);
        }
        if (result.has("compactRadioRenderer")) {
            return new YoutubeMixOrPlaylistInfoItemExtractor(
                    result.getObject("compactRadioRenderer"));
        }
        if (result.has("compactPlaylistRenderer")) {
            return new YoutubeMixOrPlaylistInfoItemExtractor(
                    result.getObject("compactPlaylistRenderer"));
        }

        final JsonObject lockupViewModel = result.getObject("lockupViewModel");
        return switch (lockupViewModel.getString("contentType", "")) {
            case "LOCKUP_CONTENT_TYPE_PLAYLIST", "LOCKUP_CONTENT_TYPE_PODCAST" ->
                    new YoutubeMixOrPlaylistLockupInfoItemExtractor(lockupViewModel);
            case "LOCKUP_CONTENT_TYPE_VIDEO" ->
                    new YoutubeStreamInfoItemLockupExtractor(lockupViewModel, timeAgoParser);
            default -> null;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorMessage() {
        return parsePlayerErrorMessage(playerResponse);
    }

    @Nullable
    static String parsePlayerErrorMessage(@Nullable final JsonObject playerResponse) {
        if (playerResponse == null) return null;
        return getTextFromObject(playerResponse.getObject(PLAYABILITY_STATUS)
                .getObject("errorScreen").getObject("playerErrorMessageRenderer")
                .getObject("reason"));
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Fetch page
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String videoId = getId();

        final Localization localization = getExtractorLocalization();
        final ContentCountry contentCountry = getExtractorContentCountry();

        final PoTokenProvider poTokenProviderInstance = poTokenProvider;
        final boolean noPoTokenProviderSet = poTokenProviderInstance == null;

        final PoTokenResult androidPoTokenResult = noPoTokenProviderSet ? null
                : poTokenProviderInstance.getAndroidClientPoToken(videoId);

        fetchAndroidClient(localization, contentCountry, videoId, androidPoTokenResult);

        setStreamType();

        if (fetchIosClient) {
            final PoTokenResult iosPoTokenResult = noPoTokenProviderSet ? null
                    : poTokenProviderInstance.getIosClientPoToken(videoId);
            fetchIosClient(localization, contentCountry, videoId, iosPoTokenResult);
        }

        fetchVisionOsClient(localization, contentCountry, videoId);

        fetchWebClientMetadataAndSetThumbnails(localization, contentCountry, videoId);

        final byte[] nextBody = JsonWriter.string(
                prepareDesktopJsonBuilder(localization, contentCountry)
                        .value(VIDEO_ID, videoId)
                        .value(CONTENT_CHECK_OK, true)
                        .value(RACY_CHECK_OK, true)
                        .done())
                .getBytes(StandardCharsets.UTF_8);
        nextResponse = getJsonPostResponse(NEXT, nextBody, localization);
    }

    static void checkPlayabilityStatus(@Nonnull final JsonObject playabilityStatus)
            throws ParsingException {
        final String status = playabilityStatus.getString("status");
        if (status == null || status.equalsIgnoreCase("ok")) return;
        final String reason = playabilityStatus.getString("reason");
        final String normalizedStatus = status.toLowerCase(Locale.ROOT);
        final String normalizedReason = reason == null ? "" : reason.toLowerCase(Locale.ROOT);

        if (normalizedStatus.equals("login_required")) {
            if (normalizedReason.contains("inappropriate for some users")) {
                throw new AgeRestrictedContentException(
                        "This age-restricted video cannot be watched anonymously");
            }
            if (normalizedReason.contains("private")) {
                throw new PrivateContentException("This video is private");
            }
            if (normalizedReason.contains("a bot")) {
                throw new SignInConfirmNotBotException(
                        "YouTube access was blocked: " + status + ": \"" + reason + "\"");
            }
        } else if (normalizedStatus.equals("unplayable") || normalizedStatus.equals("error")) {
            if (normalizedReason.contains("music premium")) {
                throw new YoutubeMusicPremiumContentException();
            }
            if (normalizedReason.contains("payment")) {
                throw new PaidContentException("This video is a paid video");
            }
            if (normalizedReason.contains("members")) {
                throw new PaidContentException("This video is only available for members of "
                        + "the channel of this video");
            }
            if (normalizedReason.contains("country")) {
                throw new GeographicRestrictionException(
                        "This video is not available in client's country.");
            }
            if (normalizedReason.contains("closed") || normalizedReason.contains("terminated")) {
                throw new AccountTerminatedException(reason);
            }
        }

        throw new ContentNotAvailableException("Got error " + status + ": \"" + reason + "\"");
    }

    private void fetchAndroidClient(@Nonnull final Localization localization,
                                    @Nonnull final ContentCountry contentCountry,
                                    @Nonnull final String videoId,
                                    @Nullable final PoTokenResult androidPoTokenResult)
            throws IOException, ExtractionException {
        androidCpn = generateContentPlaybackNonce();

        if (androidPoTokenResult == null) {
            playerResponse = YoutubeStreamHelper.getAndroidReelPlayerResponse(
                    contentCountry, localization, videoId, androidCpn);
        } else {
            playerResponse = YoutubeStreamHelper.getAndroidPlayerResponse(
                    contentCountry, localization, videoId, androidCpn,
                    androidPoTokenResult);
        }

        checkPlayabilityStatus(playerResponse.getObject(PLAYABILITY_STATUS));
        if (!isPlayerResponseValid(playerResponse, videoId)) {
            throw new ExtractionException("ANDROID player response is not valid");
        }

        androidStreamingData = playerResponse.getObject(STREAMING_DATA);

        playerCaptionsTracklistRenderer = playerResponse.getObject(CAPTIONS)
                .getObject(PLAYER_CAPTIONS_TRACKLIST_RENDERER);

        if (androidPoTokenResult != null) {
            androidStreamingUrlsPoToken = androidPoTokenResult.streamingDataPoToken;
        }
    }

    private void fetchIosClient(@Nonnull final Localization localization,
                                @Nonnull final ContentCountry contentCountry,
                                @Nonnull final String videoId,
                                @Nullable final PoTokenResult iosPoTokenResult) {
        try {
            iosCpn = generateContentPlaybackNonce();

            final JsonObject iosPlayerResponse = YoutubeStreamHelper.getIosPlayerResponse(
                    contentCountry, localization, videoId, iosCpn, iosPoTokenResult);

            if (isPlayerResponseValid(iosPlayerResponse, videoId)) {
                iosStreamingData = iosPlayerResponse.getObject(STREAMING_DATA);

                if (isNullOrEmpty(playerCaptionsTracklistRenderer)) {
                    playerCaptionsTracklistRenderer = iosPlayerResponse.getObject(CAPTIONS)
                            .getObject(PLAYER_CAPTIONS_TRACKLIST_RENDERER);
                }

                if (iosPoTokenResult != null) {
                    iosStreamingUrlsPoToken = iosPoTokenResult.streamingDataPoToken;
                }
            }
        } catch (final Exception ignored) {
            // Ignore exceptions related to IOS client fetching or parsing, as it is not
            // compulsory to play contents
        }
    }

    private void fetchVisionOsClient(@Nonnull final Localization localization,
                                     @Nonnull final ContentCountry contentCountry,
                                     @Nonnull final String videoId) {
        try {
            visionOsCpn = generateContentPlaybackNonce();

            final JsonObject visionOsPlayerResponse = YoutubeStreamHelper.getVisionOsPlayerResponse(
                    contentCountry, localization, videoId, visionOsCpn);

            if (isPlayerResponseValid(visionOsPlayerResponse, videoId)) {
                visionOsStreamingData = visionOsPlayerResponse.getObject(STREAMING_DATA);

                if (isNullOrEmpty(playerCaptionsTracklistRenderer)) {
                    playerCaptionsTracklistRenderer = visionOsPlayerResponse.getObject(CAPTIONS)
                            .getObject(PLAYER_CAPTIONS_TRACKLIST_RENDERER);
                }
            }
        } catch (final Exception ignored) {
            // Ignore exceptions related to VISIONOS client fetching or parsing, as it is not
            // compulsory to play contents
        }
    }

    private void fetchWebClientMetadataAndSetThumbnails(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId) {
        final JsonObject webPlayerResponse;
        try {
            webPlayerResponse = YoutubeStreamHelper.getWebMetadataPlayerResponse(
                    localization, contentCountry, videoId);
        } catch (final Exception ignored) {
            usePrimaryResponseMetadata();
            return;
        }

        if (!isPlayerResponseValid(webPlayerResponse, videoId)) {
            usePrimaryResponseMetadata();
            return;
        }
        playerMicroFormatRenderer = webPlayerResponse.getObject("microformat")
                .getObject("playerMicroformatRenderer");
        thumbnailsArray = preferredThumbnails(webPlayerResponse, playerResponse);
    }

    private void usePrimaryResponseMetadata() {
        playerMicroFormatRenderer = new JsonObject();
        thumbnailsArray = playerResponse.getObject(VIDEO_DETAILS)
                .getObject(THUMBNAIL)
                .getArray(THUMBNAILS);
    }

    static JsonArray preferredThumbnails(@Nonnull final JsonObject webResponse,
                                         @Nonnull final JsonObject primaryResponse) {
        final JsonArray web = webResponse.getObject(VIDEO_DETAILS)
                .getObject(THUMBNAIL).getArray(THUMBNAILS);
        return web.isEmpty() ? primaryResponse.getObject(VIDEO_DETAILS)
                .getObject(THUMBNAIL).getArray(THUMBNAILS) : web;
    }

    static boolean isPlayerResponseValid(@Nonnull final JsonObject playerResponse,
                                         @Nullable final String videoId) {
        return videoId != null && videoId.equals(playerResponse.getObject(VIDEO_DETAILS)
                .getString(VIDEO_ID));
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    @Nonnull
    private JsonObject getVideoPrimaryInfoRenderer() {
        if (videoPrimaryInfoRenderer != null) {
            return videoPrimaryInfoRenderer;
        }

        videoPrimaryInfoRenderer = getVideoInfoRenderer("videoPrimaryInfoRenderer");
        return videoPrimaryInfoRenderer;
    }

    @Nonnull
    private JsonObject getVideoSecondaryInfoRenderer() {
        if (videoSecondaryInfoRenderer != null) {
            return videoSecondaryInfoRenderer;
        }

        videoSecondaryInfoRenderer = getVideoInfoRenderer("videoSecondaryInfoRenderer");
        return videoSecondaryInfoRenderer;
    }

    @Nonnull
    private JsonObject getVideoInfoRenderer(@Nonnull final String videoRendererName) {
        return findVideoRenderer(nextResponse, videoRendererName);
    }

    @Nonnull
    static JsonObject findVideoRenderer(@Nonnull final JsonObject response,
                                        @Nonnull final String rendererName) {
        final JsonArray contents = response.getObject("contents")
                .getObject("twoColumnWatchNextResults")
                .getObject("results")
                .getObject("results")
                .getArray("contents");
        for (final JsonObject content : contents.streamAsJsonObjects().toList()) {
            if (content.has(rendererName)) return content.getObject(rendererName);
        }
        return new JsonObject();
    }

    @Nonnull
    private <T extends Stream> List<T> getItags(
            final String streamingDataKey,
            final ItagItem.ItagType itagTypeWanted,
            final java.util.function.Function<ItagInfo, T> streamBuilderHelper,
            final String streamTypeExceptionMessage) throws ParsingException {
        try {
            final String videoId = getId();
            final List<T> streamList = new ArrayList<>();

            appendDistinctStreams(streamList, getStreamsFromStreamingDataKey(videoId,
                    androidStreamingData, streamingDataKey, itagTypeWanted, androidCpn,
                    androidStreamingUrlsPoToken), streamBuilderHelper);
            appendDistinctStreams(streamList, getStreamsFromStreamingDataKey(videoId,
                    visionOsStreamingData, streamingDataKey, itagTypeWanted, visionOsCpn, null),
                    streamBuilderHelper);
            appendDistinctStreams(streamList, getStreamsFromStreamingDataKey(videoId,
                    iosStreamingData, streamingDataKey, itagTypeWanted, iosCpn,
                    iosStreamingUrlsPoToken), streamBuilderHelper);

            return streamList;
        } catch (final Exception e) {
            throw new ParsingException(
                    "Could not get " + streamTypeExceptionMessage + " streams", e);
        }
    }

    static <T extends Stream> void appendDistinctStreams(
            @Nonnull final List<T> target,
            @Nonnull final java.util.stream.Stream<ItagInfo> candidates,
            @Nonnull final java.util.function.Function<ItagInfo, T> builder) {
        candidates.map(builder).forEachOrdered(stream -> {
            if (!Stream.containSimilarStream(stream, target)) target.add(stream);
        });
    }

    @Nonnull
    private java.util.stream.Stream<ItagInfo> getStreamsFromStreamingDataKey(
            final String videoId,
            final JsonObject streamingData,
            final String streamingDataKey,
            @Nonnull final ItagItem.ItagType itagTypeWanted,
            @Nonnull final String contentPlaybackNonce,
            @Nullable final String poToken) {
        if (streamingData == null || !streamingData.has(streamingDataKey)) {
            return java.util.stream.Stream.empty();
        }

        return streamingData.getArray(streamingDataKey).streamAsJsonObjects()
                .map(formatData -> {
                    try {
                        return YoutubeStreamFormatMapper.map(videoId, formatData,
                                itagTypeWanted, streamType, contentPlaybackNonce, poToken);
                    } catch (final ExtractionException ignored) {
                        // Unsupported formats and unusable stream URLs are skipped.
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    /**
     * {@inheritDoc}
     * Should return a list of Frameset object that contains preview of stream frames
     *
     * <p><b>Warning:</b> When using this method be aware
     * that the YouTube API very rarely returns framesets,
     * that are slightly too small e.g. framesPerPageX = 5, frameWidth = 160, but the url contains
     * a storyboard that is only 795 pixels wide (5*160 &gt; 795). You will need to handle this
     * "manually" to avoid errors.</p>
     *
     * @see historical pull request #11596
     */
    @Nonnull
    @Override
    public List<Frameset> getFrames() throws ExtractionException {
        return parseStoryboardFrames(playerResponse);
    }

    static List<Frameset> parseStoryboardFrames(@Nonnull final JsonObject playerResponse)
            throws ExtractionException {
        try {
            final JsonObject storyboards = playerResponse.getObject("storyboards");
            final String rendererName = storyboards.has("playerLiveStoryboardSpecRenderer")
                    ? "playerLiveStoryboardSpecRenderer" : "playerStoryboardSpecRenderer";
            final JsonObject storyboardsRenderer = storyboards.getObject(rendererName);
            final String storyboardsRendererSpec = storyboardsRenderer.getString("spec");
            if (isNullOrEmpty(storyboardsRendererSpec)) {
                return Collections.emptyList();
            }

            final String[] spec = storyboardsRendererSpec.split("\\|");
            final String url = spec[0];
            final List<Frameset> result = new ArrayList<>(Math.max(0, spec.length - 1));

            for (int i = 1; i < spec.length; ++i) {
                final String[] parts = spec[i].split("#");
                if (parts.length != 8) {
                    continue;
                }
                final int durationPerFrame = Integer.parseInt(parts[5]);
                if (durationPerFrame <= 0) continue;
                final int totalCount = Integer.parseInt(parts[2]);
                final int framesPerPageX = Integer.parseInt(parts[3]);
                final int framesPerPageY = Integer.parseInt(parts[4]);
                if (totalCount < 0 || framesPerPageX <= 0 || framesPerPageY <= 0) continue;
                final String baseUrl = url.replace("$L", String.valueOf(i - 1))
                        .replace("$N", parts[6]) + "&sigh=" + parts[7];
                final List<String> urls;
                if (baseUrl.contains("$M")) {
                    final long framesPerPage = (long) framesPerPageX * framesPerPageY;
                    final int totalPages = (int) ((totalCount + framesPerPage - 1) / framesPerPage);
                    urls = new ArrayList<>(totalPages);
                    for (int j = 0; j < totalPages; j++) {
                        urls.add(baseUrl.replace("$M", String.valueOf(j)));
                    }
                } else {
                    urls = Collections.singletonList(baseUrl);
                }
                result.add(new Frameset(
                        urls,
                        /*frameWidth=*/Integer.parseInt(parts[0]),
                        /*frameHeight=*/Integer.parseInt(parts[1]),
                        totalCount,
                        durationPerFrame,
                        framesPerPageX,
                        framesPerPageY
                ));
            }
            return result;
        } catch (final Exception e) {
            throw new ExtractionException("Could not get frames", e);
        }
    }

    @Nonnull
    @Override
    public Privacy getPrivacy() {
        return parsePrivacy(playerMicroFormatRenderer, getVideoPrimaryInfoRenderer());
    }

    static Privacy parsePrivacy(@Nonnull final JsonObject microFormat,
                                @Nonnull final JsonObject primaryInfo) {
        if (microFormat.getBoolean("isUnlisted")) return Privacy.UNLISTED;
        for (final JsonObject badge : primaryInfo.getArray("badges").streamAsJsonObjects().toList()) {
            if ("PRIVACY_UNLISTED".equals(badge.getObject("metadataBadgeRenderer")
                    .getObject("icon").getString("iconType"))) return Privacy.UNLISTED;
        }
        return Privacy.PUBLIC;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return parseCategory(playerMicroFormatRenderer);
    }

    static String parseCategory(@Nonnull final JsonObject microFormat) {
        return microFormat.getString("category", "");
    }

    @Nonnull
    @Override
    public String getLicence() throws ParsingException {
        return parseLicense(getVideoSecondaryInfoRenderer());
    }

    static String parseLicense(@Nonnull final JsonObject secondaryInfo) {
        final JsonArray rows = secondaryInfo.getObject("metadataRowContainer")
                .getObject("metadataRowContainerRenderer").getArray("rows");
        for (int index = 0; index < rows.size(); index++) {
            final JsonObject row = rows.getObject(index).getObject("metadataRowRenderer");
            if (!"Licence".equals(getTextFromObject(row.getObject(TITLE)))) continue;
            final JsonArray contents = row.getArray("contents");
            if (contents.isEmpty()) continue;
            final String license = getTextFromObject(contents.getObject(0));
            if (!isNullOrEmpty(license)) return license;
        }
        return "YouTube licence";
    }

    @Override
    public Locale getLanguageInfo() {
        return null;
    }

    @Nonnull
    @Override
    public List<String> getTags() {
        return parseTags(playerResponse.getObject(VIDEO_DETAILS));
    }

    static List<String> parseTags(@Nonnull final JsonObject videoDetails) {
        final List<String> tags = new ArrayList<>();
        final JsonArray keywords = videoDetails.getArray("keywords");
        for (int index = 0; index < keywords.size(); index++) {
            final String tag = keywords.getString(index);
            if (tag != null) tags.add(tag);
        }
        return tags;
    }

    @Nonnull
    @Override
    public List<StreamSegment> getStreamSegments() throws ParsingException {
        return parseStreamSegments(nextResponse, getUrl(), getLength());
    }

    static List<StreamSegment> parseStreamSegments(final JsonObject nextResponse,
                                                   final String videoUrl,
                                                   final long duration)
            throws ParsingException {
        final JsonArray panels = nextResponse.getArray("engagementPanels");
        if (panels.isEmpty()) {
            return Collections.emptyList();
        }

        final JsonArray segmentsArray = panels
                .streamAsJsonObjects()
                .filter(panel -> "engagement-panel-macro-markers-description-chapters".equals(
                        panel.getObject("engagementPanelSectionListRenderer")
                                .getString("panelIdentifier")))
                .map(panel -> panel
                        .getObject("engagementPanelSectionListRenderer")
                        .getObject("content")
                        .getObject("macroMarkersListRenderer")
                        .getArray("contents"))
                .findFirst()
                .orElse(null);

        if (segmentsArray == null || segmentsArray.isEmpty()) {
            return Collections.emptyList();
        }

        final List<StreamSegment> segments = new ArrayList<>();
        for (final JsonObject item : segmentsArray.streamAsJsonObjects().toList()) {
            final JsonObject segmentJson = item.getObject("macroMarkersListItemRenderer");
            final int startTimeSeconds = segmentJson.getObject("onTap")
                    .getObject("watchEndpoint").getInt("startTimeSeconds", -1);

            if (startTimeSeconds < 0) {
                throw new ParsingException("Could not get stream segment start time.");
            }
            if (startTimeSeconds > duration) {
                break;
            }

            final String title = getTextFromObject(segmentJson.getObject(TITLE));
            if (isNullOrEmpty(title)) {
                throw new ParsingException("Could not get stream segment title.");
            }

            final StreamSegment segment = new StreamSegment(title, startTimeSeconds);
            final int fragmentIndex = videoUrl.indexOf('#');
            final String url = fragmentIndex < 0 ? videoUrl : videoUrl.substring(0, fragmentIndex);
            final String fragment = fragmentIndex < 0 ? "" : videoUrl.substring(fragmentIndex);
            final String separator = url.contains("?")
                    ? (url.endsWith("?") || url.endsWith("&") ? "" : "&") : "?";
            segment.setUrl(url + separator + "t=" + startTimeSeconds + fragment);
            final JsonArray previews = segmentJson.getObject(THUMBNAIL).getArray(THUMBNAILS);
            if (!previews.isEmpty()) {
                final String previewUrl = previews.getObject(previews.size() - 1).getString("url");
                if (previewUrl != null) {
                    segment.setPreviewUrl(fixThumbnailUrl(previewUrl));
                }
            }
            segments.add(segment);
        }
        return segments;
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() throws ParsingException {
        return YoutubeMetaInfoHelper.getMetaInfo(nextResponse
                .getObject("contents")
                .getObject("twoColumnWatchNextResults")
                .getObject("results")
                .getObject("results")
                .getArray("contents"));
    }

    /**
     * Set the {@link PoTokenProvider} instance to be used for fetching {@code poToken}s.
     *
     * <p>
     * This method allows setting an implementation of {@link PoTokenProvider} which will be used
     * to obtain poTokens required for YouTube player requests and streaming URLs. These tokens
     * are used by YouTube to verify the integrity of the user's device or browser and are required
     * for playback with several clients.
     * </p>
     *
     * <p>
     * Without a {@link PoTokenProvider}, the extractor makes its best effort to fetch as many
     * streams as possible, but without {@code poToken}s, some formats may be not available or
     * fetching may be slower due to additional requests done to get streams.
     * </p>
     *
     * <p>
     * Note that any provider change will be only applied on the next {@link #fetchPage()} request.
     * </p>
     *
     * @param poTokenProvider the {@link PoTokenProvider} instance to set, which can be null to
     *                        remove a provider already passed
     * @see PoTokenProvider
     */
    @SuppressWarnings("unused")
    public static void setPoTokenProvider(@Nullable final PoTokenProvider poTokenProvider) {
        YoutubeStreamExtractor.poTokenProvider = poTokenProvider;
    }

    /**
     * Set whether to fetch the iOS player responses.
     *
     * <p>
     * This method allows fetching the iOS player response, which can be useful in scenarios where
     * streams from the iOS player response are needed, especially HLS manifests.
     * </p>
     *
     * <p>
     * Note that at the time of writing, YouTube is rolling out a {@code poToken} requirement on
     * this client, formats from HLS manifests do not seem to be affected.
     * </p>
     *
     * @param fetchIosClient whether to fetch the iOS client
     */
    @SuppressWarnings("unused")
    public static void setFetchIosClient(final boolean fetchIosClient) {
        YoutubeStreamExtractor.fetchIosClient = fetchIosClient;
    }
}
