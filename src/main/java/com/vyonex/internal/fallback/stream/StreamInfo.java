// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Info;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ContentNotSupportedException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.utils.ExtractorHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Extracted metadata and playable sources for one media page. */
public class StreamInfo extends Info {
    public static class StreamExtractException extends ExtractionException {
        public StreamExtractException(final String message) { super(message); }
    }

    private StreamType streamType;
    @Nonnull private List<Image> thumbnails = List.of();
    @Nullable private String textualUploadDate;
    @Nullable private DateWrapper uploadDate;
    private long duration = -1;
    private int ageLimit;
    @Nullable private Description description;
    private long viewCount = -1;
    private long likeCount = -1;
    private long dislikeCount = -1;
    private String uploaderName = "";
    private String uploaderUrl = "";
    @Nonnull private List<Image> uploaderAvatars = List.of();
    private boolean uploaderVerified;
    private long uploaderSubscriberCount = -1;
    private String subChannelName = "";
    private String subChannelUrl = "";
    @Nonnull private List<Image> subChannelAvatars = List.of();
    @Nullable private List<VideoStream> videoStreams = List.of();
    @Nullable private List<AudioStream> audioStreams = List.of();
    @Nullable private List<VideoStream> videoOnlyStreams = List.of();
    private String dashMpdUrl = "";
    private String hlsUrl = "";
    private List<InfoItem> relatedItems = List.of();
    private long startPosition;
    private List<SubtitlesStream> subtitles = List.of();
    private String host = "";
    @Nullable private StreamExtractor.Privacy privacy;
    private String category = "";
    private String licence = "";
    private String supportInfo = "";
    @Nullable private Locale language;
    private List<String> tags = List.of();
    private List<StreamSegment> streamSegments = List.of();
    private List<MetaInfo> metaInfo = List.of();
    private boolean shortFormContent;
    @Nonnull private ContentAvailability contentAvailability = ContentAvailability.AVAILABLE;
    private List<Frameset> previewFrames = List.of();

    public StreamInfo(final int serviceId, final String url, final String originalUrl,
                      final StreamType streamType, final String id, final String name,
                      final int ageLimit) {
        super(serviceId, id, url, originalUrl, name);
        this.streamType = streamType;
        this.ageLimit = ageLimit;
    }

    @Override public String toString() {
        return "StreamInfo[serviceId=" + getServiceId() + ", url='" + getUrl()
                + "', originalUrl='" + getOriginalUrl() + "', id='" + getId()
                + "', name='" + getName() + "', streamType=" + streamType
                + ", ageLimit=" + ageLimit + ']';
    }

    public static StreamInfo getInfo(final String url) throws IOException, ExtractionException {
        return getInfo(VyonEx.getServiceByUrl(url), url);
    }

    public static StreamInfo getInfo(@Nonnull final StreamingService service, final String url)
            throws IOException, ExtractionException {
        return getInfo(service.getStreamExtractor(url));
    }

    public static StreamInfo getInfo(@Nonnull final StreamExtractor extractor)
            throws ExtractionException, IOException {
        extractor.fetchPage();
        try {
            final StreamInfo info = importantData(extractor);
            extractStreams(info, extractor);
            extractOptionalData(info, extractor);
            return info;
        } catch (final ExtractionException error) {
            final String message = extractor.getErrorMessage();
            if (message == null || message.isEmpty()) throw error;
            throw new ContentNotAvailableException(message, error);
        }
    }

    private static StreamInfo importantData(final StreamExtractor extractor)
            throws ExtractionException {
        final String url = extractor.getUrl();
        final StreamType type = extractor.getStreamType();
        final String id = extractor.getId();
        final String name = extractor.getName();
        final int ageLimit = extractor.getAgeLimit();
        if (type == StreamType.NONE || empty(url) || empty(id) || name == null || ageLimit == -1) {
            throw new ExtractionException("Some important stream information was not given.");
        }
        return new StreamInfo(extractor.getServiceId(), url, extractor.getOriginalUrl(),
                type, id, name, ageLimit);
    }

