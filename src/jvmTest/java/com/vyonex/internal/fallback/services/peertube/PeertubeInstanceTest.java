// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PeertubeInstanceTest {
    @Test
    void readsTheDisplayNameFromInstanceConfiguration() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                assertEquals("https://tube.example/api/v1/config", request.url());
                return new Response(200, "OK", Map.of(),
                        "{\"instance\":{\"name\":\"VyonEx Tube\"}}", request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final PeertubeInstance instance = new PeertubeInstance("https://tube.example");
            instance.fetchInstanceMetaData();
            assertEquals("VyonEx Tube", instance.getName());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void reportsConfigurationWithoutAnInstanceName() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(200, "OK", Map.of(), "{\"instance\":{}}", request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final Exception error = assertThrows(Exception.class,
                    () -> new PeertubeInstance("https://tube.example").fetchInstanceMetaData());
            assertEquals("Unable to parse PeerTube instance metadata", error.getMessage());
            assertEquals("PeerTube configuration has no instance name", error.getCause().getMessage());
        } finally {
            VyonEx.init(previous);
        }
    }
}
