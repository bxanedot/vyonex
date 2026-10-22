// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
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
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import javax.annotation.Nonnull;
import java.io.IOException;

public class MediaCCCConferenceKiosk extends KioskExtractor<ChannelInfoItem> {
    public static final String KIOSK_ID = "conferences";

    private JsonObject data;

    public MediaCCCConferenceKiosk(final StreamingService service,
                                   final ListLinkHandler linkHandler,
                                   final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        try {
            data = JsonParser.object().from(downloader.get(
                    getLinkHandler().getUrl(), getExtractorLocalization()).responseBody());
        } catch (JsonParserException e) {
            throw new ExtractionException("Could not parse conference list.", e);
        }
    }

    @Nonnull
    @Override
    public InfoItemsPage<ChannelInfoItem> getInitialPage() {
        final JsonArray conferences = data.getArray("conferences");
        final ChannelInfoItemsCollector collector = new ChannelInfoItemsCollector(getServiceId());
        conferences.streamAsJsonObjects()
                .map(MediaCCCConferenceInfoItemExtractor::new)
                .forEach(collector::commit);
        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<ChannelInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException { return data.getString("Conferences"); }
}
