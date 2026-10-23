// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.comments.CommentsExtractor;
import com.vyonex.internal.fallback.comments.CommentsInfoItem;
import com.vyonex.internal.fallback.comments.CommentsInfoItemsCollector;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import com.vyonex.internal.json.JsonWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL;

public class BandcampCommentsExtractor extends CommentsExtractor {
    private static final String REVIEWS_API_URL = BASE_API_URL + "/tralbumcollectors/2/reviews";
    private Document document;

    public BandcampCommentsExtractor(final StreamingService service,
                                     final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        document = Jsoup.parse(downloader.get(getLinkHandler().getUrl()).responseBody());
    }

    @Nonnull
    @Override
    public InfoItemsPage<CommentsInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final JsonObject data = embeddedData("#collectors-data", "data-blob");
        final JsonArray reviews = data.getArray("reviews");
        final CommentsInfoItemsCollector items = collect(reviews);
        if (!data.getBoolean("more_reviews_available")) return new InfoItemsPage<>(items, null);
        return new InfoItemsPage<>(items, new Page(List.of(trackId(), nextToken(reviews))));
    }

    @Override
    public InfoItemsPage<CommentsInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        final List<String> ids = page.getIds();
        if (ids.size() != 2) throw new ParsingException("Invalid Bandcamp review cursor");
        final byte[] body = JsonWriter.string().object()
                .value("tralbum_type", "t")
                .value("tralbum_id", ids.get(0))
                .value("token", ids.get(1))
                .value("count", 7)
                .array("exclude_fan_ids").end()
                .end().done().getBytes(StandardCharsets.UTF_8);
        final JsonObject response;
        try {
            response = JsonParser.object().from(getDownloader().postWithContentTypeJson(
                    REVIEWS_API_URL, Map.of(), body).responseBody());
        } catch (final ReCaptchaException | JsonParserException e) {
            throw new ParsingException("Bandcamp reviews response is invalid", e);
        }
        final JsonArray reviews = response.getArray("results");
        final CommentsInfoItemsCollector items = collect(reviews);
        if (!response.getBoolean("more_available")) return new InfoItemsPage<>(items, null);
        return new InfoItemsPage<>(items, new Page(List.of(ids.get(0), nextToken(reviews))));
    }

    @Override
    public boolean isCommentsDisabled() throws ExtractionException {
        return BandcampExtractorHelper.isRadioUrl(getUrl());
    }

    private CommentsInfoItemsCollector collect(final JsonArray reviews) throws ParsingException {
        final CommentsInfoItemsCollector items = new CommentsInfoItemsCollector(getServiceId());
        for (final Object value : reviews) {
            if (value instanceof JsonObject review) {
                items.commit(new BandcampCommentsInfoItemExtractor(review, getUrl()));
            }
        }
        return items;
    }

    private JsonObject embeddedData(final String selector, final String attribute)
            throws ParsingException {
        final Element element = document.selectFirst(selector + "[" + attribute + "]");
        if (element == null) throw new ParsingException("Bandcamp page has no review data");
        try {
            return JsonParser.object().from(element.attr(attribute));
        } catch (final JsonParserException e) {
            throw new ParsingException("Bandcamp review data is invalid", e);
        }
    }

    private String trackId() throws ParsingException {
        final Element properties = document.selectFirst("meta[name=bc-page-properties][content]");
        if (properties == null) throw new ParsingException("Bandcamp page has no item metadata");
        try {
            final long id = JsonParser.object().from(properties.attr("content")).getLong("item_id");
            if (id <= 0) throw new ParsingException("Bandcamp page has an invalid item ID");
            return Long.toString(id);
        } catch (final JsonParserException e) {
            throw new ParsingException("Bandcamp item metadata is invalid", e);
        }
    }

    private String nextToken(final JsonArray reviews) throws ParsingException {
        String token = null;
        for (final Object value : reviews) {
            if (value instanceof JsonObject review) token = review.getString("token", token);
        }
        if (token == null || token.isBlank()) {
            throw new ParsingException("Bandcamp reviews did not include a next-page token");
        }
        return token;
    }
}
