// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.comments.CommentsInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.json.JsonObject;

import java.util.List;
import java.util.Objects;

public final class SoundcloudCommentsInfoItemExtractor implements CommentsInfoItemExtractor {
    private final JsonObject comment;
    private final String url;

    public SoundcloudCommentsInfoItemExtractor(final JsonObject comment, final String url) {
        this.comment = comment;
        this.url = url;
    }

    private JsonObject user() { return comment.getObject("user"); }
    @Override public String getCommentId() { return Objects.toString(comment.get("id"), null); }
    @Override public Description getCommentText() {
        return new Description(comment.getString("body", ""), Description.PLAIN_TEXT);
    }
    @Override public String getUploaderName() { return user().getString("username", ""); }
    @Override public List<Image> getUploaderAvatars() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(
                user().getString("avatar_url"));
    }
    @Override public boolean isUploaderVerified() { return user().getBoolean("verified"); }
    @Override public int getStreamPosition() { return comment.getInt("timestamp") / 1000; }
    @Override public String getUploaderUrl() {
        return SoundcloudParsingHelper.toHttpsUrl(user().getString("permalink_url", ""));
    }
    @Override public String getTextualUploadDate() { return comment.getString("created_at", ""); }
    @Override public DateWrapper getUploadDate() throws ParsingException {
        return SoundcloudParsingHelper.parseDate(getTextualUploadDate());
    }
    @Override public String getName() { return user().getString("permalink", ""); }
    @Override public String getUrl() { return url; }
    @Override public List<Image> getThumbnails() { return getUploaderAvatars(); }
}
