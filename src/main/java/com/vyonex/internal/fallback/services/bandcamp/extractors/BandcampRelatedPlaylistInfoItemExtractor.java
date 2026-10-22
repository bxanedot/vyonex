// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.util.List;

public class BandcampRelatedPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final Element album;

    public BandcampRelatedPlaylistInfoItemExtractor(@Nonnull final Element album) {
        this.album = album;
    }

    @Override
    public String getName() {
        final Element title = album.selectFirst(".release-title");
        return title == null ? "" : title.text();
    }

    @Override
    public String getUrl() {
        final Element link = album.selectFirst(".album-link[href]");
        return link == null ? "" : link.absUrl("href").replaceFirst("^http://", "https://");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        final Element image = album.selectFirst(".album-art[src]");
        return BandcampExtractorHelper.getImagesFromImageUrl(image == null ? "" : image.attr("src"));
    }

    @Override
    public String getUploaderName() {
        final Element artist = album.selectFirst(".by-artist");
        return artist == null ? "" : artist.text().replaceFirst("(?i)^by\\s+", "");
    }

    @Override
    public String getUploaderUrl() { return null; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() { return -1; }
}
