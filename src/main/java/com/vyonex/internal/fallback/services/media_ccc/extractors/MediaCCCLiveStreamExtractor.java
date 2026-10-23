// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.DeliveryMethod;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.Stream;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.vyonex.internal.fallback.stream.AudioStream.UNKNOWN_BITRATE;
import static com.vyonex.internal.fallback.stream.Stream.ID_UNKNOWN;
import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getThumbnailsFromLiveStreamItem;

public class MediaCCCLiveStreamExtractor extends StreamExtractor {
    @Nullable private LiveRoom room;

    public MediaCCCLiveStreamExtractor(final StreamingService service,
                                       final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String target = getId();
        for (final JsonObject conference : MediaCCCParsingHelper.getLiveStreams(
                downloader, getExtractorLocalization()).streamAsJsonObjects().toList()) {
            final String slug = conference.getString("slug");
            for (final JsonObject section : conference.getArray("groups")
                    .streamAsJsonObjects().toList()) {
                for (final JsonObject source : section.getArray("rooms")
                        .streamAsJsonObjects().toList()) {
                    if (!(slug + "/" + source.getString("slug")).equals(target)) continue;
                    room = readRoom(conference, section, source);
                    return;
                }
            }
        }
        throw new ExtractionException("No live room matches " + target);
    }

    private static LiveRoom readRoom(final JsonObject conference, final JsonObject section,
                                    final JsonObject source) {
        final List<LiveStream> streams = new ArrayList<>();
        for (final JsonObject stream : source.getArray("streams").streamAsJsonObjects().toList()) {
            final JsonArray size = stream.getArray("videoSize");
            final Map<String, MediaSource> formats = new LinkedHashMap<>();
            for (final Map.Entry<String, Object> entry : stream.getObject("urls").entrySet()) {
                if (!(entry.getValue() instanceof JsonObject)) continue;
                final JsonObject format = (JsonObject) entry.getValue();
                formats.put(entry.getKey(), new MediaSource(
                        format.getString("tech", ID_UNKNOWN), format.getString("url")));
            }
            streams.add(new LiveStream(stream.getString("type", ""), size.getInt(0),
                    size.getInt(1), Collections.unmodifiableMap(formats)));
        }
        return new LiveRoom(source.getString("display"), conference.getString("conference"),
                conference.getString("slug"),
                conference.getString("description"), section.getString("group", ""),
                getThumbnailsFromLiveStreamItem(source), List.copyOf(streams));
    }

    @Nonnull @Override public String getName() throws ParsingException { return room.display(); }
    @Nonnull @Override public List<Image> getThumbnails() { return room.thumbnails(); }
    @Nonnull @Override public Description getDescription() {
        return new Description(room.conferenceDescription() + " - " + room.group(),
                Description.PLAIN_TEXT);
    }
    @Override public long getViewCount() { return -1; }
    @Nonnull @Override public String getUploaderUrl() throws ParsingException {
        return "https://streaming.media.ccc.de/" + room.conferenceSlug();
    }
    @Nonnull @Override public String getUploaderName() throws ParsingException {
        return room.conferenceName();
    }
    @Nonnull @Override public String getDashMpdUrl() { return manifest("dash"); }
    @Nonnull @Override public String getHlsUrl() { return manifest("hls"); }

    private String manifest(final String format) {
        for (final LiveStream stream : room.streams()) {
            final MediaSource source = stream.formats().get(format);
            if (source != null) return source.url() == null ? "" : source.url();
        }
        return "";
    }

    @Override public List<AudioStream> getAudioStreams() {
        final List<AudioStream> result = new ArrayList<>();
        for (final LiveStream stream : room.streams()) {
            if (!"audio".equals(stream.type())) continue;
            for (final Map.Entry<String, MediaSource> entry : stream.formats().entrySet()) {
                if ("dash".equals(entry.getKey())) continue;
                result.add(audio(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    private static AudioStream audio(final String format, final MediaSource source) {
        final AudioStream.Builder audio = new AudioStream.Builder()
                .setId(source.id())
                .setContent(source.url(), true)
                .setAverageBitrate(UNKNOWN_BITRATE);
        if ("hls".equals(format)) audio.setDeliveryMethod(DeliveryMethod.HLS);
        else audio.setMediaFormat(MediaFormat.getFromSuffix(format));
        return audio.build();
    }

    @Override public List<VideoStream> getVideoStreams() {
        final List<VideoStream> result = new ArrayList<>();
        for (final LiveStream stream : room.streams()) {
            if (!"video".equals(stream.type())) continue;
            for (final Map.Entry<String, MediaSource> entry : stream.formats().entrySet()) {
                if ("dash".equals(entry.getKey())) continue;
                result.add(video(stream, entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    private static VideoStream video(final LiveStream stream, final String format,
                                     final MediaSource source) {
        final VideoStream.Builder video = new VideoStream.Builder()
                .setId(source.id())
                .setContent(source.url(), true)
                .setIsVideoOnly(false)
                .setResolution(stream.width() + "x" + stream.height());
        if ("hls".equals(format)) video.setDeliveryMethod(DeliveryMethod.HLS);
        else video.setMediaFormat(MediaFormat.getFromSuffix(format));
        return video.build();
    }

    @Override public List<VideoStream> getVideoOnlyStreams() { return List.of(); }
    @Override public StreamType getStreamType() { return StreamType.LIVE_STREAM; }
    @Nonnull @Override public String getCategory() { return room.group(); }

    private record LiveRoom(@Nullable String display, @Nullable String conferenceName,
                            @Nullable String conferenceSlug,
                            @Nullable String conferenceDescription, String group,
                            List<Image> thumbnails, List<LiveStream> streams) { }

    private record LiveStream(String type, int width, int height,
                              Map<String, MediaSource> formats) { }

    private record MediaSource(String id, @Nullable String url) { }
}
