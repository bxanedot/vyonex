// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamInfoItemsCollectorContractTest {
    private static final Image IMAGE = new Image("https://img.example/cover.jpg", 90, 160,
            Image.ResolutionLevel.LOW);

    @Test
    void buildsAStreamItemFromAvailableExtractorFields() throws Exception {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(42);
        final StreamInfoItem item = collector.extract(new ExampleExtractor(false, false));

        assertEquals(42, item.getServiceId());
        assertEquals(StreamType.VIDEO_STREAM, item.getStreamType());
        assertEquals("https://media.example/watch/clip", item.getUrl());
        assertEquals("A clip", item.getName());
        assertEquals("Creator", item.getUploaderName());
        assertEquals(1234, item.getViewCount());
        assertEquals(77, item.getDuration());
        assertEquals("https://media.example/channel/creator", item.getUploaderUrl());
        assertEquals(List.of(IMAGE), item.getUploaderAvatars());
        assertEquals("yesterday", item.getTextualUploadDate());
        assertEquals(Instant.parse("2026-01-02T03:04:05Z"), item.getUploadDate().getInstant());
        assertTrue(item.isUploaderVerified());
        assertTrue(item.isShortFormContent());
        assertEquals("Description", item.getShortDescription());
        assertEquals(ContentAvailability.MEMBERSHIP, item.getContentAvailability());
        assertEquals(List.of(IMAGE), item.getThumbnails());
        assertTrue(collector.getErrors().isEmpty());
    }

    @Test
    void skipsAdsAndKeepsItemsWhenAnOptionalFieldFails() {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(1);
        collector.commit(new ExampleExtractor(true, false));
        collector.commit(new ExampleExtractor(false, true));

        assertEquals(1, collector.getItems().size());
        assertEquals(-1, collector.getItems().get(0).getDuration());
        assertEquals(1, collector.getErrors().size());
    }

    private static final class ExampleExtractor implements StreamInfoItemExtractor {
        private final boolean ad;
        private final boolean brokenDuration;

        private ExampleExtractor(final boolean ad, final boolean brokenDuration) {
            this.ad = ad;
            this.brokenDuration = brokenDuration;
        }

        @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }
        @Override public boolean isAd() { return ad; }
        @Override public String getUrl() { return "https://media.example/watch/clip"; }
        @Override public String getName() { return "A clip"; }
        @Override public long getDuration() throws ParsingException {
            if (brokenDuration) throw new ParsingException("duration unavailable");
            return 77;
        }
        @Override public long getViewCount() { return 1234; }
        @Override public String getUploaderName() { return "Creator"; }
        @Override public String getUploaderUrl() { return "https://media.example/channel/creator"; }
        @Override public List<Image> getUploaderAvatars() { return List.of(IMAGE); }
        @Override public boolean isUploaderVerified() { return true; }
        @Override public String getTextualUploadDate() { return "yesterday"; }
        @Override public DateWrapper getUploadDate() {
            return new DateWrapper(Instant.parse("2026-01-02T03:04:05Z"));
        }
        @Override public String getShortDescription() { return "Description"; }
        @Override public boolean isShortFormContent() { return true; }
        @Override public ContentAvailability getContentAvailability() {
            return ContentAvailability.MEMBERSHIP;
        }
        @Override public List<Image> getThumbnails() { return List.of(IMAGE); }
    }
}
