// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.services.youtube.ItagItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VideoStream extends Stream {
    public static final String RESOLUTION_UNKNOWN = "";

    /** @deprecated Use {@link #getResolution()} instead. */
    @Deprecated public final String resolution;
    /** @deprecated Use {@link #isVideoOnly()} instead. */
    @Deprecated public final boolean isVideoOnly;

    private int itag = ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE;
    private int bitrate;
    private int initStart;
    private int initEnd;
    private int indexStart;
    private int indexEnd;
    private int width;
    private int height;
    private int fps;
    @Nullable private String quality;
    @Nullable private String codec;
    @Nullable private ItagItem itagItem;

    public static final class Builder {
        private String id;
        private String content;
        private boolean isUrl;
        private DeliveryMethod deliveryMethod = DeliveryMethod.PROGRESSIVE_HTTP;
        @Nullable private MediaFormat mediaFormat;
        @Nullable private String manifestUrl;
        @Nullable private Boolean videoOnly;
        @Nullable private String resolution;
        @Nullable private ItagItem itagItem;

        public Builder setId(@Nonnull final String id) { this.id = id; return this; }
        public Builder setContent(@Nonnull final String content, final boolean isUrl) {
            this.content = content;
            this.isUrl = isUrl;
            return this;
        }
        public Builder setMediaFormat(@Nullable final MediaFormat format) {
            mediaFormat = format;
            return this;
        }
        public Builder setDeliveryMethod(@Nonnull final DeliveryMethod method) {
            deliveryMethod = method;
            return this;
        }
        public Builder setManifestUrl(@Nullable final String url) {
            manifestUrl = url;
            return this;
        }
        public Builder setIsVideoOnly(final boolean videoOnly) {
            this.videoOnly = videoOnly;
            return this;
        }
        public Builder setResolution(@Nonnull final String resolution) {
            this.resolution = resolution;
            return this;
        }
        public Builder setItagItem(@Nullable final ItagItem item) { itagItem = item; return this; }

        @Nonnull public VideoStream build() {
            if (id == null || content == null || deliveryMethod == null || videoOnly == null
                    || resolution == null) {
                throw new IllegalStateException(
                        "Video stream requires an id, content, delivery method, video-only flag, and resolution");
            }
            return new VideoStream(this);
        }
    }

    private VideoStream(final Builder builder) {
        super(builder.id, builder.content, builder.isUrl, builder.mediaFormat,
                builder.deliveryMethod, builder.manifestUrl);
        resolution = builder.resolution;
        isVideoOnly = builder.videoOnly;
        itagItem = builder.itagItem;
        if (itagItem != null) {
            itag = itagItem.id;
            bitrate = itagItem.getBitrate();
            initStart = itagItem.getInitStart();
            initEnd = itagItem.getInitEnd();
            indexStart = itagItem.getIndexStart();
            indexEnd = itagItem.getIndexEnd();
            codec = itagItem.getCodec();
            height = itagItem.getHeight();
            width = itagItem.getWidth();
            quality = itagItem.getQuality();
            fps = itagItem.getFps();
        }
    }

    @Override public boolean equalStats(final Stream other) {
        return super.equalStats(other) && other instanceof VideoStream video
                && resolution.equals(video.resolution) && isVideoOnly == video.isVideoOnly;
    }

    @Nonnull public String getResolution() { return resolution; }
    public boolean isVideoOnly() { return isVideoOnly; }
    public int getItag() { return itag; }
    public int getBitrate() { return bitrate; }
    public int getInitStart() { return initStart; }
    public int getInitEnd() { return initEnd; }
    public int getIndexStart() { return indexStart; }
    public int getIndexEnd() { return indexEnd; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getFps() { return fps; }
    @Nullable public String getQuality() { return quality; }
    @Nullable public String getCodec() { return codec; }
    @Nullable @Override public ItagItem getItagItem() { return itagItem; }
}
