// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeParsingValuesTest {
    @Test
    void parsesSecondMinuteHourDayAndDotSeparatedDurations() throws Exception {
        assertEquals(9, YoutubeParsingHelper.parseDurationString("9"));
        assertEquals(62, YoutubeParsingHelper.parseDurationString("1:02"));
        assertEquals(3723, YoutubeParsingHelper.parseDurationString("1:02:03"));
        assertEquals(93784, YoutubeParsingHelper.parseDurationString("1:02:03:04"));
        assertEquals(3723, YoutubeParsingHelper.parseDurationString("1.02.03"));
        assertEquals(0, YoutubeParsingHelper.parseDurationString("SHORTS"));
    }

    @Test
    void rejectsMalformedAndOverflowingDurations() {
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper.parseDurationString(""));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper.parseDurationString("1::3"));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper.parseDurationString("1:2:3:4:5"));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper.parseDurationString("1:2.3"));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper.parseDurationString("999999:00:00"));
    }

    @Test
    void mapsFeedAndMixIdentifiers() throws Exception {
        assertEquals("https://www.youtube.com/feeds/videos.xml?user=alice",
                YoutubeParsingHelper.getFeedUrlFrom("user/alice"));
        assertEquals("https://www.youtube.com/feeds/videos.xml?channel_id=UCchannel",
                YoutubeParsingHelper.getFeedUrlFrom("channel/UCchannel"));
        assertEquals("https://www.youtube.com/feeds/videos.xml?channel_id=UCchannel",
                YoutubeParsingHelper.getFeedUrlFrom("UCchannel"));

        assertEquals(PlaylistInfo.PlaylistType.MIX_STREAM,
                YoutubeParsingHelper.extractPlaylistTypeFromPlaylistId("RDMMabcdefghijk"));
        assertTrue(YoutubeParsingHelper.isYoutubeMixId("RDabcdefghijk"));
        assertFalse(YoutubeParsingHelper.isYoutubeMixId("RDnot-a-mix"));
        assertEquals(PlaylistInfo.PlaylistType.MIX_MUSIC,
                YoutubeParsingHelper.extractPlaylistTypeFromPlaylistId("RDAMVMabcdefghijk"));
        assertEquals(PlaylistInfo.PlaylistType.MIX_GENRE,
                YoutubeParsingHelper.extractPlaylistTypeFromPlaylistId("RDGMEMexample"));
        assertEquals(PlaylistInfo.PlaylistType.NORMAL,
                YoutubeParsingHelper.extractPlaylistTypeFromPlaylistId("PLabcdefghijk"));
        assertEquals("abcdefghijk", YoutubeParsingHelper.extractVideoIdFromMixId("RDMMabcdefghijk"));
        assertEquals("abcdefghijk", YoutubeParsingHelper.extractVideoIdFromMixId("RDabcdefghijk"));
        assertThrows(ParsingException.class,
                () -> YoutubeParsingHelper.extractVideoIdFromMixId("RDGMEMexample"));
        assertEquals(PlaylistInfo.PlaylistType.MIX_STREAM,
                YoutubeParsingHelper.extractPlaylistTypeFromPlaylistUrl(
                        "https://www.youtube.com/playlist?list=RDMMabcdefghijk"));
    }

    @Test
    void rejectsMalformedPlaylistUrlQueryEncoding() {
        assertThrows(ParsingException.class, () ->
                YoutubeParsingHelper.extractPlaylistTypeFromPlaylistUrl(
                        "https://www.youtube.com/playlist?list=%"));
    }

    @Test
    void recognizesYoutubeAndAlternativeHostsWithoutPrefixCollisions() throws Exception {
        assertTrue(YoutubeParsingHelper.isYoutubeURL(new URL("https://m.youtube.com/watch?v=x")));
        assertTrue(YoutubeParsingHelper.isYoutubeServiceURL(
                new URL("https://youtu.be/abcdefghijk")));
        assertTrue(YoutubeParsingHelper.isYoutubeServiceURL(
                new URL("https://www.youtube-nocookie.com/embed/abcdefghijk")));
        assertTrue(YoutubeParsingHelper.isHooktubeURL(new URL("https://hooktube.com/watch?v=x")));
        assertTrue(YoutubeParsingHelper.isInvidiousURL(
                new URL("https://invidious.example.org/watch?v=x")));
        assertTrue(YoutubeParsingHelper.isInvidiousURL(new URL("https://yewtu.be/watch?v=x")));
        assertTrue(YoutubeParsingHelper.isInvidiousURL(new URL("https://piped.example/watch?v=x")));
        assertTrue(YoutubeParsingHelper.isY2ubeURL(new URL("https://y2u.be/watch?v=x")));
        assertFalse(YoutubeParsingHelper.isYoutubeURL(new URL("https://youtube.com.example.org/")));
        assertFalse(YoutubeParsingHelper.isInvidiousURL(new URL("https://example.org/watch?v=x")));
    }

    @Test
    void unwrapsCachedTargetsBeforeGoogleHostCheck() {
        assertTrue(YoutubeParsingHelper.isGoogleURL("https://www.google.com/search?q=x"));
        assertTrue(YoutubeParsingHelper.isGoogleURL("https://google.co.uk/search?q=x"));
        assertFalse(YoutubeParsingHelper.isGoogleURL("https://notgoogle.com/"));
        assertFalse(YoutubeParsingHelper.isGoogleURL("https://google.com.example.org/"));
        assertFalse(YoutubeParsingHelper.isGoogleURL("ftp://google.com/"));
        assertFalse(YoutubeParsingHelper.isGoogleURL(null));
        assertEquals("https://video.example/watch?v=1",
                YoutubeParsingHelper.extractCachedUrlIfNeeded(
                        "https://webcache.googleusercontent.com/search?q=cache%3Ahttps%3A%2F%2Fvideo.example%2Fwatch%3Fv%3D1"));
        assertEquals("https://video.example/watch?v=1",
                YoutubeParsingHelper.extractCachedUrlIfNeeded(
                        "https://webcache.googleusercontent.com/search?q=cache%3Aabc%3Ahttps%3A%2F%2Fvideo.example%2Fwatch%3Fv%3D1"));
        assertEquals("https://example.org/path", YoutubeParsingHelper.extractCachedUrlIfNeeded(
                "https://example.org/path"));
    }
}
