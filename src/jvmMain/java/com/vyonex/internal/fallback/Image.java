// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

public final class Image implements Serializable {
    public static final int HEIGHT_UNKNOWN = -1;
    public static final int WIDTH_UNKNOWN = -1;

    @Nonnull private final String url;
    private final int height;
    private final int width;
    @Nonnull private final ResolutionLevel estimatedResolutionLevel;

    public Image(@Nonnull final String url, final int height, final int width,
                 @Nonnull final ResolutionLevel estimatedResolutionLevel) {
        this.url = Objects.requireNonNull(url, "url is null");
        this.height = height;
        this.width = width;
        this.estimatedResolutionLevel = Objects.requireNonNull(
                estimatedResolutionLevel, "estimatedResolutionLevel is null");
    }

    @Nonnull public String getUrl() { return url; }
    public int getHeight() { return height; }
    public int getWidth() { return width; }
    @Nonnull public ResolutionLevel getEstimatedResolutionLevel() {
        return estimatedResolutionLevel;
    }

    @Nonnull
    @Override
    public String toString() {
        return "Image {url=" + url + ", height=" + height + ", width=" + width
                + ", estimatedResolutionLevel=" + estimatedResolutionLevel + "}";
    }

    public enum ResolutionLevel {
        HIGH,
        MEDIUM,
        LOW,
        UNKNOWN;

        public static ResolutionLevel fromHeight(final int heightPx) {
            if (heightPx <= 0) return UNKNOWN;
            if (heightPx < 175) return LOW;
            if (heightPx < 720) return MEDIUM;
            return HIGH;
        }
    }
}
