// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

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
        assertTrue(service.getKioskList().getAvailableKiosks().contains("trending_music"));
        assertTrue(service.getSupportedCountries().stream()
                .anyMatch(country -> "US".equals(country.getCountryCode())));
    }
}
