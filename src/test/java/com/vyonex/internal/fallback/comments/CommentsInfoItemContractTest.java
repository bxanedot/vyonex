// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.comments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommentsInfoItemContractTest {
    @Test
    void unavailableCountsStartAtTheirDocumentedSentinels() {
        final CommentsInfoItem item = new CommentsInfoItem(1, "url", "comment");

        assertEquals(CommentsInfoItem.NO_LIKE_COUNT, item.getLikeCount());
        assertEquals(CommentsInfoItem.NO_STREAM_POSITION, item.getStreamPosition());
        assertEquals(CommentsInfoItem.UNKNOWN_REPLY_COUNT, item.getReplyCount());
    }
}
