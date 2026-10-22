// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Comparator;

public class MediaCCCRecentKiosk extends KioskExtractor<StreamInfoItem> {
    public static final String KIOSK_ID = "recent";
    private static final String RECENT_EVENTS_URL = "https://api.media.ccc.de/public/events/recent";
    private JsonObject data;

    public MediaCCCRecentKiosk(final StreamingService service, final ListLinkHandler link,
                               final String kioskId) {
        super(service, link, kioskId);
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        try {
            data = JsonParser.object().from(downloader.get(
                    RECENT_EVENTS_URL, getExtractorLocalization()).responseBody());
        } catch (final JsonParserException error) {
            throw new ExtractionException("Could not parse recent events.", error);
        }
    }

    @Nonnull @Override public InfoItemsPage<StreamInfoItem> getInitialPage() {
        final Comparator<StreamInfoItem> newestFirst = Comparator.comparing(
                StreamInfoItem::getUploadDate,
                Comparator.nullsLast(Comparator.comparing(DateWrapper::getInstant).reversed()));
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId(), newestFirst);
        final JsonArray events = data.getArray("events");
        events.streamAsJsonObjects().map(MediaCCCRecentKioskExtractor::new)
                .filter(event -> event.getDuration() > 0).forEach(items::commit);
        return new InfoItemsPage<>(items, null);
    }
    @Override public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }
    @Nonnull @Override public String getName() throws ParsingException { return KIOSK_ID; }
}
