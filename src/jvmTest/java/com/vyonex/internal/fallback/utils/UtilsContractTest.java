// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsContractTest {
    @Test
    void encodesTextAndReadsCounts() throws Exception {
        assertEquals("caf%C3%A9+%26+jazz", Utils.encodeUrlUtf8("café & jazz"));
        assertEquals("café & jazz", Utils.decodeUrlUtf8("caf%C3%A9+%26+jazz"));
        assertEquals("1234", Utils.removeNonDigitCharacters("1,234 plays"));
        assertEquals(1_250, Utils.mixedNumberWordToLong("1.25K views"));
        assertEquals(2_000_000, Utils.mixedNumberWordToLong("2M"));
        assertEquals(3_000_000_000L, Utils.mixedNumberWordToLong("3B"));
        assertThrows(ParsingException.class, () -> Utils.mixedNumberWordToLong("unknown"));
        Utils.checkUrl("youtube\\.com", "https://youtube.com/watch");
        assertThrows(ParsingException.class,
                () -> Utils.checkUrl("youtube\\.com", "https://example.org/watch"));
        assertEquals("https://example.org/watch", Utils.replaceHttpWithHttps(
                "http://example.org/watch"));
    }

    @Test
    void keepsUrlHelpersOnTheirDocumentedBoundaries() throws Exception {
        final URL url = new URL("https://example.org/path?tag=first+value&tag=second&flag");
        assertEquals("first value", Utils.getQueryValue(url, "tag"));
        assertNull(Utils.getQueryValue(url, "flag"));
        assertEquals("https://example.org/path", Utils.stringToURL("example.org/path").toString());
        assertTrue(Utils.isHTTP(new URL("https://example.org/path")));
        assertFalse(Utils.isHTTP(new URL("https://example.org:8443/path")));
        assertFalse(Utils.isHTTP(new URL("file:/tmp/example")));
        assertEquals("https://youtube.com/watch?v=1",
                Utils.removeMAndWWWFromUrl("https://m.youtube.com/watch?v=1"));
        assertEquals("https://youtube.com/watch?v=1",
                Utils.removeMAndWWWFromUrl("https://www.youtube.com/watch?v=1"));
        assertEquals("text", Utils.removeUTF8BOM("\uFEFFtext\uFEFF"));
        assertEquals("https://example.org:8443",
                Utils.getBaseUrl("https://example.org:8443/path"));
        assertEquals("https://example.org", Utils.getBaseUrl("example.org/path"));
        assertEquals("vnd.youtube", Utils.getBaseUrl("vnd.youtube://watch/1"));
    }

    @Test
    void followsGoogleRedirectsEvenWhenTheTargetIsTheFirstQueryParameter() {
        assertEquals("https://example.org/a?x=1", Utils.followGoogleRedirectIfNeeded(
                "https://www.google.com/url?url=https%3A%2F%2Fexample.org%2Fa%3Fx%3D1"));
        assertEquals("https://example.org/", Utils.followGoogleRedirectIfNeeded(
                "https://www.google.ch/url?url=https%3A%2F%2Fexample.org%2F"));
        assertEquals("https://example.org/", Utils.followGoogleRedirectIfNeeded(
                "https://example.org/"));
    }

    @Test
    void handlesCollectionsStringsAndRegexFallbacks() throws Exception {
        assertTrue(Utils.isNullOrEmpty((String) null));
        assertTrue(Utils.isNullOrEmpty(List.of()));
        assertTrue(Utils.isNullOrEmpty(Map.of()));
        assertTrue(Utils.isBlank(" \t"));
        final Map<String, String> values = new LinkedHashMap<>();
        values.put("a", "1");
        values.put("b", "2");
        assertEquals("a=1&b=2", Utils.join("&", "=", values));
        assertEquals("one/two", Utils.nonEmptyAndNullJoin("/", "one", null, "", "null", "two"));
        assertEquals("ok", Utils.getStringResultFromRegexArray("name=ok", new String[]{
                "missing=(\\w+)", "name=(\\w+)"}, 1));
        assertEquals("value=x", Utils.getStringResultFromRegexArray("value=x",
                new String[]{"value=(\\w+)"}));
        assertEquals("x", Utils.getStringResultFromRegexArray("value=x", new Pattern[]{
                Pattern.compile("value=(\\w+)")}, 1));
        assertThrows(Parser.RegexException.class, () -> Utils.getStringResultFromRegexArray(
                "nothing", new String[]{"id=(\\d+)"}, 1));
    }
}
