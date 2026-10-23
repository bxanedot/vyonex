// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class MediaCCCChannelTabExtractor extends ChannelTabExtractor {
    @Nullable private List<MediaCCCStreamInfoItemExtractor> events;

    public MediaCCCChannelTabExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler,
                                       @Nullable final List<MediaCCCStreamInfoItemExtractor> events) {
        super(service, linkHandler);
        this.events = events;
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws ExtractionException, IOException {
        if (events == null) {
            final MediaCCCConferenceExtractor.ConferenceData conference =
                    MediaCCCConferenceExtractor.fetchConferenceData(downloader, getId());
            events = conference.events();
        }
    }

    @Nonnull @Override public ListExtractor.InfoItemsPage<InfoItem> getInitialPage() {
        if (events == null) throw new IllegalStateException("Conference was not fetched");
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        events.forEach(items::commit);
        return new ListExtractor.InfoItemsPage<>(items, null);
    }

    @Override public ListExtractor.InfoItemsPage<InfoItem> getPage(final Page page) {
        return ListExtractor.InfoItemsPage.emptyPage();
    }
}
