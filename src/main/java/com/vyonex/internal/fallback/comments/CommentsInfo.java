// bxane
package com.vyonex.internal.fallback.comments;

import com.vyonex.internal.fallback.ListExtractor.InfoItemsPage;
import com.vyonex.internal.fallback.ListInfo;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.utils.ExtractorHelper;

import java.io.IOException;

public final class CommentsInfo extends ListInfo<CommentsInfoItem> {

    private CommentsInfo(
            final int serviceId,
            final ListLinkHandler listUrlIdHandler,
            final String name) {
        super(serviceId, listUrlIdHandler, name);
    }

    public static CommentsInfo getInfo(final String url) throws IOException, ExtractionException {
        return getInfo(VyonEx.getServiceByUrl(url), url);
    }

    public static CommentsInfo getInfo(final StreamingService service, final String url)
            throws ExtractionException, IOException {
        return getInfo(service.getCommentsExtractor(url));
    }

    public static CommentsInfo getInfo(final CommentsExtractor commentsExtractor)
            throws IOException, ExtractionException {
        // for services which do not have a comments extractor
        if (commentsExtractor == null) {
            return null;
        }

        commentsExtractor.fetchPage();

        final String name = commentsExtractor.getName();
        final int serviceId = commentsExtractor.getServiceId();
        final ListLinkHandler listUrlIdHandler = commentsExtractor.getLinkHandler();

        final CommentsInfo commentsInfo = new CommentsInfo(serviceId, listUrlIdHandler, name);
        commentsInfo.setCommentsExtractor(commentsExtractor);
        final InfoItemsPage<CommentsInfoItem> initialCommentsPage =
                ExtractorHelper.getItemsPageOrLogError(commentsInfo, commentsExtractor);
        commentsInfo.setCommentsDisabled(commentsExtractor.isCommentsDisabled());
        commentsInfo.setRelatedItems(initialCommentsPage.getItems());
        try {
            commentsInfo.setCommentsCount(commentsExtractor.getCommentsCount());
        } catch (final Exception e) {
            commentsInfo.addError(e);
        }
        commentsInfo.setNextPage(initialCommentsPage.getNextPage());

        return commentsInfo;
    }

    public static InfoItemsPage<CommentsInfoItem> getMoreItems(
            final CommentsInfo commentsInfo,
            final Page page) throws ExtractionException, IOException {
        return getMoreItems(VyonEx.getService(commentsInfo.getServiceId()), commentsInfo.getUrl(),
                page);
    }

    public static InfoItemsPage<CommentsInfoItem> getMoreItems(
            final StreamingService service,
            final CommentsInfo commentsInfo,
            final Page page) throws IOException, ExtractionException {
        return getMoreItems(service, commentsInfo.getUrl(), page);
    }

    public static InfoItemsPage<CommentsInfoItem> getMoreItems(
            final StreamingService service,
            final String url,
            final Page page) throws IOException, ExtractionException {
        return service.getCommentsExtractor(url).getPage(page);
    }

    private transient CommentsExtractor commentsExtractor;
    private boolean commentsDisabled = false;
    private int commentsCount;

    public CommentsExtractor getCommentsExtractor() {
        return commentsExtractor;
    }

    public void setCommentsExtractor(final CommentsExtractor commentsExtractor) {
        this.commentsExtractor = commentsExtractor;
    }

    /**
     * @return {@code true} if the comments are disabled otherwise {@code false} (default)
     * @see CommentsExtractor#isCommentsDisabled()
     */
    public boolean isCommentsDisabled() {
        return commentsDisabled;
    }

    /**
     * @param commentsDisabled {@code true} if the comments are disabled otherwise {@code false}
     */
    public void setCommentsDisabled(final boolean commentsDisabled) {
        this.commentsDisabled = commentsDisabled;
    }

    /**
     * Returns the total number of comments.
     *
     * @return the total number of comments
     */
    public int getCommentsCount() {
        return commentsCount;
    }

    /**
     * Sets the total number of comments.
     *
     * @param commentsCount the commentsCount to set.
     */
    public void setCommentsCount(final int commentsCount) {
        this.commentsCount = commentsCount;
    }
}
