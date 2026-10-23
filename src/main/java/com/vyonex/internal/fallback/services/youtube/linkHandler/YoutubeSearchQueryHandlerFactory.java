// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class YoutubeSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    public static final String ALL = "all";
    public static final String VIDEOS = "videos";
    public static final String CHANNELS = "channels";
    public static final String PLAYLISTS = "playlists";
    public static final String MUSIC_SONGS = "music_songs";
    public static final String MUSIC_VIDEOS = "music_videos";
    public static final String MUSIC_ALBUMS = "music_albums";
    public static final String MUSIC_PLAYLISTS = "music_playlists";
    public static final String MUSIC_ARTISTS = "music_artists";
    private static final YoutubeSearchQueryHandlerFactory INSTANCE =
            new YoutubeSearchQueryHandlerFactory();
    private static final String SEARCH_URL = "https://www.youtube.com/results?search_query=";
    private static final String MUSIC_URL = "https://music.youtube.com/search?q=";

    private YoutubeSearchQueryHandlerFactory() { }

    public static YoutubeSearchQueryHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getUrl(final String query, final List<String> filters,
                                   final String sortFilter) {
        final String filter = filters.isEmpty() ? ALL : filters.get(0);
        final String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        if (filter.startsWith("music_")) return MUSIC_URL + encodedQuery;
        return SEARCH_URL + encodedQuery + "&sp="
                + URLEncoder.encode(getSearchParameter(filter), StandardCharsets.UTF_8);
    }

    @Override public String[] getAvailableContentFilter() {
        return new String[]{ALL, VIDEOS, CHANNELS, PLAYLISTS, MUSIC_SONGS, MUSIC_VIDEOS,
                MUSIC_ALBUMS, MUSIC_PLAYLISTS};
    }

    public static String getSearchParameter(final String filter) {
        if (filter == null || filter.isBlank() || ALL.equals(filter)) return "8AEB";
        return switch (filter) {
            case VIDEOS -> "EgIQAfABAQ%3D%3D";
            case CHANNELS -> "EgIQAvABAQ%3D%3D";
            case PLAYLISTS -> "EgIQA_ABAQ%3D%3D";
            case MUSIC_SONGS, MUSIC_VIDEOS, MUSIC_ALBUMS, MUSIC_PLAYLISTS, MUSIC_ARTISTS -> "";
            default -> "8AEB";
        };
    }
}
