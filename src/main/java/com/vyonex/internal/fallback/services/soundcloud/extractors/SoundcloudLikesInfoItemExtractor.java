// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.json.JsonObject;

public final class SoundcloudLikesInfoItemExtractor extends SoundcloudStreamInfoItemExtractor {
    public SoundcloudLikesInfoItemExtractor(final JsonObject like) {
        super(like.getObject("track"));
    }
}
