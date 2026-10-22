// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCService;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferencesListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCLiveListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCRecentListLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaCCCKioskTest {
    @Test
    void conferenceKioskMapsItemsFromOfflineResponse() throws Exception {
        final String body = """
                {"Conferences":"Conferences","conferences":[
                  {"title":"FOSDEM","url":"https://media.ccc.de/c/fosdem",
                   "logo_url":"https://cdn.example.test/fosdem.jpg"}]}
                """;
        final ListLinkHandler link = MediaCCCConferencesListLinkHandlerFactory.getInstance()
                .fromUrl("https://media.ccc.de/public/conferences");
        final StubDownloader downloader = new StubDownloader(body);
        VyonEx.init(downloader);
        final MediaCCCConferenceKiosk kiosk = new MediaCCCConferenceKiosk(
                new MediaCCCService(8), link, MediaCCCConferenceKiosk.KIOSK_ID);

        kiosk.onFetchPage(downloader);

        final var page = kiosk.getInitialPage();
        assertEquals(1, page.getItems().size());
        assertEquals("FOSDEM", page.getItems().get(0).getName());
        assertEquals("https://media.ccc.de/c/fosdem", page.getItems().get(0).getUrl());
        assertEquals("https://media.ccc.de/public/conferences", downloader.requestedUrl);
    }

    @Test
    void recentKioskDropsInvalidDurationsAndSortsNewestFirst() throws Exception {
        final String body = """
                {"events":[
                  {"title":"Older","frontend_link":"https://media.ccc.de/v/older",
                   "conference_title":"FOSDEM","conference_url":"https://api.media.ccc.de/public/conferences/fosdem",
                   "duration":60,"view_count":2,"date":"2026-02-01T12:00:00.000UTC"},
                  {"title":"Broken","frontend_link":"https://media.ccc.de/v/broken",
                   "duration":0,"date":"2026-02-03T12:00:00.000UTC"},
                  {"title":"Newer","frontend_link":"https://media.ccc.de/v/newer",
                   "conference_title":"FOSDEM","conference_url":"https://api.media.ccc.de/public/conferences/fosdem",
                   "duration":120,"view_count":7,"date":"2026-02-03T12:00:00.000UTC"},
                  {"title":"No date","frontend_link":"https://media.ccc.de/v/no-date",
                   "duration":30,"view_count":1}]}
                """;
        final ListLinkHandler link = MediaCCCRecentListLinkHandlerFactory.getInstance()
                .fromUrl("https://media.ccc.de/recent");
        final StubDownloader downloader = new StubDownloader(body);
        VyonEx.init(downloader);
        final MediaCCCRecentKiosk kiosk = new MediaCCCRecentKiosk(
                new MediaCCCService(8), link, MediaCCCRecentKiosk.KIOSK_ID);

        kiosk.onFetchPage(downloader);

        final List<StreamInfoItem> items = kiosk.getInitialPage().getItems();
        assertEquals(3, items.size());
        assertEquals("Newer", items.get(0).getName());
        assertEquals("Older", items.get(1).getName());
        assertEquals("No date", items.get(2).getName());
        assertEquals("https://api.media.ccc.de/public/events/recent", downloader.requestedUrl);
    }

    @Test
    void liveKioskIncludesRoomsOnlyFromActiveConferences() throws Exception {
        final String body = """
                [{"conference":"Chaos","slug":"chaos-2026","isCurrentlyStreaming":true,
                  "groups":[{"group":"Hall A","rooms":[
                    {"talks":{"current":{"title":"Live video"}},"link":"https://streaming.media.ccc.de/chaos-2026/a",
                     "display":"Room A","streams":[{"type":"video"}]},
                    {"talks":{"current":{"title":"Live audio"}},"link":"https://streaming.media.ccc.de/chaos-2026/b",
                     "display":"Room B","streams":[{"type":"audio"}]}]}]},
                 {"conference":"Past","slug":"past","isCurrentlyStreaming":false,"groups":[]}] 
                """;
        final var link = MediaCCCLiveListLinkHandlerFactory.getInstance()
                .fromUrl("https://media.ccc.de/live");
        final StubDownloader downloader = new StubDownloader(body);
        VyonEx.init(downloader);
        final MediaCCCLiveStreamKiosk kiosk = new MediaCCCLiveStreamKiosk(
                new MediaCCCService(8), link, MediaCCCLiveStreamKiosk.KIOSK_ID);

        kiosk.onFetchPage(downloader);

        final List<StreamInfoItem> items = kiosk.getInitialPage().getItems();
        assertEquals(2, items.size());
        assertEquals("Live video", items.get(0).getName());
        assertEquals("Live audio", items.get(1).getName());
        assertEquals("Chaos - Hall A - Room A", items.get(0).getUploaderName());
        assertEquals("Chaos - Hall A - Room B", items.get(1).getUploaderName());
        assertEquals("https://streaming.media.ccc.de/streams/v2.json", downloader.requestedUrl);
    }

    private static final class StubDownloader extends Downloader {
        private final String body;
        private String requestedUrl;

        private StubDownloader(final String body) { this.body = body; }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            requestedUrl = request.url();
            return new Response(200, "OK", Map.of(), body, request.url());
        }
    }
}
