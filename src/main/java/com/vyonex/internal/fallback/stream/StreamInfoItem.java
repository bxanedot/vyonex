// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.localization.DateWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Metadata for a stream shown in a list. */
public class StreamInfoItem extends InfoItem {
    private final StreamType streamType;
    private String uploaderName;
    private String shortDescription;
    private String textualUploadDate;
    @Nullable private DateWrapper uploadDate;
    private long viewCount = -1;
    private long duration = -1;
    private String uploaderUrl;
    @Nonnull private List<Image> uploaderAvatars = List.of();
    private boolean uploaderVerified;
    private boolean shortFormContent;
    @Nonnull private ContentAvailability contentAvailability = ContentAvailability.AVAILABLE;

    public StreamInfoItem(final int serviceId, final String url, final String name,
                          final StreamType streamType) {
        super(InfoType.STREAM, serviceId, url, name);
        this.streamType = streamType;
    }

    public StreamType getStreamType() { return streamType; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(final String uploaderName) { this.uploaderName = uploaderName; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(final long viewCount) { this.viewCount = viewCount; }
    public long getDuration() { return duration; }
    public void setDuration(final long duration) { this.duration = duration; }
    public String getUploaderUrl() { return uploaderUrl; }
    public void setUploaderUrl(final String uploaderUrl) { this.uploaderUrl = uploaderUrl; }

    @Nonnull
    public List<Image> getUploaderAvatars() { return uploaderAvatars; }

    public void setUploaderAvatars(@Nonnull final List<Image> uploaderAvatars) {
        this.uploaderAvatars = uploaderAvatars;
    }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(final String shortDescription) {
        this.shortDescription = shortDescription;
    }
    @Nullable public String getTextualUploadDate() { return textualUploadDate; }
    public void setTextualUploadDate(final String textualUploadDate) {
        this.textualUploadDate = textualUploadDate;
    }
    @Nullable public DateWrapper getUploadDate() { return uploadDate; }
    public void setUploadDate(@Nullable final DateWrapper uploadDate) {
        this.uploadDate = uploadDate;
    }
    public boolean isUploaderVerified() { return uploaderVerified; }
    public void setUploaderVerified(final boolean uploaderVerified) {
        this.uploaderVerified = uploaderVerified;
    }
    public boolean isShortFormContent() { return shortFormContent; }
    public void setShortFormContent(final boolean shortFormContent) {
        this.shortFormContent = shortFormContent;
    }

    @Nonnull
    public ContentAvailability getContentAvailability() { return contentAvailability; }

    public void setContentAvailability(@Nonnull final ContentAvailability availability) {
        this.contentAvailability = availability;
    }

    @Override
    public String toString() {
        return "StreamInfoItem{"
                + "streamType=" + streamType
                + ", uploaderName='" + uploaderName + '\''
                + ", textualUploadDate='" + textualUploadDate + '\''
                + ", viewCount=" + viewCount
                + ", duration=" + duration
                + ", uploaderUrl='" + uploaderUrl + '\''
                + ", infoType=" + getInfoType()
                + ", serviceId=" + getServiceId()
                + ", url='" + getUrl() + '\''
                + ", name='" + getName() + '\''
                + ", thumbnails='" + getThumbnails() + '\''
                + ", uploaderVerified='" + isUploaderVerified() + '\''
                + '}';
    }
}
