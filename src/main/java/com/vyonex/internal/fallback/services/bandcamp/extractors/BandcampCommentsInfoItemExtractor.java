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
    private final JsonObject review;
    private final String url;

    public BandcampCommentsInfoItemExtractor(final JsonObject review, final String url) {
        this.review = review;
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
        return new Description(review.getString("why", ""), Description.PLAIN_TEXT);
    }

    @Override
    public String getUploaderName() { return review.getString("name", ""); }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        return BandcampExtractorHelper.getImagesFromImageId(review.getLong("image_id"), false);
    }
}
