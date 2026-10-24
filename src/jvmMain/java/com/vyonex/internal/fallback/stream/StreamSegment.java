// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import javax.annotation.Nullable;
import java.io.Serializable;

public class StreamSegment implements Serializable {
    private String title;
    private String channelName;
    private int startTimeSeconds;
    @Nullable public String url;
    @Nullable private String previewUrl;

    public StreamSegment(final String title, final int startTimeSeconds) {
        this.title = title;
        this.startTimeSeconds = startTimeSeconds;
    }

    public String getTitle() { return title; }
    public void setTitle(final String title) { this.title = title; }
    public int getStartTimeSeconds() { return startTimeSeconds; }
    public void setStartTimeSeconds(final int seconds) { startTimeSeconds = seconds; }
    @Nullable public String getChannelName() { return channelName; }
    public void setChannelName(@Nullable final String name) { channelName = name; }
    @Nullable public String getUrl() { return url; }
    public void setUrl(@Nullable final String value) { url = value; }
    @Nullable public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(@Nullable final String value) { previewUrl = value; }
}
