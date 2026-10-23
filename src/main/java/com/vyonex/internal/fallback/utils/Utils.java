// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import com.vyonex.internal.fallback.exceptions.ParsingException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Utils {
    public static final String HTTP = "http://";
    public static final String HTTPS = "https://";
    private static final Pattern COUNT = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)([KMBkmb]*)");

    private Utils() { }

    public static String encodeUrlUtf8(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static String decodeUrlUtf8(final String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Nonnull
    public static String removeNonDigitCharacters(@Nonnull final String value) {
        return value.replaceAll("\\D+", "");
    }

    public static long mixedNumberWordToLong(final String value)
            throws NumberFormatException, ParsingException {
        final Matcher number = COUNT.matcher(value);
        if (!number.find()) throw new Parser.RegexException("No number in value: " + value);

        final double amount = Double.parseDouble(number.group(1).replace(',', '.'));
        final double multiplier = switch (number.group(2).toUpperCase(Locale.ROOT)) {
            case "K" -> 1_000d;
            case "M" -> 1_000_000d;
            case "B" -> 1_000_000_000d;
            default -> 1d;
        };
        return (long) (amount * multiplier);
    }

    public static void checkUrl(final String expression, final String url) throws ParsingException {
        checkUrl(Pattern.compile(expression), url);
    }

    public static void checkUrl(final Pattern expression, final String url)
            throws ParsingException {
        if (isNullOrEmpty(url)) throw new IllegalArgumentException("Url can't be null or empty");
        if (!expression.matcher(url.toLowerCase(Locale.ROOT)).find()) {
            throw new ParsingException("Url doesn't match the pattern");
        }
    }

    public static String replaceHttpWithHttps(final String url) {
        if (url == null) return null;
        return url.startsWith(HTTP) ? HTTPS + url.substring(HTTP.length()) : url;
    }

    @Nullable
    public static String getQueryValue(@Nonnull final URL url, final String parameterName) {
        final String query = url.getQuery();
        if (query == null) return null;

        for (final String pair : query.split("&")) {
            final int separator = pair.indexOf('=');
            final String key = separator < 0 ? pair : pair.substring(0, separator);
            if (parameterName.equals(decodeUrlUtf8(key))) {
                return separator < 0 ? null : decodeUrlUtf8(pair.substring(separator + 1));
            }
        }
        return null;
    }

    @Nonnull
    public static URL stringToURL(final String value) throws MalformedURLException {
        try {
            return new URL(value);
        } catch (final MalformedURLException error) {
            if (error.getMessage() != null && error.getMessage().equals("no protocol: " + value)) {
                return new URL(HTTPS + value);
            }
            throw error;
        }
    }

    public static boolean isHTTP(@Nonnull final URL url) {
        final String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
        if (!protocol.equals("http") && !protocol.equals("https")) return false;
        return url.getPort() == -1 || url.getPort() == url.getDefaultPort();
    }

    public static String removeMAndWWWFromUrl(final String url) {
        return url.replaceFirst("(?i)^(https?://)(?:m|www)\\.", "$1");
    }

    @Nonnull
    public static String removeUTF8BOM(@Nonnull final String value) {
        int start = value.startsWith("\uFEFF") ? 1 : 0;
        int end = value.endsWith("\uFEFF") ? value.length() - 1 : value.length();
        return value.substring(start, end);
    }

    @Nonnull
    public static String getBaseUrl(final String value) throws ParsingException {
        try {
            final URL url = stringToURL(value);
            return url.getProtocol() + "://" + url.getAuthority();
        } catch (final MalformedURLException error) {
            final String message = error.getMessage();
            if (message != null && message.startsWith("unknown protocol: ")) {
                return message.substring("unknown protocol: ".length());
            }
            throw new ParsingException("Malformed url: " + value, error);
        }
    }

    public static String followGoogleRedirectIfNeeded(final String value) {
        try {
            final URL url = stringToURL(value);
            final String host = url.getHost().toLowerCase(Locale.ROOT);
            if (!isGoogleHost(host) || !"/url".equals(url.getPath())) {
                return value;
            }
            final String destination = getQueryValue(url, "url");
            return destination == null ? value : destination;
        } catch (final Exception ignored) {
            return value;
        }
    }

    private static boolean isGoogleHost(final String host) {
        final int start = host.indexOf("google.");
        if (start < 0 || (start > 0 && host.charAt(start - 1) != '.')) return false;
        return host.substring(start + 7).matches("(?:com|[a-z]{2}|(?:co|com)\\.[a-z]{2})");
    }

    public static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    public static boolean isNullOrEmpty(final Collection<?> values) {
        return values == null || values.isEmpty();
    }

    public static <K, V> boolean isNullOrEmpty(final Map<K, V> values) {
        return values == null || values.isEmpty();
    }

    public static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    @Nonnull
    public static String join(final String delimiter, final String mapJoin,
                              @Nonnull final Map<? extends CharSequence,
                                      ? extends CharSequence> values) {
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + mapJoin + entry.getValue())
                .collect(Collectors.joining(delimiter));
    }

    @Nonnull
    public static String nonEmptyAndNullJoin(final CharSequence delimiter,
                                             final String... values) {
        return Arrays.stream(values)
                .filter(value -> !isNullOrEmpty(value) && !"null".equals(value))
                .collect(Collectors.joining(delimiter));
    }

    @Nonnull
    public static String getStringResultFromRegexArray(@Nonnull final String input,
                                                       @Nonnull final String[] expressions)
            throws Parser.RegexException {
        return getStringResultFromRegexArray(input, expressions, 0);
    }

    @Nonnull
    public static String getStringResultFromRegexArray(@Nonnull final String input,
                                                       @Nonnull final Pattern[] expressions)
            throws Parser.RegexException {
        return getStringResultFromRegexArray(input, expressions, 0);
    }

    @Nonnull
    public static String getStringResultFromRegexArray(@Nonnull final String input,
                                                       @Nonnull final String[] expressions,
                                                       final int group)
            throws Parser.RegexException {
        final Pattern[] compiled = Arrays.stream(expressions)
                .filter(expression -> expression != null)
                .map(Pattern::compile)
                .toArray(Pattern[]::new);
        return getStringResultFromRegexArray(input, compiled, group);
    }

    @Nonnull
    public static String getStringResultFromRegexArray(@Nonnull final String input,
                                                       @Nonnull final Pattern[] expressions,
                                                       final int group)
            throws Parser.RegexException {
        for (final Pattern expression : expressions) {
            if (expression == null) continue;
            final Matcher match = expression.matcher(input);
            if (match.find()) {
                final String result = match.group(group);
                if (result != null) return result;
            }
        }
        throw new Parser.RegexException("No regex matched the input on group " + group);
    }
}
