// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.comments.CommentsInfoItemExtractor;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;

public class BandcampCommentsInfoItemExtractor implements CommentsInfoItemExtractor {
    private final String text;
    private final String uploaderName;
    private final long avatarId;
    private final String url;

    public BandcampCommentsInfoItemExtractor(final JsonObject review, final String url) {
        text = review.getString("why", "");
        uploaderName = review.getString("name", "");
        avatarId = review.getLong("image_id");
        this.url = url;
    }

    @Override
    public String getName() { return getCommentText().getContent(); }

    @Override
    public String getUrl() { return url; }

    @Nonnull
    @Override
    public List<Image> getThumbnails() { return getUploaderAvatars(); }

    @Nonnull
    @Override
    public Description getCommentText() {
        return new Description(text, Description.PLAIN_TEXT);
    }

    @Override
    public String getUploaderName() { return uploaderName; }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        return BandcampExtractorHelper.getImagesFromImageId(avatarId, false);
    }
}
