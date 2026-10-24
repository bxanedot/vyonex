// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeStreamUploadDateTest {
    @Test
    void parsesExactAndApproximateDateLayouts() throws Exception {
        final DateWrapper exact = YoutubeStreamExtractor.parseUploadDate(
                "2020-02-21T10:30:00Z");
        assertEquals(Instant.parse("2020-02-21T10:30:00Z"), exact.getInstant());
        assertFalse(exact.isApproximation());

        final DateWrapper monthFirst = YoutubeStreamExtractor.parseUploadDate("Feb 21, 2020");
        final DateWrapper dayFirst = YoutubeStreamExtractor.parseUploadDate("21 Feb 2020");
        assertTrue(monthFirst.isApproximation());
        assertTrue(dayFirst.isApproximation());
        assertEquals(monthFirst.getInstant(), dayFirst.getInstant());
    }

    @Test
    void preservesMissingDatesAndRejectsUnknownLayouts() throws Exception {
        assertNull(YoutubeStreamExtractor.parseUploadDate(null));
        assertThrows(ParsingException.class,
                () -> YoutubeStreamExtractor.parseUploadDate("not a date"));
    }
}
