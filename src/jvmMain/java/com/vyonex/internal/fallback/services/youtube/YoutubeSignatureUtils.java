// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.utils.JavaScript;
import com.vyonex.internal.primary.cipher.CipherScriptSupport;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class YoutubeSignatureUtils {
    static final String DEOBFUSCATION_FUNCTION_NAME = "deobfuscate";
    private static final Pattern SIGNATURE_TIMESTAMP =
            Pattern.compile("signatureTimestamp\\s*[=:]\\s*(\\d+)");

    private YoutubeSignatureUtils() { }

    @Nonnull
    static String getSignatureTimestamp(@Nonnull final String playerCode)
            throws ParsingException {
        final Matcher matcher = SIGNATURE_TIMESTAMP.matcher(playerCode);
        if (!matcher.find()) {
            throw new ParsingException("Player script has no signature timestamp");
        }
        return matcher.group(1);
    }

    @Nonnull
    static String getDeobfuscationCode(@Nonnull final String playerCode)
            throws ParsingException {
        final String script = CipherScriptSupport.signatureSolverScript(playerCode);
        if (script == null) {
            throw new ParsingException("Player script has no supported signature transform");
        }
        try {
            JavaScript.compileOrThrow(script);
            return script;
        } catch (final Exception e) {
            throw new ParsingException("Could not compile the signature transform", e);
        }
    }
}
