// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class YoutubePlayerErrorMessageTest {
    @Test
    void readsErrorReasonFromPlayerResponse() throws Exception {
        final var response = JsonParser.object().from("{\"playabilityStatus\":{"
                + "\"errorScreen\":{\"playerErrorMessageRenderer\":{\"reason\":{"
                + "\"simpleText\":\"Unavailable\"}}}}}");

        assertEquals("Unavailable", YoutubeStreamExtractor.parsePlayerErrorMessage(response));
    }

    @Test
    void returnsNullWhenResponseHasNoErrorReason() throws Exception {
        assertNull(YoutubeStreamExtractor.parsePlayerErrorMessage(JsonParser.object().from("{}")));
        assertNull(YoutubeStreamExtractor.parsePlayerErrorMessage(null));
    }
}
