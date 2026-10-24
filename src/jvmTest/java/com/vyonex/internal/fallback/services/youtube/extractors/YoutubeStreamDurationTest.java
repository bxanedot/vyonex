// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YoutubeStreamDurationTest {
    @Test
    void skipsMissingClientDataAndFindsDurationInLaterFormat() throws Exception {
        final var streamingData = JsonParser.object().from("{\"adaptiveFormats\":[{},"
                + "{\"approxDurationMs\":\"12678\"}]}");

        assertEquals(13, YoutubeStreamExtractor.getDurationFromFirstAdaptiveFormat(
                Arrays.asList(null, JsonParser.object().from("{}"), streamingData)));
    }
}
