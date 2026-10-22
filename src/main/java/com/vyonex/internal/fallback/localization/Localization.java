// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.localization;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.utils.LocaleCompat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Localization implements Serializable {
    public static final Localization DEFAULT = new Localization("en", "GB");

    @Nonnull
    private final String languageCode;
    @Nullable
    private final String countryCode;

    @Nonnull
    public static List<Localization> listFrom(final String... codes) {
        final List<Localization> result = new ArrayList<>(codes.length);
        for (final String code : codes) {
            result.add(fromLocalizationCode(code).orElseThrow(
                    () -> new IllegalArgumentException("Not a localization code: " + code)));
        }
        return Collections.unmodifiableList(result);
    }

    @Nonnull
    public static Optional<Localization> fromLocalizationCode(final String code) {
        return LocaleCompat.forLanguageTag(code).map(Localization::fromLocale);
    }

    public Localization(@Nonnull final String languageCode, @Nullable final String countryCode) {
        this.languageCode = languageCode;
        this.countryCode = countryCode;
    }

    public Localization(@Nonnull final String languageCode) {
        this(languageCode, null);
    }

    @Nonnull
    public String getLanguageCode() { return languageCode; }

    @Nonnull
    public String getCountryCode() { return countryCode == null ? "" : countryCode; }

    public static Localization fromLocale(@Nonnull final Locale locale) {
        return new Localization(locale.getLanguage(), locale.getCountry());
    }

    public String getLocalizationCode() {
        return languageCode + (countryCode == null ? "" : "-" + countryCode);
    }

    @Override
    public String toString() {
        return "Localization[" + getLocalizationCode() + "]";
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof Localization that)) return false;
        return languageCode.equals(that.languageCode) && Objects.equals(countryCode, that.countryCode);
    }

    @Override
    public int hashCode() {
        int result = languageCode.hashCode();
        result = 31 * result + Objects.hashCode(countryCode);
        return result;
    }

    @Nonnull
    public static Locale getLocaleFromThreeLetterCode(@Nonnull final String code)
            throws ParsingException {
        for (final String language : Locale.getISOLanguages()) {
            final Locale locale = Locale.forLanguageTag(language);
            if (locale.getISO3Language().equals(code)) return locale;
        }
        throw new ParsingException("Could not get Locale from this three letter language code" + code);
    }
}
