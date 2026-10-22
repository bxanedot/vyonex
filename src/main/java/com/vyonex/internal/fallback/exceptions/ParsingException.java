// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.exceptions;

public class ParsingException extends ExtractionException {
    public ParsingException(final String message) {
        super(message);
    }

    public ParsingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
