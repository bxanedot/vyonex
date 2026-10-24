// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudSearchUrl;

import java.io.IOException;
import java.util.List;

public final class SoundcloudSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    public static final String TRACKS = "tracks";
    public static final String USERS = "users";
    public static final String PLAYLISTS = "playlists";
    public static final String ALL = "all";
    public static final int ITEMS_PER_PAGE = 10;
    private static final SoundcloudSearchQueryHandlerFactory INSTANCE =
            new SoundcloudSearchQueryHandlerFactory();

    private SoundcloudSearchQueryHandlerFactory() { }

    public static SoundcloudSearchQueryHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getUrl(final String query, final List<String> contentFilter,
                                   final String sortFilter) throws ParsingException {
        try {
            return SoundcloudSearchUrl.INSTANCE.build(query, contentFilter,
                    SoundcloudParsingHelper.clientId(), ITEMS_PER_PAGE);
        } catch (final IOException | ExtractionException e) {
            throw new ParsingException("Unable to create SoundCloud search URL", e);
        }
    }

    @Override public String[] getAvailableContentFilter() {
        return new String[]{ALL, TRACKS, USERS, PLAYLISTS};
    }
}
