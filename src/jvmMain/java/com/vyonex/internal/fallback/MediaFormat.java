// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Describes a media format and its file and content types. */
public enum MediaFormat {
    MPEG_4(0x0, "MPEG-4", "mp4", "video/mp4"),
    v3GPP(0x10, "3GPP", "3gp", "video/3gpp"),
    WEBM(0x20, "WebM", "webm", "video/webm"),
    M4A(0x100, "m4a", "m4a", "audio/mp4"),
    WEBMA(0x200, "WebM", "webm", "audio/webm"),
    MP3(0x300, "MP3", "mp3", "audio/mpeg"),
    MP2(0x310, "MP2", "mp2", "audio/mpeg"),
    OPUS(0x400, "opus", "opus", "audio/opus"),
    OGG(0x500, "ogg", "ogg", "audio/ogg"),
    WEBMA_OPUS(0x200, "WebM Opus", "webm", "audio/webm"),
    AIFF(0x600, "AIFF", "aiff", "audio/aiff"),
    AIF(0x600, "AIFF", "aif", "audio/aiff"),
    WAV(0x700, "WAV", "wav", "audio/wav"),
    FLAC(0x800, "FLAC", "flac", "audio/flac"),
    ALAC(0x900, "ALAC", "alac", "audio/alac"),
    VTT(0x1000, "WebVTT", "vtt", "text/vtt"),
    TTML(0x2000, "Timed Text Markup Language", "ttml", "application/ttml+xml"),
    TRANSCRIPT1(0x3000, "TranScript v1", "srv1", "text/xml"),
    TRANSCRIPT2(0x4000, "TranScript v2", "srv2", "text/xml"),
    TRANSCRIPT3(0x5000, "TranScript v3", "srv3", "text/xml"),
    SRT(0x6000, "SubRip file format", "srt", "text/srt");

    public final int id;
    @Nonnull public final String name;
    @Nonnull public final String suffix;
    @Nonnull public final String mimeType;

    MediaFormat(final int id, @Nonnull final String name,
                @Nonnull final String suffix, @Nonnull final String mimeType) {
        this.id = id;
        this.name = name;
        this.suffix = suffix;
        this.mimeType = mimeType;
    }

    @Nonnull
    public static String getNameById(final int id) {
        final MediaFormat format = getFormatById(id);
        return format == null ? "" : format.name;
    }

    @Nonnull
    public static String getSuffixById(final int id) {
        final MediaFormat format = getFormatById(id);
        return format == null ? "" : format.suffix;
    }

    @Nullable
    public static String getMimeById(final int id) {
        final MediaFormat format = getFormatById(id);
        return format == null ? null : format.mimeType;
    }

    @Nullable
    public static MediaFormat getFromMimeType(final String mimeType) {
        for (final MediaFormat format : values()) {
            if (format.mimeType.equals(mimeType)) return format;
        }
        return null;
    }

    @Nonnull
    public static List<MediaFormat> getAllFromMimeType(final String mimeType) {
        final List<MediaFormat> matches = new ArrayList<>();
        for (final MediaFormat format : values()) {
            if (format.mimeType.equals(mimeType)) matches.add(format);
        }
        return matches;
    }

    @Nullable
    public static MediaFormat getFormatById(final int id) {
        for (final MediaFormat format : values()) {
            if (format.id == id) return format;
        }
        return null;
    }

    @Nullable
    public static MediaFormat getFromSuffix(final String suffix) {
        for (final MediaFormat format : values()) {
            if (format.suffix.equals(suffix)) return format;
        }
        return null;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getSuffix() {
        return suffix;
    }

    @Nonnull
    public String getMimeType() {
        return mimeType;
    }
}
