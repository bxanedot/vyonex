// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.comments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class CommentsInfoContractTest {
    @Test
    void unsupportedCommentsExtractionReturnsNull() throws Exception {
        assertNull(CommentsInfo.getInfo((CommentsExtractor) null));
    }
}
