// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.exceptions;

public class AccountTerminatedException extends ContentNotAvailableException {
    private final Reason reason;

    public AccountTerminatedException(final String message) {
        this(message, Reason.UNKNOWN);
    }

    public AccountTerminatedException(final String message, final Reason reason) {
        super(message);
        this.reason = reason;
    }

    public AccountTerminatedException(final String message, final Throwable cause) {
        super(message, cause);
        this.reason = Reason.UNKNOWN;
    }

    public Reason getReason() { return reason; }

    public enum Reason {
        UNKNOWN,
        VIOLATION
    }
}
