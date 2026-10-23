// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.feed;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.ListInfo;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.utils.ExtractorHelper;

import java.io.IOException;
import java.util.List;

public class FeedInfo extends ListInfo<StreamInfoItem> {
    public FeedInfo(final int serviceId,
                    final String id,
                    final String url,
                    final String originalUrl,
                    final String name,
                    final List<String> contentFilters,
                    final String sortFilter) {
        super(serviceId, id, url, originalUrl, name, contentFilters, sortFilter);
    }

    public static FeedInfo getInfo(final String url) throws IOException, ExtractionException {
        return getInfo(VyonEx.getServiceByUrl(url), url);
    }

    public static FeedInfo getInfo(final StreamingService service, final String url)
            throws IOException, ExtractionException {
        final FeedExtractor extractor = service.getFeedExtractor(url);
        if (extractor == null) {
            throw new IllegalArgumentException("Service \"" + service.getServiceInfo().getName()
                    + "\" doesn't support FeedExtractor.");
        }
        extractor.fetchPage();
        return getInfo(extractor);
    }

    public static FeedInfo getInfo(final FeedExtractor extractor)
            throws IOException, ExtractionException {
        extractor.fetchPage();
        final FeedInfo info = new FeedInfo(extractor.getServiceId(), extractor.getId(),
                extractor.getUrl(), extractor.getOriginalUrl(), extractor.getName(), null, null);
        final ListExtractor.InfoItemsPage<StreamInfoItem> firstPage =
                ExtractorHelper.getItemsPageOrLogError(info, extractor);
        info.setRelatedItems(firstPage.getItems());
        info.setNextPage(firstPage.getNextPage());
        return info;
    }
}
