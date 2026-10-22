// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeTrendingLinkHandlerFactory;
import org.junit.jupiter.api.Test;

class PeertubeServiceTest {
    @Test
    void preservesCustomInstanceAndTrendingKiosks() throws Exception {
        final PeertubeService service = new PeertubeService(42,
                new PeertubeInstance("https://tube.example", "Test instance"));

        assertEquals("https://tube.example", service.getBaseUrl());
        assertEquals("PeerTube", service.getServiceInfo().getName());
        assertTrue(service.getServiceInfo().getMediaCapabilities().contains(MediaCapability.VIDEO));
        assertTrue(service.getServiceInfo().getMediaCapabilities().contains(MediaCapability.COMMENTS));

        final var kiosks = service.getKioskList();
        assertEquals(PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING,
                kiosks.getDefaultKioskId());
        assertTrue(kiosks.getAvailableKiosks().contains(
                PeertubeTrendingLinkHandlerFactory.KIOSK_MOST_LIKED));
    }
}
