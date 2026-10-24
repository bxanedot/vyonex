// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.List;

public abstract class InfoItem implements Serializable {
    private final InfoType infoType;
    private final int serviceId;
    private final String url;
    private final String name;
    @Nonnull private List<Image> thumbnails = List.of();

    public InfoItem(final InfoType infoType, final int serviceId,
                    final String url, final String name) {
        this.infoType = infoType;
        this.serviceId = serviceId;
        this.url = url;
        this.name = name;
    }

    public InfoType getInfoType() { return infoType; }
    public int getServiceId() { return serviceId; }
    public String getUrl() { return url; }
    public String getName() { return name; }

    public void setThumbnails(@Nonnull final List<Image> thumbnails) {
        this.thumbnails = thumbnails;
    }

    @Nonnull
    public List<Image> getThumbnails() { return thumbnails; }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[url=\"" + url + "\", name=\"" + name + "\"]";
    }

    public enum InfoType {
        STREAM,
        PLAYLIST,
        CHANNEL,
        COMMENT
    }
}
