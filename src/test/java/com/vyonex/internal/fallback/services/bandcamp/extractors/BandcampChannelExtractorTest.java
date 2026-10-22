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
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BandcampChannelExtractorTest {
    @Test
    void createsOnlyTheArtistTabsThatHaveItems() throws Exception {
        final String details = "{\"name\":\"Artist\",\"bandcamp_url\":"
                + "\"https://artist.bandcamp.com\",\"bio\":\"Artist bio\","
                + "\"bio_image_id\":5,\"discography\":[{\"item_type\":\"track\","
                + "\"title\":\"Song\",\"band_name\":\"Artist\",\"band_id\":10,"
                + "\"item_id\":20,\"art_id\":30},{\"item_type\":\"album\","
                + "\"title\":\"Record\",\"band_name\":\"Artist\",\"band_id\":10,"
                + "\"item_id\":21,\"art_id\":31}]}";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                if (request.url().endsWith("/band_details")) {
                    return new Response(200, "OK", Map.of(), details, request.url());
                }
                if (request.url().contains("tralbum_details")) {
                    return new Response(200, "OK", Map.of(),
                            "{\"bandcamp_url\":\"https://artist.bandcamp.com/track/song\"}",
                            request.url());
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
            assertEquals(List.of(ChannelTabs.TRACKS, ChannelTabs.ALBUMS),
                    List.of(tabs.get(0).getContentFilters().get(0),
                            tabs.get(1).getContentFilters().get(0)));
            final var trackTab = service.getChannelTabExtractor(tabs.get(0));
            trackTab.onFetchPage(offline);
            assertEquals("Song", trackTab.getInitialPage().getItems().get(0).getName());
            final var albumTab = service.getChannelTabExtractor(tabs.get(1));
            albumTab.onFetchPage(offline);
            assertEquals("Record", albumTab.getInitialPage().getItems().get(0).getName());
        } finally {
            VyonEx.init(previous);
        }
    }
}
