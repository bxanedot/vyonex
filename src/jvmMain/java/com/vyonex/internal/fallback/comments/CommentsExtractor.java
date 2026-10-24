// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.comments;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

import javax.annotation.Nonnull;

public abstract class CommentsExtractor extends ListExtractor<CommentsInfoItem> {
    public CommentsExtractor(final StreamingService service,
                             final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    public boolean isCommentsDisabled() throws ExtractionException { return false; }
    public int getCommentsCount() throws ExtractionException { return -1; }

    @Nonnull
    @Override
    public String getName() throws ParsingException { return "Comments"; }
}
