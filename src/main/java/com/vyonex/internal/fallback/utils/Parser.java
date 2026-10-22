// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import com.vyonex.internal.fallback.exceptions.ParsingException;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Parser {
    private Parser() { }

    public static class RegexException extends ParsingException {
        public RegexException(final String message) { super(message); }
    }

    @Nonnull public static Matcher matchOrThrow(@Nonnull final Pattern pattern,
                                                 final String input) throws RegexException {
        final Matcher matcher = pattern.matcher(input);
        if (matcher.find()) return matcher;
        throw noMatch(pattern, input, 1024);
    }

    @Nonnull public static String matchGroup1(final String pattern, final String input)
            throws RegexException {
        return matchGroup(pattern, input, 1);
    }

    @Nonnull public static String matchGroup1(final Pattern pattern, final String input)
            throws RegexException {
        return matchGroup(pattern, input, 1);
    }

    @Nonnull public static String matchGroup(final String pattern, final String input,
                                             final int group) throws RegexException {
        return matchGroup(Pattern.compile(pattern), input, group);
    }

    @Nonnull public static String matchGroup(@Nonnull final Pattern pattern, final String input,
                                             final int group) throws RegexException {
        return matchOrThrow(pattern, input).group(group);
    }

    public static String matchGroup1MultiplePatterns(final Pattern[] patterns,
                                                      final String input) throws RegexException {
        return matchMultiplePatterns(patterns, input).group(1);
    }

    public static Matcher matchMultiplePatterns(final Pattern[] patterns, final String input)
            throws RegexException {
        for (final Pattern pattern : patterns) {
            final Matcher matcher = pattern.matcher(input);
            if (matcher.find()) return matcher;
        }
        if (patterns.length == 0) {
            throw new RegexException("Empty patterns array passed to matchMultiplePatterns");
        }
        throw noMatch(patterns[0], input, 1000);
    }

    public static boolean isMatch(final String pattern, final String input) {
        return isMatch(Pattern.compile(pattern), input);
    }

    public static boolean isMatch(@Nonnull final Pattern pattern, final String input) {
        return pattern.matcher(input).find();
    }

    @Nonnull public static Map<String, String> compatParseMap(@Nonnull final String input) {
        final Map<String, String> result = new HashMap<>();
        for (final String argument : input.split("&")) {
            final int separator = argument.indexOf('=');
            if (separator >= 0) {
                result.put(argument.substring(0, separator),
                        Utils.decodeUrlUtf8(argument.substring(separator + 1)));
            }
        }
        return result;
    }

    private static RegexException noMatch(final Pattern pattern, final String input,
                                           final int maxInputLength) {
        return new RegexException("Failed to find pattern \"" + pattern.pattern() + "\""
                + (input.length() <= maxInputLength ? " inside of \"" + input + "\"" : ""));
    }
}
