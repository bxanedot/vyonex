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
    private final String name;
    private final String url;
    private final String imageUrl;
    private final String uploaderName;

    public BandcampRelatedPlaylistInfoItemExtractor(@Nonnull final Element album) {
        final Element title = album.selectFirst(".release-title");
        final Element link = album.selectFirst(".album-link[href]");
        final Element image = album.selectFirst(".album-art[src]");
        final Element artist = album.selectFirst(".by-artist");
        name = title == null ? "" : title.text();
        url = link == null ? "" : link.absUrl("href").replaceFirst("^http://", "https://");
        imageUrl = image == null ? "" : image.attr("src");
        uploaderName = artist == null ? "" : artist.text().replaceFirst("(?i)^by\\s+", "");
    }

    @Override public String getName() { return name; }

    @Override public String getUrl() { return url; }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageUrl(imageUrl);
    }

    @Override public String getUploaderName() { return uploaderName; }

    @Override
    public String getUploaderUrl() { return null; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() { return -1; }
}
