// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeStreamMetadataTest {
    @Test
    void prefersOriginalTitleAndFallsBackToRenderedTitle() throws Exception {
        assertEquals("Original", YoutubeStreamExtractor.extractVideoTitle(
                JsonParser.object().from("{\"title\":\"Original\"}"),
                JsonParser.object().from("{\"title\":{\"simpleText\":\"Rendered\"}}")));
        assertEquals("Rendered", YoutubeStreamExtractor.extractVideoTitle(
                JsonParser.object().from("{}"),
                JsonParser.object().from("{\"title\":{\"simpleText\":\"Rendered\"}}")));
    }

    @Test
    void rejectsResponsesWithoutATitle() throws Exception {
        assertThrows(ParsingException.class, () -> YoutubeStreamExtractor.extractVideoTitle(
                JsonParser.object().from("{}"), JsonParser.object().from("{}")));
    }

    @Test
    void selectsUploadDateFromVideoAndLiveMetadata() throws Exception {
        assertEquals("upload", YoutubeStreamExtractor.extractTextualUploadDate(
                JsonParser.object().from("{\"uploadDate\":\"upload\","
                        + "\"publishDate\":\"published\"}"),
                JsonParser.object().from("{}"), StreamType.VIDEO_STREAM));
        assertEquals("ended", YoutubeStreamExtractor.extractTextualUploadDate(
                JsonParser.object().from("{\"liveBroadcastDetails\":{"
                        + "\"endTimestamp\":\"ended\",\"startTimestamp\":\"started\"}}"),
                JsonParser.object().from("{}"), StreamType.POST_LIVE_STREAM));
    }

    @Test
    void stripsPremiereLabelsAndAllowsUndatedLiveStreams() throws Exception {
        assertEquals("Feb 21, 2020", YoutubeStreamExtractor.extractTextualUploadDate(
                JsonParser.object().from("{}"), JsonParser.object().from(
                        "{\"dateText\":{\"simpleText\":\"Premiered on Feb 21, 2020\"}}"),
                StreamType.VIDEO_STREAM));
        assertNull(YoutubeStreamExtractor.extractTextualUploadDate(JsonParser.object().from("{}"),
                JsonParser.object().from("{}"), StreamType.LIVE_STREAM));
    }

    @Test
    void detectsAgeRestrictionInMetadataRows() throws Exception {
        final var restricted = JsonParser.object().from("{\"metadataRowContainer\":{"
                + "\"metadataRowContainerRenderer\":{\"rows\":[{\"metadataRowRenderer\":{"
                + "\"contents\":[{\"runs\":[{\"text\":\"Age-restricted\"}]}]}}]}}}");
        assertEquals(18, YoutubeStreamExtractor.parseAgeLimit(restricted));
        assertEquals(0, YoutubeStreamExtractor.parseAgeLimit(JsonParser.object().from("{}")));
    }
}
