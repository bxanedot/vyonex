// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.playlist;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistInfoItemsCollectorContractTest {
    @Test
    void optionalFieldFailureDoesNotDiscardThePlaylistItem() {
        final PlaylistInfoItemsCollector collector = new PlaylistInfoItemsCollector(8);
        collector.commit(new PlaylistInfoItemExtractor() {
            @Override
            public String getName() { return "playlist"; }

            @Override
            public String getUrl() { return "https://media.example/playlist/1"; }

            @Override
            public List<Image> getThumbnails() { return List.of(); }

            @Override
            public String getUploaderName() throws ParsingException {
                throw new ParsingException("uploader unavailable");
            }

            @Override
            public String getUploaderUrl() { return "https://media.example/uploader"; }

            @Override
            public boolean isUploaderVerified() { return true; }

            @Override
            public long getStreamCount() { return 23; }
        });

        assertEquals(1, collector.getItems().size());
        final PlaylistInfoItem item = collector.getItems().get(0);
        assertEquals(23, item.getStreamCount());
        assertEquals("https://media.example/uploader", item.getUploaderUrl());
        assertTrue(item.isUploaderVerified());
        assertEquals("uploader unavailable", collector.getErrors().get(0).getMessage());
    }
}
