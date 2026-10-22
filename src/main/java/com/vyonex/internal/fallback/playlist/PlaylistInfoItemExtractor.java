// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.playlist;

import com.vyonex.internal.fallback.InfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.Description;

import javax.annotation.Nonnull;

public interface PlaylistInfoItemExtractor extends InfoItemExtractor {
    String getUploaderName() throws ParsingException;
    String getUploaderUrl() throws ParsingException;
    boolean isUploaderVerified() throws ParsingException;
    long getStreamCount() throws ParsingException;

    @Nonnull
    default Description getDescription() throws ParsingException {
        return Description.EMPTY_DESCRIPTION;
    }

    @Nonnull
    default PlaylistInfo.PlaylistType getPlaylistType() throws ParsingException {
        return PlaylistInfo.PlaylistType.NORMAL;
    }
}
