// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.ListExtractor.InfoItemsPage;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.UnsupportedContentInCountryException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.youtube.extractors.kiosk.YoutubeChartsBaseKioskExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeChartsBaseKioskExtractorContractTest {
    @Test
    void sendsChartRequestAndMapsVideosForAnyRequestedCountry() throws Exception {
        final var previousDownloader = VyonEx.getDownloader();
        final var previousLocalization = VyonEx.getPreferredLocalization();
        final var previousCountry = VyonEx.getPreferredContentCountry();
        final RecordingDownloader downloader = new RecordingDownloader(chartResponse());
        try {
            VyonEx.init(downloader, previousLocalization, previousCountry);
            final YoutubeChartsBaseKioskExtractor extractor = extractor("trending_music",
                    "TRENDING_VIDEOS", true, "MY");

            extractor.onFetchPage(downloader);
            final InfoItemsPage<StreamInfoItem> page = extractor.getInitialPage();

            assertEquals("https://charts.youtube.com/youtubei/v1/browse?alt=json&prettyPrint=false",
                    downloader.request.url());
            assertEquals("POST", downloader.request.httpMethod());
            assertEquals("application/json", downloader.request.headers()
                    .get("Content-Type").get(0));
            final JsonObject requestBody = JsonParser.object().from(new String(
                    downloader.request.dataToSend(), StandardCharsets.UTF_8));
            assertEquals("FEmusic_analytics_charts_home", requestBody.getString("browseId"));
            assertEquals("perspective=CHART_DETAILS&chart_params_country_code=MY"
                    + "&chart_params_chart_type=TRENDING_VIDEOS", requestBody.getString("query"));

            assertEquals("Trending Music Videos", extractor.getName());
            assertEquals(1, page.getItems().size());
            final StreamInfoItem video = page.getItems().get(0);
            assertEquals("Chart title", video.getName());
            assertEquals("https://www.youtube.com/watch?v=abcdefghijk", video.getUrl());
            assertEquals("Chart channel", video.getUploaderName());
            assertEquals("https://www.youtube.com/channel/UC123", video.getUploaderUrl());
            assertEquals(213, video.getDuration());
            assertEquals(-1, video.getViewCount());
            assertEquals(Instant.parse("2024-05-06T00:00:00Z"),
                    video.getUploadDate().getInstant());
            assertTrue(video.getUploadDate().isApproximation());
            assertEquals(1, video.getThumbnails().size());
            assertFalse(page.hasNextPage());
        } finally {
            VyonEx.init(previousDownloader, previousLocalization, previousCountry);
        }
    }

    @Test
    void reportsUnsupportedCountryFromChartsApiResponse() throws Exception {
        final var previousDownloader = VyonEx.getDownloader();
        final var previousLocalization = VyonEx.getPreferredLocalization();
        final var previousCountry = VyonEx.getPreferredContentCountry();
        final JsonObject response = json("error", json("code", 400,
                "message", "Request contains an invalid argument.",
                "status", "INVALID_ARGUMENT"), "padding", "x".repeat(100));
        final RecordingDownloader downloader = new RecordingDownloader(response, 400);
        try {
            VyonEx.init(downloader, previousLocalization, previousCountry);
            final YoutubeChartsBaseKioskExtractor extractor = extractor("trending_music",
                    "TRENDING_VIDEOS", true, "MY");

            assertThrows(UnsupportedContentInCountryException.class,
                    () -> extractor.onFetchPage(downloader));
            assertTrue(downloader.request.url().contains("charts.youtube.com/youtubei/v1/browse"));
            assertEquals("perspective=CHART_DETAILS&chart_params_country_code=MY"
                            + "&chart_params_chart_type=TRENDING_VIDEOS",
                    JsonParser.object().from(new String(downloader.request.dataToSend(),
                            StandardCharsets.UTF_8)).getString("query"));
        } finally {
            VyonEx.init(previousDownloader, previousLocalization, previousCountry);
        }
    }

    private static YoutubeChartsBaseKioskExtractor extractor(final String kioskId,
                                                              final String chartType,
                                                              final boolean countryRestricted,
                                                              final String country) {
        VyonEx.setupLocalization(new com.vyonex.internal.fallback.localization.Localization(
                "en", "US"), new com.vyonex.internal.fallback.localization.ContentCountry(country));
        return new YoutubeChartsBaseKioskExtractor(new YoutubeService(11),
                new ListLinkHandler("https://charts.youtube.com/charts/TrendingVideos",
                        "https://charts.youtube.com/charts/TrendingVideos", "", List.of(), ""),
                kioskId, chartType, "Trending Music Videos", countryRestricted);
    }

    private static JsonObject chartResponse() {
        final JsonObject video = json("id", "abcdefghijk", "title", "Chart title",
                "videoDuration", 213, "channelName", "Chart channel",
                "externalChannelId", "UC123",
                "releaseDate", json("year", 2024, "month", 5, "day", 6),
                "thumbnail", json("thumbnails", new JsonArray(List.of(
                        json("url", "https://i.ytimg.com/vi/abcdefghijk/default.jpg",
                                "width", 120, "height", 90)))));
        return json("contents", json("sectionListRenderer", json("contents",
                new JsonArray(List.of(json("musicAnalyticsSectionRenderer", json("content",
                        json("videos", new JsonArray(List.of(json("videoViews",
                                new JsonArray(List.of(video)))))))))))),
                "padding", "x".repeat(100));
    }

    private static JsonObject json(final Object... fields) {
        final JsonObject object = new JsonObject();
        for (int i = 0; i < fields.length; i += 2) object.put((String) fields[i], fields[i + 1]);
        return object;
    }

    private static final class RecordingDownloader extends Downloader {
        private final JsonObject response;
        private final int responseCode;
        private Request request;

        private RecordingDownloader(final JsonObject response) { this(response, 200); }

        private RecordingDownloader(final JsonObject response, final int responseCode) {
            this.response = response;
            this.responseCode = responseCode;
        }

        @Override
        public Response execute(final Request value) throws IOException {
            request = value;
            return new Response(responseCode, "response", Map.of("Content-Type",
                    List.of("application/json")), JsonWriter.string(response), value.url());
        }
    }
}
