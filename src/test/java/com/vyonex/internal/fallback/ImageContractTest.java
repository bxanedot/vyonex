// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageContractTest {
    @Test
    void rejectsMissingImageUrls() {
        assertThrows(NullPointerException.class,
                () -> new Image(null, 10, 20, Image.ResolutionLevel.UNKNOWN));
    }

    @Test
    void derivesResolutionFromHeightBoundaries() {
        assertEquals(Image.ResolutionLevel.UNKNOWN, Image.ResolutionLevel.fromHeight(0));
        assertEquals(Image.ResolutionLevel.LOW, Image.ResolutionLevel.fromHeight(174));
        assertEquals(Image.ResolutionLevel.MEDIUM, Image.ResolutionLevel.fromHeight(175));
        assertEquals(Image.ResolutionLevel.HIGH, Image.ResolutionLevel.fromHeight(720));
    }
}
