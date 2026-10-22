// bxane
package com.vyonex.internal.fallback.comments;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

import javax.annotation.Nonnull;

public abstract class CommentsExtractor extends ListExtractor<CommentsInfoItem> {

    public CommentsExtractor(final StreamingService service, final ListLinkHandler uiHandler) {
        super(service, uiHandler);
    }

    /**
     * @apiNote Warning: This method is experimental and may get removed in a future release.
     * @return <code>true</code> if the comments are disabled otherwise <code>false</code> (default)
     */
    public boolean isCommentsDisabled() throws ExtractionException {
        return false;
    }

    /**
     * @return the total number of comments
     */
    public int getCommentsCount() throws ExtractionException {
        return -1;
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return "Comments";
    }
}
