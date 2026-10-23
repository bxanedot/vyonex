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
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.DeliveryMethod;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.Stream;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

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
    private List<Transcoding> transcodings = List.of();
    private boolean available = true;

    public SoundcloudStreamExtractor(final StreamingService service, final LinkHandler handler) {
        super(service, handler);
    }

    @Override public void onFetchPage(final Downloader downloader)
            throws IOException, ExtractionException {
        final JsonObject track = SoundcloudParsingHelper.resolveFor(downloader, getUrl());
        final String policy = track.getString("policy", "");
        if (!"ALLOW".equals(policy) && !"MONETIZE".equals(policy)) {
            available = false;
            if ("SNIP".equals(policy)) throw new SoundCloudGoPlusContentException();
            if ("BLOCK".equals(policy)) {
                throw new GeographicRestrictionException("Track is unavailable in this region");
            }
            throw new ContentNotAvailableException("Track is unavailable (policy: " + policy + ")");
        }
        available = true;

        final JsonObject user = track.getObject("user");
        id = String.valueOf(track.get("id"));
        name = track.getString("title", "");
        uploadDate = track.getString("created_at");
        artworkUrl = track.getString("artwork_url");
        if (artworkUrl == null) artworkUrl = user.getString("avatar_url");
        description = track.getString("description", "");
        length = track.getLong("duration") / 1000;
        viewCount = track.getLong("playback_count");
        likeCount = track.getLong("likes_count", -1);
        uploaderUrl = SoundcloudParsingHelper.toHttpsUrl(user.getString("permalink_url", ""));
        uploaderName = user.getString("username", "");
        uploaderAvatarUrl = SoundcloudParsingHelper.toHttpsUrl(user.getString("avatar_url", ""));
        uploaderVerified = user.getBoolean("verified");
        streamable = track.getBoolean("streamable");
        trackAuthorization = track.getString("track_authorization");
        sharing = track.getString("sharing", "");
        category = track.getString("genre", "");
        licence = track.getString("license", "");
        tagList = track.getString("tag_list", "");

        final List<Transcoding> availableTranscodings = new ArrayList<>();
        for (final Object value : track.getObject("media").getArray("transcodings")) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject transcoding = (JsonObject) value;
            availableTranscodings.add(new Transcoding(transcoding.getString("url"),
                    transcoding.getObject("format").getString("protocol", ""),
                    transcoding.getString("preset", Stream.ID_UNKNOWN)));
        }
        transcodings = List.copyOf(availableTranscodings);
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
        for (final Transcoding transcoding : transcodings) {
            final String endpoint = transcoding.endpoint();
            final String protocol = transcoding.protocol();
            if (endpoint == null || endpoint.isBlank() || protocol.contains("encrypted")) continue;
            final String preset = transcoding.preset();
            final MediaFormat format = formatFor(preset);
            if (format == null) continue;
            try {
                final AudioStream.Builder builder = new AudioStream.Builder()
                        .setId(preset).setMediaFormat(format)
                        .setContent(getTranscodingUrl(endpoint, trackAuthorization), true);
                if ("hls".equals(protocol)) builder.setDeliveryMethod(DeliveryMethod.HLS);
                if (format == MediaFormat.MP3) builder.setAverageBitrate(128);
                else if (format == MediaFormat.OPUS) builder.setAverageBitrate(64);
                else if (format == MediaFormat.M4A) builder.setAverageBitrate(160);
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

    private static MediaFormat formatFor(final String preset) {
        if (preset.contains("mp3")) return MediaFormat.MP3;
        if (preset.contains("opus")) return MediaFormat.OPUS;
        if (preset.contains("aac_160k")) return MediaFormat.M4A;
        return null;
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

    private record Transcoding(String endpoint, String protocol, String preset) { }

    static List<String> parseTags(final String input) {
        if (input == null || input.isBlank()) return List.of();
        final List<String> tags = new ArrayList<>();
        final StringBuilder tag = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()
                    && (input.charAt(i + 1) == '"' || input.charAt(i + 1) == '\\')) {
                tag.append(input.charAt(++i));
                continue;
            }
            if (c == '"') {
                quoted = !quoted;
            } else if (Character.isWhitespace(c) && !quoted) {
                if (!tag.isEmpty()) { tags.add(tag.toString()); tag.setLength(0); }
            } else {
                tag.append(c);
            }
        }
        if (!tag.isEmpty()) tags.add(tag.toString());
        return List.copyOf(tags);
    }
}
