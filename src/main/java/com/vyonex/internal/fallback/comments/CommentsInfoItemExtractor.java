// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.comments;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItemExtractor;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.Description;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface CommentsInfoItemExtractor extends InfoItemExtractor {
    default int getLikeCount() throws ParsingException {
        return CommentsInfoItem.NO_LIKE_COUNT;
    }

    default String getTextualLikeCount() throws ParsingException { return ""; }

    @Nonnull
    default Description getCommentText() throws ParsingException {
        return Description.EMPTY_DESCRIPTION;
    }

    default String getTextualUploadDate() throws ParsingException { return ""; }

    @Nullable
    default DateWrapper getUploadDate() throws ParsingException { return null; }

    default String getCommentId() throws ParsingException { return ""; }
    default String getUploaderUrl() throws ParsingException { return ""; }
    default String getUploaderName() throws ParsingException { return ""; }

    @Nonnull
    default List<Image> getUploaderAvatars() throws ParsingException { return List.of(); }

    default boolean isHeartedByUploader() throws ParsingException { return false; }
    default boolean isPinned() throws ParsingException { return false; }
    default boolean isUploaderVerified() throws ParsingException { return false; }

    default int getStreamPosition() throws ParsingException {
        return CommentsInfoItem.NO_STREAM_POSITION;
    }

    default int getReplyCount() throws ParsingException {
        return CommentsInfoItem.UNKNOWN_REPLY_COUNT;
    }

    @Nullable
    default Page getReplies() throws ParsingException { return null; }

    default boolean isChannelOwner() throws ParsingException { return false; }
    default boolean hasCreatorReply() throws ParsingException { return false; }
}
