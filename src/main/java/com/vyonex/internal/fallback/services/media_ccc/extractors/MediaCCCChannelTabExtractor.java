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
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class MediaCCCChannelTabExtractor extends ChannelTabExtractor {
    @Nullable private List<MediaCCCStreamInfoItemExtractor> events;

    public MediaCCCChannelTabExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler,
                                       @Nullable final JsonObject conference) {
        super(service, linkHandler);
        if (conference != null) events = eventItems(conference.getArray("events"));
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws ExtractionException, IOException {
        if (events == null) {
            final JsonObject conference = MediaCCCConferenceExtractor.fetchConferenceData(
                    downloader, getId());
            events = eventItems(conference.getArray("events"));
        }
    }

    private static List<MediaCCCStreamInfoItemExtractor> eventItems(final JsonArray source) {
        return source.streamAsJsonObjects().map(MediaCCCStreamInfoItemExtractor::new).toList();
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
