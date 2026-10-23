// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.localization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public class ContentCountry implements Serializable {
    public static final ContentCountry DEFAULT =
            new ContentCountry(Localization.DEFAULT.getCountryCode());

    @Nonnull
    private final String countryCode;

    @Nonnull
    public static List<ContentCountry> listFrom(final String... codes) {
        final List<ContentCountry> result = new ArrayList<>(codes.length);
        for (final String code : codes) result.add(new ContentCountry(code));
        return Collections.unmodifiableList(result);
    }

    public ContentCountry(@Nonnull final String countryCode) {
        this.countryCode = countryCode;
    }

    @Nonnull
    public String getCountryCode() { return countryCode; }

    @Override
    public String toString() { return countryCode; }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof ContentCountry that
                && countryCode.equals(that.countryCode);
    }

    @Override
    public int hashCode() { return countryCode.hashCode(); }
}
