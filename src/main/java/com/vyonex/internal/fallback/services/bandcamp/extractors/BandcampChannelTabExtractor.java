// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampDiscographStreamInfoItemExtractor;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class BandcampChannelTabExtractor extends ChannelTabExtractor {
    private final String itemType;
    private JsonArray discography;

    public BandcampChannelTabExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler) {
        super(service, linkHandler);
        final List<String> filters = linkHandler.getContentFilters();
        if (filters.isEmpty()) throw new IllegalArgumentException("Channel tab is missing its type");
        itemType = switch (filters.get(0)) {
            case ChannelTabs.TRACKS -> "track";
            case ChannelTabs.ALBUMS -> "album";
            default -> throw new IllegalArgumentException("Unsupported channel tab: " + filters.get(0));
        };
    }

    public static BandcampChannelTabExtractor fromDiscography(final StreamingService service,
                                                              final ListLinkHandler linkHandler,
                                                              final JsonArray discography) {
        final BandcampChannelTabExtractor extractor =
                new BandcampChannelTabExtractor(service, linkHandler);
        extractor.discography = discography;
        return extractor;
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws ParsingException {
        if (discography == null) {
            discography = BandcampExtractorHelper.getArtistDetails(getId()).getArray("discography");
        }
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        for (final Object value : discography) {
            if (!(value instanceof JsonObject entry)
                    || !itemType.equals(entry.getString("item_type"))) continue;
            if ("track".equals(itemType)) {
                items.commit(new BandcampDiscographStreamInfoItemExtractor(entry, getUrl()));
            } else {
                items.commit(new BandcampAlbumInfoItemExtractor(entry, getUrl()));
            }
        }
        return new InfoItemsPage<>(items, null);
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page) { return null; }
}
