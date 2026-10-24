// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.utils.JavaScript;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loads one player script and caches its extracted transforms and results. */
public final class YoutubeJavaScriptPlayerManager {
    private static final Object CACHE_LOCK = new Object();
    private static final Pattern N_PARAMETER = Pattern.compile("([?&]n=)([^&#]+)");
    private static final Map<String, String> N_RESULTS = new HashMap<>();

    private static String playerCode;
    private static Integer signatureTimestamp;
    private static String signatureScript;
    private static String throttlingScript;
    private static ParsingException signatureTimestampFailure;
    private static ParsingException signatureScriptFailure;
    private static ParsingException throttlingScriptFailure;

    private YoutubeJavaScriptPlayerManager() { }

    @Nonnull
    public static Integer getSignatureTimestamp(@Nonnull final String videoId)
            throws ParsingException {
        synchronized (CACHE_LOCK) {
            if (signatureTimestamp != null) return signatureTimestamp;
            if (signatureTimestampFailure != null) throw signatureTimestampFailure;
            try {
                signatureTimestamp = Integer.valueOf(
                        YoutubeSignatureUtils.getSignatureTimestamp(getPlayerCode(videoId)));
                return signatureTimestamp;
            } catch (final NumberFormatException e) {
                signatureTimestampFailure = new ParsingException(
                        "Player signature timestamp is outside the supported range", e);
                throw signatureTimestampFailure;
            } catch (final ParsingException e) {
                signatureTimestampFailure = e;
                throw e;
            }
        }
    }

    @Nonnull
    public static String deobfuscateSignature(@Nonnull final String videoId,
                                              @Nonnull final String obfuscatedSignature)
            throws ParsingException {
        final String script;
        synchronized (CACHE_LOCK) {
            if (signatureScriptFailure != null) throw signatureScriptFailure;
            if (signatureScript == null) {
                try {
                    signatureScript = YoutubeSignatureUtils.getDeobfuscationCode(
                            getPlayerCode(videoId));
                } catch (final ParsingException e) {
                    signatureScriptFailure = e;
                    throw e;
                }
            }
            script = signatureScript;
        }

        try {
            return Objects.requireNonNullElse(
                    JavaScript.run(script, YoutubeSignatureUtils.DEOBFUSCATION_FUNCTION_NAME,
                            obfuscatedSignature), "");
        } catch (final Exception e) {
            throw new ParsingException("Could not run the signature transform", e);
        }
    }

    @Nonnull
    public static String getUrlWithThrottlingParameterDeobfuscated(
            @Nonnull final String videoId,
            @Nonnull final String streamingUrl) throws ParsingException {
        final String input = YoutubeThrottlingParameterUtils
                .getThrottlingParameterFromStreamingUrl(streamingUrl);
        if (input == null) return streamingUrl;

        final String script;
        synchronized (CACHE_LOCK) {
            final String cached = N_RESULTS.get(input);
            if (cached != null) return replaceNParameter(streamingUrl, cached);
            if (throttlingScriptFailure != null) throw throttlingScriptFailure;
            if (throttlingScript == null) {
                try {
                    throttlingScript = YoutubeThrottlingParameterUtils
                            .getDeobfuscationFunction(getPlayerCode(videoId));
                } catch (final ParsingException e) {
                    throttlingScriptFailure = e;
                    throw e;
                }
            }
            script = throttlingScript;
        }

        final String output;
        try {
            output = JavaScript.run(script,
                    YoutubeThrottlingParameterUtils.DEOBFUSCATION_FUNCTION_NAME, input);
        } catch (final Exception e) {
            throw new ParsingException("Could not run the throttling transform", e);
        }
        if (output == null || output.isEmpty() || output.equals(input)) {
            throw new ParsingException("The throttling transform returned no new value");
        }
        synchronized (CACHE_LOCK) {
            if (throttlingScript == script) N_RESULTS.put(input, output);
        }
        return replaceNParameter(streamingUrl, output);
    }

    public static int getThrottlingParametersCacheSize() {
        synchronized (CACHE_LOCK) {
            return N_RESULTS.size();
        }
    }

    public static void clearAllCaches() {
        synchronized (CACHE_LOCK) {
            playerCode = null;
            signatureTimestamp = null;
            signatureScript = null;
            throttlingScript = null;
            signatureTimestampFailure = null;
            signatureScriptFailure = null;
            throttlingScriptFailure = null;
            N_RESULTS.clear();
        }
    }

    public static void clearThrottlingParametersCache() {
        synchronized (CACHE_LOCK) {
            N_RESULTS.clear();
        }
    }

    private static String getPlayerCode(final String videoId) throws ParsingException {
        if (playerCode == null) {
            playerCode = YoutubeJavaScriptExtractor.extractJavaScriptPlayerCode(videoId);
        }
        return playerCode;
    }

    private static String replaceNParameter(final String url, final String value)
            throws ParsingException {
        final Matcher matcher = N_PARAMETER.matcher(url);
        if (!matcher.find()) {
            throw new ParsingException("Streaming URL lost its throttling parameter");
        }
        return url.substring(0, matcher.start()) + matcher.group(1) + value
                + url.substring(matcher.end());
    }
}
