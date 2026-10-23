// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.subscription;

import com.vyonex.internal.fallback.services.soundcloud.SoundcloudService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubscriptionExtractorContractTest {
    @Test
    void supportedSourcesAreAnImmutableSnapshot() {
        final List<SubscriptionExtractor.ContentSource> sources =
                new ArrayList<>(List.of(SubscriptionExtractor.ContentSource.CHANNEL_URL));
        final SubscriptionExtractor extractor = new SubscriptionExtractor(
                new SoundcloudService(31), sources) {
            @Override
            public String getRelatedUrl() { return null; }
        };

        sources.add(SubscriptionExtractor.ContentSource.INPUT_STREAM);

        assertEquals(List.of(SubscriptionExtractor.ContentSource.CHANNEL_URL),
                extractor.getSupportedSources());
        assertThrows(UnsupportedOperationException.class,
                () -> extractor.getSupportedSources().add(
                        SubscriptionExtractor.ContentSource.INPUT_STREAM));
    }
}
