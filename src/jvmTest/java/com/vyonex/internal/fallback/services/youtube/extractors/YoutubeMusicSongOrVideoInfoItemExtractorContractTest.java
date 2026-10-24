// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeMusicSongOrVideoInfoItemExtractorContractTest {
    @Test
    void mapsSongMetadataAndUsesArtistEndpointInDetails() throws Exception {
        final var item = JsonParser.object().from("""
                {
                  "playlistItemData":{"videoId":"abcdefghijk"},
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"simpleText":"Song"}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[
                      {"text":"Artist","navigationEndpoint":{"browseEndpoint":{"browseId":"UC123"}}}
                    ]}}}
                  ],
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[
                    {"url":"//img.example/song.jpg","width":120,"height":120}
                  ]}}}
                }
                """);
        final var description = JsonParser.array().from("""
                [{"text":"Artist"},{"text":" • "},{"text":"Album"},{"text":" • "},{"text":"4:05"}]
                """);
        final var song = new YoutubeMusicSongOrVideoInfoItemExtractor(item, description, MUSIC_SONGS);

        assertEquals("Song", song.getName());
        assertEquals("https://music.youtube.com/watch?v=abcdefghijk", song.getUrl());
        assertEquals("https://img.example/song.jpg", song.getThumbnails().get(0).getUrl());
        assertEquals(StreamType.VIDEO_STREAM, song.getStreamType());
        assertEquals(245, song.getDuration());
        assertEquals("Artist", song.getUploaderName());
        assertEquals("https://www.youtube.com/channel/UC123", song.getUploaderUrl());
        assertEquals(-1, song.getViewCount());
        assertFalse(song.isAd());
        assertFalse(song.isUploaderVerified());
        assertNull(song.getTextualUploadDate());
        assertNull(song.getUploadDate());
    }

    @Test
    void mapsVideoViewsAndGetsArtistFromMenu() throws Exception {
        final var item = JsonParser.object().from("""
                {
                  "playlistItemData":{"videoId":"zyxwvutsrq1"},
                  "menu":{"menuRenderer":{"items":[{"menuNavigationItemRenderer":{
                    "icon":{"iconType":"ARTIST"},
                    "navigationEndpoint":{"browseEndpoint":{"browseId":"UC456"}}
                  }}]}}
                }
                """);
        final var description = JsonParser.array().from("""
                [{"text":"Artist"},{"text":" • "},{"text":"1.2M views"},{"text":" • "},{"text":"3:02"}]
                """);
        final var video = new YoutubeMusicSongOrVideoInfoItemExtractor(item, description, MUSIC_VIDEOS);

        assertEquals(1_200_000, video.getViewCount());
        assertEquals(182, video.getDuration());
        assertEquals("https://www.youtube.com/channel/UC456", video.getUploaderUrl());
    }

    @Test
    void treatsUncountedVideoViewsAsZeroAndRejectsMissingRequiredData() throws Exception {
        final var noViews = new YoutubeMusicSongOrVideoInfoItemExtractor(JsonParser.object().from("{}"),
                JsonParser.array().from("[{\"text\":\"Artist\"},{\"text\":\" • \"},{\"text\":\"No views\"},{\"text\":\" • \"},{\"text\":\"2:10\"}]"),
                MUSIC_VIDEOS);
        final var missing = new YoutubeMusicSongOrVideoInfoItemExtractor(JsonParser.object().from("{}"),
                JsonParser.array().from("[]"), MUSIC_SONGS);

        assertEquals(0, noViews.getViewCount());
        assertNull(noViews.getUploaderUrl());
        assertThrows(ParsingException.class, missing::getName);
        assertThrows(ParsingException.class, missing::getUrl);
        assertThrows(ParsingException.class, missing::getUploaderName);
        assertThrows(ParsingException.class, missing::getDuration);
    }
}
