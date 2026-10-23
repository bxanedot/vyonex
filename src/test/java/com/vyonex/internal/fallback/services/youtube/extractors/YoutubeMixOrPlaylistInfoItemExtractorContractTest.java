// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeMixOrPlaylistInfoItemExtractorContractTest {
    @Test
    void mapsPlaylistCardFieldsAndFiniteCount() throws Exception {
        final var extractor = new YoutubeMixOrPlaylistInfoItemExtractor(JsonParser.object().from("""
                {
                  "title":{"simpleText":"Daily mix"},
                  "shareUrl":"https://www.youtube.com/watch?v=abcdefghijk&list=RDMMabcdefghijk",
                  "thumbnail":{"thumbnails":[{"url":"//img.example/mix.jpg","width":320,"height":180}]},
                  "longBylineText":{"simpleText":"Made for you"},
                  "videoCountShortText":{"simpleText":"80"}
                }
                """));

        assertEquals("Daily mix", extractor.getName());
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk&list=RDMMabcdefghijk", extractor.getUrl());
        assertEquals("https://img.example/mix.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals("Made for you", extractor.getUploaderName());
        assertNull(extractor.getUploaderUrl());
        assertFalse(extractor.isUploaderVerified());
        assertEquals(80, extractor.getStreamCount());
        assertEquals(PlaylistInfo.PlaylistType.MIX_STREAM, extractor.getPlaylistType());
    }

    @Test
    void treatsPlusCountsAsUnboundedAndRejectsMissingFields() throws Exception {
        final var infinite = new YoutubeMixOrPlaylistInfoItemExtractor(JsonParser.object().from("""
                {"title":{"simpleText":"Mix"},"shareUrl":"https://www.youtube.com/watch?v=abcdefghijk&list=RDMMabcdefghijk",
                 "videoCountShortText":{"simpleText":"50+"}}
                """));
        final var abbreviated = new YoutubeMixOrPlaylistInfoItemExtractor(JsonParser.object().from("""
                {"videoCountShortText":{"simpleText":"1.2K"}}
                """));
        final var missing = new YoutubeMixOrPlaylistInfoItemExtractor(JsonParser.object().from("{}"));

        assertEquals(ListExtractor.ITEM_COUNT_INFINITE, infinite.getStreamCount());
        assertEquals(1200, abbreviated.getStreamCount());
        assertThrows(ParsingException.class, missing::getName);
        assertThrows(ParsingException.class, missing::getUrl);
        assertThrows(ParsingException.class, missing::getStreamCount);
    }

    @Test
    void rejectsPlaylistCardUrlsOutsideYoutube() throws Exception {
        final var extractor = new YoutubeMixOrPlaylistInfoItemExtractor(JsonParser.object().from("""
                {"shareUrl":"https://attacker.example/watch?v=abcdefghijk&list=RDMMabcdefghijk"}
                """));

        assertThrows(ParsingException.class, extractor::getUrl);
    }
}
