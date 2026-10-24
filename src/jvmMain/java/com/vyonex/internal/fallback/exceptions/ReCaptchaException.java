// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.exceptions;

public class ReCaptchaException extends ExtractionException {
    private final String url;

    public ReCaptchaException(final String message, final String url) {
        super(message);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
