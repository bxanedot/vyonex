// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.comments;

import com.vyonex.internal.fallback.ListExtractor.InfoItemsPage;
import com.vyonex.internal.fallback.ListInfo;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.utils.ExtractorHelper;

import java.io.IOException;

public final class CommentsInfo extends ListInfo<CommentsInfoItem> {
    private transient CommentsExtractor commentsExtractor;
    private boolean commentsDisabled;
    private int commentsCount;

    private CommentsInfo(final int serviceId,
                         final ListLinkHandler linkHandler,
                         final String name) {
        super(serviceId, linkHandler, name);
    }

    public static CommentsInfo getInfo(final String url) throws IOException, ExtractionException {
        return getInfo(VyonEx.getServiceByUrl(url), url);
    }

    public static CommentsInfo getInfo(final StreamingService service, final String url)
            throws ExtractionException, IOException {
        return getInfo(service.getCommentsExtractor(url));
    }

    public static CommentsInfo getInfo(final CommentsExtractor source)
            throws IOException, ExtractionException {
        if (source == null) return null;

        source.fetchPage();
        final CommentsInfo info = new CommentsInfo(
                source.getServiceId(), source.getLinkHandler(), source.getName());
        info.setCommentsExtractor(source);

        final InfoItemsPage<CommentsInfoItem> firstPage =
                ExtractorHelper.getItemsPageOrLogError(info, source);
        info.setCommentsDisabled(source.isCommentsDisabled());
        info.setRelatedItems(firstPage.getItems());
        try {
            info.setCommentsCount(source.getCommentsCount());
        } catch (final Exception error) {
            info.addError(error);
        }
        info.setNextPage(firstPage.getNextPage());
        return info;
    }

    public static InfoItemsPage<CommentsInfoItem> getMoreItems(
            final CommentsInfo info, final Page page) throws ExtractionException, IOException {
        return getMoreItems(VyonEx.getService(info.getServiceId()), info.getUrl(), page);
    }

    public static InfoItemsPage<CommentsInfoItem> getMoreItems(
            final StreamingService service, final CommentsInfo info, final Page page)
            throws IOException, ExtractionException {
        return getMoreItems(service, info.getUrl(), page);
    }

    public static InfoItemsPage<CommentsInfoItem> getMoreItems(
            final StreamingService service, final String url, final Page page)
            throws IOException, ExtractionException {
        return service.getCommentsExtractor(url).getPage(page);
    }

    public CommentsExtractor getCommentsExtractor() { return commentsExtractor; }
    public void setCommentsExtractor(final CommentsExtractor commentsExtractor) {
        this.commentsExtractor = commentsExtractor;
    }

    public boolean isCommentsDisabled() { return commentsDisabled; }
    public void setCommentsDisabled(final boolean commentsDisabled) {
        this.commentsDisabled = commentsDisabled;
    }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(final int commentsCount) { this.commentsCount = commentsCount; }
}
