// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.channel;

import com.vyonex.internal.fallback.InfoItem;

public class ChannelInfoItem extends InfoItem {
    private String description;
    private long subscriberCount = -1;
    private long streamCount = -1;
    private boolean verified;

    public ChannelInfoItem(final int serviceId, final String url, final String name) {
        super(InfoType.CHANNEL, serviceId, url, name);
    }

    public String getDescription() { return description; }
    public void setDescription(final String description) { this.description = description; }
    public long getSubscriberCount() { return subscriberCount; }
    public void setSubscriberCount(final long subscriberCount) {
        this.subscriberCount = subscriberCount;
    }
    public long getStreamCount() { return streamCount; }
    public void setStreamCount(final long streamCount) { this.streamCount = streamCount; }
    public boolean isVerified() { return verified; }
    public void setVerified(final boolean verified) { this.verified = verified; }
}
