// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InfoContractTest {
    @Test
    void rendersMissingUrlsWithoutFailing() {
        final Info info = new ProbeInfo(2, null, null, null, "untitled");

        assertEquals("ProbeInfo[url=\"null\", name=\"untitled\"]", info.toString());
    }

    @Test
    void preservesIdentityServiceAndCollectedErrors() {
        final LinkHandler handler = new LinkHandler(
                "https://media.ccc.de/v/source", "https://media.ccc.de/v/talk", "talk");
        final Info info = new ProbeInfo(2, handler, "Talk");
        final Exception first = new Exception("first");
        final Exception second = new Exception("second");

        info.addError(first);
        info.addAllErrors(List.of(second));

        assertEquals("talk", info.getId());
        assertEquals("https://media.ccc.de/v/talk", info.getUrl());
        assertEquals("https://media.ccc.de/v/source", info.getOriginalUrl());
        assertSame(ServiceList.MediaCCC, info.getService());
        assertEquals(List.of(first, second), info.getErrors());
        assertEquals("ProbeInfo[url=\"https://media.ccc.de/v/talk\" "
                + "(originalUrl=\"https://media.ccc.de/v/source\"), name=\"Talk\"]",
                info.toString());
    }

    private static final class ProbeInfo extends Info {
        private ProbeInfo(final int serviceId, final String id, final String url,
                          final String originalUrl, final String name) {
            super(serviceId, id, url, originalUrl, name);
        }

        private ProbeInfo(final int serviceId, final LinkHandler handler, final String name) {
            super(serviceId, handler, name);
        }
    }
}
