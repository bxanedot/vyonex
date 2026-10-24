// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.exceptions;

public class YoutubeMusicPremiumContentException extends ContentNotAvailableException {
    public YoutubeMusicPremiumContentException() {
        super("This video is a YouTube Music Premium video");
    }

    public YoutubeMusicPremiumContentException(final Throwable cause) {
        super("This video is a YouTube Music Premium video", cause);
    }
}
