// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.stream.Description;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaInfoContractTest {
    @Test
    void defaultMetadataHasAnEmptyDescriptionAndUsableLinkLists() throws Exception {
        final MetaInfo info = new MetaInfo();
        final URL url = new URL("https://example.test/source");

        assertEquals("", info.getTitle());
        assertEquals("", info.getContent().getContent());
        assertEquals(Description.PLAIN_TEXT, info.getContent().getType());
        assertTrue(info.getUrls().isEmpty());
        assertTrue(info.getUrlTexts().isEmpty());

        info.addUrl(url);
        info.addUrlText("Source");
        assertEquals(List.of(url), info.getUrls());
        assertEquals(List.of("Source"), info.getUrlTexts());
    }
}
