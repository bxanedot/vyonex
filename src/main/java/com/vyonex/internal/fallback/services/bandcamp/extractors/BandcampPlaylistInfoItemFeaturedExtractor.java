// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;

public class BandcampPlaylistInfoItemFeaturedExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject story;

    public BandcampPlaylistInfoItemFeaturedExtractor(final JsonObject story) {
        this.story = story;
    }

    @Override
    public String getUploaderName() { return story.getString("band_name", ""); }

    @Override
    public String getUploaderUrl() { return null; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() { return story.getLong("num_streamable_tracks", -1); }

    @Override
    public String getName() { return story.getString("album_title", ""); }

    @Override
    public String getUrl() { return https(story.getString("item_url", "")); }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        final String key = story.isNumber("art_id") ? "art_id" : "item_art_id";
        return BandcampExtractorHelper.getImagesFromImageId(story.getLong(key), true);
    }

    private static String https(final String value) {
        return value.startsWith("http://") ? "https://" + value.substring(7) : value;
    }
}
