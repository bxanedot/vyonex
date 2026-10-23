// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeStreamSegmentsTest {
    @Test
    void readsChaptersAndStopsAfterVideoDuration() throws Exception {
        final var response = JsonParser.object().from("{\"engagementPanels\":[{"
                + "\"engagementPanelSectionListRenderer\":{\"panelIdentifier\":\"other\"}},{"
                + "\"engagementPanelSectionListRenderer\":{\"panelIdentifier\":"
                + "\"engagement-panel-macro-markers-description-chapters\",\"content\":{"
                + "\"macroMarkersListRenderer\":{\"contents\":["
                + "{\"macroMarkersListItemRenderer\":{\"onTap\":{\"watchEndpoint\":{"
                + "\"startTimeSeconds\":10}},\"title\":{\"simpleText\":\"Intro\"},"
                + "\"thumbnail\":{\"thumbnails\":[{\"url\":\"//img.example/chapter.jpg\"}]}}},"
                + "{\"macroMarkersListItemRenderer\":{\"onTap\":{\"watchEndpoint\":{"
                + "\"startTimeSeconds\":30}},\"title\":{\"simpleText\":\"End\"}}}]}}}}]}");

        final var segments = YoutubeStreamExtractor.parseStreamSegments(response,
                "https://www.youtube.com/watch?v=video", 20);

        assertEquals(1, segments.size());
        assertEquals("Intro", segments.get(0).getTitle());
        assertEquals(10, segments.get(0).getStartTimeSeconds());
        assertEquals("https://www.youtube.com/watch?v=video&t=10", segments.get(0).getUrl());
        assertEquals("https://img.example/chapter.jpg", segments.get(0).getPreviewUrl());
    }

    @Test
    void rejectsChaptersWithoutStartTimes() throws Exception {
        final var response = JsonParser.object().from("{\"engagementPanels\":[{"
                + "\"engagementPanelSectionListRenderer\":{\"panelIdentifier\":"
                + "\"engagement-panel-macro-markers-description-chapters\",\"content\":{"
                + "\"macroMarkersListRenderer\":{\"contents\":[{"
                + "\"macroMarkersListItemRenderer\":{\"title\":{"
                + "\"simpleText\":\"Intro\"}}}]}}}}]}");
        assertThrows(ParsingException.class, () -> YoutubeStreamExtractor.parseStreamSegments(
                response, "https://www.youtube.com/watch?v=video", 20));
    }
}
