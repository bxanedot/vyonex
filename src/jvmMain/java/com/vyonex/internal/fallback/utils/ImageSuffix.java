// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import com.vyonex.internal.fallback.Image.ResolutionLevel;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnull;

public final class ImageSuffix implements Serializable {
    @Nonnull
    private final String suffix;
    private final int height;
    private final int width;
    @Nonnull
    private final ResolutionLevel resolutionLevel;

    public ImageSuffix(@Nonnull final String suffix, final int height, final int width,
                       @Nonnull final ResolutionLevel estimatedResolutionLevel) {
        this.suffix = suffix;
        this.height = height;
        this.width = width;
        this.resolutionLevel = Objects.requireNonNull(estimatedResolutionLevel,
                "estimatedResolutionLevel is null");
    }

    @Nonnull
    public String getSuffix() { return suffix; }

    public int getHeight() { return height; }

    public int getWidth() { return width; }

    @Nonnull
    public ResolutionLevel getResolutionLevel() { return resolutionLevel; }

    @Nonnull
    @Override
    public String toString() {
        return "ImageSuffix {suffix=" + suffix + ", height=" + height + ", width=" + width
                + ", resolutionLevel=" + resolutionLevel + "}";
    }
}
