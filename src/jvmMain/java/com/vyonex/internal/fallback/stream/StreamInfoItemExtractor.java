// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface StreamInfoItemExtractor extends InfoItemExtractor {
    StreamType getStreamType() throws ParsingException;
    boolean isAd() throws ParsingException;
    long getDuration() throws ParsingException;
    long getViewCount() throws ParsingException;
    String getUploaderName() throws ParsingException;
    String getUploaderUrl() throws ParsingException;

    @Nonnull default List<Image> getUploaderAvatars() throws ParsingException { return List.of(); }

    boolean isUploaderVerified() throws ParsingException;
    @Nullable String getTextualUploadDate() throws ParsingException;
    @Nullable DateWrapper getUploadDate() throws ParsingException;
    @Nullable default String getShortDescription() throws ParsingException { return null; }
    default boolean isShortFormContent() throws ParsingException { return false; }

    @Nonnull default ContentAvailability getContentAvailability() throws ParsingException {
        return ContentAvailability.UNKNOWN;
    }
}
