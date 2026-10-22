// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.suggestion;

import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SuggestionExtractorContractTest {
    @Test
    void forcedLocaleAndCountryOverrideServiceDefaultsAndCanBeCleared() {
        final SoundcloudService service = new SoundcloudService(31);
        final SuggestionExtractor extractor = new SuggestionExtractor(service) {
            @Override
            public List<String> suggestionList(final String query) { return List.of(); }
        };
        final Localization locale = new Localization("de", "CH");
        final ContentCountry country = new ContentCountry("CH");

        extractor.forceLocalization(locale);
        extractor.forceContentCountry(country);
        assertSame(locale, extractor.getExtractorLocalization());
        assertSame(country, extractor.getExtractorContentCountry());

        extractor.forceLocalization(null);
        extractor.forceContentCountry(null);
        assertEquals(service.getLocalization(), extractor.getExtractorLocalization());
        assertEquals(service.getContentCountry(), extractor.getExtractorContentCountry());
        assertEquals(service.getServiceId(), extractor.getServiceId());
    }
}
