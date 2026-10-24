// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.kiosk;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.ListInfo;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.utils.ExtractorHelper;

import java.io.IOException;

public final class KioskInfo extends ListInfo<StreamInfoItem> {
    private KioskInfo(final int serviceId, final ListLinkHandler linkHandler, final String name) {
        super(serviceId, linkHandler, name);
    }

    public static ListExtractor.InfoItemsPage<StreamInfoItem> getMoreItems(
            final StreamingService service, final String url, final Page page)
            throws IOException, ExtractionException {
        return service.getKioskList().getExtractorByUrl(url, page).getPage(page);
    }

    public static KioskInfo getInfo(final String url) throws IOException, ExtractionException {
        return getInfo(VyonEx.getServiceByUrl(url), url);
    }

    public static KioskInfo getInfo(final StreamingService service, final String url)
            throws IOException, ExtractionException {
        final KioskExtractor extractor = service.getKioskList().getExtractorByUrl(url, null);
        extractor.fetchPage();
        return getInfo(extractor);
    }

    // shortcut: results stay stream-typed for API compatibility; broaden when callers support mixed items.
    public static KioskInfo getInfo(final KioskExtractor extractor) throws ExtractionException {
        final KioskInfo info = new KioskInfo(extractor.getServiceId(), extractor.getLinkHandler(),
                extractor.getName());
        final ListExtractor.InfoItemsPage<StreamInfoItem> itemsPage
                = ExtractorHelper.getItemsPageOrLogError(info, extractor);
        info.setRelatedItems(itemsPage.getItems());
        info.setNextPage(itemsPage.getNextPage());
        return info;
    }
}
