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
import com.vyonex.internal.fallback.services.youtube.YoutubeDescriptionHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/** Maps YouTube's entity-update comment data into VyonEx's comment API. */
class YoutubeCommentsEUVMInfoItemExtractor implements CommentsInfoItemExtractor {
    private final JsonObject viewModel;
    @Nullable private final JsonObject replies;
    private final JsonObject entity;
    private final JsonObject engagement;
    private final String videoUrl;
    private final TimeAgoParser timeAgoParser;

    YoutubeCommentsEUVMInfoItemExtractor(@Nonnull final JsonObject viewModel,
                                         @Nullable final JsonObject replies,
                                         @Nonnull final JsonObject entity,
                                         @Nonnull final JsonObject engagement,
                                         @Nonnull final String videoUrl,
                                         @Nonnull final TimeAgoParser timeAgoParser) {
        this.viewModel = Objects.requireNonNull(viewModel, "comment view model is required");
        this.replies = replies;
        this.entity = Objects.requireNonNull(entity, "comment entity is required");
        this.engagement = Objects.requireNonNull(engagement, "engagement state is required");
        this.videoUrl = Objects.requireNonNull(videoUrl, "video URL is required");
        this.timeAgoParser = Objects.requireNonNull(timeAgoParser, "date parser is required");
    }

    @Override public String getName() { return getUploaderName(); }
    @Override public String getUrl() { return videoUrl; }
    @Nonnull @Override public List<Image> getThumbnails() throws ParsingException {
        return getUploaderAvatars();
    }

    @Override
    public int getLikeCount() throws ParsingException {
        final String label = getTextualLikeCount();
        if (label == null || label.isBlank()) return 0;
        try {
            return Math.toIntExact(Utils.mixedNumberWordToLong(label));
        } catch (final ParsingException | NumberFormatException | ArithmeticException error) {
            throw new ParsingException("Could not parse comment likes", error);
        }
    }

    @Override
    public String getTextualLikeCount() {
        return entity.getObject("toolbar").getString("likeCountNotliked");
    }

    @Nonnull
    @Override
    public Description getCommentText() {
        final String html = YoutubeDescriptionHelper.attributedDescriptionToHtml(
                entity.getObject("properties").getObject("content"));
        return new Description(html, Description.HTML);
    }

    @Override
    public String getTextualUploadDate() {
        return entity.getObject("properties").getString("publishedTime");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        final String label = getTextualUploadDate();
        return label == null || label.isBlank() ? null : timeAgoParser.parse(label);
    }

    @Override
    public String getCommentId() throws ParsingException {
        String id = entity.getObject("properties").getString("commentId");
        if (id == null || id.isBlank()) id = viewModel.getString("commentId");
        if (id == null || id.isBlank()) throw new ParsingException("Comment ID is missing");
        return id;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        final JsonObject author = entity.getObject("author");
        String channelId = author.getString("channelId");
        if (channelId == null || channelId.isBlank()) {
            channelId = author.getObject("channelCommand").getObject("innertubeCommand")
                    .getObject("browseEndpoint").getString("browseId");
        }
        if (channelId == null || channelId.isBlank()) {
            channelId = author.getObject("avatar").getObject("endpoint")
                    .getObject("innertubeCommand").getObject("browseEndpoint")
                    .getString("browseId");
        }
        if (channelId == null || channelId.isBlank()) {
            throw new ParsingException("Comment author channel ID is missing");
        }
        return "https://www.youtube.com/channel/" + channelId;
    }

    @Override public String getUploaderName() {
        return entity.getObject("author").getString("displayName");
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() throws ParsingException {
        final JsonArray sources = entity.getObject("avatar").getObject("image")
                .getArray("sources");
        return YoutubeParsingHelper.getImagesFromThumbnailsArray(sources);
    }

    @Override
    public boolean isHeartedByUploader() {
        return "TOOLBAR_HEART_STATE_HEARTED".equals(engagement.getString("heartState"));
    }

    @Override public boolean isPinned() { return viewModel.has("pinnedText"); }

    @Override
    public boolean isUploaderVerified() {
        final JsonObject author = entity.getObject("author");
        return author.getBoolean("isVerified") || author.getBoolean("isArtist");
    }

    @Override
    public int getReplyCount() throws ParsingException {
        final String label = entity.getObject("toolbar").getString("replyCount");
        if (label == null || label.isBlank()) return 0;
        try {
            return Math.toIntExact(Utils.mixedNumberWordToLong(label));
        } catch (final ParsingException | NumberFormatException | ArithmeticException error) {
            throw new ParsingException("Could not parse comment reply count", error);
        }
    }

    @Nullable
    @Override
    public Page getReplies() throws ParsingException {
        if (replies == null) return null;
        for (final JsonObject content : replies.getArray("contents").streamAsJsonObjects().toList()) {
            final String token = YoutubeContinuationTokenParser.INSTANCE.parse(
                    JsonWriter.string(content.getObject("continuationItemRenderer")));
            if (token != null && !token.isBlank()) return new Page(videoUrl, token);
        }
        throw new ParsingException("Comment replies continuation is missing");
    }

    @Override
    public boolean isChannelOwner() {
        return entity.getObject("author").getBoolean("isCreator");
    }

    @Override public boolean hasCreatorReply() {
        return replies != null && replies.has("viewRepliesCreatorThumbnail");
    }
}
