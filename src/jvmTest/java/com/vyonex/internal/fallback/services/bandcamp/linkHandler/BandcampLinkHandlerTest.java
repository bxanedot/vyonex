// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampFeaturedExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandcampLinkHandlerTest {
    @Test
    void routesTrackAlbumAndRadioUrls() throws Exception {
        final BandcampStreamLinkHandlerFactory tracks = BandcampStreamLinkHandlerFactory.getInstance();
        final BandcampPlaylistLinkHandlerFactory albums = BandcampPlaylistLinkHandlerFactory.getInstance();
        final BandcampCommentsLinkHandlerFactory comments = BandcampCommentsLinkHandlerFactory.getInstance();

        assertTrue(tracks.onAcceptUrl("https://artist.bandcamp.com/track/song"));
        assertEquals("https://artist.bandcamp.com/track/song",
                tracks.getId("http://artist.bandcamp.com/track/song"));
        assertFalse(tracks.onAcceptUrl("https://artist.bandcamp.com/album/record"));
        assertTrue(albums.onAcceptUrl("https://artist.bandcamp.com/album/record"));
        assertEquals("https://artist.bandcamp.com/album/record",
                albums.getId("http://artist.bandcamp.com/album/record"));
        assertTrue(comments.onAcceptUrl("https://artist.bandcamp.com/track/song"));
        assertTrue(comments.onAcceptUrl("https://artist.bandcamp.com/album/record"));
        assertTrue(tracks.onAcceptUrl("https://bandcamp.com/?show=123"));
        assertEquals("123", tracks.getId("https://bandcamp.com/?show=123"));
        assertEquals("https://bandcamp.com/?show=123", tracks.getUrl("123"));
        assertFalse(tracks.onAcceptUrl("https://example.test/track/song"));
    }

    @Test
    void routesArtistTabsAndSearchTerms() throws Exception {
        final BandcampChannelLinkHandlerFactory artists = BandcampChannelLinkHandlerFactory.getInstance();
        final BandcampChannelTabLinkHandlerFactory tabs = BandcampChannelTabLinkHandlerFactory.getInstance();
        final BandcampSearchQueryHandlerFactory search = BandcampSearchQueryHandlerFactory.getInstance();

        assertTrue(artists.onAcceptUrl("https://artist.bandcamp.com"));
        assertFalse(artists.onAcceptUrl("https://daily.bandcamp.com"));
        assertEquals("/track", BandcampChannelTabLinkHandlerFactory.getUrlSuffix(ChannelTabs.TRACKS));
        assertEquals("/album", BandcampChannelTabLinkHandlerFactory.getUrlSuffix(ChannelTabs.ALBUMS));
        assertThrows(UnsupportedTabException.class,
                () -> BandcampChannelTabLinkHandlerFactory.getUrlSuffix("unsupported"));
        assertEquals(List.of(ChannelTabs.TRACKS, ChannelTabs.ALBUMS),
                List.of(tabs.getAvailableContentFilter()));
        assertEquals("https://bandcamp.com/search?q=music+%26+caf%C3%A9&page=1",
                search.getUrl("music & café", List.of(), ""));
    }

    @Test
    void mapsFeaturedAndRadioKiosks() throws Exception {
        final BandcampFeaturedLinkHandlerFactory kiosks = BandcampFeaturedLinkHandlerFactory.getInstance();

        assertEquals(BandcampFeaturedExtractor.FEATURED_API_URL,
                kiosks.getUrl(BandcampFeaturedExtractor.KIOSK_FEATURED, List.of(), ""));
        assertEquals(BandcampRadioExtractor.RADIO_API_URL,
                kiosks.getUrl(BandcampRadioExtractor.KIOSK_RADIO, List.of(), ""));
        assertEquals(BandcampFeaturedExtractor.KIOSK_FEATURED,
                kiosks.getId(BandcampFeaturedExtractor.FEATURED_API_URL));
        assertEquals(BandcampRadioExtractor.KIOSK_RADIO,
                kiosks.getId("https://bandcamp.com/?show=123"));
        assertFalse(kiosks.onAcceptUrl("https://bandcamp.com/?show=not-a-number"));
    }
}
