// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.json;

public class JsonParserException extends Exception {
    private static final long serialVersionUID = 1L;

    public JsonParserException(final String message, final Throwable cause) { super(message, cause); }
    public JsonParserException(final String message) { super(message); }
}
