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
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;

public class MediaCCCLiveStreamKiosk extends KioskExtractor<StreamInfoItem> {
    public static final String KIOSK_ID = "live";
    private JsonArray conferences;

    public MediaCCCLiveStreamKiosk(final StreamingService service, final ListLinkHandler link,
                                   final String kioskId) {
        super(service, link, kioskId);
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader) throws ExtractionException {
        conferences = MediaCCCParsingHelper.getLiveStreams(downloader, getExtractorLocalization());
    }

    @Nonnull @Override public InfoItemsPage<StreamInfoItem> getInitialPage() {
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        conferences.streamAsJsonObjects()
                .filter(conference -> conference.getBoolean("isCurrentlyStreaming"))
                .forEach(conference -> appendRooms(items, conference));
        return new InfoItemsPage<>(items, null);
    }

    private static void appendRooms(final StreamInfoItemsCollector items,
                                    final JsonObject conference) {
        for (final Object groupValue : conference.getArray("groups")) {
            if (!(groupValue instanceof JsonObject)) continue;
            final JsonObject group = (JsonObject) groupValue;
            final String label = group.getString("group");
            for (final Object roomValue : group.getArray("rooms")) {
                if (roomValue instanceof JsonObject) {
                    items.commit(new MediaCCCLiveStreamKioskExtractor(
                            conference, label, (JsonObject) roomValue));
                }
            }
        }
    }

    @Override public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }
    @Nonnull @Override public String getName() throws ParsingException { return KIOSK_ID; }
}
