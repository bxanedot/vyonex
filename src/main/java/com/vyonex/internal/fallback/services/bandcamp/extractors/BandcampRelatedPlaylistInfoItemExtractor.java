// Created by Fynn Godau 2021, licensed GNU GPL version 3 or later

// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import org.jsoup.nodes.Element;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;

import javax.annotation.Nonnull;
import java.util.List;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageUrl;

/**
 * Extracts recommended albums from tracks' website
 */
public class BandcampRelatedPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final Element relatedAlbum;

    public BandcampRelatedPlaylistInfoItemExtractor(@Nonnull final Element relatedAlbum) {
        this.relatedAlbum = relatedAlbum;
    }

    @Override
    public String getName() throws ParsingException {
        return relatedAlbum.getElementsByClass("release-title").text();
    }

    @Override
    public String getUrl() throws ParsingException {
        return relatedAlbum.getElementsByClass("album-link").attr("abs:href");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return getImagesFromImageUrl(relatedAlbum.getElementsByClass("album-art").attr("src"));
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return relatedAlbum.getElementsByClass("by-artist").text().replace("by ", "");
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        return null;
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        return -1;
    }
}
