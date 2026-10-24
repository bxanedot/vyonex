// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeChannelInfoItemExtractor;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeChannelInfoItemExtractorTest {
    @Test
    void readsClassicChannelSummaryFields() throws Exception {
        final var channel = new YoutubeChannelInfoItemExtractor(JsonParser.object().from(
                "{\"channelId\":\"UC123\",\"title\":{\"simpleText\":\"Artist\"},"
                        + "\"subscriberCountText\":{\"simpleText\":\"1.2K subscribers\"},"
                        + "\"videoCountText\":{\"simpleText\":\"1,234 videos\"},"
                        + "\"descriptionSnippet\":{\"simpleText\":\"Music\"},"
                        + "\"ownerBadges\":[{\"metadataBadgeRenderer\":{"
                        + "\"style\":\"BADGE_STYLE_TYPE_VERIFIED\"}}],"
                        + "\"thumbnail\":{\"thumbnails\":[{\"url\":\"//img.example/a.jpg\","
                        + "\"height\":100,\"width\":100}]}}"));

        assertEquals("Artist", channel.getName());
        assertEquals("https://www.youtube.com/channel/UC123", channel.getUrl());
        assertEquals(1200, channel.getSubscriberCount());
        assertEquals(1234, channel.getStreamCount());
        assertEquals("Music", channel.getDescription());
        assertTrue(channel.isVerified());
        assertEquals("https://img.example/a.jpg", channel.getThumbnails().get(0).getUrl());
    }

    @Test
    void readsHandleLayoutAndUnknownCounts() throws Exception {
        final var channel = new YoutubeChannelInfoItemExtractor(JsonParser.object().from(
                "{\"channelId\":\"UC456\",\"title\":{\"simpleText\":\"Creator\"},"
                        + "\"subscriberCountText\":{\"simpleText\":\"@creator\"},"
                        + "\"videoCountText\":{\"simpleText\":\"2.4K subscribers\"}}"));
        assertEquals(2400, channel.getSubscriberCount());
        assertEquals(ListExtractor.ITEM_COUNT_UNKNOWN, channel.getStreamCount());
        assertNull(channel.getDescription());
        assertFalse(channel.isVerified());

        final var noCounts = new YoutubeChannelInfoItemExtractor(
                JsonParser.object().from("{\"channelId\":\"UC789\"}"));
        assertEquals(-1, noCounts.getSubscriberCount());
        assertEquals(ListExtractor.ITEM_COUNT_UNKNOWN, noCounts.getStreamCount());

        final var malformed = new YoutubeChannelInfoItemExtractor(JsonParser.object().from(
                "{\"channelId\":\"UC999\",\"videoCountText\":{\"simpleText\":\"many\"}}"));
        assertThrows(ParsingException.class, malformed::getStreamCount);
    }

    @Test
    void rejectsAChannelResultWithoutAnIdentifier() throws Exception {
        final var channel = new YoutubeChannelInfoItemExtractor(JsonParser.object().from(
                "{\"title\":{\"simpleText\":\"No ID\"}}"));
        assertThrows(ParsingException.class, channel::getUrl);
    }
}
