// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.CPN;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.youtube.ItagItem;
import com.vyonex.internal.fallback.services.youtube.YoutubeJavaScriptPlayerManager;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.DeliveryMethod;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.fallback.utils.LocaleCompat;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class YoutubeStreamFormatMapper {
    private static final String UNKNOWN = "-1";

    private YoutubeStreamFormatMapper() { }

    @Nullable
    static ItagInfo map(@Nonnull final String videoId,
                        @Nonnull final JsonObject format,
                        @Nonnull final ItagItem.ItagType type,
                        @Nonnull final StreamType streamType,
                        @Nonnull final String playbackNonce,
                        @Nullable final String poToken) throws ExtractionException {
        final ItagItem item = ItagItem.fromStreamFormat(format, type);
        String url = format.getString("url");
        if (url == null || url.isBlank()) {
            url = cipheredUrl(videoId, format);
            if (url == null) return null;
        }

        url = YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId, url);
        url = appendQueryParameter(url, CPN, playbackNonce);
        if (poToken != null) url = appendQueryParameter(url, "pot", poToken);

        final JsonObject initRange = format.getObject("initRange");
        final JsonObject indexRange = format.getObject("indexRange");
        final String mimeType = format.getString("mimeType", "");

        item.setBitrate(format.getInt("bitrate"));
        item.setWidth(format.getInt("width"));
        item.setHeight(format.getInt("height"));
        item.setInitStart(integer(initRange.getString("start"), -1));
        item.setInitEnd(integer(initRange.getString("end"), -1));
        item.setIndexStart(integer(indexRange.getString("start"), -1));
        item.setIndexEnd(integer(indexRange.getString("end"), -1));
        item.setQuality(format.getString("quality"));
        item.setCodec(codec(mimeType));
        item.setIsDrc(format.getBoolean("isDrc", false));
        item.setLastModified(number(format, "lastModified", -1));
        item.setXtags(format.getString("xtags"));

        if (streamType == StreamType.LIVE_STREAM || streamType == StreamType.POST_LIVE_STREAM) {
            item.setTargetDurationSec(format.getInt("targetDurationSec"));
        }
        if (type == ItagItem.ItagType.VIDEO || type == ItagItem.ItagType.VIDEO_ONLY) {
            item.setFps(format.getInt("fps", ItagItem.FPS_NOT_APPLICABLE_OR_UNKNOWN));
        } else if (type == ItagItem.ItagType.AUDIO) {
            mapAudioTrack(format, item);
        }

        item.setContentLength(number(format, "contentLength", ItagItem.CONTENT_LENGTH_UNKNOWN));
        item.setApproxDurationMs(number(format, "approxDurationMs",
                ItagItem.APPROX_DURATION_MS_UNKNOWN));

        final ItagInfo info = new ItagInfo(url, item);
        info.setIsUrl(streamType == StreamType.VIDEO_STREAM
                ? !"FORMAT_STREAM_TYPE_OTF".equalsIgnoreCase(format.getString("type", ""))
                : streamType != StreamType.POST_LIVE_STREAM);
        return info;
    }

    @Nonnull
    static AudioStream audioStream(@Nonnull final ItagInfo info,
                                   @Nonnull final StreamType streamType) {
        final ItagItem item = info.getItagItem();
        final AudioStream.Builder builder = new AudioStream.Builder()
                .setId(String.valueOf(item.id))
                .setContent(info.getContent(), info.getIsUrl())
                .setMediaFormat(item.getMediaFormat())
                .setAverageBitrate(item.getAverageBitrate())
                .setAudioTrackId(item.getAudioTrackId())
                .setAudioTrackName(item.getAudioTrackName())
                .setAudioLocale(item.getAudioLocale())
                .setAudioTrackType(item.getAudioTrackType())
                .setItagItem(item);
        if (streamType == StreamType.LIVE_STREAM || streamType == StreamType.POST_LIVE_STREAM
                || !info.getIsUrl()) {
            builder.setDeliveryMethod(DeliveryMethod.DASH);
        }
        return builder.build();
    }

    @Nonnull
    static VideoStream videoStream(@Nonnull final ItagInfo info,
                                   @Nonnull final StreamType streamType,
                                   final boolean videoOnly) {
        final ItagItem item = info.getItagItem();
        final String resolution = item.getResolutionString();
        final VideoStream.Builder builder = new VideoStream.Builder()
                .setId(String.valueOf(item.id))
                .setContent(info.getContent(), info.getIsUrl())
                .setMediaFormat(item.getMediaFormat())
                .setIsVideoOnly(videoOnly)
                .setResolution(resolution == null ? "" : resolution)
                .setItagItem(item);
        if (streamType != StreamType.VIDEO_STREAM || !info.getIsUrl()) {
            builder.setDeliveryMethod(DeliveryMethod.DASH);
        }
        return builder.build();
    }

    @Nullable
    private static String cipheredUrl(@Nonnull final String videoId,
                                      @Nonnull final JsonObject format)
            throws ParsingException {
        final String cipher = format.getString("cipher", format.getString("signatureCipher"));
        if (cipher == null || cipher.isBlank()) return null;

        final Map<String, String> values = new LinkedHashMap<>();
        try {
            for (final String field : cipher.split("&")) {
                final int separator = field.indexOf('=');
                if (separator < 0) continue;
                values.put(decode(field.substring(0, separator)),
                        decode(field.substring(separator + 1)));
            }
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("Invalid stream signature data", e);
        }

        final String baseUrl = values.get("url");
        final String obfuscated = values.get("s");
        if (baseUrl == null || baseUrl.isBlank() || obfuscated == null || obfuscated.isEmpty()) {
            return null;
        }
        final String signature = YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                videoId, obfuscated);
        return appendQueryParameter(baseUrl, values.getOrDefault("sp", "signature"), signature);
    }

    private static String appendQueryParameter(@Nonnull final String url,
                                               @Nonnull final String name,
                                               @Nonnull final String value) {
        final int fragmentStart = url.indexOf('#');
        final String base = fragmentStart < 0 ? url : url.substring(0, fragmentStart);
        final String fragment = fragmentStart < 0 ? "" : url.substring(fragmentStart);
        final String separator = !base.contains("?") ? "?"
                : base.endsWith("?") || base.endsWith("&") ? "" : "&";
        return base + separator + encode(name) + "=" + encode(value) + fragment;
    }

    private static String decode(@Nonnull final String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String encode(@Nonnull final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String codec(@Nonnull final String mimeType) {
        final int firstQuote = mimeType.indexOf('"');
        final int lastQuote = mimeType.lastIndexOf('"');
        return firstQuote >= 0 && lastQuote > firstQuote
                ? mimeType.substring(firstQuote + 1, lastQuote) : "";
    }

    private static void mapAudioTrack(@Nonnull final JsonObject format,
                                      @Nonnull final ItagItem item) {
        item.setSampleRate(integer(format.getString("audioSampleRate"), -1));
        item.setAudioChannels(format.getInt("audioChannels", 2));

        final JsonObject track = format.getObject("audioTrack");
        final String trackId = track.getString("id");
        if (trackId != null && !trackId.isBlank()) {
            item.setAudioTrackId(trackId);
            final int localeEnd = trackId.indexOf('.');
            if (localeEnd > 0) {
                LocaleCompat.forLanguageTag(trackId.substring(0, localeEnd))
                        .ifPresent(item::setAudioLocale);
            }
            item.setAudioTrackType(YoutubeParsingHelper.extractAudioTrackType(item.getXtags()));
        }
        item.setAudioTrackName(track.getString("displayName"));
    }

    private static int integer(@Nullable final String value, final int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long number(@Nonnull final JsonObject object,
                               @Nonnull final String key,
                               final long fallback) {
        if (object.isNumber(key)) return object.getLong(key);
        final String value = object.getString(key, UNKNOWN);
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException ignored) {
            return fallback;
        }
    }
}
