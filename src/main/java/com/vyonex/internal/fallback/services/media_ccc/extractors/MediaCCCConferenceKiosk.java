// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelInfoItem;
import com.vyonex.internal.fallback.channel.ChannelInfoItemsCollector;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCConferenceInfoItemExtractor;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class MediaCCCConferenceKiosk extends KioskExtractor<ChannelInfoItem> {
    public static final String KIOSK_ID = "conferences";
    private String title;
    private List<MediaCCCConferenceInfoItemExtractor> conferences;

    public MediaCCCConferenceKiosk(final StreamingService service,
                                   final ListLinkHandler link, final String kioskId) {
        super(service, link, kioskId);
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final JsonObject data = MediaCCCParsingHelper.fetchJsonObject(downloader,
                getLinkHandler().getUrl(), getExtractorLocalization());
        title = data.getString("Conferences");
        conferences = data.getArray("conferences").streamAsJsonObjects()
                .map(MediaCCCConferenceInfoItemExtractor::new).toList();
    }

    @Nonnull @Override public InfoItemsPage<ChannelInfoItem> getInitialPage() {
        final ChannelInfoItemsCollector items = new ChannelInfoItemsCollector(getServiceId());
        conferences.forEach(items::commit);
        return new InfoItemsPage<>(items, null);
    }
    @Override public InfoItemsPage<ChannelInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }
    @Nonnull @Override public String getName() throws ParsingException {
        return title;
    }
}
