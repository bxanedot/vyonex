// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import com.vyonex.internal.fallback.Image.ResolutionLevel;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FallbackDataHolderTest {
    @Test
    void pairRetainsMutableValuesAndValueEquality() {
        final Pair<Serializable, Serializable> pair = new Pair<>("first", 2);
        pair.setFirst("updated");
        pair.setSecond(3);

        assertEquals("updated", pair.getFirst());
        assertEquals(3, pair.getSecond());
        assertEquals(pair, new Pair<>("updated", 3));
        assertEquals(pair.hashCode(), new Pair<>("updated", 3).hashCode());
        assertEquals("{updated, 3}", pair.toString());
        assertNotEquals(pair, new Pair<>("updated", 4));
    }

    @Test
    void imageSuffixKeepsDimensionsAndRequiresAResolution() {
        final ImageSuffix suffix = new ImageSuffix("hq.jpg", 360, 480, ResolutionLevel.MEDIUM);

        assertEquals("hq.jpg", suffix.getSuffix());
        assertEquals(360, suffix.getHeight());
        assertEquals(480, suffix.getWidth());
        assertEquals(ResolutionLevel.MEDIUM, suffix.getResolutionLevel());
        assertEquals("ImageSuffix {suffix=hq.jpg, height=360, width=480, "
                + "resolutionLevel=MEDIUM}", suffix.toString());
        assertThrows(NullPointerException.class,
                () -> new ImageSuffix("x", 1, 1, null));
    }
}
