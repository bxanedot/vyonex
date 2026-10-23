// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.downloader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Response {
    private final int responseCode;
    @Nullable private final String responseMessage;
    private final Map<String, List<String>> responseHeaders;
    private final String responseBody;
    @Nullable private final String latestUrl;

    public Response(final int responseCode, @Nullable final String responseMessage,
                    @Nullable final Map<String, List<String>> responseHeaders,
                    @Nullable final String responseBody, @Nullable final String latestUrl) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.responseHeaders = immutableHeaders(responseHeaders);
        this.responseBody = responseBody == null ? "" : responseBody;
        this.latestUrl = latestUrl;
    }

    public int responseCode() { return responseCode; }
    @Nullable public String responseMessage() { return responseMessage; }
    public Map<String, List<String>> responseHeaders() { return responseHeaders; }
    @Nonnull public String responseBody() { return responseBody; }
    @Nullable public String latestUrl() { return latestUrl; }

    @Nullable
    public String getHeader(final String name) {
        if (name == null) return null;
        for (final Map.Entry<String, List<String>> header : responseHeaders.entrySet()) {
            final String key = header.getKey();
            final List<String> values = header.getValue();
            if (key != null && key.equalsIgnoreCase(name) && values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }
        return null;
    }

    private static Map<String, List<String>> immutableHeaders(
            @Nullable final Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) return Collections.emptyMap();
        final Map<String, List<String>> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, List<String>> header : headers.entrySet()) {
            final List<String> values = header.getValue();
            copy.put(header.getKey(), values == null ? null
                    : Collections.unmodifiableList(new ArrayList<>(values)));
        }
        return Collections.unmodifiableMap(copy);
    }
}
