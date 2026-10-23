// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.utils.JavaScript;
import com.vyonex.internal.primary.cipher.CipherScriptSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class YoutubeThrottlingParameterUtils {
    static final String DEOBFUSCATION_FUNCTION_NAME = "_solveN";
    private static final Pattern N_PARAMETER = Pattern.compile("[&?]n=([^&#]+)");

    private YoutubeThrottlingParameterUtils() { }

    @Nonnull
    static String getDeobfuscationFunction(@Nonnull final String playerCode)
            throws ParsingException {
        final String script = CipherScriptSupport.nSolverScript(playerCode);
        if (script == null) {
            throw new ParsingException("Player script has no supported throttling transform");
        }
        try {
            JavaScript.compileOrThrow(script);
            return script;
        } catch (final Exception e) {
            throw new ParsingException("Could not compile the throttling transform", e);
        }
    }

    @Nullable
    static String getThrottlingParameterFromStreamingUrl(@Nonnull final String streamingUrl) {
        if (!streamingUrl.contains("&n=") && !streamingUrl.contains("?n=")) return null;
        final Matcher matcher = N_PARAMETER.matcher(streamingUrl);
        return matcher.find() ? matcher.group(1) : null;
    }
}
