// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.linkhandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinkHandlerFactoryContractTest {
    @Test
    void buildsHandlersFromAcceptedUrlsAndIds() throws Exception {
        final LinkHandlerFactory factory = new ExampleFactory();
        final LinkHandler fromUrl = factory.fromUrl("https://media.example/watch/clip-1");

        assertEquals("https://media.example/watch/clip-1", fromUrl.getOriginalUrl());
        assertEquals("https://media.example/watch/clip-1", fromUrl.getUrl());
        assertEquals("clip-1", fromUrl.getId());
        assertEquals("https://media.example/watch/clip-2",
                factory.fromId("clip-2").getUrl());
        assertEquals("https://media.example/watch/clip-3",
                factory.fromId("clip-3", "https://media.example").getUrl());
    }

    @Test
    void rejectsEmptyAndUnsupportedUrls() throws Exception {
        final LinkHandlerFactory factory = new ExampleFactory();

        assertThrows(IllegalArgumentException.class, () -> factory.fromUrl(""));
        assertThrows(ParsingException.class,
                () -> factory.fromUrl("https://elsewhere.example/watch/clip-1"));
    }

    private static final class ExampleFactory extends LinkHandlerFactory {
        @Override
        public String getId(final String url) {
            return url.substring(url.lastIndexOf('/') + 1);
        }

        @Override
        public String getUrl(final String id) {
            return "https://media.example/watch/" + id;
        }

        @Override
        public String getUrl(final String id, final String baseUrl) {
            return baseUrl + "/watch/" + id;
        }

        @Override
        public boolean onAcceptUrl(final String url) {
            return url != null && url.startsWith("https://media.example/watch/");
        }
    }
}
