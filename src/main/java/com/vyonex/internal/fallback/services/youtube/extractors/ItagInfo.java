// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.services.youtube.ItagItem;

import javax.annotation.Nonnull;
import java.io.Serializable;

final class ItagInfo implements Serializable {
    @Nonnull private final String content;
    @Nonnull private final ItagItem itagItem;
    private boolean url;

    ItagInfo(@Nonnull final String content, @Nonnull final ItagItem itagItem) {
        this.content = content;
        this.itagItem = itagItem;
    }

    void setIsUrl(final boolean value) {
        url = value;
    }

    @Nonnull String getContent() { return content; }
    @Nonnull ItagItem getItagItem() { return itagItem; }
    boolean getIsUrl() { return url; }
}
