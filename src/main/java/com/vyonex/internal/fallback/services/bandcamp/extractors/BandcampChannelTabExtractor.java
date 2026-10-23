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
import java.util.ArrayList;
import java.util.List;

public class BandcampChannelTabExtractor extends ChannelTabExtractor {
    private final String itemType;
    private List<JsonObject> items;

    public BandcampChannelTabExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler) {
        super(service, linkHandler);
        final List<String> filters = linkHandler.getContentFilters();
        if (filters.isEmpty()) throw new IllegalArgumentException("Channel tab is missing its type");
        itemType = switch (filters.get(0)) {
            case ChannelTabs.TRACKS -> "track";
            case ChannelTabs.ALBUMS -> "album";
            default -> throw new IllegalArgumentException(
                    "Unsupported channel tab: " + filters.get(0));
        };
    }

    public static BandcampChannelTabExtractor fromDiscography(final StreamingService service,
                                                              final ListLinkHandler linkHandler,
                                                              final JsonArray discography) {
        final BandcampChannelTabExtractor extractor =
                new BandcampChannelTabExtractor(service, linkHandler);
        extractor.items = selectItems(discography, extractor.itemType);
        return extractor;
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws ParsingException {
        if (items == null) {
            final JsonArray discography = BandcampExtractorHelper
                    .getArtistDetails(downloader, getId()).getArray("discography");
            items = selectItems(discography, itemType);
        }
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        if (items == null) throw new ParsingException("Bandcamp tab has not been fetched");
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        for (final JsonObject item : items) {
            collector.commit("track".equals(itemType)
                    ? new BandcampDiscographStreamInfoItemExtractor(item, getUrl())
                    : new BandcampAlbumInfoItemExtractor(item, getUrl()));
        }
        return new InfoItemsPage<>(collector, null);
    }

    @Override public InfoItemsPage<InfoItem> getPage(final Page page) { return null; }

    private static List<JsonObject> selectItems(final JsonArray discography, final String type) {
        if (discography == null) return List.of();
        final List<JsonObject> selected = new ArrayList<>();
        for (final Object value : discography) {
            if (value instanceof JsonObject item && type.equals(item.getString("item_type"))) {
                selected.add(item);
            }
        }
        return List.copyOf(selected);
    }
}
