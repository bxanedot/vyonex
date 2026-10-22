// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeServiceTest {
    @Test
    void exposesServiceRoutesAndKioskRegistration() throws Exception {
        final YoutubeService service = new YoutubeService(11);
        assertEquals("YouTube", service.getServiceInfo().getName());
        assertEquals("https://youtube.com", service.getBaseUrl());
        assertEquals("live", service.getKioskList().getDefaultKioskId());
        assertTrue(service.getKioskList().getAvailableKiosks().containsAll(java.util.List.of(
                "live", "trending_podcasts_episodes", "trending_gaming",
                "trending_movies_and_shows", "trending_music")));
        assertTrue(service.getSupportedCountries().stream()
                .anyMatch(country -> "US".equals(country.getCountryCode())));
    }

    @Test
    void fallsBackToSupportedLanguageAndCountry() {
        final Localization previousLocalization = VyonEx.getPreferredLocalization();
        final ContentCountry previousCountry = VyonEx.getPreferredContentCountry();
        try {
            VyonEx.setupLocalization(new Localization("en", "US"), new ContentCountry("AQ"));
            final YoutubeService service = new YoutubeService(11);
            assertEquals(new Localization("en", "GB"), service.getLocalization());
            assertEquals(ContentCountry.DEFAULT, service.getContentCountry());
        } finally {
            VyonEx.setupLocalization(previousLocalization, previousCountry);
        }
    }
}
