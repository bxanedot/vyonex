// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.comments.CommentsInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudCommentInfoItem;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudProfileInfoItem;
import com.vyonex.internal.fallback.stream.Description;

import javax.annotation.Nullable;
import java.util.List;

public final class SoundcloudCommentsInfoItemExtractor implements CommentsInfoItemExtractor {
    @Nullable private final String commentId;
    private final String text;
    private final String uploaderName;
    @Nullable private final String avatarUrl;
    private final boolean uploaderVerified;
    private final int streamPosition;
    private final String uploaderUrl;
    private final String textualUploadDate;
    private final String name;
    private final String url;

    public SoundcloudCommentsInfoItemExtractor(final SoundcloudCommentInfoItem comment,
                                               final String url) {
        final SoundcloudProfileInfoItem user = comment.getUser();
        commentId = comment.getId();
        text = comment.getBody();
        uploaderName = user.getName();
        avatarUrl = user.getAvatarUrl();
        uploaderVerified = user.getVerified();
        streamPosition = (int) comment.getTimestampMillis() / 1000;
        uploaderUrl = SoundcloudParsingHelper.toHttpsUrl(user.getPermalinkUrl());
        textualUploadDate = comment.getCreatedAt();
        name = comment.getName();
        this.url = url;
    }

    @Override public String getCommentId() { return commentId; }
    @Override public Description getCommentText() {
        return new Description(text, Description.PLAIN_TEXT);
    }
    @Override public String getUploaderName() { return uploaderName; }
    @Override public List<Image> getUploaderAvatars() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(avatarUrl);
    }
    @Override public boolean isUploaderVerified() { return uploaderVerified; }
    @Override public int getStreamPosition() { return streamPosition; }
    @Override public String getUploaderUrl() { return uploaderUrl; }
    @Override public String getTextualUploadDate() { return textualUploadDate; }
    @Override public DateWrapper getUploadDate() throws ParsingException {
        return SoundcloudParsingHelper.parseDate(getTextualUploadDate());
    }
    @Override public String getName() { return name; }
    @Override public String getUrl() { return url; }
    @Override public List<Image> getThumbnails() { return getUploaderAvatars(); }
}
