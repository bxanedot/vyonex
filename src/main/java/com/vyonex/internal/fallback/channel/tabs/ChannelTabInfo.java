// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.channel.tabs;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.ListInfo;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.utils.ExtractorHelper;

import javax.annotation.Nonnull;
import java.io.IOException;

public class ChannelTabInfo extends ListInfo<InfoItem> {
    public ChannelTabInfo(final int serviceId, @Nonnull final ListLinkHandler linkHandler) {
        super(serviceId, linkHandler, linkHandler.getContentFilters().get(0));
    }

    @Nonnull
    public static ChannelTabInfo getInfo(@Nonnull final StreamingService service,
                                         @Nonnull final ListLinkHandler linkHandler)
            throws ExtractionException, IOException {
        final ChannelTabExtractor extractor = service.getChannelTabExtractor(linkHandler);
        extractor.fetchPage();
        return getInfo(extractor);
    }

    @Nonnull
    public static ChannelTabInfo getInfo(@Nonnull final ChannelTabExtractor extractor) {
        final ChannelTabInfo info = new ChannelTabInfo(
                extractor.getServiceId(), extractor.getLinkHandler());
        try {
            info.setOriginalUrl(extractor.getOriginalUrl());
        } catch (final Exception error) {
            info.addError(error);
        }

        final ListExtractor.InfoItemsPage<InfoItem> firstPage =
                ExtractorHelper.getItemsPageOrLogError(info, extractor);
        info.setRelatedItems(firstPage.getItems());
        info.setNextPage(firstPage.getNextPage());
        return info;
    }

    public static ListExtractor.InfoItemsPage<InfoItem> getMoreItems(
            @Nonnull final StreamingService service,
            @Nonnull final ListLinkHandler linkHandler,
            @Nonnull final Page page) throws ExtractionException, IOException {
        return service.getChannelTabExtractor(linkHandler).getPage(page);
    }
}
