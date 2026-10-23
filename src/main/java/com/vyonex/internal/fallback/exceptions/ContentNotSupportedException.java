// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.exceptions;

public class ContentNotSupportedException extends ParsingException {
    public ContentNotSupportedException(final String message) {
        super(message);
    }

    public ContentNotSupportedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
