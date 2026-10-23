// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BandcampRadioExtractorTest {
    @Test
    void createsRadioItemsFromThePublicListingResponse() throws Exception {
        final String body = "{\"results\":[{\"id\":321,\"subtitle\":\"Weekly episode\","
                + "\"title\":\"Bandcamp Weekly\",\"desc\":\"About the show\","
                + "\"date\":\"15 Jun 2024 12:30:00 UTC\",\"image_id\":55}]}";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                assertEquals(BandcampRadioExtractor.RADIO_API_URL, request.url());
                return new Response(200, "OK", Map.of(), body, request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final BandcampRadioExtractor extractor = new BandcampRadioExtractor(
                    new BandcampService(1),
                    new ListLinkHandler(BandcampRadioExtractor.RADIO_API_URL,
                            BandcampRadioExtractor.RADIO_API_URL, "Radio", List.of(), ""),
                    BandcampRadioExtractor.KIOSK_RADIO);
            extractor.onFetchPage(offline);
            final var item = extractor.getInitialPage().getItems().get(0);

            assertEquals("Weekly episode", item.getName());
            assertEquals("https://bandcamp.com/?show=321", item.getUrl());
            assertEquals("Bandcamp Weekly", item.getUploaderName());
            assertEquals("About the show", item.getShortDescription());
            assertEquals("2024-06-15T12:30:00Z", item.getUploadDate().getInstant().toString());
            assertEquals("https://f4.bcbits.com/img/55_10.jpg",
                    item.getThumbnails().get(0).getUrl());
        } finally {
            VyonEx.init(previous);
        }
    }
}
