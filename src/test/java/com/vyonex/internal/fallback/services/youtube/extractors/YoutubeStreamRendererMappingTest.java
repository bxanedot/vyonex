// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeStreamRendererMappingTest {
    @Test
    void selectsTheFirstMatchingRendererFromWatchResults() throws Exception {
        final JsonObject response = JsonParser.object().from("{\"contents\":{"
                + "\"twoColumnWatchNextResults\":{\"results\":{\"results\":{"
                + "\"contents\":[{\"other\":{}},{\"videoPrimaryInfoRenderer\":{"
                + "\"title\":{\"text\":\"first\"}}},{\"videoPrimaryInfoRenderer\":{"
                + "\"title\":{\"text\":\"second\"}}}]}}}}}");

        assertEquals("first", YoutubeStreamExtractor.findVideoRenderer(response,
                "videoPrimaryInfoRenderer").getObject("title").getString("text"));
    }

    @Test
    void returnsAnEmptyObjectWhenRendererIsAbsent() throws Exception {
        final JsonObject response = JsonParser.object().from("{\"contents\":{"
                + "\"twoColumnWatchNextResults\":{\"results\":{\"results\":{"
                + "\"contents\":[{\"other\":{}}]}}}}}");

        assertTrue(YoutubeStreamExtractor.findVideoRenderer(response,
                "videoSecondaryInfoRenderer").isEmpty());
    }

    @Test
    void prefersNonemptyWebThumbnailsAndFallsBackToThePrimaryResponse() throws Exception {
        final JsonObject web = JsonParser.object().from("{\"videoDetails\":{\"thumbnail\":{"
                + "\"thumbnails\":[{\"url\":\"web-image\"}]}}}");
        final JsonObject primary = JsonParser.object().from("{\"videoDetails\":{\"thumbnail\":{"
                + "\"thumbnails\":[{\"url\":\"primary-image\"}]}}}");
        final JsonObject emptyWeb = JsonParser.object().from("{\"videoDetails\":{\"thumbnail\":{"
                + "\"thumbnails\":[]}}}");

        assertEquals("web-image", YoutubeStreamExtractor.preferredThumbnails(web, primary)
                .getObject(0).getString("url"));
        assertEquals("primary-image", YoutubeStreamExtractor.preferredThumbnails(emptyWeb, primary)
                .getObject(0).getString("url"));
    }
}
