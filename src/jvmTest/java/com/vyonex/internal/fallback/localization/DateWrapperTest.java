// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.localization;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateWrapperTest {
    @Test
    void preservesInstantsZonesAndApproximation() {
        final Instant instant = Instant.parse("2026-10-22T10:00:00Z");
        final DateWrapper precise = new DateWrapper(instant);
        final DateWrapper approximate = new DateWrapper(
                LocalDateTime.of(2026, 10, 22, 12, 0), true);

        assertEquals(instant, precise.getInstant());
        assertEquals(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC), precise.offsetDateTime());
        assertEquals(LocalDateTime.ofInstant(instant, ZoneId.of("Europe/Zurich")),
                precise.getLocalDateTime(ZoneId.of("Europe/Zurich")));
        assertFalse(precise.isApproximation());
        assertTrue(approximate.isApproximation());
        assertEquals(LocalDateTime.of(2026, 10, 22, 12, 0)
                        .atZone(ZoneId.systemDefault()).toInstant(), approximate.getInstant());
    }

    @Test
    void parsesIsoInstantsAndWrapsInvalidValues() throws Exception {
        assertEquals(Instant.parse("2026-10-22T10:00:00Z"),
                DateWrapper.fromOffsetDateTime("2026-10-22T12:00:00+02:00").getInstant());
        assertEquals(Instant.parse("2026-10-22T10:00:00Z"),
                DateWrapper.fromInstant("2026-10-22T10:00:00Z").getInstant());
        assertNull(DateWrapper.fromOffsetDateTime(null));
        assertNull(DateWrapper.fromInstant(null));
        assertThrows(ParsingException.class, () -> DateWrapper.fromInstant("invalid"));
    }
}
