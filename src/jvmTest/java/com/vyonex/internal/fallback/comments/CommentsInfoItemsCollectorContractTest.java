// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.comments;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.Description;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommentsInfoItemsCollectorContractTest {
    @Test
    void optionalFieldFailureIsRecordedWithoutDroppingTheComment() {
        final CommentsInfoItemsCollector collector = new CommentsInfoItemsCollector(4);
        collector.commit(new CommentsInfoItemExtractor() {
            @Override
            public String getName() { return "comment"; }

            @Override
            public String getUrl() { return "https://media.example/comments/1"; }

            @Override
            public List<Image> getThumbnails() { return List.of(); }

            @Override
            public Description getCommentText() throws ParsingException {
                throw new ParsingException("text unavailable");
            }

            @Override
            public int getLikeCount() { return 7; }
        });

        assertEquals(1, collector.getCommentsInfoItemList().size());
        assertEquals(Description.EMPTY_DESCRIPTION,
                collector.getCommentsInfoItemList().get(0).getCommentText());
        assertEquals(7, collector.getCommentsInfoItemList().get(0).getLikeCount());
        assertEquals("text unavailable", collector.getErrors().get(0).getMessage());
    }
}
