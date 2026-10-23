// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class BandcampPlaylistStreamInfoItemExtractor extends BandcampStreamInfoItemExtractor {
    private final JsonObject track;
    private final StreamingService service;
    private final List<Image> substituteCovers;

    public BandcampPlaylistStreamInfoItemExtractor(final JsonObject track,
                                                   final String uploaderUrl,
                                                   final StreamingService service) {
        super(uploaderUrl);
        this.track = track;
        this.service = service;
        substituteCovers = List.of();
    }

    public BandcampPlaylistStreamInfoItemExtractor(final JsonObject track,
                                                   final String uploaderUrl,
                                                   final List<Image> substituteCovers) {
        super(uploaderUrl);
        this.track = track;
        service = null;
        this.substituteCovers = List.copyOf(substituteCovers);
    }

    @Override
    public String getName() throws ParsingException {
        final String title = track.getString("title");
        if (title == null) throw new ParsingException("Bandcamp track has no title");
        return title;
    }

    @Override
    public String getUrl() throws ParsingException {
        final String path = track.getString("title_link");
        if (path == null || path.isBlank()) return null;
        try {
            return new URI(getUploaderUrl()).resolve(path).toString()
                    .replaceFirst("^http://", "https://");
        } catch (final URISyntaxException | NullPointerException e) {
            throw new ParsingException("Bandcamp track has an invalid URL", e);
        }
    }

    @Override
    public long getDuration() {
        return (long) track.getDouble("duration");
    }

    @Override
    public String getUploaderName() {
        return track.getString("artist", "");
    }

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
