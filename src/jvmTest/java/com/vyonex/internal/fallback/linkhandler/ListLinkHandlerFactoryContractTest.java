// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.linkhandler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ListLinkHandlerFactoryContractTest {
    @Test
    void urlAndQueryHandlersKeepTheirDifferentFilterSources() throws Exception {
        final ExampleListFactory factory = new ExampleListFactory();

        final ListLinkHandler fromUrl = factory.fromUrl("https://media.example/list/featured");
        assertEquals("featured", fromUrl.getId());
        assertEquals(List.of(), fromUrl.getContentFilters());
        assertEquals("", fromUrl.getSortFilter());

        final ListLinkHandler fromQuery = factory.fromQuery("creator",
                List.of("videos", "live"), "recent");
        assertEquals("creator", fromQuery.getId());
        assertEquals(List.of("videos", "live"), fromQuery.getContentFilters());
        assertEquals("recent", fromQuery.getSortFilter());
        assertEquals("https://media.example/list/creator?videos,live&recent",
                fromQuery.getUrl());
    }

    @Test
    void searchFactoryKeepsQueryAsIdAndDoesNotAcceptUrlsByDefault() throws Exception {
        final ExampleSearchFactory factory = new ExampleSearchFactory();
        final SearchQueryHandler query = factory.fromQuery("ambient");

        assertEquals("ambient", query.getId());
        assertEquals("ambient", query.getSearchString());
        assertEquals("https://media.example/search/ambient", query.getUrl());
        assertFalse(factory.acceptUrl("https://media.example/search/ambient"));
    }

    private static class ExampleListFactory extends ListLinkHandlerFactory {
        @Override
        public String getId(final String url) {
            return url.substring(url.lastIndexOf('/') + 1);
        }

        @Override
        public String getUrl(final String id) {
            return "https://media.example/list/" + id;
        }

        @Override
        public String getUrl(final String id,
                             final List<String> contentFilter,
                             final String sortFilter) {
            return getUrl(id) + "?" + String.join(",", contentFilter) + "&" + sortFilter;
        }

        @Override
        public boolean onAcceptUrl(final String url) {
            return url != null && url.startsWith("https://media.example/list/");
        }
    }

    private static final class ExampleSearchFactory extends SearchQueryHandlerFactory {
        @Override
        public String getUrl(final String query,
                             final List<String> contentFilter,
                             final String sortFilter) {
            return "https://media.example/search/" + query;
        }

        @Override
        public String getSearchString(final String url) {
            return url.substring(url.lastIndexOf('/') + 1);
        }
    }
}
