// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.localization;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeAgoParserTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 12, 0);

    @Test
    void parsesLocalizedNumberedRelativeDates() throws Exception {
        assertEquals(NOW.minusDays(2).truncatedTo(ChronoUnit.DAYS),
                parser("en").parse("2 days ago").getLocalDateTime());
        assertEquals(NOW.minusHours(3), parser("en").parse("3h ago").getLocalDateTime());
        assertEquals(NOW.minusDays(5).truncatedTo(ChronoUnit.DAYS),
                parser("ru").parse("5 дней назад").getLocalDateTime());
        assertEquals(NOW.minusDays(2).truncatedTo(ChronoUnit.DAYS),
                parser("fr").parse("il y a 2 jours").getLocalDateTime());
        assertEquals(NOW.minusDays(2).truncatedTo(ChronoUnit.DAYS),
                parser("zh", "CN").parse("2天前").getLocalDateTime());
    }

    @Test
    void parsesTextualRelativeDatesAndApproximatesCalendarUnits() throws Exception {
        final DateWrapper yesterday = parser("en").parse("yesterday");
        assertEquals(NOW.minusDays(1).truncatedTo(ChronoUnit.DAYS), yesterday.getLocalDateTime());
        assertTrue(yesterday.isApproximation());

        final DateWrapper lastWeek = parser("en").parse("last week");
        assertEquals(NOW.minusWeeks(1).truncatedTo(ChronoUnit.DAYS), lastWeek.getLocalDateTime());
        assertFalse(parser("en").parse("2 hours ago").isApproximation());
    }

    @Test
    void keepsUnsupportedLocaleAndUnknownUnitBehavior() {
        assertNull(TimeAgoPatternsManager.getTimeAgoParserFor(new Localization("not-a-language"), NOW));
        assertThrows(ParsingException.class, () -> parser("en").parse("recently"));
    }

    private static TimeAgoParser parser(final String language, final String... country) {
        final Localization localization = country.length == 0
                ? new Localization(language)
                : new Localization(language, country[0]);
        return TimeAgoPatternsManager.getTimeAgoParserFor(localization, NOW);
    }
}
