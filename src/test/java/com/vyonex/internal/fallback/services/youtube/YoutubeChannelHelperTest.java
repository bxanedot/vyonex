// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeChannelHelperTest {
    @Test
    void readsClassicCarouselAndPageChannelHeaders() throws Exception {
        final var classic = parse("{\"header\":{\"c4TabbedHeaderRenderer\":{"
                + "\"channelId\":\"UCclassic\",\"title\":\"Classic\","
                + "\"badges\":[{\"metadataBadgeRenderer\":{"
                + "\"style\":\"BADGE_STYLE_TYPE_VERIFIED\"}}]}}}");
        final var classicHeader = YoutubeChannelHelper.getChannelHeader(classic);
        assertEquals(YoutubeChannelHelper.ChannelHeader.HeaderType.C4_TABBED,
                classicHeader.headerType);
        assertEquals("UCclassic", YoutubeChannelHelper.getChannelId(classicHeader, classic, null));
        assertTrue(YoutubeChannelHelper.isChannelVerified(classicHeader));
        assertEquals("Classic", YoutubeChannelHelper.getChannelName(classicHeader, null, classic));

        final var carousel = parse("{\"header\":{\"carouselHeaderRenderer\":{\"contents\":["
                + "{\"topicChannelDetailsRenderer\":{\"title\":{\"simpleText\":\"Music\"},"
                + "\"navigationEndpoint\":{\"browseEndpoint\":{\"browseId\":\"UCtopic\"}}}}]}}}");
        final var carouselHeader = YoutubeChannelHelper.getChannelHeader(carousel);
        assertEquals(YoutubeChannelHelper.ChannelHeader.HeaderType.CAROUSEL,
                carouselHeader.headerType);
        assertEquals("UCtopic", YoutubeChannelHelper.getChannelId(
                carouselHeader, carousel, null));
        assertTrue(YoutubeChannelHelper.isChannelVerified(carouselHeader));
        assertEquals("Music", YoutubeChannelHelper.getChannelName(carouselHeader, null, carousel));

        final var page = parse("{\"header\":{\"pageHeaderRenderer\":{\"pageTitle\":\"Page\","
                + "\"content\":{\"pageHeaderViewModel\":{\"title\":{\"dynamicTextViewModel\":"
                + "{\"text\":{\"content\":\"System\"}}},\"image\":{\"contentPreviewImageViewModel\":{}}}}}}}");
        final var pageHeader = YoutubeChannelHelper.getChannelHeader(page);
        assertEquals(YoutubeChannelHelper.ChannelHeader.HeaderType.PAGE, pageHeader.headerType);
        assertEquals("System", YoutubeChannelHelper.getChannelName(pageHeader, null, page));
        assertTrue(YoutubeChannelHelper.isChannelVerified(pageHeader));
    }

    @Test
    void usesAgeGateMetadataAndFallbackChannelIdentifiers() throws Exception {
        final var response = parse("{\"metadata\":{\"channelMetadataRenderer\":{"
                + "\"externalChannelId\":\"UCmetadata\",\"title\":\"Metadata\"}},"
                + "\"microformat\":{\"microformatDataRenderer\":{\"title\":\"Microformat\"}}}");
        assertEquals("UCmetadata", YoutubeChannelHelper.getChannelId(null, response, null));
        assertEquals("Metadata", YoutubeChannelHelper.getChannelName(null, null, response));
        assertEquals("fallback", YoutubeChannelHelper.getChannelId(null, parse("{}"), "fallback"));
        assertThrows(ParsingException.class,
                () -> YoutubeChannelHelper.getChannelId(null, parse("{}"), null));

        final var ageResponse = parse("{\"contents\":{\"twoColumnBrowseResultsRenderer\":{"
                + "\"tabs\":[{\"tabRenderer\":{\"content\":{\"sectionListRenderer\":{"
                + "\"contents\":[{\"channelAgeGateRenderer\":{\"channelTitle\":\"Age-gated\"}}]}}}}]}}}");
        final var ageGate = YoutubeChannelHelper.getChannelAgeGateRenderer(ageResponse);
        assertNotNull(ageGate);
        assertEquals("Age-gated", YoutubeChannelHelper.getChannelName(null, ageGate, ageResponse));
        assertNull(YoutubeChannelHelper.getChannelAgeGateRenderer(parse("{}")));
    }

    @Test
    void followsChannelRedirectsUsingOfflineResponses() throws Exception {
        final StubDownloader downloader = new StubDownloader(List.of(
                "{\"onResponseReceivedActions\":[{\"navigateAction\":{\"endpoint\":{"
                        + "\"commandMetadata\":{\"webCommandMetadata\":{"
                        + "\"webPageType\":\"WEB_PAGE_TYPE_BROWSE\"}},"
                        + "\"browseEndpoint\":{\"browseId\":\"UCnext\"}}}}],"
                        + "\"padding\":\"" + "x".repeat(70) + "\"}",
                "{\"onResponseReceivedActions\":[{\"navigateAction\":{\"endpoint\":{"
                        + "\"commandMetadata\":{\"webCommandMetadata\":{"
                        + "\"webPageType\":\"WEB_PAGE_TYPE_UNKNOWN\"}}}}}],"
                        + "\"marker\":\"final\",\"padding\":\"" + "x".repeat(70) + "\"}"));
        final var previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            final var response = YoutubeChannelHelper.getChannelResponse("UCstart",
                    "tab-params", Localization.DEFAULT, ContentCountry.DEFAULT);

            assertEquals("UCnext", response.channelId);
            assertTrue(response.jsonResponse.getString("marker").equals("final"));
            assertEquals(2, downloader.browseRequests.size());
            assertEquals("UCstart", JsonParser.object().from(new String(
                    downloader.browseRequests.get(0).dataToSend(), StandardCharsets.UTF_8))
                    .getString("browseId"));
            assertEquals("tab-params", JsonParser.object().from(new String(
                    downloader.browseRequests.get(1).dataToSend(), StandardCharsets.UTF_8))
                    .getString("params"));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    @Test
    void returnsChannelIdsAlreadyPresentInPaths() throws Exception {
        assertEquals("UC123", YoutubeChannelHelper.resolveChannelId("channel/UC123"));
        assertEquals("UC456", YoutubeChannelHelper.resolveChannelId("UC456/videos"));
    }

    @Test
    void resolvesHandlesThroughTheNavigationEndpointOffline() throws Exception {
        final StubDownloader downloader = new StubDownloader(List.of(
                "{\"endpoint\":{\"commandMetadata\":{\"webCommandMetadata\":{"
                        + "\"webPageType\":\"WEB_PAGE_TYPE_CHANNEL\"}},"
                        + "\"browseEndpoint\":{\"browseId\":\"UCresolved\"}},"
                        + "\"padding\":\"" + "x".repeat(70) + "\"}"));
        final var previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            assertEquals("UCresolved", YoutubeChannelHelper.resolveChannelId("@creator"));
            assertEquals("https://www.youtube.com/youtubei/v1/navigation/resolve_url"
                            + "?prettyPrint=false", downloader.browseRequests.get(0).url());
            assertEquals("https://www.youtube.com/@creator",
                    JsonParser.object().from(new String(
                            downloader.browseRequests.get(0).dataToSend(),
                            StandardCharsets.UTF_8)).getString("url"));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static com.vyonex.internal.json.JsonObject parse(final String source)
            throws JsonParserException {
        return JsonParser.object().from(source);
    }

    private static final class StubDownloader extends Downloader {
        private final List<String> browseResponses;
        private final List<Request> browseRequests = new ArrayList<>();
        private int responseIndex;

        private StubDownloader(final List<String> browseResponses) {
            this.browseResponses = browseResponses;
        }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            if (request.url().endsWith("/sw.js")) {
                return new Response(200, "OK", Map.of(),
                        "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261021.01.00\"",
                        request.url());
            }
            browseRequests.add(request);
            if (responseIndex >= browseResponses.size()) {
                throw new IOException("No offline channel response for " + request.url());
            }
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    browseResponses.get(responseIndex++), request.url());
        }
    }
}
