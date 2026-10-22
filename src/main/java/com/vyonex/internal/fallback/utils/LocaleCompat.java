// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import java.util.Locale;
import java.util.Optional;

public final class LocaleCompat {
    private LocaleCompat() { }

    public static Optional<Locale> forLanguageTag(final String tag) {
        if (tag == null || tag.isBlank()) return Optional.empty();
        final Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
        return locale.getLanguage().isEmpty() ? Optional.empty() : Optional.of(locale);
    }
}
