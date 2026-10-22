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
}
