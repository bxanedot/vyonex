// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.linkhandler;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.List;

public class ReadyChannelTabListLinkHandler extends ListLinkHandler {
    public interface ChannelTabExtractorBuilder extends Serializable {
        @Nonnull
        ChannelTabExtractor build(@Nonnull StreamingService service,
                                  @Nonnull ListLinkHandler linkHandler);
    }

    private final ChannelTabExtractorBuilder extractorBuilder;

    public ReadyChannelTabListLinkHandler(
            final String url,
            final String channelId,
            @Nonnull final String channelTab,
            @Nonnull final ChannelTabExtractorBuilder extractorBuilder) {
        super(url, url, channelId, List.of(channelTab), "");
        this.extractorBuilder = extractorBuilder;
    }

    @Nonnull
    public ChannelTabExtractor getChannelTabExtractor(@Nonnull final StreamingService service) {
        return extractorBuilder.build(service, new ListLinkHandler(this));
    }
}
