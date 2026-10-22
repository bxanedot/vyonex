// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Page;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandcampFeaturedExtractorTest {
    @Test
    void mapsFeaturedAlbumsAndUsesTheLastStoryAsPageCursor() throws Exception {
        final String initial = "{\"feed_content\":{\"stories\":{\"featured\":["
                + "{\"album_title\":\"Record\",\"band_name\":\"Artist\","
                + "\"item_url\":\"http://artist.bandcamp.com/album/record\","
                + "\"art_id\":12,\"num_streamable_tracks\":3,\"story_date\":100,"
                + "\"ntid\":200,\"story_type\":\"album\"},{\"story_date\":101,"
                + "\"ntid\":201,\"story_type\":\"video\"}]}}}";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                if ("POST".equals(request.httpMethod())) {
                    assertEquals(BandcampFeaturedExtractor.FEATURED_API_URL, request.url());
                    return new Response(200, "OK", Map.of(), initial, request.url());
                }
                return new Response(200, "OK", Map.of(), "{\"stories\":{\"featured\":[]}}",
                        request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final String url = BandcampFeaturedExtractor.FEATURED_API_URL;
            final BandcampFeaturedExtractor extractor = new BandcampFeaturedExtractor(
                    new BandcampService(1), new ListLinkHandler(url, url, "Featured", List.of(), ""),
                    BandcampFeaturedExtractor.KIOSK_FEATURED);
            extractor.onFetchPage(offline);
            final var firstPage = extractor.getInitialPage();

            assertEquals(1, firstPage.getItems().size());
            assertEquals("Record", firstPage.getItems().get(0).getName());
            assertEquals("Artist", firstPage.getItems().get(0).getUploaderName());
            assertEquals("https://artist.bandcamp.com/album/record",
                    firstPage.getItems().get(0).getUrl());
            assertEquals("https://f4.bcbits.com/img/a12_10.jpg",
                    firstPage.getItems().get(0).getThumbnails().get(0).getUrl());
            assertEquals(BandcampFeaturedExtractor.MORE_FEATURED_API_URL
                    + "?story_groups=featured:101:video:201", firstPage.getNextPage().getUrl());

            final var next = extractor.getPage(new Page(firstPage.getNextPage().getUrl()));
            assertTrue(next.getItems().isEmpty());
            assertFalse(next.hasNextPage());
        } finally {
            VyonEx.init(previous);
        }
    }
}