    private static void extractStreams(final StreamInfo info, final StreamExtractor extractor)
            throws ExtractionException {
        try {
            info.setDashMpdUrl(extractor.getDashMpdUrl());
        } catch (final Exception error) {
            info.addError(new ExtractionException("Couldn't get DASH manifest", error));
        }
        try {
            info.setHlsUrl(extractor.getHlsUrl());
        } catch (final Exception error) {
            info.addError(new ExtractionException("Couldn't get HLS manifest", error));
        }
        try {
            info.setAudioStreams(extractor.getAudioStreams());
        } catch (final ContentNotSupportedException unsupported) {
            throw unsupported;
        } catch (final Exception error) {
            info.addError(new ExtractionException("Couldn't get audio streams", error));
        }
        try {
            info.setVideoStreams(extractor.getVideoStreams());
        } catch (final Exception error) {
            info.addError(new ExtractionException("Couldn't get video streams", error));
        }
        try {
            info.setVideoOnlyStreams(extractor.getVideoOnlyStreams());
        } catch (final Exception error) {
            info.addError(new ExtractionException("Couldn't get video only streams", error));
        }
        if (info.videoStreams.isEmpty() && info.audioStreams.isEmpty()
                && empty(info.dashMpdUrl) && empty(info.hlsUrl)) {
            throw new StreamExtractException(
                    "Could not get any stream. See error variable to get further details.");
        }
    }

    private static void extractOptionalData(final StreamInfo info, final StreamExtractor extractor) {
        capture(info, extractor::getThumbnails, info::setThumbnails);
        capture(info, extractor::getLength, info::setDuration);
        capture(info, extractor::getUploaderName, info::setUploaderName);
        capture(info, extractor::getUploaderUrl, info::setUploaderUrl);
        capture(info, extractor::getUploaderAvatars, info::setUploaderAvatars);
        capture(info, extractor::isUploaderVerified, info::setUploaderVerified);
        capture(info, extractor::getUploaderSubscriberCount, info::setUploaderSubscriberCount);
        capture(info, extractor::getSubChannelName, info::setSubChannelName);
        capture(info, extractor::getSubChannelUrl, info::setSubChannelUrl);
        capture(info, extractor::getSubChannelAvatars, info::setSubChannelAvatars);
        capture(info, extractor::getDescription, info::setDescription);
        capture(info, extractor::getViewCount, info::setViewCount);
        capture(info, extractor::getTextualUploadDate, info::setTextualUploadDate);
        capture(info, extractor::getUploadDate, info::setUploadDate);
        capture(info, extractor::getTimeStamp, info::setStartPosition);
        capture(info, extractor::getLikeCount, info::setLikeCount);
        capture(info, extractor::getDislikeCount, info::setDislikeCount);
        capture(info, extractor::getSubtitlesDefault, info::setSubtitles);
        capture(info, extractor::getHost, info::setHost);
        capture(info, extractor::getPrivacy, info::setPrivacy);
        capture(info, extractor::getCategory, info::setCategory);
        capture(info, extractor::getLicence, info::setLicence);
        capture(info, extractor::getLanguageInfo, info::setLanguageInfo);
        capture(info, extractor::getTags, info::setTags);
        capture(info, extractor::getSupportInfo, info::setSupportInfo);
        capture(info, extractor::getStreamSegments, info::setStreamSegments);
        capture(info, extractor::getMetaInfo, info::setMetaInfo);
        capture(info, extractor::getFrames, info::setPreviewFrames);
        capture(info, extractor::isShortFormContent, info::setShortFormContent);
        capture(info, extractor::getContentAvailability, info::setContentAvailability);
        info.setRelatedItems(ExtractorHelper.getRelatedItemsOrLogError(info, extractor));
    }

    private static <T> void capture(final StreamInfo info, final Extraction<T> extraction,
                                    final Consumer<T> setter) {
        try {
            setter.accept(extraction.get());
        } catch (final Exception error) {
            info.addError(error);
        }
    }

