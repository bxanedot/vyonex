// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.ContentAvailability;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class YoutubeStreamInfoItemExtractor implements StreamInfoItemExtractor {
    // shortcut: English accessibility labels only, add locales when fixtures require them.
    private static final Pattern ACCESSIBLE_VIEW_COUNT = Pattern.compile("([\\d,]+) views$");
    private static final String NO_VIEWS = "no views";
    private static final List<String> BYLINE_FIELDS =
            List.of("longBylineText", "ownerText", "shortBylineText");

    private final JsonObject item;
    private final TimeAgoParser dates;
    private final StreamType streamType;

    public YoutubeStreamInfoItemExtractor(final JsonObject item,
                                          @Nullable final TimeAgoParser timeAgoParser) {
        this.item = item;
        dates = timeAgoParser;
        streamType = isLive(item) ? StreamType.LIVE_STREAM : StreamType.VIDEO_STREAM;
    }

    @Override public StreamType getStreamType() { return streamType; }

    @Override
    public boolean isAd() throws ParsingException {
        final String name = getName();
        return isPremium() || "[Private video]".equals(name) || "[Deleted video]".equals(name);
    }

    @Override
    public String getUrl() throws ParsingException {
        final String videoId = item.getString("videoId");
        if (isNullOrEmpty(videoId)) throw new ParsingException("Could not get video ID");
        try {
            return YoutubeStreamLinkHandlerFactory.getInstance().getUrl(videoId);
        } catch (final Exception error) {
            throw new ParsingException("Could not build video URL", error);
        }
    }

    @Override
    public String getName() throws ParsingException {
        final JsonObject title = item.getObject("title");
        final String name = getTextFromObject(title);
        if (!isNullOrEmpty(name)) return name;
        if (!title.isEmpty() && !title.has("runs")) return "";
        throw new ParsingException("Could not get name");
    }

    @Override
    public long getDuration() throws ParsingException {
        if (streamType == StreamType.LIVE_STREAM) return -1;

        final String duration = firstNonEmpty(
                getTextFromObject(item.getObject("lengthText")), item.getString("lengthSeconds"));
        if (!isNullOrEmpty(duration)) return YoutubeParsingHelper.parseDurationString(duration);

        for (final JsonObject overlay : objects(item.getArray("thumbnailOverlays"))) {
            final String text = getTextFromObject(
                    overlay.getObject("thumbnailOverlayTimeStatusRenderer").getObject("text"));
            if (isNullOrEmpty(text)) continue;
            try {
                return YoutubeParsingHelper.parseDurationString(text);
            } catch (final ParsingException | NumberFormatException ignored) {
                // Decorative overlays can contain text that is not a duration.
            }
        }
        if (item.has("upcomingEventData")) return -1;
        throw new ParsingException("Could not get duration");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        final String name = firstText(BYLINE_FIELDS);
        if (isNullOrEmpty(name)) throw new ParsingException("Could not get uploader name");
        return name;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        for (final String field : BYLINE_FIELDS) {
            for (final JsonObject run : objects(item.getObject(field).getArray("runs"))) {
                final String url = getUrlFromNavigationEndpoint(
                        run.getObject("navigationEndpoint"));
                if (!isNullOrEmpty(url)) return url;
            }
        }
        throw new ParsingException("Could not get uploader url");
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        final JsonObject channelImages = item.getObject("channelThumbnailSupportedRenderers")
                .getObject("channelThumbnailWithLinkRenderer").getObject("thumbnail");
        if (!channelImages.isEmpty()) {
            return getImagesFromThumbnailsArray(channelImages.getArray("thumbnails"));
        }
        return getImagesFromThumbnailsArray(
                item.getObject("channelThumbnail").getArray("thumbnails"));
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return YoutubeParsingHelper.isVerified(item.getArray("ownerBadges"));
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        if (streamType == StreamType.LIVE_STREAM) return null;
        if (item.has("upcomingEventData")) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault()).format(premiereStart());
        }
        final String published = getTextFromObject(item.getObject("publishedTimeText"));
        if (!isNullOrEmpty(published)) return published;
        final JsonArray details = item.getObject("videoInfo").getArray("runs");
        return details.size() > 2 ? details.getObject(2).getString("text") : null;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        if (streamType == StreamType.LIVE_STREAM) return null;
        if (item.has("upcomingEventData")) return new DateWrapper(premiereStart());
        final String text = getTextualUploadDate();
        if (dates == null || isNullOrEmpty(text)) return null;
        try {
            return dates.parse(text);
        } catch (final ParsingException error) {
            throw new ParsingException("Could not get upload date", error);
        }
    }

    @Override
    public long getViewCount() {
        if (isPremium() || item.has("upcomingEventData")) return -1;

        final Long viewCount = parseCount(getTextFromObject(item.getObject("viewCountText")), false);
        if (viewCount != null) return viewCount;

        if (streamType != StreamType.LIVE_STREAM) {
            final String label = item.getObject("title").getObject("accessibility")
                    .getObject("accessibilityData").getString("label", "");
            final Matcher accessible = ACCESSIBLE_VIEW_COUNT.matcher(label);
            if (label.toLowerCase(Locale.ROOT).endsWith(NO_VIEWS)) return 0;
            if (accessible.find()) {
                final Long count = parseCount(accessible.group(1), false);
                if (count != null) return count;
            }
        }

        final JsonArray details = item.getObject("videoInfo").getArray("runs");
        if (!details.isEmpty()) {
            final Long count = parseCount(details.getObject(0).getString("text"), true);
            if (count != null) return count;
        }
        final Long shortCount = parseCount(getTextFromObject(item.getObject("shortViewCountText")), true);
        return shortCount == null ? -1 : shortCount;
    }

    @Nullable
    private static Long parseCount(@Nullable final String text, final boolean suffixes) {
        if (isNullOrEmpty(text)) return null;
        final String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains(NO_VIEWS)) return 0L;
        if (lower.contains("recommended")) return -1L;
        try {
            return suffixes ? Utils.mixedNumberWordToLong(text)
                    : Long.parseLong(Utils.removeNonDigitCharacters(text));
        } catch (final ParsingException | RuntimeException ignored) {
            return null;
        }
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return getThumbnailsFromInfoItem(item);
    }

    @Nullable
    @Override
    public String getShortDescription() {
        final JsonArray snippets = item.getArray("detailedMetadataSnippets");
        if (!snippets.isEmpty()) {
            return getTextFromObject(snippets.getObject(0).getObject("snippetText"));
        }
        return item.has("descriptionSnippet")
                ? getTextFromObject(item.getObject("descriptionSnippet")) : null;
    }

    @Override
    public boolean isShortFormContent() throws ParsingException {
        try {
            final JsonObject endpoint = item.getObject("navigationEndpoint");
            final JsonObject page = endpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata");
            if ("WEB_PAGE_TYPE_SHORTS".equals(page.getString("webPageType"))
                    || endpoint.has("reelWatchEndpoint")) return true;

            for (final JsonObject overlay : objects(item.getArray("thumbnailOverlays"))) {
                final JsonObject status = overlay.getObject("thumbnailOverlayTimeStatusRenderer");
                final String style = status.getString("style", "");
                final String icon = status.getObject("icon").getString("iconType", "");
                if ("SHORTS".equalsIgnoreCase(style)
                        || icon.toLowerCase(Locale.ROOT).contains("shorts")) return true;
            }
            return false;
        } catch (final RuntimeException error) {
            throw new ParsingException("Could not determine if this is short-form content", error);
        }
    }

    @Nonnull
    @Override
    public ContentAvailability getContentAvailability() {
        if (item.has("upcomingEventData")) return ContentAvailability.UPCOMING;
        if (hasBadgeStyle("BADGE_STYLE_TYPE_MEMBERS_ONLY")) return ContentAvailability.MEMBERSHIP;
        if (isPremium()) return ContentAvailability.PAID;
        return ContentAvailability.AVAILABLE;
    }

    private boolean isPremium() {
        for (final JsonObject badge : objects(item.getArray("badges"))) {
            if ("Premium".equals(badge.getObject("metadataBadgeRenderer").getString("label"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBadgeStyle(final String style) {
        for (final JsonObject badge : objects(item.getArray("badges"))) {
            if (style.equals(badge.getObject("metadataBadgeRenderer").getString("style"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLive(final JsonObject item) {
        for (final JsonObject badge : objects(item.getArray("badges"))) {
            final JsonObject data = badge.getObject("metadataBadgeRenderer");
            if ("BADGE_STYLE_TYPE_LIVE_NOW".equals(data.getString("style"))
                    || "LIVE NOW".equals(data.getString("label"))) return true;
        }
        for (final JsonObject overlay : objects(item.getArray("thumbnailOverlays"))) {
            if ("LIVE".equalsIgnoreCase(overlay
                    .getObject("thumbnailOverlayTimeStatusRenderer").getString("style", ""))) {
                return true;
            }
        }
        return false;
    }

    private Instant premiereStart() throws ParsingException {
        final String value = item.getObject("upcomingEventData").getString("startTime");
        try {
            return Instant.ofEpochSecond(Long.parseLong(value));
        } catch (final RuntimeException error) {
            throw new ParsingException("Could not parse premiere start time", error);
        }
    }

    private String firstText(final List<String> fields) {
        for (final String field : fields) {
            final String text = getTextFromObject(item.getObject(field));
            if (!isNullOrEmpty(text)) return text;
        }
        return null;
    }

    private static String firstNonEmpty(final String first, final String second) {
        return isNullOrEmpty(first) ? second : first;
    }

    private static List<JsonObject> objects(final JsonArray array) {
        return array.streamAsJsonObjects().toList();
    }
}
