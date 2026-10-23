// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.ContentAvailability;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.utils.JsonUtils;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Maps YouTube's lockup card data to a VyonEx stream item. */
public class YoutubeStreamInfoItemLockupExtractor implements StreamInfoItemExtractor {
    private static final String VIDEO_PREMIERE_PREFIX = "Premieres ";
    private static final String LIVE_PREMIERE_PREFIX = "Scheduled for ";
    private static final DateTimeFormatter PREMIERE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");

    private final JsonObject item;
    private final JsonArray metadataRows;
    private final TimeAgoParser timeAgoParser;
    private StreamType streamType;
    private String uploadLabel;

    public YoutubeStreamInfoItemLockupExtractor(@Nonnull final JsonObject item,
                                                @Nullable final TimeAgoParser timeAgoParser) {
        this.item = Objects.requireNonNull(item, "video lockup is required");
        this.timeAgoParser = timeAgoParser;
        this.metadataRows = item.getObject("metadata").getObject("lockupMetadataViewModel")
                .getObject("metadata").getObject("contentMetadataViewModel")
                .getArray("metadataRows");
    }

    /** Lets channel and course-list callers select their one-row live-count behavior. */
    protected boolean isChannelOrCoursePlaylistLockupItem() {
        return false;
    }

    @Override
    public StreamType getStreamType() {
        if (streamType != null) return streamType;
        for (final JsonObject overlay : overlays().streamAsJsonObjects().toList()) {
            for (final JsonObject wrapper : overlay.getObject("thumbnailOverlayBadgeViewModel")
                    .getArray("thumbnailBadges").streamAsJsonObjects().toList()) {
                final JsonObject badge = wrapper.getObject("thumbnailBadgeViewModel");
                if (isLiveBadge(badge)) return streamType = StreamType.LIVE_STREAM;
            }
            for (final JsonObject wrapper : overlay.getObject("thumbnailBottomOverlayViewModel")
                    .getArray("badges").streamAsJsonObjects().toList()) {
                if (isLiveBadge(wrapper.getObject("thumbnailBadgeViewModel"))) {
                    return streamType = StreamType.LIVE_STREAM;
                }
            }
        }
        return streamType = StreamType.VIDEO_STREAM;
    }

    private boolean isLiveBadge(@Nonnull final JsonObject badge) {
        if ("THUMBNAIL_OVERLAY_BADGE_STYLE_LIVE".equals(badge.getString("badgeStyle"))) {
            return true;
        }
        return badge.getObject("icon").getArray("sources").streamAsJsonObjects()
                .anyMatch(source -> "LIVE".equals(source.getObject("clientResource")
                        .getString("imageName")));
    }

    @Override
    public boolean isAd() throws ParsingException {
        final String title = getName();
        return "[Private video]".equals(title) || "[Deleted video]".equals(title);
    }

    @Override
    public String getUrl() throws ParsingException {
        String videoId = item.getString("contentId");
        if (videoId == null || videoId.isBlank()) {
            videoId = item.getObject("rendererContext").getObject("commandContext")
                    .getObject("onTap").getObject("innertubeCommand")
                    .getObject("watchEndpoint").getString("videoId");
        }
        if (videoId == null || videoId.isBlank()) throw new ParsingException("Video ID is missing");
        try {
            return YoutubeStreamLinkHandlerFactory.getInstance().getUrl(videoId);
        } catch (final RuntimeException error) {
            throw new ParsingException("Could not build video URL", error);
        }
    }

    @Override
    public String getName() throws ParsingException {
        final String title = item.getObject("metadata").getObject("lockupMetadataViewModel")
                .getObject("title").getString("content");
        if (title == null || title.isBlank()) throw new ParsingException("Video title is missing");
        return title;
    }

