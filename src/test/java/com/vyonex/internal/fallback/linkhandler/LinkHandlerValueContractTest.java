// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.linkhandler;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinkHandlerValueContractTest {
    @Test
    void listHandlersKeepAnImmutableSnapshotOfContentFilters() {
        final List<String> filters = new ArrayList<>(List.of("videos"));
        final ListLinkHandler handler = new ListLinkHandler(
                "original", "url", "id", filters, "recent");

        filters.add("channels");

        assertEquals(List.of("videos"), handler.getContentFilters());
        assertThrows(UnsupportedOperationException.class,
                () -> handler.getContentFilters().add("playlists"));
    }

    @Test
    void searchHandlersExposeTheSearchStringAsTheirId() {
        final SearchQueryHandler handler = new SearchQueryHandler(
                "original", "url", "ambient", List.of("music"), "recent");

        assertEquals("ambient", handler.getId());
        assertEquals("ambient", handler.getSearchString());
        assertEquals(List.of("music"), handler.getContentFilters());
    }
}
