// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.kiosk;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

import javax.annotation.Nonnull;

public abstract class KioskExtractor<T extends InfoItem> extends ListExtractor<T> {
    private final String id;

    public KioskExtractor(final StreamingService service, final ListLinkHandler linkHandler,
                          final String kioskId) {
        super(service, linkHandler);
        id = kioskId;
    }

    @Nonnull
    @Override
    public String getId() { return id; }

    @Nonnull
    @Override
    public abstract String getName() throws ParsingException;
}
