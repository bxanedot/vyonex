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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.vyonex.internal.fallback.stream.AudioStream.UNKNOWN_BITRATE;
import static com.vyonex.internal.fallback.stream.Stream.ID_UNKNOWN;
import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getThumbnailsFromLiveStreamItem;

public class MediaCCCLiveStreamExtractor extends StreamExtractor {
    private JsonObject conference;
    private JsonObject room;
    private String group = "";

    public MediaCCCLiveStreamExtractor(final StreamingService service,
                                       final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final JsonArray conferences = MediaCCCParsingHelper.getLiveStreams(
                downloader, getExtractorLocalization());
        final String target = getId();
        for (final JsonObject candidate : conferences.streamAsJsonObjects().toList()) {
            for (final JsonObject section : candidate.getArray("groups").streamAsJsonObjects().toList()) {
                for (final JsonObject candidateRoom : section.getArray("rooms")
                        .streamAsJsonObjects().toList()) {
                    final String id = candidate.getString("slug") + "/"
                            + candidateRoom.getString("slug");
                    if (!target.equals(id)) continue;
                    conference = candidate;
                    room = candidateRoom;
                    group = section.getString("group", "");
                    return;
                }
            }
        }
        throw new ExtractionException("No live room matches " + target);
    }

    @Nonnull @Override public String getName() throws ParsingException {
        return room.getString("display");
    }
    @Nonnull @Override public List<Image> getThumbnails() {
        return getThumbnailsFromLiveStreamItem(room);
    }
    @Nonnull @Override public Description getDescription() {
        return new Description(conference.getString("description") + " - " + group,
                Description.PLAIN_TEXT);
    }
    @Override public long getViewCount() { return -1; }
    @Nonnull @Override public String getUploaderUrl() throws ParsingException {
        return "https://streaming.media.ccc.de/" + conference.getString("slug");
    }
    @Nonnull @Override public String getUploaderName() throws ParsingException {
        return conference.getString("conference");
    }
    @Nonnull @Override public String getDashMpdUrl() throws ParsingException {
        return manifest("dash");
    }
    @Nonnull @Override public String getHlsUrl() throws ParsingException {
        return manifest("hls");
    }

    private String manifest(final String kind) {
        for (final JsonObject stream : room.getArray("streams").streamAsJsonObjects().toList()) {
            final JsonObject urls = stream.getObject("urls");
            if (urls.has(kind)) return urls.getObject(kind).getString("url", "");
        }
        return "";
    }

    @Override public List<AudioStream> getAudioStreams() throws IOException, ExtractionException {
        final List<AudioStream> result = new ArrayList<>();
        for (final JsonObject stream : room.getArray("streams").streamAsJsonObjects().toList()) {
            if (!"audio".equals(stream.getString("type"))) continue;
            appendAudioStreams(result, stream.getObject("urls"));
        }
        return result;
    }

    private static void appendAudioStreams(final List<AudioStream> result, final JsonObject urls) {
        for (final var entry : urls.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject)) continue;
            final String format = entry.getKey();
            if ("dash".equals(format)) continue;
            final JsonObject source = (JsonObject) entry.getValue();
            final AudioStream.Builder audio = new AudioStream.Builder()
                    .setId(source.getString("tech", ID_UNKNOWN))
                    .setContent(source.getString("url"), true)
                    .setAverageBitrate(UNKNOWN_BITRATE);
            if ("hls".equals(format)) audio.setDeliveryMethod(DeliveryMethod.HLS);
            else audio.setMediaFormat(MediaFormat.getFromSuffix(format));
            result.add(audio.build());
        }
    }

    @Override public List<VideoStream> getVideoStreams() throws IOException, ExtractionException {
        final List<VideoStream> result = new ArrayList<>();
        for (final JsonObject stream : room.getArray("streams").streamAsJsonObjects().toList()) {
            if (!"video".equals(stream.getString("type"))) continue;
            appendVideoStreams(result, stream);
        }
        return result;
    }

    private static void appendVideoStreams(final List<VideoStream> result,
                                          final JsonObject stream) {
        final JsonArray dimensions = stream.getArray("videoSize");
        final String resolution = dimensions.getInt(0) + "x" + dimensions.getInt(1);
        for (final var entry : stream.getObject("urls").entrySet()) {
            if (!(entry.getValue() instanceof JsonObject)) continue;
            final String format = entry.getKey();
            if ("dash".equals(format)) continue;
            final JsonObject source = (JsonObject) entry.getValue();
            final VideoStream.Builder video = new VideoStream.Builder()
                    .setId(source.getString("tech", ID_UNKNOWN))
                    .setContent(source.getString("url"), true)
                    .setIsVideoOnly(false)
                    .setResolution(resolution);
            if ("hls".equals(format)) video.setDeliveryMethod(DeliveryMethod.HLS);
            else video.setMediaFormat(MediaFormat.getFromSuffix(format));
            result.add(video.build());
        }
    }

    @Override public List<VideoStream> getVideoOnlyStreams() { return List.of(); }
    @Override public StreamType getStreamType() { return StreamType.LIVE_STREAM; }
    @Nonnull @Override public String getCategory() { return group; }
}