    @Override
    public long getDuration() throws ParsingException {
        if (isLive() || isPremiere()) return -1;

        ParsingException invalidDuration = null;
        for (final JsonObject overlay : overlays().streamAsJsonObjects().toList()) {
            for (final JsonObject wrapper : overlay.getObject("thumbnailBottomOverlayViewModel")
                    .getArray("badges").streamAsJsonObjects().toList()) {
                final String label = wrapper.getObject("thumbnailBadgeViewModel").getString("text");
                if (label == null || label.codePoints().noneMatch(Character::isDigit)) continue;
                try {
                    return YoutubeParsingHelper.parseDurationString(label);
                } catch (final ParsingException error) {
                    invalidDuration = error;
                } catch (final NumberFormatException error) {
                    invalidDuration = new ParsingException("Invalid duration: " + label, error);
                }
            }
        }
        if (invalidDuration != null) {
            throw new ParsingException("Could not parse video duration", invalidDuration);
        }
        return -1;
    }

    @Override
    public String getUploaderName() throws ParsingException {
        final List<JsonArray> rows = metadataPartsRows();
        if (rows.isEmpty() || rows.get(0).isEmpty()) {
            throw new ParsingException("Uploader metadata is missing");
        }
        final String name = text(rows.get(0).getObject(0));
        if (name == null || name.isBlank()) throw new ParsingException("Uploader name is missing");
        return name;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        JsonObject avatarModel = item.getObject("metadata").getObject("lockupMetadataViewModel")
                .getObject("image").getObject("decoratedAvatarViewModel", null);
        if (avatarModel == null) {
            final JsonObject stack = item.getObject("metadata").getObject("lockupMetadataViewModel")
                    .getObject("image").getObject("avatarStackViewModel", null);
            final JsonObject firstAvatar = stack == null ? null : stack.getArray("avatars").getObject(0);
            avatarModel = firstAvatar == null ? null : firstAvatar.getObject("rendererContext")
                    .getObject("commandContext").getObject("onTap").getObject("innertubeCommand")
                    .getObject("showDialogCommand").getObject("panelLoadingStrategy")
                    .getObject("inlineContent").getObject("dialogViewModel")
                    .getObject("customContent").getObject("listViewModel")
                    .getArray("listItems").getObject(0).getObject("listItemViewModel");
        }
        if (avatarModel == null) throw new ParsingException("Uploader route is missing");

        final JsonObject command = avatarModel.getObject("rendererContext")
                .getObject("commandContext").getObject("onTap").getObject("innertubeCommand");
        final JsonObject browse = command.getObject("browseEndpoint");
        final String channelId = browse.getString("browseId");
        if (channelId != null && channelId.startsWith("UC")) {
            return YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/" + channelId);
        }

        String path = browse.getString("canonicalBaseUrl");
        if (path == null || path.isBlank()) {
            path = command.getObject("commandMetadata").getObject("webCommandMetadata")
                    .getString("url");
        }
        if (path == null || path.isBlank()) throw new ParsingException("Uploader URL is missing");
        if (path.startsWith("https://www.youtube.com/")) {
            path = path.substring("https://www.youtube.com/".length());
        } else if (path.startsWith("https://youtube.com/")) {
            path = path.substring("https://youtube.com/".length());
        }
        return YoutubeChannelLinkHandlerFactory.getInstance().getUrl(
                path.startsWith("/") ? path.substring(1) : path);
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() throws ParsingException {
        final JsonObject image = item.getObject("metadata").getObject("lockupMetadataViewModel")
                .getObject("image");
        JsonObject avatar = image.getObject("decoratedAvatarViewModel", null);
        if (avatar != null) {
            avatar = avatar.getObject("avatar");
        } else {
            final JsonObject stack = image.getObject("avatarStackViewModel", null);
            avatar = stack == null ? null : stack.getArray("avatars").getObject(0);
        }
        if (avatar == null) throw new ParsingException("Uploader avatar is missing");
        return YoutubeParsingHelper.getImagesFromThumbnailsArray(
                JsonUtils.getArray(avatar, "avatarViewModel.image.sources"));
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        final List<JsonArray> rows = metadataPartsRows();
        if (rows.isEmpty() || rows.get(0).isEmpty()) {
            throw new ParsingException("Uploader metadata is missing");
        }
        final JsonArray attachments = rows.get(0).getObject(0).getObject("text")
                .getArray("attachmentRuns");
        return YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment(attachments);
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        if (isLive()) return null;
        final String label = getDateText();
        if (label.startsWith(VIDEO_PREMIERE_PREFIX)) return label.substring(VIDEO_PREMIERE_PREFIX.length());
        if (label.startsWith(LIVE_PREMIERE_PREFIX)) return label.substring(LIVE_PREMIERE_PREFIX.length());
        return label;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        if (timeAgoParser == null) return null;
        final String label = getTextualUploadDate();
        if (label == null) return null;
        if (isPremiere()) {
            try {
                return new DateWrapper(LocalDateTime.parse(label, PREMIERE_FORMAT)
                        .atZone(ZoneOffset.UTC).toInstant(), false);
            } catch (final DateTimeParseException error) {
                throw new ParsingException("Could not parse premiere date", error);
            }
        }
        return timeAgoParser.parse(label);
    }

    @Override
    public long getViewCount() throws ParsingException {
        if (isMembersOnly()) return -1;
        final List<JsonArray> rows = metadataPartsRows();
        if (rows.isEmpty()) {
            if (isLive() && isChannelOrCoursePlaylistLockupItem()) return 0;
            throw new ParsingException("View count metadata is missing");
        }
        if (isPremiere()) return -1;
        if (isLive() && rows.size() == 1 && !isChannelOrCoursePlaylistLockupItem()) return 0;

        final JsonArray finalRow = rows.get(rows.size() - 1);
        if (finalRow.isEmpty()) throw new ParsingException("View count metadata is empty");
        final String label = text(finalRow.getObject(0));
        if (label == null || label.isBlank()) throw new ParsingException("View count is missing");
        if (label.toLowerCase(java.util.Locale.ROOT).contains("no views")) return 0;
        if (label.toLowerCase(java.util.Locale.ROOT).contains("recommended")) return -1;
        return Utils.mixedNumberWordToLong(label);
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return YoutubeParsingHelper.getImagesFromThumbnailsArray(JsonUtils.getArray(item,
                "contentImage.thumbnailViewModel.image.sources"));
    }

    @Nonnull
    @Override
    public ContentAvailability getContentAvailability() throws ParsingException {
        if (isMembersOnly()) return ContentAvailability.MEMBERSHIP;
        if (isLive()) return ContentAvailability.AVAILABLE;
        return isPremiere() ? ContentAvailability.UPCOMING : ContentAvailability.AVAILABLE;
    }

    private boolean isLive() {
        return getStreamType() != StreamType.VIDEO_STREAM;
    }

    private boolean isPremiere() throws ParsingException {
        final String label = getDateText();
        return label.startsWith(VIDEO_PREMIERE_PREFIX) || label.startsWith(LIVE_PREMIERE_PREFIX);
    }

    private boolean isMembersOnly() {
        for (final JsonObject row : metadataRows.streamAsJsonObjects().toList()) {
            for (final JsonObject badge : row.getArray("badges").streamAsJsonObjects().toList()) {
                if ("BADGE_MEMBERS_ONLY".equals(badge.getObject("badgeViewModel")
                        .getString("badgeStyle"))) return true;
            }
        }
        return false;
    }

    private String getDateText() throws ParsingException {
        final List<JsonArray> rows = metadataPartsRows();
        if (rows.isEmpty()) throw new ParsingException("Upload date metadata is missing");
        final JsonArray finalRow = rows.get(rows.size() - 1);
        if (finalRow.isEmpty()) throw new ParsingException("Upload date metadata is empty");
        final String label = text(finalRow.getObject(finalRow.size() - 1));
        if (label == null || label.isBlank()) throw new ParsingException("Upload date is missing");
        return label;
    }

    @Nullable
    private static String text(@Nonnull final JsonObject part) {
        return part.getObject("text").getString("content");
    }

    @Nonnull
    private List<JsonArray> metadataPartsRows() {
        final List<JsonArray> result = new ArrayList<>();
        for (final JsonObject row : metadataRows.streamAsJsonObjects().toList()) {
            if (row.has("metadataParts")) result.add(row.getArray("metadataParts"));
        }
        return result;
    }

    @Nonnull
    private JsonArray overlays() {
        return item.getObject("contentImage").getObject("thumbnailViewModel")
                .getArray("overlays");
    }
}
