// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.exceptions;

public class SoundCloudGoPlusContentException extends ContentNotAvailableException {
    public SoundCloudGoPlusContentException() {
        super("This track is a SoundCloud Go+ track");
    }

    public SoundCloudGoPlusContentException(final Throwable cause) {
        super("This track is a SoundCloud Go+ track", cause);
    }
}
