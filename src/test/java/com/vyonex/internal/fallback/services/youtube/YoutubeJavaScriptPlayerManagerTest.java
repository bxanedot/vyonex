// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YoutubeJavaScriptPlayerManagerTest {
    private static final String PLAYER_CODE = "signatureTimestamp:54321;"
            + "var nTransform=function(a){a=a.split(\"\");a.reverse();return a.join(\"\")};"
            + "var signatureTransform=function(a){a=a.split(\"\");"
            + "a.splice(0,1);return a.join(\"\")};";

    @AfterEach
    void resetState() {
        YoutubeJavaScriptPlayerManager.clearAllCaches();
        VyonEx.init(null);
    }

    @Test
    void reusesThePlayerScriptAndReplacesOnlyTheNValue() throws Exception {
        VyonEx.init(new PlayerDownloader());

        assertEquals(54321, YoutubeJavaScriptPlayerManager.getSignatureTimestamp("abcdefghijk"));
        assertEquals("bcdef", YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                "abcdefghijk", "abcdef"));

        final String input = "https://r.googlevideo.com/videoplayback?n=abcdef&keep=abcdef";
        assertEquals("https://r.googlevideo.com/videoplayback?n=fedcba&keep=abcdef",
                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                        "abcdefghijk", input));
        assertEquals(1, YoutubeJavaScriptPlayerManager.getThrottlingParametersCacheSize());
    }

    private static final class PlayerDownloader extends Downloader {
        @Override
        public Response execute(final Request request) throws IOException {
            final String body = request.url().endsWith("iframe_api")
                    ? "player\\/1234abcd\\/player_ias.vflset/en_GB/base.js"
                    : PLAYER_CODE;
            return new Response(200, "OK", Map.of(), body, request.url());
        }
    }
}
