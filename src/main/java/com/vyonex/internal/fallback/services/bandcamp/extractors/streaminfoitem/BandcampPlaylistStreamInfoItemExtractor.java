// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem;

import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.StreamExtractor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class BandcampPlaylistStreamInfoItemExtractor extends BandcampStreamInfoItemExtractor {

    private final JsonObject track;
    private List<Image> substituteCovers;
    private final StreamingService service;

    public BandcampPlaylistStreamInfoItemExtractor(final JsonObject track,
                                                   final String uploaderUrl,
                                                   final StreamingService service) {
        super(uploaderUrl);
        this.track = track;
        this.service = service;
        substituteCovers = Collections.emptyList();
    }

    public BandcampPlaylistStreamInfoItemExtractor(final JsonObject track,
                                                   final String uploaderUrl,
                                                   final List<Image> substituteCovers) {
        this(track, uploaderUrl, (StreamingService) null);
        this.substituteCovers = substituteCovers;
    }

    @Override
    public String getName() {
        return track.getString("title");
    }

    @Override
    public String getUrl() {
        final String relativeUrl = track.getString("title_link");
        if (relativeUrl != null) {
            return getUploaderUrl() + relativeUrl;
        } else {
            return null;
        }
    }

    @Override
    public long getDuration() {
        return track.getLong("duration");
    }

    @Override
    public String getUploaderName() {
        /* Tracks can have an individual artist name, but it is not included in the
         * given JSON.
         */
        return "";
    }

    /**
     * Each track can have its own cover art. Therefore, unless a substitute is provided,
     * the thumbnail is extracted using a stream extractor.
     */
    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        if (substituteCovers.isEmpty() && getUrl() != null) {
            try {
                final StreamExtractor extractor = service.getStreamExtractor(getUrl());
                extractor.fetchPage();
                return extractor.getThumbnails();
            } catch (final ExtractionException | IOException e) {
                throw new ParsingException("Could not download cover art location", e);
            }
        }

        return substituteCovers;
    }
}
