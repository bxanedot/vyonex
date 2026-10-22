// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
