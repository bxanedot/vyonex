// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import org.junit.jupiter.api.Test;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceListContractTest {
    @Test
    void preservesRegisteredServicesAndStableIds() {
        final List<StreamingService> services = ServiceList.all();

        assertEquals(List.of(0, 1, 2, 3, 4),
                services.stream().map(StreamingService::getServiceId).toList());
        assertSame(ServiceList.YouTube, services.get(0));
        assertSame(ServiceList.SoundCloud, services.get(1));
        assertSame(ServiceList.MediaCCC, services.get(2));
        assertSame(ServiceList.PeerTube, services.get(3));
        assertSame(ServiceList.Bandcamp, services.get(4));
        assertThrows(UnsupportedOperationException.class, services::clear);
    }

    @Test
    void resolvesServicesAndKeepsLocalizationDefaults() throws Exception {
        assertSame(ServiceList.YouTube, VyonEx.getService(0));
        assertSame(ServiceList.YouTube, VyonEx.getService("YouTube"));
        assertSame(ServiceList.MediaCCC, VyonEx.getServiceByUrl("https://media.ccc.de/v/test"));
        assertThrows(ExtractionException.class, () -> VyonEx.getService(999));
        assertThrows(ExtractionException.class, () -> VyonEx.getService("unknown"));
        assertThrows(ExtractionException.class,
                () -> VyonEx.getServiceByUrl("https://example.test/video"));

        final Localization oldLocalization = VyonEx.getPreferredLocalization();
        final ContentCountry oldCountry = VyonEx.getPreferredContentCountry();
        try {
            final Localization swissGerman = new Localization("de", "CH");
            VyonEx.setupLocalization(swissGerman);
            assertEquals(swissGerman, VyonEx.getPreferredLocalization());
            assertEquals(new ContentCountry("CH"), VyonEx.getPreferredContentCountry());

            final ContentCountry selectedCountry = new ContentCountry("CA");
            VyonEx.setupLocalization(new Localization("fr"), selectedCountry);
            assertEquals(selectedCountry, VyonEx.getPreferredContentCountry());
        } finally {
            VyonEx.setupLocalization(oldLocalization, oldCountry);
        }
    }
}
