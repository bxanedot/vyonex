// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserContractTest {
    @Test
    void returnsTheFirstMatchingRegexGroup() throws Exception {
        assertEquals("42", Parser.matchGroup1("id=(\\d+)", "prefix id=42 suffix"));
        assertEquals("x", Parser.matchGroup1MultiplePatterns(new Pattern[]{
                Pattern.compile("missing=(.)"), Pattern.compile("value=(.)")}, "value=x"));
        assertTrue(Parser.isMatch(Pattern.compile("value=\\w+"), "value=x"));
        assertFalse(Parser.isMatch("absent", "value=x"));
        assertThrows(Parser.RegexException.class,
                () -> Parser.matchGroup1("id=(\\d+)", "no identifier"));
    }

    @Test
    void parsesEncodedQueryParametersAndKeepsValuesAfterEquals() {
        assertEquals(Map.of("a", "last", "b", "x=y", "empty", ""),
                Parser.compatParseMap("a=one+two&b=x%3Dy&a=last&empty=&flag"));
    }
}
