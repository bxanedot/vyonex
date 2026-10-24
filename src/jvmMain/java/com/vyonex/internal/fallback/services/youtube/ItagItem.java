// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.AudioTrackType;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Locale;

public class ItagItem implements Serializable {
    public static final int AVERAGE_BITRATE_UNKNOWN = -1;
    public static final int SAMPLE_RATE_UNKNOWN = -1;
    public static final int FPS_NOT_APPLICABLE_OR_UNKNOWN = -1;
    public static final int TARGET_DURATION_SEC_UNKNOWN = -1;
    public static final int AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN = -1;
    public static final long CONTENT_LENGTH_UNKNOWN = -1;
    public static final long APPROX_DURATION_MS_UNKNOWN = -1;
    public static final long LAST_MODIFIED_UNKOWN = -1;

    public enum ItagType { AUDIO, VIDEO, VIDEO_ONLY }

    private final MediaFormat mediaFormat;
    public final int id;
    public final ItagType itagType;

    /** @deprecated Use {@link #getAverageBitrate()} instead. */
    @Deprecated public int avgBitrate = AVERAGE_BITRATE_UNKNOWN;
    private int sampleRate = SAMPLE_RATE_UNKNOWN;
    private int audioChannels = AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN;

    /** @deprecated Use {@link #getResolutionString()} instead. */
    @Deprecated public String resolutionString;
    /** @deprecated Use {@link #getFps()} and {@link #setFps(int)} instead. */
    @Deprecated public int fps = FPS_NOT_APPLICABLE_OR_UNKNOWN;

    private int bitrate;
    private int width;
    private int height;
    private int initStart;
    private int initEnd;
    private int indexStart;
    private int indexEnd;
    private String quality;
    private String codec;
    private int targetDurationSec = TARGET_DURATION_SEC_UNKNOWN;
    private long approxDurationMs = APPROX_DURATION_MS_UNKNOWN;
    private long contentLength = CONTENT_LENGTH_UNKNOWN;
    private String audioTrackId;
    private String audioTrackName;
    @Nullable private AudioTrackType audioTrackType;
    @Nullable private Locale audioLocale;
    private boolean isDrc;
    private long lastModified;
    private String xtags;

    public ItagItem(final int id, final ItagType type, final MediaFormat format,
                    final String resolution) {
        this(id, type, format, resolution, 30);
    }

    public ItagItem(final int id, final ItagType type, final MediaFormat format,
                    final String resolution, final int fps) {
        this.id = id;
        itagType = type;
        mediaFormat = format;
        resolutionString = resolution;
        this.fps = fps;
    }

    public ItagItem(final int id, final ItagType type, final MediaFormat format,
                    final int averageBitrate) {
        this.id = id;
        itagType = type;
        mediaFormat = format;
        avgBitrate = averageBitrate;
    }

    public ItagItem(@Nonnull final ItagItem source) {
        id = source.id;
        itagType = source.itagType;
        mediaFormat = source.mediaFormat;
        avgBitrate = source.avgBitrate;
        sampleRate = source.sampleRate;
        audioChannels = source.audioChannels;
        resolutionString = source.resolutionString;
        fps = source.fps;
        bitrate = source.bitrate;
        width = source.width;
        height = source.height;
        initStart = source.initStart;
        initEnd = source.initEnd;
        indexStart = source.indexStart;
        indexEnd = source.indexEnd;
        quality = source.quality;
        codec = source.codec;
        targetDurationSec = source.targetDurationSec;
        approxDurationMs = source.approxDurationMs;
        contentLength = source.contentLength;
        audioTrackId = source.audioTrackId;
        audioTrackName = source.audioTrackName;
        audioTrackType = source.audioTrackType;
        audioLocale = source.audioLocale;
        isDrc = source.isDrc;
        lastModified = source.lastModified;
        xtags = source.xtags;
    }

