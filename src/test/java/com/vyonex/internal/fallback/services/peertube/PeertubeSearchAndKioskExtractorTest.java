// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeSearchQueryHandlerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PeertubeSearchAndKioskExtractorTest {
    @Test
    void rejectsSearchBodiesReturnedWithAnHttpFailure() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        final String url = "https://tube.example/api/v1/search/videos?search=hello&start=0&count=12";
        final Downloader offline = new Downloader() {
            @Override public Response execute(final Request request) {
                return new Response(503, "Unavailable", Map.of(),
                        "{\"total\":0,\"data\":[]}", request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final PeertubeService service = new PeertubeService(3,
                    new PeertubeInstance("https://tube.example", "Offline"));
            final String queryUrl = PeertubeSearchQueryHandlerFactory.getInstance().getUrl(
                    "hello", List.of(PeertubeSearchQueryHandlerFactory.VIDEOS), "",
                    "https://tube.example");
            final SearchExtractor search = service.getSearchExtractor(new SearchQueryHandler(
                    queryUrl, queryUrl, "hello",
                    List.of(PeertubeSearchQueryHandlerFactory.VIDEOS), ""));

            assertThrows(ExtractionException.class, () -> search.getPage(new Page(url)));
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void searchReturnsVideoResultsAndNextPage() throws Exception {
        final String url = "https://tube.example/api/v1/search/videos?search=hello&start=0&count=12";
        final StubDownloader downloader = new StubDownloader(Map.of(url, videoResponse("video-1", null)));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3,
                new PeertubeInstance("https://tube.example", "Offline"));
        final String queryUrl = PeertubeSearchQueryHandlerFactory.getInstance().getUrl(
                "hello", List.of(PeertubeSearchQueryHandlerFactory.VIDEOS), "",
                "https://tube.example");
        final var query = new SearchQueryHandler(queryUrl, queryUrl, "hello",
                List.of(PeertubeSearchQueryHandlerFactory.VIDEOS), "");
        final SearchExtractor search = service.getSearchExtractor(query);

        final var page = search.getInitialPage();

        assertEquals(1, page.getItems().size());
        assertEquals("Video video-1", page.getItems().get(0).getName());
        assertEquals("https://tube.example/videos/watch/video-1", page.getItems().get(0).getUrl());
        assertEquals(url.replace("start=0", "start=12"), page.getNextPage().getUrl());
        assertEquals(List.of(url), downloader.requestedUrls);
    }

    @Test
    void sepiaSearchUsesTheVideoHostFromEachResult() throws Exception {
        final String url = PeertubeSearchQueryHandlerFactory.SEPIA_BASE_URL
                + "/api/v1/search/videos?search=hello&start=0&count=12";
        final StubDownloader downloader = new StubDownloader(Map.of(url,
                videoResponse("remote-1", "https://remote.example/videos/embed/remote-1")));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3);
        final String queryUrl = PeertubeSearchQueryHandlerFactory.getInstance().getUrl(
                "hello", List.of(PeertubeSearchQueryHandlerFactory.SEPIA_VIDEOS), "",
                PeertubeSearchQueryHandlerFactory.SEPIA_BASE_URL);
        final var query = new SearchQueryHandler(queryUrl, queryUrl, "hello",
                List.of(PeertubeSearchQueryHandlerFactory.SEPIA_VIDEOS), "");

        final var page = service.getSearchExtractor(query).getInitialPage();

        assertEquals("https://remote.example/videos/watch/remote-1",
                page.getItems().get(0).getUrl());
    }

    @Test
    void defaultKioskLoadsTrendingItems() throws Exception {
        final String url = "https://framatube.org/api/v1/videos?sort=-trending&start=0&count=12";
        final StubDownloader downloader = new StubDownloader(Map.of(url, videoResponse("trend-1", null)));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3);
        final KioskExtractor<?> kiosk = service.getKioskList().getDefaultKioskExtractor();

        final var page = kiosk.getInitialPage();

        assertEquals("Trending", kiosk.getId());
        assertEquals("Video trend-1", page.getItems().get(0).getName());
        assertNotNull(page.getNextPage());
    }

    @Test
    void rejectsMissingPagesBeforeMakingRequests() throws Exception {
        final PeertubeService service = new PeertubeService(3,
                new PeertubeInstance("https://tube.example", "Offline"));
        final String queryUrl = PeertubeSearchQueryHandlerFactory.getInstance().getUrl(
                "hello", List.of(PeertubeSearchQueryHandlerFactory.VIDEOS), "",
                "https://tube.example");
        final SearchExtractor search = service.getSearchExtractor(new SearchQueryHandler(
                queryUrl, queryUrl, "hello",
                List.of(PeertubeSearchQueryHandlerFactory.VIDEOS), ""));
        final KioskExtractor<?> kiosk = service.getKioskList().getDefaultKioskExtractor();

        assertThrows(IllegalArgumentException.class, () -> search.getPage(null));
        assertThrows(IllegalArgumentException.class, () -> kiosk.getPage(null));
    }

    private static String videoResponse(final String id, final String embedUrl) {
        final String embed = embedUrl == null ? "" : ",\"embedUrl\":\"" + embedUrl + "\"";
        return "{\"total\":13,\"data\":[{\"uuid\":\"" + id
                + "\",\"name\":\"Video " + id
                + "\",\"account\":{\"name\":\"alice\",\"host\":\"tube.example\","
                + "\"displayName\":\"Alice\"}" + embed + "}]}";
    }

    private static final class StubDownloader extends Downloader {
        private final Map<String, String> bodies;
        private final List<String> requestedUrls = new ArrayList<>();

        private StubDownloader(final Map<String, String> bodies) { this.bodies = bodies; }

        @Override public Response execute(final Request request)
                throws IOException, ReCaptchaException {
            requestedUrls.add(request.url());
            final String body = bodies.get(request.url());
            if (body == null) throw new IOException("No offline response for " + request.url());
            return new Response(200, "OK", Map.of(), body, request.url());
        }
    }
}