    private static boolean empty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }

    @FunctionalInterface
    private interface Extraction<T> { T get() throws Exception; }

    public StreamType getStreamType() { return streamType; }
    public void setStreamType(final StreamType value) { streamType = value; }
    @Nonnull public List<Image> getThumbnails() { return thumbnails; }
    public void setThumbnails(@Nonnull final List<Image> value) { thumbnails = value; }
    @Nullable public String getTextualUploadDate() { return textualUploadDate; }
    public void setTextualUploadDate(@Nullable final String value) { textualUploadDate = value; }
    @Nullable public DateWrapper getUploadDate() { return uploadDate; }
    public void setUploadDate(@Nullable final DateWrapper value) { uploadDate = value; }
    public long getDuration() { return duration; }
    public void setDuration(final long value) { duration = value; }
    public int getAgeLimit() { return ageLimit; }
    public void setAgeLimit(final int value) { ageLimit = value; }
    @Nullable public Description getDescription() { return description; }
    public void setDescription(@Nullable final Description value) { description = value; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(final long value) { viewCount = value; }
    public long getLikeCount() { return likeCount; }
    public void setLikeCount(final long value) { likeCount = value; }
    public long getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(final long value) { dislikeCount = value; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(final String value) { uploaderName = value; }
    public String getUploaderUrl() { return uploaderUrl; }
    public void setUploaderUrl(final String value) { uploaderUrl = value; }
    @Nonnull public List<Image> getUploaderAvatars() { return uploaderAvatars; }
    public void setUploaderAvatars(@Nonnull final List<Image> value) { uploaderAvatars = value; }
    public boolean isUploaderVerified() { return uploaderVerified; }
    public void setUploaderVerified(final boolean value) { uploaderVerified = value; }
    public long getUploaderSubscriberCount() { return uploaderSubscriberCount; }
    public void setUploaderSubscriberCount(final long value) { uploaderSubscriberCount = value; }
    public String getSubChannelName() { return subChannelName; }
    public void setSubChannelName(final String value) { subChannelName = value; }
    public String getSubChannelUrl() { return subChannelUrl; }
    public void setSubChannelUrl(final String value) { subChannelUrl = value; }
    @Nonnull public List<Image> getSubChannelAvatars() { return subChannelAvatars; }
    public void setSubChannelAvatars(@Nonnull final List<Image> value) {
        subChannelAvatars = value;
    }
    @Nullable public List<VideoStream> getVideoStreams() { return videoStreams; }
    public void setVideoStreams(@Nullable final List<VideoStream> value) { videoStreams = value; }
    @Nullable public List<AudioStream> getAudioStreams() { return audioStreams; }
    public void setAudioStreams(@Nullable final List<AudioStream> value) { audioStreams = value; }
    @Nullable public List<VideoStream> getVideoOnlyStreams() { return videoOnlyStreams; }
    public void setVideoOnlyStreams(@Nullable final List<VideoStream> value) {
        videoOnlyStreams = value;
    }
    public String getDashMpdUrl() { return dashMpdUrl; }
    public void setDashMpdUrl(final String value) { dashMpdUrl = value; }
    public String getHlsUrl() { return hlsUrl; }
    public void setHlsUrl(final String value) { hlsUrl = value; }
    public List<InfoItem> getRelatedItems() { return relatedItems; }
    @Deprecated public List<InfoItem> getRelatedStreams() { return getRelatedItems(); }
    public void setRelatedItems(final List<InfoItem> value) { relatedItems = value; }
    @Deprecated public void setRelatedStreams(final List<InfoItem> value) {
        setRelatedItems(value);
    }
    public long getStartPosition() { return startPosition; }
    public void setStartPosition(final long value) { startPosition = value; }
    public List<SubtitlesStream> getSubtitles() { return subtitles; }
    public void setSubtitles(final List<SubtitlesStream> value) { subtitles = value; }
    public String getHost() { return host; }
    public void setHost(final String value) { host = value; }
    @Nullable public StreamExtractor.Privacy getPrivacy() { return privacy; }
    public void setPrivacy(@Nullable final StreamExtractor.Privacy value) { privacy = value; }
    public String getCategory() { return category; }
    public void setCategory(final String value) { category = value; }
    public String getLicence() { return licence; }
    public void setLicence(final String value) { licence = value; }
    @Nullable public Locale getLanguageInfo() { return language; }
    public void setLanguageInfo(@Nullable final Locale value) { language = value; }
    public List<String> getTags() { return tags; }
    public void setTags(final List<String> value) { tags = value; }
    public String getSupportInfo() { return supportInfo; }
    public void setSupportInfo(final String value) { supportInfo = value; }
    public List<StreamSegment> getStreamSegments() { return streamSegments; }
    public void setStreamSegments(final List<StreamSegment> value) { streamSegments = value; }
    public void setMetaInfo(final List<MetaInfo> value) { metaInfo = value; }
    public List<Frameset> getPreviewFrames() { return previewFrames; }
    public void setPreviewFrames(final List<Frameset> value) { previewFrames = value; }
    @Nonnull public List<MetaInfo> getMetaInfo() { return metaInfo; }
    public boolean isShortFormContent() { return shortFormContent; }
    public void setShortFormContent(final boolean value) { shortFormContent = value; }
    @Nonnull public ContentAvailability getContentAvailability() { return contentAvailability; }
    public void setContentAvailability(@Nonnull final ContentAvailability value) {
        contentAvailability = value;
    }
}
