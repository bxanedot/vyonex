// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import javax.annotation.Nullable;

/** Supplies optional proof-of-origin tokens for player requests. Implementations must be thread-safe. */
public interface PoTokenProvider {
    /** Returns tokens for the desktop web client, or {@code null} when unavailable. */
    @Nullable
    PoTokenResult getWebClientPoToken(String videoId);

    /** Returns tokens for the embedded web client, or {@code null} when unavailable. */
    @Nullable
    PoTokenResult getWebEmbedClientPoToken(String videoId);

    /** Returns tokens for the Android client, or {@code null} when unavailable. */
    @Nullable
    PoTokenResult getAndroidClientPoToken(String videoId);

    /** Returns tokens for the iOS client, or {@code null} when unavailable. */
    @Nullable
    PoTokenResult getIosClientPoToken(String videoId);
}
