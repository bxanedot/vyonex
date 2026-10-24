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

class RequestContractTest {
    @Test
    void appendsHeaderValuesInsteadOfReplacingEarlierValues() {
        final Request request = Request.newBuilder()
                .get("https://example.test/resource")
                .addHeader("Accept", "text/plain")
                .addHeader("Accept", "application/json")
                .build();

        assertEquals(List.of("text/plain", "application/json"),
                request.headers().get("Accept"));
    }

    @Test
    void keepsHeaderSnapshotIndependentAndReadOnly() {
        final List<String> values = new ArrayList<>(List.of("one"));
        final Map<String, List<String>> source = new LinkedHashMap<>();
        source.put("X-Value", values);
        final Request request = Request.newBuilder()
                .get("https://example.test/resource")
                .headers(source)
                .build();

        values.add("two");
        source.put("X-Later", List.of("three"));

        assertEquals(List.of("one"), request.headers().get("X-Value"));
        assertNull(request.headers().get("X-Later"));
        assertThrows(UnsupportedOperationException.class,
                () -> request.headers().get("X-Value").add("four"));
    }
}
