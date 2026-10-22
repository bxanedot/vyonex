// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.downloader;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.localization.Localization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Downloader {
    public Response get(final String url) throws IOException, ReCaptchaException {
        return get(url, null, VyonEx.getPreferredLocalization());
    }

    public Response get(final String url, final Localization localization)
            throws IOException, ReCaptchaException {
        return get(url, null, localization);
    }

    public Response get(final String url, @Nullable final Map<String, List<String>> headers)
            throws IOException, ReCaptchaException {
        return get(url, headers, VyonEx.getPreferredLocalization());
    }

    public Response get(final String url, @Nullable final Map<String, List<String>> headers,
                        final Localization localization)
            throws IOException, ReCaptchaException {
        return execute(Request.newBuilder().get(url).headers(headers)
                .localization(localization).build());
    }

    public Response head(final String url) throws IOException, ReCaptchaException {
        return head(url, null);
    }

    public Response head(final String url, @Nullable final Map<String, List<String>> headers)
            throws IOException, ReCaptchaException {
        return execute(Request.newBuilder().head(url).headers(headers).build());
    }

    public Response post(final String url, @Nullable final Map<String, List<String>> headers,
                         @Nullable final byte[] dataToSend)
            throws IOException, ReCaptchaException {
        return post(url, headers, dataToSend, VyonEx.getPreferredLocalization());
    }

    public Response post(final String url, @Nullable final Map<String, List<String>> headers,
                         @Nullable final byte[] dataToSend, final Localization localization)
            throws IOException, ReCaptchaException {
        return execute(Request.newBuilder().post(url, dataToSend).headers(headers)
                .localization(localization).build());
    }

    public Response postWithContentType(final String url,
                                        @Nullable final Map<String, List<String>> headers,
                                        @Nullable final byte[] dataToSend,
                                        final Localization localization,
                                        final String contentType)
            throws IOException, ReCaptchaException {
        final Map<String, List<String>> requestHeaders = new HashMap<>();
        if (headers != null) requestHeaders.putAll(headers);
        requestHeaders.put("Content-Type", Collections.singletonList(contentType));
        return post(url, requestHeaders, dataToSend, localization);
    }

    public Response postWithContentType(final String url,
                                        @Nullable final Map<String, List<String>> headers,
                                        @Nullable final byte[] dataToSend,
                                        final String contentType)
            throws IOException, ReCaptchaException {
        return postWithContentType(url, headers, dataToSend,
                VyonEx.getPreferredLocalization(), contentType);
    }

    public Response postWithContentTypeJson(final String url,
                                            @Nullable final Map<String, List<String>> headers,
                                            @Nullable final byte[] dataToSend,
                                            final Localization localization)
            throws IOException, ReCaptchaException {
        return postWithContentType(url, headers, dataToSend, localization, "application/json");
    }

    public Response postWithContentTypeJson(final String url,
                                            @Nullable final Map<String, List<String>> headers,
                                            @Nullable final byte[] dataToSend)
            throws IOException, ReCaptchaException {
        return postWithContentTypeJson(url, headers, dataToSend,
                VyonEx.getPreferredLocalization());
    }

    public abstract Response execute(@Nonnull Request request)
            throws IOException, ReCaptchaException;

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
