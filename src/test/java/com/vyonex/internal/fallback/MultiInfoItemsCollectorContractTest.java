// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultiInfoItemsCollectorContractTest {
    @Test
    void rejectsExtractorTypesWithoutACollector() {
        final InfoItemExtractor unsupported = new InfoItemExtractor() {
            @Override public String getName() { return "unknown"; }
            @Override public String getUrl() { return "https://example.invalid/item"; }
            @Override public List<Image> getThumbnails() { return List.of(); }
        };

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new MultiInfoItemsCollector(1).extract(unsupported));
        assertEquals("Invalid extractor type: " + unsupported, error.getMessage());
    }
}
