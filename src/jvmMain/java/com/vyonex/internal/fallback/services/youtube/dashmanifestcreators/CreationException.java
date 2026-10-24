// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.dashmanifestcreators;

import javax.annotation.Nonnull;

public final class CreationException extends RuntimeException {
    public CreationException(final String message) {
        super(message);
    }

    public CreationException(final String message, final Exception cause) {
        super(message, cause);
    }

    @Nonnull
    public static CreationException couldNotAddElement(final String element,
                                                       final Exception cause) {
        return new CreationException("Could not add " + element + " element", cause);
    }

    @Nonnull
    public static CreationException couldNotAddElement(final String element,
                                                       final String reason) {
        return new CreationException("Could not add " + element + " element: " + reason);
    }
}
