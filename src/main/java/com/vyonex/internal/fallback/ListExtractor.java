// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class ListExtractor<R extends InfoItem> extends Extractor {
    public static final long ITEM_COUNT_UNKNOWN = -1;
    public static final long ITEM_COUNT_INFINITE = -2;
    public static final long ITEM_COUNT_MORE_THAN_100 = -3;

    public ListExtractor(final StreamingService service, final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nonnull public abstract InfoItemsPage<R> getInitialPage()
            throws IOException, ExtractionException;

    public abstract InfoItemsPage<R> getPage(Page page) throws IOException, ExtractionException;

    @Nonnull @Override public ListLinkHandler getLinkHandler() {
        return (ListLinkHandler) super.getLinkHandler();
    }

    public static class InfoItemsPage<T extends InfoItem> {
        private static final InfoItemsPage<InfoItem> EMPTY = new InfoItemsPage<>(
                Collections.emptyList(), null, Collections.emptyList());

        private final List<T> itemsList;
        @Nullable private final Page nextPage;
        private final List<Throwable> errors;

        @SuppressWarnings("unchecked")
        public static <T extends InfoItem> InfoItemsPage<T> emptyPage() {
            return (InfoItemsPage<T>) EMPTY;
        }

        public InfoItemsPage(final InfoItemsCollector<T, ?> collector,
                             @Nullable final Page nextPage) {
            this(collector.getItems(), nextPage, collector.getErrors());
        }

        public InfoItemsPage(final List<T> itemsList, @Nullable final Page nextPage,
                             final List<Throwable> errors) {
            this.itemsList = itemsList;
            this.nextPage = nextPage;
            this.errors = errors;
        }

        public boolean hasNextPage() { return Page.isValid(nextPage); }
        public List<T> getItems() { return itemsList; }
        @Nullable public Page getNextPage() { return nextPage; }
        public List<Throwable> getErrors() { return errors; }
    }
}
