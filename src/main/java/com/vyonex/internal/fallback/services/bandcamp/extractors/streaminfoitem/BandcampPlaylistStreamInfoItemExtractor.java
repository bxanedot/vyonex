// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.StreamExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class BandcampPlaylistStreamInfoItemExtractor extends BandcampStreamInfoItemExtractor {
    @Nullable private final String title;
    @Nullable private final String titleLink;
    private final long duration;
    private final String artist;
    private final StreamingService service;
    private final List<Image> substituteCovers;

    public BandcampPlaylistStreamInfoItemExtractor(final String title,
                                                   final String titleLink,
                                                   final long duration,
                                                   final String artist,
                                                   final String uploaderUrl,
                                                   @Nullable final StreamingService service,
                                                   final List<Image> substituteCovers) {
        super(uploaderUrl);
        this.title = title;
        this.titleLink = titleLink;
        this.duration = duration;
        this.artist = artist;
        this.service = service;
        this.substituteCovers = List.copyOf(substituteCovers);
    }

    @Override
    public String getName() throws ParsingException {
        if (title == null) throw new ParsingException("Bandcamp track has no title");
        return title;
    }

    @Override
    public String getUrl() throws ParsingException {
        if (titleLink == null || titleLink.isBlank()) return null;
        try {
            return new URI(getUploaderUrl()).resolve(titleLink).toString()
                    .replaceFirst("^http://", "https://");
        } catch (final URISyntaxException | NullPointerException e) {
            throw new ParsingException("Bandcamp track has an invalid URL", e);
        }
    }

    @Override
    public long getDuration() { return duration; }

    @Override
    public String getUploaderName() { return artist; }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        if (!substituteCovers.isEmpty()) return substituteCovers;
        final String url = getUrl();
        if (url == null || service == null) return List.of();
        try {
            final StreamExtractor extractor = service.getStreamExtractor(url);
            extractor.fetchPage();
            return extractor.getThumbnails();
        } catch (final ExtractionException | IOException e) {
            throw new ParsingException("Could not load the track cover", e);
        }
    }

}
