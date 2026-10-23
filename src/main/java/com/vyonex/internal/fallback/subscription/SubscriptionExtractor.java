// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.subscription;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SubscriptionExtractor {
    public static class InvalidSourceException extends ParsingException {
        public InvalidSourceException() { this(null, null); }
        public InvalidSourceException(@Nullable final String detailMessage) {
            this(detailMessage, null);
        }
        public InvalidSourceException(final Throwable cause) { this(null, cause); }
        public InvalidSourceException(@Nullable final String detailMessage, final Throwable cause) {
            super("Not a valid source" + (detailMessage == null ? ""
                    : " (" + detailMessage + ")"), cause);
        }
    }

    public enum ContentSource { CHANNEL_URL, INPUT_STREAM }

    private final List<ContentSource> supportedSources;
    protected final StreamingService service;

    public SubscriptionExtractor(final StreamingService service,
                                 final List<ContentSource> supportedSources) {
        this.service = service;
        this.supportedSources = Collections.unmodifiableList(new ArrayList<>(supportedSources));
    }

    public List<ContentSource> getSupportedSources() { return supportedSources; }

    @Nullable
    public abstract String getRelatedUrl();

    public List<SubscriptionItem> fromChannelUrl(final String channelUrl)
            throws IOException, ExtractionException {
        throw new UnsupportedOperationException("Service " + service.getServiceInfo().getName()
                + " doesn't support extracting from a channel url");
    }

    public List<SubscriptionItem> fromInputStream(@Nonnull final InputStream contentInputStream)
            throws ExtractionException {
        throw new UnsupportedOperationException("Service " + service.getServiceInfo().getName()
                + " doesn't support extracting from an InputStream");
    }

    public List<SubscriptionItem> fromInputStream(@Nonnull final InputStream contentInputStream,
                                                  @Nonnull final String contentType)
            throws ExtractionException {
        throw new UnsupportedOperationException("Service " + service.getServiceInfo().getName()
                + " doesn't support extracting from an InputStream");
    }
}
