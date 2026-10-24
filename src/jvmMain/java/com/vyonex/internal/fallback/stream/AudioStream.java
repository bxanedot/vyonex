// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.services.youtube.ItagItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

public final class AudioStream extends Stream {
    public static final int UNKNOWN_BITRATE = -1;

    private final int averageBitrate;
    private int itag = ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE;
    private int bitrate;
    private int initStart;
    private int initEnd;
    private int indexStart;
    private int indexEnd;
    @Nullable private String quality;
    @Nullable private String codec;
    @Nullable private final String audioTrackId;
    @Nullable private final String audioTrackName;
    @Nullable private final Locale audioLocale;
    @Nullable private final AudioTrackType audioTrackType;
    @Nullable private ItagItem itagItem;

    public static final class Builder {
        private String id;
        private String content;
        private boolean isUrl;
        private DeliveryMethod deliveryMethod = DeliveryMethod.PROGRESSIVE_HTTP;
        @Nullable private MediaFormat mediaFormat;
        @Nullable private String manifestUrl;
        private int averageBitrate = UNKNOWN_BITRATE;
        @Nullable private String audioTrackId;
        @Nullable private String audioTrackName;
        @Nullable private Locale audioLocale;
        @Nullable private AudioTrackType audioTrackType;
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
        public Builder setAverageBitrate(final int bitrate) { averageBitrate = bitrate; return this; }
        public Builder setAudioTrackId(@Nullable final String id) {
            audioTrackId = id;
            return this;
        }
        public Builder setAudioTrackName(@Nullable final String name) {
            audioTrackName = name;
            return this;
        }
        public Builder setAudioLocale(@Nullable final Locale locale) {
            audioLocale = locale;
            return this;
        }
        public Builder setAudioTrackType(@Nullable final AudioTrackType type) {
            audioTrackType = type;
            return this;
        }
        public Builder setItagItem(@Nullable final ItagItem item) { itagItem = item; return this; }

        @Nonnull public AudioStream build() {
            if (id == null || content == null || deliveryMethod == null) {
                throw new IllegalStateException(
                        "Audio stream requires an id, content, and delivery method");
            }
            return new AudioStream(this);
        }
    }

    AudioStream(final Builder builder) {
        super(builder.id, builder.content, builder.isUrl, builder.mediaFormat,
                builder.deliveryMethod, builder.manifestUrl);
        averageBitrate = builder.averageBitrate;
        audioTrackId = builder.audioTrackId;
        audioTrackName = builder.audioTrackName;
        audioLocale = builder.audioLocale;
        audioTrackType = builder.audioTrackType;
        itagItem = builder.itagItem;
        if (itagItem != null) {
            itag = itagItem.id;
            quality = itagItem.getQuality();
            bitrate = itagItem.getBitrate();
            initStart = itagItem.getInitStart();
            initEnd = itagItem.getInitEnd();
            indexStart = itagItem.getIndexStart();
            indexEnd = itagItem.getIndexEnd();
            codec = itagItem.getCodec();
        }
    }

    @Override public boolean equalStats(final Stream other) {
        return super.equalStats(other) && other instanceof AudioStream audio
                && averageBitrate == audio.averageBitrate
                && Objects.equals(audioTrackId, audio.audioTrackId)
                && audioTrackType == audio.audioTrackType
                && Objects.equals(audioLocale, audio.audioLocale);
    }

    public int getAverageBitrate() { return averageBitrate; }
    public int getItag() { return itag; }
    public int getBitrate() { return bitrate; }
    public int getInitStart() { return initStart; }
    public int getInitEnd() { return initEnd; }
    public int getIndexStart() { return indexStart; }
    public int getIndexEnd() { return indexEnd; }
    @Nullable public String getQuality() { return quality; }
    @Nullable public String getCodec() { return codec; }
    @Nullable public String getAudioTrackId() { return audioTrackId; }
    @Nullable public String getAudioTrackName() { return audioTrackName; }
    @Nullable public Locale getAudioLocale() { return audioLocale; }
    @Nullable public AudioTrackType getAudioTrackType() { return audioTrackType; }
    @Nullable @Override public ItagItem getItagItem() { return itagItem; }
}
