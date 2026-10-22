// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import static com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory.ITEMS_PER_PAGE;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.InfoItemExtractor;
import com.vyonex.internal.fallback.InfoItemsCollector;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.utils.Parser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;

import javax.annotation.Nonnull;

public class SoundcloudSearchExtractor extends SearchExtractor {
    private JsonObject initialSearchObject;
    private static final String COLLECTION = "collection";
    private static final String TOTAL_RESULTS = "total_results";

    public SoundcloudSearchExtractor(final StreamingService service,
                                     final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() {
        return "";
    }

    @Override
    public boolean isCorrectedSearch() {
        return false;
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        if (initialSearchObject.getInt(TOTAL_RESULTS) > ITEMS_PER_PAGE) {
            return new InfoItemsPage<>(
                    collectItems(initialSearchObject.getArray(COLLECTION)),
                    getNextPageFromCurrentUrl(getUrl(), currentOffset -> ITEMS_PER_PAGE));
        } else {
            return new InfoItemsPage<>(
                    collectItems(initialSearchObject.getArray(COLLECTION)), null);
        }

    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page) throws IOException,
            ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }

        final Downloader dl = getDownloader();
        final JsonArray searchCollection;
        final int totalResults;
        try {
            final String response = dl.get(page.getUrl(), getExtractorLocalization())
                    .responseBody();
            final JsonObject result = JsonParser.object().from(response);
            searchCollection = result.getArray(COLLECTION);
            totalResults = result.getInt(TOTAL_RESULTS);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }

        if (getOffsetFromUrl(page.getUrl()) + ITEMS_PER_PAGE < totalResults) {
            return new InfoItemsPage<>(collectItems(searchCollection),
                    getNextPageFromCurrentUrl(page.getUrl(),
                            currentOffset -> currentOffset + ITEMS_PER_PAGE));
        }
        return new InfoItemsPage<>(collectItems(searchCollection), null);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws IOException,
            ExtractionException {
        final Downloader dl = getDownloader();
        final String url = getUrl();
        try {
            final String response = dl.get(url, getExtractorLocalization()).responseBody();
            initialSearchObject = JsonParser.object().from(response);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }

        if (initialSearchObject.getArray(COLLECTION).isEmpty()) {
            throw new SearchExtractor.NothingFoundException("Nothing found");
        }
    }

    private InfoItemsCollector<InfoItem, InfoItemExtractor> collectItems(
            final JsonArray searchCollection) {
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

        for (final Object result : searchCollection) {
            if (!(result instanceof JsonObject)) {
                continue;
            }

            final JsonObject searchResult = (JsonObject) result;
            final String kind = searchResult.getString("kind", "");
            switch (kind) {
                case "user":
                    collector.commit(new SoundcloudChannelInfoItemExtractor(searchResult));
                    break;
                case "track":
                    collector.commit(new SoundcloudStreamInfoItemExtractor(searchResult));
                    break;
                case "playlist":
                    collector.commit(new SoundcloudPlaylistInfoItemExtractor(searchResult));
                    break;
            }
        }

        return collector;
    }

    private Page getNextPageFromCurrentUrl(final String currentUrl,
                                           final IntUnaryOperator newPageOffsetCalculator)
            throws ParsingException {
        final int currentPageOffset = getOffsetFromUrl(currentUrl);

        return new Page(
                currentUrl.replace(
                        "&offset=" + currentPageOffset,
                        "&offset=" + newPageOffsetCalculator.applyAsInt(currentPageOffset)));
    }

    private int getOffsetFromUrl(final String url) throws ParsingException {
        try {
            return Integer.parseInt(Parser.compatParseMap(new URL(url).getQuery()).get("offset"));
        } catch (final MalformedURLException e) {
            throw new ParsingException("Could not get offset from page URL", e);
        }
    }
}
