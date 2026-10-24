// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.localization;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.utils.LocaleCompat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizationTest {
    @Test
    void normalizesLanguageTagsAndPreservesCountry() {
        final Localization localization = Localization.fromLocalizationCode("fr_CH").orElseThrow();

        assertEquals("fr", localization.getLanguageCode());
        assertEquals("CH", localization.getCountryCode());
        assertEquals("fr-CH", localization.getLocalizationCode());
        assertEquals(new Localization("fr", "CH"), localization);
        assertNotEquals(localization, new Localization("fr"));
        assertTrue(LocaleCompat.forLanguageTag("").isEmpty());
        assertEquals(Locale.US, LocaleCompat.forLanguageTag("en_US").orElseThrow());
    }

    @Test
    void parsesListsAndIsoThreeLetterLanguageCodes() throws Exception {
        assertEquals(List.of(new Localization("en", "GB"), new Localization("de", "CH")),
                Localization.listFrom("en-GB", "de_CH"));
        assertEquals(Locale.GERMAN, Localization.getLocaleFromThreeLetterCode("deu"));
        assertThrows(IllegalArgumentException.class, () -> Localization.listFrom(""));
        assertThrows(ParsingException.class,
                () -> Localization.getLocaleFromThreeLetterCode("xyz"));
    }

    @Test
    void countryValuesCompareByCode() {
        final ContentCountry canada = new ContentCountry("CA");

        assertEquals("CA", canada.toString());
        assertEquals(canada, new ContentCountry("CA"));
        assertEquals(canada.hashCode(), new ContentCountry("CA").hashCode());
        assertFalse(canada.equals(new ContentCountry("CH")));
    }
}
