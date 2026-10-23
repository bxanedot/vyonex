// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.subscription.SubscriptionExtractor.InvalidSourceException;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeSubscriptionExtractorContractTest {
    private final YoutubeSubscriptionExtractor extractor =
            new YoutubeSubscriptionExtractor(new YoutubeService(9));

    @Test
    void importsJsonAndCsvSubscriptions() throws Exception {
        final String id = "UCsXVk37bltHxD1rDPwtNM8Q";
        final var jsonItems = extractor.fromInputStream(bytes("""
                [{"snippet":{"resourceId":{"channelId":"%s"},"title":"Creator"}}]
                """.formatted(id)), "application/json");
        final var csvItems = extractor.fromInputStream(bytes(
                "Channel Id,Channel Url,Channel Title\n" + id
                        + ",http://www.youtube.com/channel/" + id
                        + ",\"Creator, \"\"Live\"\"\"\n"), "text/csv");

        assertEquals(1, jsonItems.size());
        assertEquals(9, jsonItems.get(0).getServiceId());
        assertEquals("https://www.youtube.com/channel/" + id, jsonItems.get(0).getUrl());
        assertEquals("Creator", jsonItems.get(0).getName());
        assertEquals("https://www.youtube.com/channel/" + id, csvItems.get(0).getUrl());
        assertEquals("Creator, \"Live\"", csvItems.get(0).getName());
    }

    @Test
    void scansZipUntilItFindsAUsableSubscriptionCsv() throws Exception {
        final ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(archive, StandardCharsets.UTF_8)) {
            addEntry(zip, "other.csv", "Name,Date\nA,B\n");
            addEntry(zip, "Takeout/YouTube/subscriptions/subscriptions.csv",
                    "Channel Id,Channel Url,Channel Title\n"
                            + "UCsXVk37bltHxD1rDPwtNM8Q,https://www.youtube.com/channel/"
                            + "UCsXVk37bltHxD1rDPwtNM8Q,Creator\n");
        }

        final var items = extractor.fromInputStream(new ByteArrayInputStream(archive.toByteArray()),
                "application/zip");
        assertEquals(List.of("Creator"), items.stream().map(item -> item.getName()).toList());
    }

    @Test
    void reportsInvalidAndUnsupportedInputs() {
        assertThrows(InvalidSourceException.class,
                () -> extractor.fromInputStream(bytes("not json"), "application/json"));
        assertThrows(InvalidSourceException.class,
                () -> extractor.fromInputStream(bytes(""), "application/octet-stream"));
    }

    private static ByteArrayInputStream bytes(final String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void addEntry(final ZipOutputStream zip, final String name, final String value)
            throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
