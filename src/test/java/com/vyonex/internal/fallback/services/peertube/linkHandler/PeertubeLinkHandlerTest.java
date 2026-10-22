// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeertubeLinkHandlerTest {
    private static final String BASE = "https://tube.example";

    @Test
    void recognizesVideoRoutesAndBuildsCanonicalWatchUrls() throws Exception {
        final var handler = PeertubeStreamLinkHandlerFactory.getInstance();
        for (final String url : List.of(BASE + "/w/video-id", BASE + "/videos/video-id",
                BASE + "/videos/watch/video-id", BASE + "/videos/embed/video-id")) {
            assertTrue(handler.onAcceptUrl(url));
            assertEquals("video-id", handler.getId(url));
        }
        assertEquals(BASE + "/videos/watch/video-id", handler.getUrl("video-id", BASE));
        assertFalse(handler.onAcceptUrl(BASE + "/videos/watch/playlist/list-id"));
        assertFalse(handler.onAcceptUrl("ftp://tube.example/w/video-id"));
        assertFalse(handler.onAcceptUrl("https://user@tube.example/w/video-id"));
    }

    @Test
    void recognizesPlaylistRoutesAndBuildsApiUrls() throws Exception {
        final var handler = PeertubePlaylistLinkHandlerFactory.getInstance();
        for (final String url : List.of(BASE + "/w/p/list-id",
                BASE + "/videos/watch/playlist/list-id",
                BASE + "/api/v1/video-playlists/list-id")) {
            assertTrue(handler.onAcceptUrl(url));
            assertEquals("list-id", handler.getId(url));
        }
        assertEquals(BASE + "/api/v1/video-playlists/list-id",
                handler.getUrl("list-id", List.of(), "", BASE));
    }

    @Test
    void normalizesChannelAliasesAndSearchFilters() throws Exception {
        final var channels = PeertubeChannelLinkHandlerFactory.getInstance();
        assertEquals("accounts/alice", channels.getId(BASE + "/a/alice/videos"));
        assertEquals("video-channels/creator",
                channels.getId(BASE + "/c/creator/video-playlists"));
        assertEquals("accounts/alice", channels.getId(BASE + "/api/v1/accounts/alice"));
        assertEquals(BASE + "/video-channels/creator",
                channels.getUrl("c/creator", List.of(), "", BASE));
        assertFalse(channels.onAcceptUrl(BASE + "/other/a/alice"));

        final var search = PeertubeSearchQueryHandlerFactory.getInstance();
        assertEquals(BASE + "/api/v1/search/videos?search=art+%26+caf%C3%A9",
                search.getUrl("art & café", List.of("videos"), "", BASE));
        assertEquals(PeertubeSearchQueryHandlerFactory.SEPIA_BASE_URL
                        + "/api/v1/search/videos?search=ambient",
                search.getUrl("ambient", List.of("sepia_videos"), ""));
        assertEquals(BASE + "/api/v1/search/video-channels?search=artist",
                search.getUrl("artist", List.of("channels"), "", BASE));
    }

    @Test
    void routesChannelTabsCommentsAndTrendingKiosks() throws Exception {
        final var channelTabs = PeertubeChannelTabLinkHandlerFactory.getInstance();
        assertEquals(BASE + "/accounts/alice/videos",
                channelTabs.getUrl("accounts/alice", List.of(ChannelTabs.VIDEOS), "", BASE));
        assertEquals("accounts/alice", channelTabs.getId(BASE + "/accounts/alice/videos"));

        final var comments = PeertubeCommentsLinkHandlerFactory.getInstance();
        assertTrue(comments.onAcceptUrl(BASE + "/w/video-id"));
        assertFalse(comments.onAcceptUrl(BASE + "/w/p/playlist-id"));
        assertEquals("video-id", comments.getId(BASE + "/videos/watch/video-id"));
        assertEquals(BASE + "/api/v1/videos/video-id/comment-threads",
                comments.getUrl("video-id", List.of(), "", BASE));

        final var kiosks = PeertubeTrendingLinkHandlerFactory.getInstance();
        for (final String id : PeertubeTrendingLinkHandlerFactory.KIOSK_MAP.keySet()) {
            final String url = kiosks.getUrl(id, List.of(), "", BASE);
            assertTrue(kiosks.onAcceptUrl(url));
            assertEquals(id, kiosks.getId(url));
        }
        assertEquals(PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING,
                kiosks.getId(BASE + "/videos/trending"));
        assertFalse(kiosks.onAcceptUrl(BASE + "/api/v1/videos?sort=%"));
    }
}