    @Nonnull
    public static ItagItem fromStreamFormat(final JsonObject format,
                                           @Nonnull final ItagType type)
            throws ParsingException {
        final int id = format.getInt("itag");
        final String mime = format.getString("mimeType", "").split(";", 2)[0].trim();
        final MediaFormat mediaFormat = MediaFormat.getFromMimeType(mime);
        if (mediaFormat == null) throw new ParsingException("Unsupported stream MIME type: " + mime);

        if (type == ItagType.AUDIO) {
            return new ItagItem(id, type, mediaFormat,
                    format.getInt("averageBitrate", AVERAGE_BITRATE_UNKNOWN));
        }

        String resolution = format.getString("qualityLabel", "");
        if (resolution.isBlank()) {
            final int streamHeight = format.getInt("height", 0);
            if (streamHeight > 0) resolution = streamHeight + "p";
        }
        return new ItagItem(id, type, mediaFormat, resolution,
                format.getInt("fps", FPS_NOT_APPLICABLE_OR_UNKNOWN));
    }

    @Nullable public MediaFormat getMediaFormat() { return mediaFormat; }
    public int getBitrate() { return bitrate; }
    public void setBitrate(final int value) { bitrate = value; }
    public int getWidth() { return width; }
    public void setWidth(final int value) { width = value; }
    public int getHeight() { return height; }
    public void setHeight(final int value) { height = value; }
    public int getFps() { return fps; }
    public void setFps(final int value) { fps = value > 0 ? value : FPS_NOT_APPLICABLE_OR_UNKNOWN; }
    public int getInitStart() { return initStart; }
    public void setInitStart(final int value) { initStart = value; }
    public int getInitEnd() { return initEnd; }
    public void setInitEnd(final int value) { initEnd = value; }
    public int getIndexStart() { return indexStart; }
    public void setIndexStart(final int value) { indexStart = value; }
    public int getIndexEnd() { return indexEnd; }
    public void setIndexEnd(final int value) { indexEnd = value; }
    @Nullable public String getQuality() { return quality; }
    public void setQuality(@Nullable final String value) { quality = value; }
    @Nullable public String getResolutionString() { return resolutionString; }
    @Nullable public String getCodec() { return codec; }
    public void setCodec(@Nullable final String value) { codec = value; }
    public int getAverageBitrate() { return avgBitrate; }
    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(final int value) {
        sampleRate = value > 0 ? value : SAMPLE_RATE_UNKNOWN;
    }
    public int getAudioChannels() { return audioChannels; }
    public void setAudioChannels(final int value) {
        audioChannels = value > 0 ? value : AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN;
    }
    public int getTargetDurationSec() { return targetDurationSec; }
    public void setTargetDurationSec(final int value) {
        targetDurationSec = value > 0 ? value : TARGET_DURATION_SEC_UNKNOWN;
    }
    public long getApproxDurationMs() { return approxDurationMs; }
    public void setApproxDurationMs(final long value) {
        approxDurationMs = value > 0 ? value : APPROX_DURATION_MS_UNKNOWN;
    }
    public long getContentLength() { return contentLength; }
    public void setContentLength(final long value) {
        contentLength = value > 0 ? value : CONTENT_LENGTH_UNKNOWN;
    }
    @Nullable public String getAudioTrackId() { return audioTrackId; }
    public void setAudioTrackId(@Nullable final String value) { audioTrackId = value; }
    @Nullable public String getAudioTrackName() { return audioTrackName; }
    public void setAudioTrackName(@Nullable final String value) { audioTrackName = value; }
    @Nullable public AudioTrackType getAudioTrackType() { return audioTrackType; }
    public void setAudioTrackType(@Nullable final AudioTrackType value) { audioTrackType = value; }
    @Nullable public Locale getAudioLocale() { return audioLocale; }
    public void setAudioLocale(@Nullable final Locale value) { audioLocale = value; }
    public Boolean isDrc() { return isDrc; }
    public void setIsDrc(final Boolean value) { isDrc = value; }
    public long getLastModified() { return lastModified; }
    public void setLastModified(final long value) { lastModified = value; }
    @Nullable public String getXtags() { return xtags; }
    public void setXtags(@Nullable final String value) { xtags = value; }
}
