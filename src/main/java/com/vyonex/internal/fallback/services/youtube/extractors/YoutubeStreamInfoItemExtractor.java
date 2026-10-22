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
    private static final Pattern ACCESSIBILITY_VIEW_COUNT = Pattern.compile("([\\d,]+) views$");
    private static final String NO_VIEWS = "no views";

    private final JsonObject item;
    private final TimeAgoParser timeAgoParser;
    private StreamType streamType;
    private Boolean premiere;

    public YoutubeStreamInfoItemExtractor(final JsonObject item,
                                          @Nullable final TimeAgoParser timeAgoParser) {
        this.item = item;
        this.timeAgoParser = timeAgoParser;
    }

    @Override
    public StreamType getStreamType() {
        if (streamType != null) return streamType;
        for (final JsonObject badge : item.getArray("badges").streamAsJsonObjects().toList()) {
            final JsonObject renderer = badge.getObject("metadataBadgeRenderer");
            if ("BADGE_STYLE_TYPE_LIVE_NOW".equals(renderer.getString("style"))
                    || "LIVE NOW".equals(renderer.getString("label"))) {
                return streamType = StreamType.LIVE_STREAM;
            }
        }
        for (final JsonObject overlay : item.getArray("thumbnailOverlays")
                .streamAsJsonObjects().toList()) {
            if ("LIVE".equalsIgnoreCase(overlay.getObject("thumbnailOverlayTimeStatusRenderer")
                    .getString("style", ""))) {
                return streamType = StreamType.LIVE_STREAM;
            }
        }
        return streamType = StreamType.VIDEO_STREAM;
    }

    @Override
    public boolean isAd() throws ParsingException {
        return isPremium() || "[Private video]".equals(getName())
                || "[Deleted video]".equals(getName());
    }

    @Override
    public String getUrl() throws ParsingException {
        try {
            return YoutubeStreamLinkHandlerFactory.getInstance().getUrl(item.getString("videoId"));
        } catch (final Exception e) {
            throw new ParsingException("Could not get url", e);
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
        if (getStreamType() == StreamType.LIVE_STREAM) return -1;
        String duration = getTextFromObject(item.getObject("lengthText"));
        if (isNullOrEmpty(duration)) duration = item.getString("lengthSeconds");
        if (isNullOrEmpty(duration)) {
            for (final JsonObject overlay : item.getArray("thumbnailOverlays")
                    .streamAsJsonObjects().toList()) {
                final JsonObject status = overlay.getObject("thumbnailOverlayTimeStatusRenderer");
                final String text = getTextFromObject(status.getObject("text"));
                if (isNullOrEmpty(text)) continue;
                try {
                    return YoutubeParsingHelper.parseDurationString(text);
                } catch (final ParsingException | NumberFormatException ignored) {
                    // Try another thumbnail duration when the overlay is decorative.
                }
            }
        } else {
            return YoutubeParsingHelper.parseDurationString(duration);
        }
        if (isPremiere()) return -1;
        throw new ParsingException("Could not get duration");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        final String name = firstText("longBylineText", "ownerText", "shortBylineText");
        if (isNullOrEmpty(name)) throw new ParsingException("Could not get uploader name");
        return name;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        for (final String field : List.of("longBylineText", "ownerText", "shortBylineText")) {
            for (final JsonObject run : item.getObject(field).getArray("runs")
                    .streamAsJsonObjects().toList()) {
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
        if (item.has("channelThumbnailSupportedRenderers")) {
            return getImagesFromThumbnailsArray(item.getObject("channelThumbnailSupportedRenderers")
                    .getObject("channelThumbnailWithLinkRenderer").getObject("thumbnail")
                    .getArray("thumbnails"));
        }
        if (item.has("channelThumbnail")) {
            return getImagesFromThumbnailsArray(item.getObject("channelThumbnail")
                    .getArray("thumbnails"));
        }
        return List.of();
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return YoutubeParsingHelper.isVerified(item.getArray("ownerBadges"));
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) return null;
        if (isPremiere()) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault()).format(getInstantFromPremiere());
        }
        final String published = getTextFromObject(item.getObject("publishedTimeText"));
        if (!isNullOrEmpty(published)) return published;
        final JsonArray runs = item.getObject("videoInfo").getArray("runs");
        return runs.size() > 2 ? runs.getObject(2).getString("text") : null;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) return null;
        if (isPremiere()) return new DateWrapper(getInstantFromPremiere());
        final String textualDate = getTextualUploadDate();
        if (timeAgoParser == null || isNullOrEmpty(textualDate)) return null;
        try {
            return timeAgoParser.parse(textualDate);
        } catch (final ParsingException e) {
            throw new ParsingException("Could not get upload date", e);
        }
    }

    @Override
    public long getViewCount() {
        if (isPremium() || isPremiere()) return -1;
        final String fullCount = getTextFromObject(item.getObject("viewCountText"));
        final Long parsed = parseViewCount(fullCount, false);
        if (parsed != null) return parsed;
        if (getStreamType() != StreamType.LIVE_STREAM) {
            final String accessibility = item.getObject("title").getObject("accessibility")
                    .getObject("accessibilityData").getString("label", "");
            final Matcher matcher = ACCESSIBILITY_VIEW_COUNT.matcher(accessibility);
            if (accessibility.toLowerCase(Locale.ROOT).endsWith(NO_VIEWS)) return 0;
            if (matcher.find()) {
                final Long accessibleCount = parseViewCount(matcher.group(1), false);
                if (accessibleCount != null) return accessibleCount;
            }
        }
        final JsonArray playlistInfo = item.getObject("videoInfo").getArray("runs");
        if (!playlistInfo.isEmpty()) {
            final Long playlistCount = parseViewCount(
                    playlistInfo.getObject(0).getString("text"), true);
            if (playlistCount != null) return playlistCount;
        }
        final Long shortCount = parseViewCount(
                getTextFromObject(item.getObject("shortViewCountText")), true);
        return shortCount == null ? -1 : shortCount;
    }

    @Nullable
    private static Long parseViewCount(@Nullable final String text, final boolean mixedNumber) {
        if (isNullOrEmpty(text)) return null;
        final String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains(NO_VIEWS)) return 0L;
        if (normalized.contains("recommended")) return -1L;
        try {
            return mixedNumber ? Utils.mixedNumberWordToLong(text)
                    : Long.parseLong(Utils.removeNonDigitCharacters(text));
        } catch (final Exception ignored) {
            return null;
        }
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return getThumbnailsFromInfoItem(item);
    }

    private boolean isPremium() {
        for (final JsonObject badge : item.getArray("badges").streamAsJsonObjects().toList()) {
            if ("Premium".equals(badge.getObject("metadataBadgeRenderer").getString("label"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPremiere() {
        if (premiere == null) premiere = item.has("upcomingEventData");
        return premiere;
    }

    private Instant getInstantFromPremiere() throws ParsingException {
        final String startTime = item.getObject("upcomingEventData").getString("startTime");
        try {
            return Instant.ofEpochSecond(Long.parseLong(startTime));
        } catch (final Exception e) {
            throw new ParsingException("Could not parse date from premiere: \"" + startTime
                    + "\"", e);
        }
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
            final JsonObject metadata = endpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata");
            if ("WEB_PAGE_TYPE_SHORTS".equals(metadata.getString("webPageType"))
                    || endpoint.has("reelWatchEndpoint")) return true;
            for (final JsonObject overlay : item.getArray("thumbnailOverlays")
                    .streamAsJsonObjects().toList()) {
                final JsonObject status = overlay.getObject("thumbnailOverlayTimeStatusRenderer");
                if ("SHORTS".equalsIgnoreCase(status.getString("style", ""))
                        || status.getObject("icon").getString("iconType", "")
                        .toLowerCase(Locale.ROOT).contains("shorts")) return true;
            }
            return false;
        } catch (final Exception e) {
            throw new ParsingException("Could not determine if this is short-form content", e);
        }
    }

    private boolean isMembersOnly() {
        return item.getArray("badges").streamAsJsonObjects()
                .map(badge -> badge.getObject("metadataBadgeRenderer").getString("style"))
                .anyMatch("BADGE_STYLE_TYPE_MEMBERS_ONLY"::equals);
    }

    @Nonnull
    @Override
    public ContentAvailability getContentAvailability() {
        if (isPremiere()) return ContentAvailability.UPCOMING;
        if (isMembersOnly()) return ContentAvailability.MEMBERSHIP;
        if (isPremium()) return ContentAvailability.PAID;
        return ContentAvailability.AVAILABLE;
    }

    private String firstText(final String... fields) {
        for (final String field : fields) {
            final String text = getTextFromObject(item.getObject(field));
            if (!isNullOrEmpty(text)) return text;
        }
        return null;
    }
}
