// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/** Token values associated with one player request. */
public final class PoTokenResult {
    @Nonnull
    public final String visitorData;

    @Nonnull
    public final String playerRequestPoToken;

    @Nullable
    public final String streamingDataPoToken;

    public PoTokenResult(@Nonnull final String visitorData,
                         @Nonnull final String playerRequestPoToken,
                         @Nullable final String streamingDataPoToken) {
        this.visitorData = Objects.requireNonNull(visitorData);
        this.playerRequestPoToken = Objects.requireNonNull(playerRequestPoToken);
        this.streamingDataPoToken = streamingDataPoToken;
    }
}
