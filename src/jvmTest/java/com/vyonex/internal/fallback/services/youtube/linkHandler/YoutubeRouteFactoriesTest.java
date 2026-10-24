// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeRouteFactoriesTest {
    @Test
    void recognizesKioskRoutesAndRejectsUnrelatedHosts() {
        assertTrue(YoutubeTrendingLinkHandlerFactory.INSTANCE
                .onAcceptUrl("https://www.youtube.com/feed/trending"));
        assertTrue(YoutubeTrendingGamingVideosLinkHandlerFactory.INSTANCE
                .onAcceptUrl("https://youtube.com/gaming/trending"));
        assertTrue(YoutubeTrendingPodcastsEpisodesLinkHandlerFactory.INSTANCE
                .onAcceptUrl("https://m.youtube.com/podcasts/popularepisodes"));
        assertTrue(YoutubeTrendingMusicLinkHandlerFactory.INSTANCE
                .onAcceptUrl("https://charts.youtube.com/charts/TrendingVideos"));
        assertTrue(YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory.INSTANCE
                .onAcceptUrl("https://charts.youtube.com/charts/TrendingTrailers"));
        assertFalse(YoutubeTrendingLinkHandlerFactory.INSTANCE
                .onAcceptUrl("https://example.com/feed/trending"));
        assertTrue(YoutubeLiveLinkHandlerFactory.INSTANCE.onAcceptUrl(
                "https://www.youtube.com/channel/UC4R8DWoMoI7CAwX8_LjQHig/livetab?ss=CKEK"));
        assertFalse(YoutubeLiveLinkHandlerFactory.INSTANCE.onAcceptUrl(
                "https://www.youtube.com/channel/UC4R8DWoMoI7CAwX8_LjQHig/livetab?ss=nope"));
    }

    @Test
    void keepsChannelTabRoutesAndCommentVideoIds() throws Exception {
        final YoutubeChannelTabLinkHandlerFactory tabs = YoutubeChannelTabLinkHandlerFactory.getInstance();
        assertEquals("/shorts", YoutubeChannelTabLinkHandlerFactory.getUrlSuffix(ChannelTabs.SHORTS));
        assertEquals(List.of(ChannelTabs.VIDEOS, ChannelTabs.SHORTS, ChannelTabs.LIVESTREAMS,
                        ChannelTabs.ALBUMS, ChannelTabs.PLAYLISTS),
                List.of(tabs.getAvailableContentFilter()));
        assertEquals("abcdefghijk", YoutubeCommentsLinkHandlerFactory.getInstance()
                .getId("https://www.youtube.com/watch?v=abcdefghijk"));
    }

    @Test
    void resolvesChannelAndPlaylistIdsAndBuildsSearchRoutes() throws Exception {
        final YoutubeChannelLinkHandlerFactory channels = YoutubeChannelLinkHandlerFactory.getInstance();
        assertEquals("@artist", channels.getId("https://www.youtube.com/@artist/videos"));
        assertEquals("channel/UCexample", channels.getId(
                "https://www.youtube.com/channel/UCexample/streams"));
        assertFalse(channels.onAcceptUrl("https://www.youtube.com/watch?v=abcdefghijk"));

        final YoutubePlaylistLinkHandlerFactory playlists = YoutubePlaylistLinkHandlerFactory.getInstance();
        assertEquals("PLabcdefghijk", playlists.getId(
                "https://www.youtube.com/watch?v=abcdefghijk&list=PLabcdefghijk"));
        final var mix = playlists.fromUrl(
                "https://www.youtube.com/watch?v=abcdefghijk&list=RDMMabcdefghijk");
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk&list=RDMMabcdefghijk",
                mix.getUrl());

        final YoutubeSearchQueryHandlerFactory search = YoutubeSearchQueryHandlerFactory.getInstance();
        assertEquals("https://www.youtube.com/results?search_query=ambient+pop"
                        + "&sp=EgIQAfABAQ%253D%253D",
                search.getUrl("ambient pop", List.of(YoutubeSearchQueryHandlerFactory.VIDEOS), ""));
        assertEquals("https://music.youtube.com/search?q=ambient+pop",
                search.getUrl("ambient pop", List.of(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), ""));
    }

    @Test
    void extractsVideoIdsFromSupportedUrlForms() throws Exception {
        final YoutubeStreamLinkHandlerFactory streams = YoutubeStreamLinkHandlerFactory.getInstance();
        assertEquals("abcdefghijk", streams.getId("https://www.youtube.com/shorts/abcdefghijk"));
        assertEquals("abcdefghijk", streams.getId("https://www.youtube.com/embed/abcdefghijk"));
        assertEquals("abcdefghijk", streams.getId("https://youtu.be/abcdefghijk"));
        assertEquals("abcdefghijk", streams.getId("vnd.youtube://abcdefghijk"));
        assertEquals("abcdefghijk", streams.getId(
                "https://www.youtube.com/attribution_link?u=%2Fwatch%3Fv%3Dabcdefghijk"));
        assertFalse(streams.onAcceptUrl("https://www.youtube.com/watch?v=abcdefghijk&list=PLabcdefghijk"));
    }
}
