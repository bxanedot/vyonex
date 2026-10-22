// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.utils.Utils;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.COUNT_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.START_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.collectItemsFrom;
import static com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory.getUrlSuffix;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

public class PeertubeChannelTabExtractor extends ChannelTabExtractor {
    private final String baseUrl;

    public PeertubeChannelTabExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler)
            throws ParsingException {
        super(service, linkHandler);
        baseUrl = getBaseUrl();
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return getPage(new Page(baseUrl + PeertubeChannelLinkHandlerFactory.API_ENDPOINT
                + getId() + getUrlSuffix(getName()) + "?" + START_KEY + "=0&" + COUNT_KEY + "="
                + ITEMS_PER_PAGE));
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }

        final Response response = getDownloader().get(page.getUrl());

        JsonObject pageJson = null;
        if (response != null && !Utils.isBlank(response.responseBody())) {
            try {
                pageJson = JsonParser.object().from(response.responseBody());
            } catch (final Exception e) {
                throw new ParsingException("Could not parse json data for account info", e);
            }
        }

        if (pageJson == null) {
            throw new ExtractionException("Unable to get account channel list");
        }
        PeertubeParsingHelper.validate(pageJson);

        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        collectItemsFrom(collector, pageJson, getBaseUrl());

        return new InfoItemsPage<>(collector,
                PeertubeParsingHelper.getNextPage(page.getUrl(), pageJson.getLong("total")));
    }
}
