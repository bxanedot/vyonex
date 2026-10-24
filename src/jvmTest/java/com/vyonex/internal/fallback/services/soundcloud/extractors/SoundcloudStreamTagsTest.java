// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundcloudStreamTagsTest {
    @Test
    void splitsUnquotedWhitespaceAndKeepsQuotedAndEscapedContent() {
        assertEquals(List.of("ambient", "chill beats", "say \"hello\" now", "raw\\path"),
                SoundcloudStreamExtractor.parseTags(
                        "ambient\t\"chill beats\" \"say \\\"hello\\\" now\" raw\\\\path"));
    }

    @Test
    void handlesEmptyAndUnterminatedTagLists() {
        assertEquals(List.of(), SoundcloudStreamExtractor.parseTags(null));
        assertEquals(List.of(), SoundcloudStreamExtractor.parseTags(" \t "));
        assertEquals(List.of("unfinished phrase"),
                SoundcloudStreamExtractor.parseTags("\"unfinished phrase"));
    }
}
