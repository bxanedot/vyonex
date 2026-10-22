// bxane
package com.vyonex.internal.fallback.channel.tabs;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

import javax.annotation.Nonnull;

/**
 * A {@link ListExtractor} of {@link InfoItem}s for tabs of channels.
 */
public abstract class ChannelTabExtractor extends ListExtractor<InfoItem> {

    protected ChannelTabExtractor(@Nonnull final StreamingService service,
                                  @Nonnull final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nonnull
    @Override
    public String getName() {
        return getLinkHandler().getContentFilters().get(0);
    }
}
