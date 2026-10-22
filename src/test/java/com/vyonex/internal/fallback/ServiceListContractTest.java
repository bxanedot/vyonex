// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import org.junit.jupiter.api.Test;

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
}
