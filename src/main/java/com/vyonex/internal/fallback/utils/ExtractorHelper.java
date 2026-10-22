// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import com.vyonex.internal.fallback.Info;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.InfoItemsCollector;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.ListExtractor.InfoItemsPage;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamInfo;

import java.util.Collections;
import java.util.List;

public final class ExtractorHelper {
    private ExtractorHelper() { }

    public static <T extends InfoItem> InfoItemsPage<T> getItemsPageOrLogError(
            final Info info, final ListExtractor<T> extractor) {
        try {
            final InfoItemsPage<T> page = extractor.getInitialPage();
            info.addAllErrors(page.getErrors());
            return page;
        } catch (final Exception error) {
            info.addError(error);
            return InfoItemsPage.emptyPage();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<InfoItem> getRelatedItemsOrLogError(final StreamInfo info,
                                                           final StreamExtractor extractor) {
        try {
            final InfoItemsCollector<? extends InfoItem, ?> collector = extractor.getRelatedItems();
            if (collector == null) return Collections.emptyList();
            info.addAllErrors(collector.getErrors());
            return (List<InfoItem>) collector.getItems();
        } catch (final Exception error) {
            info.addError(error);
            return Collections.emptyList();
        }
    }

    @Deprecated
    public static List<InfoItem> getRelatedVideosOrLogError(final StreamInfo info,
                                                            final StreamExtractor extractor) {
        return getRelatedItemsOrLogError(info, extractor);
    }
}
