// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.comments;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.Description;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CommentsInfoItem extends InfoItem {
    public static final int NO_LIKE_COUNT = -1;
    public static final int NO_STREAM_POSITION = -1;
    public static final int UNKNOWN_REPLY_COUNT = -1;

    private String commentId;
    @Nonnull private Description commentText = Description.EMPTY_DESCRIPTION;
    private String uploaderName;
    @Nonnull private List<Image> uploaderAvatars = List.of();
    private String uploaderUrl;
    private boolean uploaderVerified;
    private String textualUploadDate;
    @Nullable private DateWrapper uploadDate;
    private int likeCount = NO_LIKE_COUNT;
    private String textualLikeCount;
    private boolean heartedByUploader;
    private boolean pinned;
    private int streamPosition = NO_STREAM_POSITION;
    private int replyCount = UNKNOWN_REPLY_COUNT;
    @Nullable private Page replies;
    private boolean channelOwner;
    private boolean creatorReply;

    public CommentsInfoItem(final int serviceId, final String url, final String name) {
        super(InfoType.COMMENT, serviceId, url, name);
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(final String commentId) { this.commentId = commentId; }

    @Nonnull
    public Description getCommentText() { return commentText; }
    public void setCommentText(@Nonnull final Description commentText) {
        this.commentText = commentText;
    }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(final String uploaderName) { this.uploaderName = uploaderName; }

    @Nonnull
    public List<Image> getUploaderAvatars() { return uploaderAvatars; }
    public void setUploaderAvatars(@Nonnull final List<Image> uploaderAvatars) {
        this.uploaderAvatars = uploaderAvatars;
    }

    public String getUploaderUrl() { return uploaderUrl; }
    public void setUploaderUrl(final String uploaderUrl) { this.uploaderUrl = uploaderUrl; }

    public String getTextualUploadDate() { return textualUploadDate; }
    public void setTextualUploadDate(final String textualUploadDate) {
        this.textualUploadDate = textualUploadDate;
    }

    @Nullable
    public DateWrapper getUploadDate() { return uploadDate; }
    public void setUploadDate(@Nullable final DateWrapper uploadDate) {
        this.uploadDate = uploadDate;
    }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(final int likeCount) { this.likeCount = likeCount; }

    public String getTextualLikeCount() { return textualLikeCount; }
    public void setTextualLikeCount(final String textualLikeCount) {
        this.textualLikeCount = textualLikeCount;
    }

    public boolean isHeartedByUploader() { return heartedByUploader; }
    public void setHeartedByUploader(final boolean heartedByUploader) {
        this.heartedByUploader = heartedByUploader;
    }

    public boolean isPinned() { return pinned; }
    public void setPinned(final boolean pinned) { this.pinned = pinned; }

    public boolean isUploaderVerified() { return uploaderVerified; }
    public void setUploaderVerified(final boolean uploaderVerified) {
        this.uploaderVerified = uploaderVerified;
    }

    public int getStreamPosition() { return streamPosition; }
    public void setStreamPosition(final int streamPosition) {
        this.streamPosition = streamPosition;
    }

    public int getReplyCount() { return replyCount; }
    public void setReplyCount(final int replyCount) { this.replyCount = replyCount; }

    @Nullable
    public Page getReplies() { return replies; }
    public void setReplies(@Nullable final Page replies) { this.replies = replies; }

    public boolean isChannelOwner() { return channelOwner; }
    public void setChannelOwner(final boolean channelOwner) { this.channelOwner = channelOwner; }

    public boolean hasCreatorReply() { return creatorReply; }
    public void setCreatorReply(final boolean creatorReply) { this.creatorReply = creatorReply; }
}
