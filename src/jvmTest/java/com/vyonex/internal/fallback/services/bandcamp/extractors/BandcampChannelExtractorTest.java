// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandcampChannelExtractorTest {
    @Test
    void createsOnlyTheArtistTabsThatHaveItems() throws Exception {
        final String details = "{\"name\":\"Artist\",\"bandcamp_url\":"
                + "\"https://artist.bandcamp.com\",\"bio\":\"Artist bio\","
                + "\"bio_image_id\":5,\"discography\":[{\"item_type\":\"album\","
                + "\"title\":\"Record\",\"band_name\":\"Artist\",\"band_id\":10,"
                + "\"item_id\":21,\"art_id\":31},{\"item_type\":\"track\","
                + "\"title\":\"Song\",\"band_name\":\"Artist\",\"band_id\":10,"
                + "\"item_id\":20,\"art_id\":30}]}";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                if (request.url().endsWith("/band_details")) {
                    return new Response(200, "OK", Map.of(), details, request.url());
                }
                if (request.url().contains("tralbum_details")) {
                    final String itemUrl = request.url().contains("tralbum_id=21")
                            ? "https://artist.bandcamp.com/album/record"
                            : "https://artist.bandcamp.com/track/song";
                    return new Response(200, "OK", Map.of(),
                            "{\"bandcamp_url\":\"" + itemUrl + "\"}", request.url());
                }
                return new Response(200, "OK", Map.of(),
                        "<div id='customHeader'><img src='https://img.example/banner.jpg'></div>",
                        request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final String url = "https://artist.bandcamp.com";
            final BandcampService service = new BandcampService(1);
            final BandcampChannelExtractor channel = new BandcampChannelExtractor(service,
                    new ListLinkHandler(url, url, "artist", List.of(), ""));
            channel.onFetchPage(offline);

            assertEquals("Artist", channel.getName());
            assertEquals("Artist bio", channel.getDescription());
            assertEquals("https://f4.bcbits.com/img/5_10.jpg",
                    channel.getAvatars().get(0).getUrl());
            assertEquals("https://img.example/banner.jpg", channel.getBanners().get(0).getUrl());
            final var tabs = channel.getTabs();
            assertEquals(List.of(ChannelTabs.ALBUMS, ChannelTabs.TRACKS),
                    List.of(tabs.get(0).getContentFilters().get(0),
                            tabs.get(1).getContentFilters().get(0)));
            final var albumTab = service.getChannelTabExtractor(tabs.get(0));
            albumTab.onFetchPage(offline);
            final var album = albumTab.getInitialPage().getItems().get(0);
            assertEquals("Record", album.getName());
            assertEquals("https://artist.bandcamp.com/album/record", album.getUrl());
            assertEquals("https://f4.bcbits.com/img/a31_10.jpg",
                    album.getThumbnails().get(0).getUrl());
            final var trackTab = service.getChannelTabExtractor(tabs.get(1));
            trackTab.onFetchPage(offline);
            final var track = trackTab.getInitialPage().getItems().get(0);
            assertEquals("Song", track.getName());
            assertEquals("https://artist.bandcamp.com/track/song", track.getUrl());
            assertEquals("https://f4.bcbits.com/img/a30_10.jpg",
                    track.getThumbnails().get(0).getUrl());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void artistAndTabDetailsUseTheSuppliedDownloader() throws Exception {
        final String details = "{\"name\":\"Artist\",\"bandcamp_url\":"
                + "\"https://artist.bandcamp.com\",\"discography\":[{"
                + "\"item_type\":\"track\",\"title\":\"Song\",\"band_name\":\"Artist\","
                + "\"band_id\":10,\"item_id\":20,\"art_id\":30}]}";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override public Response execute(final Request request) {
                if (!request.url().equals(BandcampExtractorHelper.BASE_API_URL
                        + "/mobile/22/band_details")) {
                    assertEquals("https://artist.bandcamp.com", request.url());
                    return new Response(200, "OK", Map.of(),
                            "<div id='customHeader'><img src='https://img.example/banner.jpg'>"
                                    + "</div>", request.url());
                }
                return new Response(200, "OK", Map.of(), details, request.url());
            }
        };
        final Downloader unexpectedGlobal = new Downloader() {
            @Override public Response execute(final Request request) throws IOException {
                throw new IOException("Unexpected global downloader use");
            }
        };
        try {
            VyonEx.init(unexpectedGlobal);
            final BandcampService service = new BandcampService(1);
            final String url = "https://artist.bandcamp.com";
            final BandcampChannelExtractor channel = new BandcampChannelExtractor(service,
                    new ListLinkHandler(url, url, "artist", List.of(), ""));
            channel.onFetchPage(offline);
            assertEquals("Artist", channel.getName());
            assertEquals("https://img.example/banner.jpg", channel.getBanners().get(0).getUrl());
            assertEquals("track", BandcampExtractorHelper.getArtistDetails(offline, "artist")
                    .getArray("discography").getObject(0).getString("item_type"));

            final BandcampChannelTabExtractor tab = new BandcampChannelTabExtractor(service,
                    new ListLinkHandler(url + "/track", url + "/track", "artist",
                            List.of(ChannelTabs.TRACKS), ""));
            tab.onFetchPage(offline);
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void anEmptyArtistTabProducesAnEmptyPageAfterFetch() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override public Response execute(final Request request) {
                return new Response(200, "OK", Map.of(), "{}", request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final BandcampService service = new BandcampService(1);
            final String url = "https://artist.bandcamp.com/track";
            final BandcampChannelTabExtractor tab = new BandcampChannelTabExtractor(service,
                    new ListLinkHandler(url, url, "artist", List.of(ChannelTabs.TRACKS), ""));

            assertThrows(ParsingException.class, tab::getInitialPage);
            tab.onFetchPage(offline);
            assertTrue(tab.getInitialPage().getItems().isEmpty());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void channelLinkHandlerReadsBandIdAndProfileUrlFromOfflineResponses() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override public Response execute(final Request request) {
                if (request.url().endsWith("/band_details")) {
                    return new Response(200, "OK", Map.of(),
                            "{\"bandcamp_url\":\"https://artist.bandcamp.com\"}", request.url());
                }
                return new Response(200, "OK", Map.of(),
                        "<div data-band=' {\"id\":10} '></div>", request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final BandcampChannelLinkHandlerFactory factory = BandcampChannelLinkHandlerFactory.getInstance();
            final String url = "https://artist.bandcamp.com";

            assertEquals("10", factory.getId(url));
            assertEquals(url, factory.getUrl("10", List.of(), ""));
        } finally {
            VyonEx.init(previous);
        }
    }
}
