// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeMusicArtistInfoItemExtractorContractTest {
    @Test
    void mapsArtistNameRouteImageAndAudience() throws Exception {
        final var artist = new YoutubeMusicArtistInfoItemExtractor(JsonParser.object().from("""
                {
                  "navigationEndpoint":{"browseEndpoint":{"browseId":"UC123"}},
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Artist"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[
                      {"text":"Artist"},{"text":" • "},{"text":"1.2M subscribers"}
                    ]}}}
                  ],
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[
                    {"url":"//img.example/artist.jpg","width":240,"height":240}
                  ]}}}
                }
                """));

        assertEquals("Artist", artist.getName());
        assertEquals("https://www.youtube.com/channel/UC123", artist.getUrl());
        assertEquals("https://img.example/artist.jpg", artist.getThumbnails().get(0).getUrl());
        assertEquals(1_200_000, artist.getSubscriberCount());
        assertEquals(ListExtractor.ITEM_COUNT_UNKNOWN, artist.getStreamCount());
        assertTrue(artist.isVerified());
        assertNull(artist.getDescription());
    }

    @Test
    void mapsMissingAudienceLabelToZeroAndRejectsMissingArtistData() throws Exception {
        final var noSubscribers = new YoutubeMusicArtistInfoItemExtractor(JsonParser.object().from("""
                {"flexColumns":[{}, {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[
                  {"text":"No subscribers"}
                ]}}}]}
                """));
        final var incomplete = new YoutubeMusicArtistInfoItemExtractor(JsonParser.object().from("{}"));

        assertEquals(0, noSubscribers.getSubscriberCount());
        assertThrows(ParsingException.class, incomplete::getName);
        assertThrows(ParsingException.class, incomplete::getUrl);
        assertThrows(ParsingException.class, incomplete::getSubscriberCount);
    }
}
