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
    private final String uploaderName;
    private final long streamCount;
    private final String name;
    private final String url;
    private final long artworkId;

    public BandcampPlaylistInfoItemFeaturedExtractor(final JsonObject story) {
        uploaderName = story.getString("band_name", "");
        streamCount = story.getLong("num_streamable_tracks", -1);
        name = story.getString("album_title", "");
        url = https(story.getString("item_url", ""));
        artworkId = story.getLong(story.isNumber("art_id") ? "art_id" : "item_art_id");
    }

    @Override
    public String getUploaderName() { return uploaderName; }

    @Override
    public String getUploaderUrl() { return null; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() { return streamCount; }

    @Override
    public String getName() { return name; }

    @Override
    public String getUrl() { return url; }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(artworkId, true);
    }

    private static String https(final String value) {
        return value.startsWith("http://") ? "https://" + value.substring(7) : value;
    }
}
