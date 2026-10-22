// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeChannelTabPlaylistExtractorTest {
    @Test
    void mapsSupportedChannelTabsToSystemPlaylistIds() {
        assertEquals("UULFabc", YoutubeChannelTabPlaylistExtractor.systemPlaylistId(
                "UCabc", ChannelTabs.VIDEOS));
        assertEquals("UUSHabc", YoutubeChannelTabPlaylistExtractor.systemPlaylistId(
                "UCabc", ChannelTabs.SHORTS));
        assertEquals("UULVabc", YoutubeChannelTabPlaylistExtractor.systemPlaylistId(
                "UCabc", ChannelTabs.LIVESTREAMS));
    }

    @Test
    void rejectsMissingChannelIdsAndUnsupportedTabs() {
        assertThrows(IllegalArgumentException.class,
                () -> YoutubeChannelTabPlaylistExtractor.systemPlaylistId("channel",
                        ChannelTabs.VIDEOS));
        assertThrows(IllegalArgumentException.class,
                () -> YoutubeChannelTabPlaylistExtractor.systemPlaylistId("UCabc",
                        ChannelTabs.PLAYLISTS));
    }
}
