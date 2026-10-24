// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.channel;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelInfoItemsCollectorContractTest {
    private static final Image IMAGE = new Image("https://img.example/channel.jpg", 90, 90,
            Image.ResolutionLevel.LOW);

    @Test
    void collectsChannelMetadataAndThumbnails() throws Exception {
        final ChannelInfoItemsCollector collector = new ChannelInfoItemsCollector(9);
        final ChannelInfoItem item = collector.extract(new ExampleExtractor(false));

        assertEquals(InfoItem.InfoType.CHANNEL, item.getInfoType());
        assertEquals(9, item.getServiceId());
        assertEquals("https://media.example/channel/creator", item.getUrl());
        assertEquals("Creator", item.getName());
        assertEquals("A music channel", item.getDescription());
        assertEquals(1200, item.getSubscriberCount());
        assertEquals(35, item.getStreamCount());
        assertTrue(item.isVerified());
        assertEquals(List.of(IMAGE), item.getThumbnails());
        assertTrue(collector.getErrors().isEmpty());
    }

    @Test
    void keepsRequiredChannelDataWhenAnOptionalFieldFails() throws Exception {
        final ChannelInfoItemsCollector collector = new ChannelInfoItemsCollector(9);
        final ChannelInfoItem item = collector.extract(new ExampleExtractor(true));

        assertEquals("Creator", item.getName());
        assertEquals(1200, item.getSubscriberCount());
        assertEquals(1, collector.getErrors().size());
        assertEquals("description unavailable", collector.getErrors().get(0).getMessage());
    }

    private static final class ExampleExtractor implements ChannelInfoItemExtractor {
        private final boolean failDescription;
        private ExampleExtractor(final boolean failDescription) {
            this.failDescription = failDescription;
        }
        @Override public String getName() { return "Creator"; }
        @Override public String getUrl() { return "https://media.example/channel/creator"; }
        @Override public List<Image> getThumbnails() { return List.of(IMAGE); }
        @Override public String getDescription() throws ParsingException {
            if (failDescription) throw new ParsingException("description unavailable");
            return "A music channel";
        }
        @Override public long getSubscriberCount() { return 1200; }
        @Override public long getStreamCount() { return 35; }
        @Override public boolean isVerified() { return true; }
    }
}
