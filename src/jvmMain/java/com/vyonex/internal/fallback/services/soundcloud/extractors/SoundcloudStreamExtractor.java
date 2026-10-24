// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.GeographicRestrictionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.SoundCloudGoPlusContentException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudAudioCandidate;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudAudioFormat;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudTagParser;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudTrackPayload;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudTrackPayloadParser;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudTranscoding;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudTranscodingMapper;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.DeliveryMethod;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.Stream;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import com.vyonex.internal.json.JsonWriter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SoundcloudStreamExtractor extends StreamExtractor {
    private String id;
    private String name;
    private String uploadDate;
    private String artworkUrl;
    private String description;
    private long length;
    private long viewCount;
    private long likeCount;
    private String uploaderUrl;
    private String uploaderName;
    private String uploaderAvatarUrl;
    private boolean uploaderVerified;
    private boolean streamable;
    private String trackAuthorization;
    private String sharing;
    private String category;
    private String licence;
    private String tagList;
    private List<SoundcloudTranscoding> transcodings = List.of();
    private boolean available = true;

    public SoundcloudStreamExtractor(final StreamingService service, final LinkHandler handler) {
        super(service, handler);
    }

    @Override public void onFetchPage(final Downloader downloader)
            throws IOException, ExtractionException {
        final SoundcloudTrackPayload track = SoundcloudTrackPayloadParser.INSTANCE.parse(
                JsonWriter.string(SoundcloudParsingHelper.resolveFor(downloader, getUrl())));
        final String policy = track.getPolicy();
        if (!"ALLOW".equals(policy) && !"MONETIZE".equals(policy)) {
            available = false;
            if ("SNIP".equals(policy)) throw new SoundCloudGoPlusContentException();
            if ("BLOCK".equals(policy)) {
                throw new GeographicRestrictionException("Track is unavailable in this region");
            }
            throw new ContentNotAvailableException("Track is unavailable (policy: " + policy + ")");
        }
        available = true;

        id = track.getId();
        name = track.getTitle();
        uploadDate = track.getCreatedAt();
        artworkUrl = track.getArtworkUrl();
        description = track.getDescription();
        length = track.getDurationMillis() / 1000;
        viewCount = track.getPlaybackCount();
        likeCount = track.getLikesCount();
        uploaderUrl = SoundcloudParsingHelper.toHttpsUrl(track.getUserPermalinkUrl());
        uploaderName = track.getUserName();
        uploaderAvatarUrl = SoundcloudParsingHelper.toHttpsUrl(track.getUserAvatarUrl());
        uploaderVerified = track.getUserVerified();
        streamable = track.getStreamable();
        trackAuthorization = track.getTrackAuthorization();
        sharing = track.getSharing();
        category = track.getGenre();
        licence = track.getLicense();
        tagList = track.getTagList();
        transcodings = List.copyOf(track.getTranscodings());
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getTextualUploadDate() { return uploadDate; }
    @Override public DateWrapper getUploadDate() throws ParsingException {
        return SoundcloudParsingHelper.parseDate(getTextualUploadDate());
    }
    @Override public List<Image> getThumbnails() throws ParsingException {
        if (artworkUrl == null || artworkUrl.isBlank()) throw new ParsingException("Track has no artwork");
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(artworkUrl);
    }
    @Override public Description getDescription() {
        return new Description(description, Description.PLAIN_TEXT);
    }
    @Override public long getLength() { return length; }
    @Override public long getTimeStamp() throws ParsingException {
        final long seconds = getTimestampSeconds(
                "(#t=(?=\\d)(?:\\d+h)?(?:\\d+m)?\\d*s?(?![\\dA-Za-z]))");
        return seconds < 0 ? 0 : seconds;
    }
    @Override public long getViewCount() { return viewCount; }
    @Override public long getLikeCount() { return likeCount; }
    @Override public String getUploaderUrl() { return uploaderUrl; }
    @Override public String getUploaderName() { return uploaderName; }
    @Override public boolean isUploaderVerified() { return uploaderVerified; }
    @Override public List<Image> getUploaderAvatars() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(
                uploaderAvatarUrl);
    }

    @Override public List<AudioStream> getAudioStreams() throws IOException, ExtractionException {
        final List<AudioStream> streams = new ArrayList<>();
        if (!available || !streamable) return streams;
        for (final SoundcloudAudioCandidate candidate :
                SoundcloudTranscodingMapper.INSTANCE.map(streamable, transcodings)) {
            final String endpoint = candidate.getEndpoint();
            final String preset = candidate.getPreset();
            final MediaFormat format = switch (candidate.getFormat()) {
                case MP3 -> MediaFormat.MP3;
                case OPUS -> MediaFormat.OPUS;
                case M4A -> MediaFormat.M4A;
            };
            try {
                final AudioStream.Builder builder = new AudioStream.Builder()
                        .setId(preset).setMediaFormat(format)
                        .setContent(getTranscodingUrl(endpoint, trackAuthorization), true);
                if (candidate.getHls()) builder.setDeliveryMethod(DeliveryMethod.HLS);
                builder.setAverageBitrate(candidate.getFormat().getBitrate());
                final AudioStream stream = builder.build();
                if (!Stream.containSimilarStream(stream, streams)) streams.add(stream);
            } catch (final IOException | ExtractionException ignored) {
                // A broken transcoding should not hide the other available formats.
            }
        }
        return streams;
    }

    private String getTranscodingUrl(final String endpoint, final String authorization)
            throws IOException, ExtractionException {
        final String separator = endpoint.contains("?") ? "&" : "?";
        String requestUrl = endpoint + separator + "client_id=" + SoundcloudParsingHelper.clientId();
        if (authorization != null && !authorization.isBlank()) {
            requestUrl += "&track_authorization="
                    + URLEncoder.encode(authorization, StandardCharsets.UTF_8);
        }
        final Response response = VyonEx.getDownloader().get(requestUrl);
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud stream URL request failed with HTTP "
                    + response.responseCode());
        }
        try {
            final String url = JsonParser.object().from(response.responseBody()).getString("url");
            if (url == null || url.isBlank()) throw new ParsingException("Stream response has no URL");
            return url;
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid SoundCloud stream URL response", e);
        }
    }

    @Override public List<VideoStream> getVideoStreams() { return List.of(); }
    @Override public List<VideoStream> getVideoOnlyStreams() { return List.of(); }
    @Override public StreamType getStreamType() { return StreamType.AUDIO_STREAM; }
    @Override public StreamInfoItemsCollector getRelatedItems() throws IOException, ExtractionException {
        final StreamInfoItemsCollector related = new StreamInfoItemsCollector(getServiceId());
        final String url = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL + "tracks/"
                + URLEncoder.encode(getId(), StandardCharsets.UTF_8) + "/related?client_id="
                + URLEncoder.encode(SoundcloudParsingHelper.clientId(), StandardCharsets.UTF_8);
        SoundcloudParsingHelper.getStreamsFromApi(related, url);
        return related;
    }
    @Override public Privacy getPrivacy() {
        return switch (sharing) {
            case "public" -> Privacy.PUBLIC;
            case "private" -> Privacy.PRIVATE;
            case "unlisted" -> Privacy.UNLISTED;
            default -> Privacy.OTHER;
        };
    }
    @Override public String getCategory() { return category; }
    @Override public String getLicence() { return licence; }
    @Override public List<String> getTags() { return parseTags(tagList); }

    static List<String> parseTags(final String input) {
        return List.copyOf(SoundcloudTagParser.INSTANCE.parse(input));
    }
}
