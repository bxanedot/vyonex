// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL;

public class BandcampRadioExtractor extends KioskExtractor<StreamInfoItem> {
    public static final String KIOSK_RADIO = "Radio";
    public static final String RADIO_API_URL = BASE_API_URL + "/bcweekly/3/list";
    private JsonArray shows;

    public BandcampRadioExtractor(final StreamingService service,
                                  final ListLinkHandler linkHandler,
                                  final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        try {
            shows = JsonParser.object().from(downloader.get(RADIO_API_URL).responseBody())
                    .getArray("results");
        } catch (final JsonParserException e) {
            throw new ParsingException("Bandcamp radio response is invalid", e);
        }
    }

    @Nonnull
    @Override
    public String getName() { return KIOSK_RADIO; }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() {
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        for (final Object value : shows) {
            if (value instanceof JsonObject show) items.commit(new BandcampRadioInfoItemExtractor(show));
        }
        return new InfoItemsPage<>(items, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) { return null; }
}
