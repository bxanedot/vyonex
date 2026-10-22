// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;

import javax.annotation.Nullable;

public abstract class BandcampStreamInfoItemExtractor implements StreamInfoItemExtractor {
    private final String uploaderUrl;

    protected BandcampStreamInfoItemExtractor(final String uploaderUrl) {
        this.uploaderUrl = uploaderUrl;
    }

    @Override
    public StreamType getStreamType() { return StreamType.AUDIO_STREAM; }

    @Override
    public long getViewCount() { return -1; }

    @Override
    public String getUploaderUrl() { return uploaderUrl; }

    @Nullable
    @Override
    public String getTextualUploadDate() { return null; }

    @Nullable
    @Override
    public DateWrapper getUploadDate() { return null; }

    @Override
    public boolean isUploaderVerified() throws ParsingException { return false; }

    @Override
    public boolean isAd() { return false; }
}
