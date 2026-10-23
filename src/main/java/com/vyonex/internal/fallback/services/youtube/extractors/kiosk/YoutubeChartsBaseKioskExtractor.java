// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors.kiosk;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor.InfoItemsPage;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.UnsupportedContentInCountryException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.services.youtube.InnertubeClientRequestInfo;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;
import com.vyonex.internal.fallback.utils.JsonUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getClientHeaders;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getOriginReferrerHeaders;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareJsonBuilder;

public class YoutubeChartsBaseKioskExtractor extends KioskExtractor<StreamInfoItem> {
    private static final String ENDPOINT = "https://charts.youtube.com/youtubei/v1/browse?alt=json&"
            + DISABLE_PRETTY_PRINT_PARAMETER;

    private final String chartType;
    private final String name;
    private final boolean countryRestricted;
    private JsonObject response;

    public YoutubeChartsBaseKioskExtractor(final StreamingService service,
                                          final ListLinkHandler linkHandler,
                                          final String kioskId,
                                          final String chartType,
                                          final String name,
                                          final boolean countryRestricted) {
        super(service, linkHandler, kioskId);
        this.chartType = chartType;
        this.name = name;
        this.countryRestricted = countryRestricted;
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final Localization localization = getExtractorLocalization();
        final ContentCountry country = getExtractorContentCountry();
        final InnertubeClientRequestInfo client =
                InnertubeClientRequestInfo.ofWebMusicAnalyticsChartsClient();
        final byte[] body = JsonWriter.string(prepareJsonBuilder(localization, country, client, null)
                .value("browseId", "FEmusic_analytics_charts_home")
                .value("query", "perspective=CHART_DETAILS&chart_params_country_code="
                        + country.getCountryCode() + "&chart_params_chart_type=" + chartType)
                .done()).getBytes(StandardCharsets.UTF_8);
        final var headers = new HashMap<>(getOriginReferrerHeaders("https://charts.youtube.com"));
        headers.putAll(getClientHeaders(client.clientInfo.clientId,
                client.clientInfo.clientVersion));

        final Response result = downloader.postWithContentTypeJson(ENDPOINT, headers, body,
                localization);
        if (countryRestricted && result.responseCode() == 400
                && result.responseBody().toLowerCase(Locale.ROOT).contains("invalid argument")) {
            throw new UnsupportedContentInCountryException(
                    "YouTube Charts does not support music charts in this country");
        }
        if (result.responseCode() == 404) getValidJsonResponseBody(result);
        if (result.responseCode() < 200 || result.responseCode() >= 300) {
            throw new ParsingException("YouTube Charts returned HTTP " + result.responseCode());
        }
        response = JsonUtils.toJsonObject(getValidJsonResponseBody(result));
    }

    @Nonnull
    @Override
    public String getName() { return name; }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws ExtractionException {
        final JsonArray sections = response.getObject("contents")
                .getObject("sectionListRenderer").getArray("contents");
        if (sections.isEmpty()) throw new ParsingException("YouTube Charts response has no sections");
        final JsonArray groups = sections.getObject(0).getObject("musicAnalyticsSectionRenderer")
                .getObject("content").getArray("videos");
        if (groups.isEmpty()) throw new ParsingException("YouTube Charts response has no videos");
        final JsonArray videos = groups.getObject(0).getArray("videoViews");
        if (videos.isEmpty()) throw new ParsingException("YouTube Charts response has no video items");

        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        videos.streamAsJsonObjects().map(ChartVideo::new).forEachOrdered(items::commit);
        return new InfoItemsPage<>(items, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }

    private static final class ChartVideo implements StreamInfoItemExtractor {
        private final JsonObject data;

        private ChartVideo(final JsonObject data) { this.data = data; }

        @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }
        @Override public boolean isAd() { return false; }
        @Override public long getDuration() { return data.getInt("videoDuration", -1); }
        @Override public long getViewCount() { return -1; }
        @Override public String getUploaderName() { return data.getString("channelName"); }

        @Override
        public String getUploaderUrl() throws ParsingException {
            final String id = data.getString("externalChannelId");
            if (id == null || id.isBlank()) throw new ParsingException("Chart item has no channel ID");
            return YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/" + id);
        }

        @Override public boolean isUploaderVerified() { return false; }
        @Nullable @Override public String getTextualUploadDate() { return null; }

        @Nonnull
        @Override
        public DateWrapper getUploadDate() throws ParsingException {
            final JsonObject date = data.getObject("releaseDate");
            try {
                return new DateWrapper(LocalDate.of(date.getInt("year"), date.getInt("month"),
                        date.getInt("day")).atStartOfDay(ZoneOffset.UTC).toInstant(), true);
            } catch (final DateTimeException | NullPointerException e) {
                throw new ParsingException("Chart item has an invalid release date", e);
            }
        }

        @Override public String getName() { return data.getString("title"); }

        @Override
        public String getUrl() throws ParsingException {
            final String id = data.getString("id");
            if (id == null || id.isBlank()) throw new ParsingException("Chart item has no video ID");
            return YoutubeStreamLinkHandlerFactory.getInstance().getUrl(id);
        }

        @Nonnull
        @Override
        public List<Image> getThumbnails() throws ParsingException {
            return getThumbnailsFromInfoItem(data);
        }
    }
}
