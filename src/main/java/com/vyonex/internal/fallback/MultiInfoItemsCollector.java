// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.channel.ChannelInfoItemsCollector;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemsCollector;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiInfoItemsCollector extends InfoItemsCollector<InfoItem, InfoItemExtractor> {
    private final StreamInfoItemsCollector streams;
    private final ChannelInfoItemsCollector channels;
    private final PlaylistInfoItemsCollector playlists;

    public MultiInfoItemsCollector(final int serviceId) {
        super(serviceId);
        streams = new StreamInfoItemsCollector(serviceId);
        channels = new ChannelInfoItemsCollector(serviceId);
        playlists = new PlaylistInfoItemsCollector(serviceId);
    }

    @Override
    public List<Throwable> getErrors() {
        final List<Throwable> result = new ArrayList<>(super.getErrors());
        result.addAll(streams.getErrors());
        result.addAll(channels.getErrors());
        result.addAll(playlists.getErrors());
        return Collections.unmodifiableList(result);
    }

    @Override
    public void reset() {
        super.reset();
        streams.reset();
        channels.reset();
        playlists.reset();
    }

    @Override
    public InfoItem extract(final InfoItemExtractor extractor) throws ParsingException {
        if (extractor instanceof StreamInfoItemExtractor) {
            return streams.extract((StreamInfoItemExtractor) extractor);
        }
        if (extractor instanceof ChannelInfoItemExtractor) {
            return channels.extract((ChannelInfoItemExtractor) extractor);
        }
        if (extractor instanceof PlaylistInfoItemExtractor) {
            return playlists.extract((PlaylistInfoItemExtractor) extractor);
        }
        throw new IllegalArgumentException("Invalid extractor type: " + extractor);
    }
}
