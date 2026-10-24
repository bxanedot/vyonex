// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.services.youtube.ItagItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public abstract class Stream implements Serializable {
    public static final int FORMAT_ID_UNKNOWN = -1;
    public static final String ID_UNKNOWN = " ";
    public static final int ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE = -1;

    private final String id;
    @Nullable private final MediaFormat mediaFormat;
    private final String content;
    private final boolean isUrl;
    private final DeliveryMethod deliveryMethod;
    @Nullable private final String manifestUrl;

    public Stream(final String id, final String content, final boolean isUrl,
                  @Nullable final MediaFormat format, final DeliveryMethod deliveryMethod,
                  @Nullable final String manifestUrl) {
        this.id = id;
        this.content = content;
        this.isUrl = isUrl;
        this.mediaFormat = format;
        this.deliveryMethod = deliveryMethod;
        this.manifestUrl = manifestUrl;
    }

    public static boolean containSimilarStream(@Nonnull final Stream stream,
                                               @Nullable final List<? extends Stream> streams) {
        if (streams == null || streams.isEmpty()) return false;
        for (final Stream candidate : streams) {
            if (stream.equalStats(candidate)) return true;
        }
        return false;
    }

    public boolean equalStats(@Nullable final Stream other) {
        if (other == null || deliveryMethod != other.deliveryMethod || isUrl != other.isUrl) {
            return false;
        }
        if (mediaFormat == null || other.mediaFormat == null) {
            return Objects.equals(id, other.id);
        }
        return mediaFormat.id == other.mediaFormat.id;
    }

    public String getId() { return id; }
    @Deprecated @Nullable public String getUrl() { return isUrl ? content : null; }
    @Nonnull public String getContent() { return content; }
    public boolean isUrl() { return isUrl; }
    @Nullable public MediaFormat getFormat() { return mediaFormat; }
    public int getFormatId() { return mediaFormat == null ? FORMAT_ID_UNKNOWN : mediaFormat.id; }
    @Nonnull public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    @Nullable public String getManifestUrl() { return manifestUrl; }
    @Nullable public abstract ItagItem getItagItem();
}
