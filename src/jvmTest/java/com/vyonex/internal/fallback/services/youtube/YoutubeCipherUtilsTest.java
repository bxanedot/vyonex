// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.utils.JavaScript;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class YoutubeCipherUtilsTest {
    private static final String PLAYER_CODE = "signatureTimestamp:12345;"
            + "var nTransform=function(a){a=a.split(\"\");a.reverse();return a.join(\"\")};"
            + "var signatureTransform=function(a){a=a.split(\"\");"
            + "a.splice(0,1);return a.join(\"\")};";

    @Test
    void extractsTimestampAndRunsSignatureTransform() throws Exception {
        assertEquals("12345", YoutubeSignatureUtils.getSignatureTimestamp(PLAYER_CODE));
        final String script = YoutubeSignatureUtils.getDeobfuscationCode(PLAYER_CODE);
        assertEquals("bcdef", JavaScript.run(script, "deobfuscate", "abcdef"));
    }

    @Test
    void extractsAndRunsThrottlingTransform() throws Exception {
        final String script = YoutubeThrottlingParameterUtils.getDeobfuscationFunction(PLAYER_CODE);
        assertEquals("fedcba", JavaScript.run(script, "_solveN", "abcdef"));
    }

    @Test
    void extractsRawThrottlingValueAndIgnoresFragments() {
        assertEquals("a%2Fb", YoutubeThrottlingParameterUtils
                .getThrottlingParameterFromStreamingUrl("https://media.example/?n=a%2Fb&x=1"));
        assertNull(YoutubeThrottlingParameterUtils
                .getThrottlingParameterFromStreamingUrl("https://media.example/?q=x#n=y"));
    }
}
