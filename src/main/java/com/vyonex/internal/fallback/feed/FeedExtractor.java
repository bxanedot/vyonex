// bxane
package com.vyonex.internal.fallback.feed;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.stream.StreamInfoItem;

/**
 * This class helps to extract items from lightweight feeds that the services may provide.
 * <p>
 * YouTube is an example of a service that has this alternative available.
 */
public abstract class FeedExtractor extends ListExtractor<StreamInfoItem> {
    public FeedExtractor(final StreamingService service, final ListLinkHandler listLinkHandler) {
        super(service, listLinkHandler);
    }
}
