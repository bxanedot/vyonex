// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.localization;

import com.vyonex.internal.fallback.exceptions.ParsingException;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DateWrapper implements Serializable {
    @Nonnull
    private final Instant instant;
    private final boolean isApproximation;

    public DateWrapper(@Nonnull final OffsetDateTime dateTime) {
        this(dateTime, false);
    }

    public DateWrapper(@Nonnull final OffsetDateTime dateTime, final boolean approximation) {
        this(dateTime.toInstant(), approximation);
    }

    public DateWrapper(@Nonnull final Instant instant) {
        this(instant, false);
    }

    public DateWrapper(@Nonnull final Instant instant, final boolean approximation) {
        this.instant = instant;
        this.isApproximation = approximation;
    }

    public DateWrapper(@Nonnull final LocalDateTime dateTime, final boolean approximation) {
        this(dateTime.atZone(ZoneId.systemDefault()).toInstant(), approximation);
    }

    @Nonnull
    public Instant getInstant() {
        return instant;
    }

    @Nonnull
    public OffsetDateTime offsetDateTime() {
        return instant.atOffset(ZoneOffset.UTC);
    }

    @Nonnull
    public LocalDateTime getLocalDateTime() {
        return getLocalDateTime(ZoneId.systemDefault());
    }

    @Nonnull
    public LocalDateTime getLocalDateTime(@Nonnull final ZoneId zoneId) {
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    public boolean isApproximation() {
        return isApproximation;
    }

    @Override
    public String toString() {
        return "DateWrapper{" + "instant=" + instant + ", isApproximation=" + isApproximation + '}';
    }

    @Nullable
    public static DateWrapper fromOffsetDateTime(@Nullable final String value) throws ParsingException {
        try {
            return value == null ? null : new DateWrapper(OffsetDateTime.parse(value));
        } catch (final DateTimeParseException e) {
            throw new ParsingException("Could not parse date: \"" + value + "\"", e);
        }
    }

    @Nullable
    public static DateWrapper fromInstant(@Nullable final String value) throws ParsingException {
        try {
            return value == null ? null : new DateWrapper(Instant.parse(value));
        } catch (final DateTimeParseException e) {
            throw new ParsingException("Could not parse date: \"" + value + "\"", e);
        }
    }
}
