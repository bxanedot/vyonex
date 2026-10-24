// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.downloader;

import com.vyonex.internal.fallback.localization.Localization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Request {
    private final String httpMethod;
    private final String url;
    private final Map<String, List<String>> headers;
    @Nullable private final byte[] dataToSend;
    @Nullable private final Localization localization;

    public Request(final String httpMethod, final String url,
                   @Nullable final Map<String, List<String>> headers,
                   @Nullable final byte[] dataToSend, @Nullable final Localization localization,
                   final boolean automaticLocalizationHeader) {
        this.httpMethod = Objects.requireNonNull(httpMethod, "Request's httpMethod is null");
        this.url = Objects.requireNonNull(url, "Request's url is null");
        this.dataToSend = dataToSend;
        this.localization = localization;

        final Map<String, List<String>> actualHeaders = copyHeaders(headers);
        if (automaticLocalizationHeader && localization != null) {
            actualHeaders.putAll(getHeadersFromLocalization(localization));
        }
        this.headers = immutableHeaders(actualHeaders);
    }

    private Request(final Builder builder) {
        this(builder.httpMethod, builder.url, builder.headers, builder.dataToSend,
                builder.localization, builder.automaticLocalizationHeader);
    }

    public String httpMethod() { return httpMethod; }
    public String url() { return url; }
    public Map<String, List<String>> headers() { return headers; }
    @Nullable public byte[] dataToSend() { return dataToSend; }
    @Nullable public Localization localization() { return localization; }

    public static Builder newBuilder() { return new Builder(); }

    public static final class Builder {
        private String httpMethod;
        private String url;
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private byte[] dataToSend;
        private Localization localization;
        private boolean automaticLocalizationHeader = true;

        public Builder() { }

        public Builder httpMethod(final String value) {
            httpMethod = value;
            return this;
        }

        public Builder url(final String value) {
            url = value;
            return this;
        }

        public Builder headers(@Nullable final Map<String, List<String>> values) {
            headers.clear();
            headers.putAll(copyHeaders(values));
            return this;
        }

        public Builder dataToSend(@Nullable final byte[] value) {
            dataToSend = value;
            return this;
        }

        public Builder localization(@Nullable final Localization value) {
            localization = value;
            return this;
        }

        public Builder automaticLocalizationHeader(final boolean enabled) {
            automaticLocalizationHeader = enabled;
            return this;
        }

        public Request build() { return new Request(this); }

        public Builder get(final String value) {
            httpMethod = "GET";
            url = value;
            return this;
        }

        public Builder head(final String value) {
            httpMethod = "HEAD";
            url = value;
            return this;
        }

        public Builder post(final String value, @Nullable final byte[] body) {
            httpMethod = "POST";
            url = value;
            dataToSend = body;
            return this;
        }

        public Builder setHeaders(final String name, @Nullable final List<String> values) {
            headers.put(name, copyValues(values));
            return this;
        }

        public Builder addHeaders(final String name, final List<String> values) {
            headers.computeIfAbsent(name, ignored -> new ArrayList<>()).addAll(values);
            return this;
        }

        public Builder setHeader(final String name, final String value) {
            return setHeaders(name, Collections.singletonList(value));
        }

        public Builder addHeader(final String name, final String value) {
            return addHeaders(name, Collections.singletonList(value));
        }
    }

    @Nonnull
    public static Map<String, List<String>> getHeadersFromLocalization(
            @Nullable final Localization localization) {
        if (localization == null) return Collections.emptyMap();
        final String language = localization.getLanguageCode();
        final String value = localization.getCountryCode().isEmpty() ? language
                : localization.getLocalizationCode() + ", " + language + ";q=0.9";
        return Collections.singletonMap("Accept-Language", Collections.singletonList(value));
    }

    private static Map<String, List<String>> copyHeaders(
            @Nullable final Map<String, List<String>> source) {
        final Map<String, List<String>> copy = new LinkedHashMap<>();
        if (source != null) {
            for (final Map.Entry<String, List<String>> header : source.entrySet()) {
                copy.put(header.getKey(), copyValues(header.getValue()));
            }
        }
        return copy;
    }

    @Nullable
    private static List<String> copyValues(@Nullable final List<String> values) {
        return values == null ? null : new ArrayList<>(values);
    }

    private static Map<String, List<String>> immutableHeaders(
            final Map<String, List<String>> source) {
        final Map<String, List<String>> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, List<String>> header : source.entrySet()) {
            final List<String> values = header.getValue();
            copy.put(header.getKey(), values == null ? null
                    : Collections.unmodifiableList(values));
        }
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        final Request request = (Request) other;
        return httpMethod.equals(request.httpMethod) && url.equals(request.url)
                && headers.equals(request.headers) && Arrays.equals(dataToSend, request.dataToSend)
                && Objects.equals(localization, request.localization);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(httpMethod, url, headers, localization);
        result = 31 * result + Arrays.hashCode(dataToSend);
        return result;
    }
}
