// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestCreatorCacheTest {
    @Test
    void replacesValuesAndEvictsOldEntriesAtTheConfiguredLimit() {
        final ManifestCreatorCache<String, String> cache = new ManifestCreatorCache<>();
        cache.setMaximumSize(3);
        cache.setClearFactor(0.5);

        assertNull(cache.put("a", "one"));
        cache.put("b", "two");
        cache.put("c", "three");
        assertEquals("two", cache.put("b", "updated"));
        cache.put("d", "four");

        assertTrue(cache.containsKey("b"));
        assertFalse(cache.containsKey("a"));
        assertEquals("updated", cache.get("b").getSecond());
        assertEquals("four", cache.get("d").getSecond());
        assertEquals(3, cache.size());
    }

    @Test
    void validatesLimitsAndResetsSettings() {
        final ManifestCreatorCache<String, String> cache = new ManifestCreatorCache<>();
        assertThrows(IllegalArgumentException.class, () -> cache.setMaximumSize(0));
        assertThrows(IllegalArgumentException.class, () -> cache.setClearFactor(1));
        cache.setMaximumSize(12);
        cache.setClearFactor(0.5);

        cache.reset();

        assertEquals(ManifestCreatorCache.DEFAULT_MAXIMUM_SIZE, cache.getMaximumSize());
        assertEquals(ManifestCreatorCache.DEFAULT_CLEAR_FACTOR, cache.getClearFactor());
        assertEquals(0, cache.size());
    }

    @Test
    void serializesConcurrentPutsWithoutExceedingTheLimit() throws Exception {
        final ManifestCreatorCache<String, String> cache = new ManifestCreatorCache<>();
        cache.setMaximumSize(32);
        cache.setClearFactor(0.75);
        final var pool = Executors.newFixedThreadPool(4);
        try {
            for (int index = 0; index < 100; index++) {
                final int key = index;
                pool.submit(() -> cache.put("key-" + key, "value"));
            }
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertTrue(cache.size() <= 32);
        assertNotNull(cache.toString());
    }
}
