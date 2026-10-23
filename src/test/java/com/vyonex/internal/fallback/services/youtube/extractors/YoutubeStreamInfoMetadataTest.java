// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YoutubeStreamInfoMetadataTest {
    @Test
    void classifiesPrivacyAndReadsCategory() throws Exception {
        assertEquals(StreamExtractor.Privacy.UNLISTED,
                YoutubeStreamExtractor.parsePrivacy(JsonParser.object().from(
                        "{\"isUnlisted\":true}"), JsonParser.object().from("{}")));
        assertEquals(StreamExtractor.Privacy.UNLISTED,
                YoutubeStreamExtractor.parsePrivacy(JsonParser.object().from("{}"),
                        JsonParser.object().from("{\"badges\":[{\"metadataBadgeRenderer\":{"
                                + "\"icon\":{\"iconType\":\"PRIVACY_UNLISTED\"}}}]}")));
        assertEquals(StreamExtractor.Privacy.PUBLIC, YoutubeStreamExtractor.parsePrivacy(
                JsonParser.object().from("{}"), JsonParser.object().from("{}")));
        assertEquals("Music", YoutubeStreamExtractor.parseCategory(
                JsonParser.object().from("{\"category\":\"Music\"}")));
        assertEquals("", YoutubeStreamExtractor.parseCategory(JsonParser.object().from("{}")));
    }

    @Test
    void readsLicenseAndTagsFromResponseMetadata() throws Exception {
        assertEquals("Creative Commons", YoutubeStreamExtractor.parseLicense(JsonParser.object()
                .from("{\"metadataRowContainer\":{\"metadataRowContainerRenderer\":{"
                        + "\"rows\":[{\"metadataRowRenderer\":{\"title\":{"
                        + "\"simpleText\":\"Licence\"},\"contents\":[{"
                        + "\"simpleText\":\"Creative Commons\"}]}}]}}}")));
        assertEquals("YouTube licence",
                YoutubeStreamExtractor.parseLicense(JsonParser.object().from("{}")));
        assertEquals(List.of("one", "two"), YoutubeStreamExtractor.parseTags(JsonParser.object()
                .from("{\"keywords\":[\"one\",\"two\"]}")));
    }
}
