// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.comments.CommentsInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.services.youtube.YoutubeContinuationTokenParser;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.utils.JsonUtils;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static com.vyonex.internal.fallback.comments.CommentsInfoItem.UNKNOWN_REPLY_COUNT;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;

/** Maps a standard YouTube comment renderer into VyonEx's comment API. */
public class YoutubeCommentsInfoItemExtractor implements CommentsInfoItemExtractor {
    private final JsonObject comment;
    @Nullable private final JsonObject replies;
    private final String videoUrl;
    private final TimeAgoParser timeAgoParser;

    public YoutubeCommentsInfoItemExtractor(@Nonnull final JsonObject comment,
                                            @Nullable final JsonObject replies,
                                            @Nonnull final String videoUrl,
                                            @Nonnull final TimeAgoParser timeAgoParser) {
        this.comment = Objects.requireNonNull(comment, "comment data is required");
        this.replies = replies;
        this.videoUrl = Objects.requireNonNull(videoUrl, "video URL is required");
        this.timeAgoParser = Objects.requireNonNull(timeAgoParser, "date parser is required");
    }

    @Nonnull
    private List<Image> authorImages() throws ParsingException {
        try {
            return getImagesFromThumbnailsArray(JsonUtils.getArray(comment,
                    "authorThumbnail.thumbnails"));
        } catch (final RuntimeException error) {
            throw new ParsingException("Could not read comment author images", error);
        }
    }

    @Nonnull @Override public String getUrl() { return videoUrl; }
    @Nonnull @Override public List<Image> getThumbnails() throws ParsingException {
        return authorImages();
    }
    @Nonnull @Override public List<Image> getUploaderAvatars() throws ParsingException {
        return authorImages();
    }

    @Override
    public String getName() {
        final String name = getTextFromObject(comment.getObject("authorText"), false);
        return name == null ? "" : name;
    }

    @Override
    public String getUploaderName() {
        return getName();
    }

    @Override
    public String getTextualUploadDate() throws ParsingException {
        final String label = getTextFromObject(comment.getObject("publishedTimeText"), false);
        if (label == null) throw new ParsingException("Comment date is missing");
        return label;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        final String label = getTextualUploadDate();
        return label.isBlank() ? null : timeAgoParser.parse(label);
    }

    @Override
    public int getLikeCount() throws ParsingException {
        final String exactLabel;
        try {
            exactLabel = JsonUtils.getString(comment,
                    "actionButtons.commentActionButtonsRenderer.likeButton.toggleButtonRenderer"
                            + ".accessibilityData.accessibilityData.label");
        } catch (final ParsingException missingExactCount) {
            final String visibleCount = getTextualLikeCount();
            if (visibleCount.isBlank()) return 0;
            try {
                return Math.toIntExact(Utils.mixedNumberWordToLong(visibleCount));
            } catch (final ParsingException | NumberFormatException | ArithmeticException error) {
                throw new ParsingException("Could not parse comment likes", error);
            }
        }

        final String digits = Utils.removeNonDigitCharacters(exactLabel);
        if (digits.isBlank()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (final NumberFormatException error) {
            throw new ParsingException("Comment like count is out of range", error);
        }
    }

    @Override
    public String getTextualLikeCount() throws ParsingException {
        if (!comment.has("voteCount")) return "";
        final String label = getTextFromObject(comment.getObject("voteCount"), false);
        return label == null ? "" : label;
    }

    @Nonnull
    @Override
    public Description getCommentText() throws ParsingException {
        final JsonObject content = comment.getObject("contentText");
        if (content.isEmpty()) return Description.EMPTY_DESCRIPTION;
        final String html = getTextFromObject(content, true);
        return new Description(html == null ? "" : Utils.removeUTF8BOM(html), Description.HTML);
    }

    @Override
    public String getCommentId() throws ParsingException {
        final String id = comment.getString("commentId");
        if (id == null || id.isBlank()) throw new ParsingException("Comment ID is missing");
        return id;
    }

    @Override
    public boolean isHeartedByUploader() {
        return comment.getObject("actionButtons").getObject("commentActionButtonsRenderer")
                .has("creatorHeart");
    }

    @Override public boolean isPinned() { return comment.has("pinnedCommentBadge"); }
    @Override public boolean isUploaderVerified() { return comment.has("authorCommentBadge"); }

    @Override
    public String getUploaderUrl() {
        final String channelId = comment.getObject("authorEndpoint")
                .getObject("browseEndpoint").getString("browseId");
        return channelId == null || channelId.isBlank()
                ? "" : "https://www.youtube.com/channel/" + channelId;
    }

    @Override
    public int getReplyCount() {
        return comment.has("replyCount") ? comment.getInt("replyCount") : UNKNOWN_REPLY_COUNT;
    }

    @Nullable
    @Override
    public Page getReplies() {
        if (replies == null) return null;
        for (final JsonObject content : replies.getArray("contents").streamAsJsonObjects().toList()) {
            final String token = YoutubeContinuationTokenParser.INSTANCE.parse(
                    JsonWriter.string(content.getObject("continuationItemRenderer")));
            if (token != null && !token.isBlank()) return new Page(videoUrl, token);
        }
        return null;
    }

    @Override public boolean isChannelOwner() { return comment.getBoolean("authorIsChannelOwner"); }
    @Override public boolean hasCreatorReply() {
        return replies != null && replies.has("viewRepliesCreatorThumbnail");
    }
}
