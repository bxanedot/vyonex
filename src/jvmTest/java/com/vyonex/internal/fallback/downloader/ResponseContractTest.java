// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.downloader;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResponseContractTest {
    @Test
    void snapshotsHeadersAndDoesNotExposeMutableCollections() {
        final List<String> contentTypes = new ArrayList<>(List.of("application/json"));
        final Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Content-Type", contentTypes);
        final Response response = new Response(200, "OK", headers, null, null);

        contentTypes.add("text/html");
        headers.put("X-Later", List.of("changed"));

        assertEquals("application/json", response.getHeader("content-type"));
        assertNull(response.getHeader("x-later"));
        assertEquals("", response.responseBody());
        assertNull(response.latestUrl());
        assertThrows(UnsupportedOperationException.class,
                () -> response.responseHeaders().put("X-New", List.of("value")));
        assertThrows(UnsupportedOperationException.class,
                () -> response.responseHeaders().get("Content-Type").add("text/plain"));
    }
}
