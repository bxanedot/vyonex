// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MediaFormatContractTest {
    @Test
    void preservesFirstMatchForDuplicateIdsAndMimeTypes() {
        assertSame(MediaFormat.WEBMA, MediaFormat.getFormatById(0x200));
        assertEquals("WebM", MediaFormat.getNameById(0x200));
        assertSame(MediaFormat.AIFF, MediaFormat.getFormatById(0x600));
        assertSame(MediaFormat.WEBMA, MediaFormat.getFromMimeType("audio/webm"));
        assertSame(MediaFormat.TRANSCRIPT1, MediaFormat.getFromMimeType("text/xml"));
        assertEquals(List.of(MediaFormat.WEBMA, MediaFormat.WEBMA_OPUS),
                MediaFormat.getAllFromMimeType("audio/webm"));
    }

    @Test
    void looksUpSuffixesAndUsesDocumentedUnknownDefaults() {
        assertSame(MediaFormat.AIF, MediaFormat.getFromSuffix("aif"));
        assertEquals("mp4", MediaFormat.getSuffixById(0));
        assertEquals("video/mp4", MediaFormat.getMimeById(0));
        assertEquals("", MediaFormat.getNameById(-1));
        assertEquals("", MediaFormat.getSuffixById(-1));
        assertNull(MediaFormat.getMimeById(-1));
        assertNull(MediaFormat.getFromMimeType("unknown/type"));
    }

    @Test
    void returnsAnIndependentModifiableMimeMatchList() {
        final List<MediaFormat> formats = MediaFormat.getAllFromMimeType("audio/webm");

        formats.clear();
        assertEquals(2, MediaFormat.getAllFromMimeType("audio/webm").size());
    }
}
