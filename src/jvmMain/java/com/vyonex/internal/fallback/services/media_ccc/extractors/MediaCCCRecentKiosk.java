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
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCListingPayloadDecoder;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCListingParseException;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class MediaCCCRecentKiosk extends KioskExtractor<StreamInfoItem> {
    public static final String KIOSK_ID = "recent";
    private static final String RECENT_EVENTS_URL = "https://api.media.ccc.de/public/events/recent";
    private List<MediaCCCRecentKioskExtractor> events;

    public MediaCCCRecentKiosk(final StreamingService service, final ListLinkHandler link,
                               final String kioskId) {
        super(service, link, kioskId);
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        try {
            events = MediaCCCListingPayloadDecoder.INSTANCE.decodeRecentEvents(
                            MediaCCCParsingHelper.fetchJsonBody(downloader, RECENT_EVENTS_URL,
                                    getExtractorLocalization()))
                    .getEvents().stream()
                    .map(MediaCCCRecentKioskExtractor::new)
                    .filter(event -> event.getDuration() > 0).toList();
        } catch (MediaCCCListingParseException error) {
            throw new ExtractionException(
                    "Could not parse API response from " + RECENT_EVENTS_URL, error);
        }
    }

    @Nonnull @Override public InfoItemsPage<StreamInfoItem> getInitialPage() {
        final Comparator<StreamInfoItem> newestFirst = Comparator.comparing(
                StreamInfoItem::getUploadDate,
                Comparator.nullsLast(Comparator.comparing(DateWrapper::getInstant).reversed()));
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId(), newestFirst);
        events.forEach(items::commit);
        return new InfoItemsPage<>(items, null);
    }
    @Override public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }
    @Nonnull @Override public String getName() throws ParsingException { return KIOSK_ID; }
}
