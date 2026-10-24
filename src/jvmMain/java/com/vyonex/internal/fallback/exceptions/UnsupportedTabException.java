// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.exceptions;

public final class UnsupportedTabException extends UnsupportedOperationException {
    public UnsupportedTabException(final String unsupportedTab) {
        super("Unsupported tab " + unsupportedTab);
    }
}
