// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.channel;

import com.vyonex.internal.fallback.InfoItemsCollector;
import com.vyonex.internal.fallback.exceptions.ParsingException;

public final class ChannelInfoItemsCollector
        extends InfoItemsCollector<ChannelInfoItem, ChannelInfoItemExtractor> {
    public ChannelInfoItemsCollector(final int serviceId) {
        super(serviceId);
    }

    @Override
    public ChannelInfoItem extract(final ChannelInfoItemExtractor extractor)
            throws ParsingException {
        final ChannelInfoItem item = new ChannelInfoItem(getServiceId(), extractor.getUrl(),
                extractor.getName());
        try { item.setSubscriberCount(extractor.getSubscriberCount()); }
        catch (final Exception e) { addError(e); }
        try { item.setStreamCount(extractor.getStreamCount()); }
        catch (final Exception e) { addError(e); }
        try { item.setThumbnails(extractor.getThumbnails()); }
        catch (final Exception e) { addError(e); }
        try { item.setDescription(extractor.getDescription()); }
        catch (final Exception e) { addError(e); }
        try { item.setVerified(extractor.isVerified()); }
        catch (final Exception e) { addError(e); }
        return item;
    }
}
