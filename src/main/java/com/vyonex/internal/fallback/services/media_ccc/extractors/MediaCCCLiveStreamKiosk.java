// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
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
import java.io.IOException;

public class MediaCCCLiveStreamKiosk extends KioskExtractor<StreamInfoItem> {
    public static final String KIOSK_ID = "live";

    private JsonArray conferences;

    public MediaCCCLiveStreamKiosk(final StreamingService service,
                                   final ListLinkHandler linkHandler,
                                   final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws ExtractionException {
        conferences = MediaCCCParsingHelper.getLiveStreams(downloader, getExtractorLocalization());
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        conferences.streamAsJsonObjects()
                .filter(conference -> conference.getBoolean("isCurrentlyStreaming"))
                .forEach(conference -> addRooms(collector, conference));
        return new InfoItemsPage<>(collector, null);
    }

    private static void addRooms(final StreamInfoItemsCollector collector,
                                 final JsonObject conference) {
        conference.getArray("groups").streamAsJsonObjects().forEach(group -> {
            final String groupName = group.getString("group");
            group.getArray("rooms").streamAsJsonObjects()
                    .map(room -> new MediaCCCLiveStreamKioskExtractor(
                            conference, groupName, room))
                    .forEach(collector::commit);
        });
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException { return KIOSK_ID; }
}
