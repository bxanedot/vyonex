// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.UnsupportedContentInCountryException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void chartKiosksKeepTheirNamesAndMusicCountryRestriction() throws Exception {
        final var previousDownloader = VyonEx.getDownloader();
        final Localization previousLocalization = VyonEx.getPreferredLocalization();
        final ContentCountry previousCountry = VyonEx.getPreferredContentCountry();
        VyonEx.init(new Downloader() {
            @Override
            public Response execute(final Request request) throws IOException {
                return new Response(400, "Bad Request", Map.of("Content-Type",
                        java.util.List.of("application/json")),
                        "{\"error\":{\"message\":\"Request contains an invalid argument.\"},"
                                + "\"padding\":\"" + "x".repeat(100) + "\"}",
                        request.url());
            }
        }, previousLocalization, previousCountry);
        try {
            final YoutubeService service = new YoutubeService(11);
            final var kiosks = service.getKioskList();

            assertEquals("Trending Music Videos",
                    kiosks.getExtractorById("trending_music", null).getName());
            assertEquals("Trending Movie Trailers",
                    kiosks.getExtractorById("trending_movies_and_shows", null).getName());

            final Localization requestedLocale = new Localization("fr", "CH");
            assertEquals(requestedLocale, kiosks.getExtractorById("trending_music", null,
                    requestedLocale).getExtractorLocalization());
            final Localization forcedLocale = new Localization("de", "AT");
            kiosks.forceLocalization(forcedLocale);
            assertEquals(forcedLocale, kiosks.getExtractorById("trending_music", null,
                    requestedLocale).getExtractorLocalization());

            kiosks.forceContentCountry(new ContentCountry("MY"));
            assertThrows(UnsupportedContentInCountryException.class,
                    () -> kiosks.getExtractorById("trending_music", null)
                            .onFetchPage(VyonEx.getDownloader()));
        } finally {
            VyonEx.init(previousDownloader, previousLocalization, previousCountry);
        }
    }
}
