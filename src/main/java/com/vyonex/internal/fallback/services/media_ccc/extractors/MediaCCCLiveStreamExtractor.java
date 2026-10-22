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
import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper
        .getThumbnailsFromLiveStreamItem;

public class MediaCCCLiveStreamExtractor extends StreamExtractor {
    private JsonObject conference;
    private String group = "";
    private JsonObject room;

    public MediaCCCLiveStreamExtractor(final StreamingService service,
                                       final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final JsonArray conferences = MediaCCCParsingHelper.getLiveStreams(
                downloader, getExtractorLocalization());
        final String wantedId = getId();
        for (int conferenceIndex = 0; conferenceIndex < conferences.size(); conferenceIndex++) {
            final JsonObject candidate = conferences.getObject(conferenceIndex);
            final JsonArray groups = candidate.getArray("groups");
            for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                final JsonObject groupEntry = groups.getObject(groupIndex);
                final JsonArray rooms = groupEntry.getArray("rooms");
                for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
                    final JsonObject roomEntry = rooms.getObject(roomIndex);
                    if (wantedId.equals(candidate.getString("slug") + "/"
                            + roomEntry.getString("slug"))) {
                        conference = candidate;
                        group = groupEntry.getString("group");
                        room = roomEntry;
                        return;
                    }
                }
            }
        }
        throw new ExtractionException("No live room matches " + wantedId);
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException { return room.getString("display"); }

    @Nonnull
    @Override
    public List<Image> getThumbnails() { return getThumbnailsFromLiveStreamItem(room); }

    @Nonnull
    @Override
    public Description getDescription() {
        return new Description(conference.getString("description") + " - " + group,
                Description.PLAIN_TEXT);
    }

    @Override
    public long getViewCount() { return -1; }

    @Nonnull
    @Override
    public String getUploaderUrl() throws ParsingException {
        return "https://streaming.media.ccc.de/" + conference.getString("slug");
    }

    @Nonnull
    @Override
    public String getUploaderName() throws ParsingException {
        return conference.getString("conference");
    }

    @Nonnull
    @Override
    public String getDashMpdUrl() throws ParsingException { return getManifest("dash"); }

    @Nonnull
    @Override
    public String getHlsUrl() throws ParsingException { return getManifest("hls"); }

    private String getManifest(final String method) {
        final JsonArray streams = room.getArray("streams");
        for (int index = 0; index < streams.size(); index++) {
            final JsonObject urls = streams.getObject(index).getObject("urls");
            if (urls.has(method)) return urls.getObject(method).getString("url", "");
        }
        return "";
    }

    @Override
    public List<AudioStream> getAudioStreams() throws IOException, ExtractionException {
        final List<AudioStream> result = new ArrayList<>();
        final JsonArray streams = room.getArray("streams");
        for (int index = 0; index < streams.size(); index++) {
            final JsonObject stream = streams.getObject(index);
            if (!"audio".equals(stream.getString("type"))) continue;
            final JsonObject urls = stream.getObject("urls");
            for (final var entry : urls.entrySet()) {
                if (!(entry.getValue() instanceof JsonObject value) || "dash".equals(entry.getKey())) {
                    continue;
                }
                final AudioStream.Builder builder = new AudioStream.Builder()
                        .setId(value.getString("tech", ID_UNKNOWN))
                        .setContent(value.getString("url"), true)
                        .setAverageBitrate(UNKNOWN_BITRATE);
                if ("hls".equals(entry.getKey())) {
                    builder.setDeliveryMethod(DeliveryMethod.HLS);
                } else {
                    builder.setMediaFormat(MediaFormat.getFromSuffix(entry.getKey()));
                }
                result.add(builder.build());
            }
        }
        return result;
    }

    @Override
    public List<VideoStream> getVideoStreams() throws IOException, ExtractionException {
        final List<VideoStream> result = new ArrayList<>();
        final JsonArray streams = room.getArray("streams");
        for (int index = 0; index < streams.size(); index++) {
            final JsonObject stream = streams.getObject(index);
            if (!"video".equals(stream.getString("type"))) continue;
            final JsonArray size = stream.getArray("videoSize");
            final String resolution = size.getInt(0) + "x" + size.getInt(1);
            final JsonObject urls = stream.getObject("urls");
            for (final var entry : urls.entrySet()) {
                if (!(entry.getValue() instanceof JsonObject value) || "dash".equals(entry.getKey())) {
                    continue;
                }
                final VideoStream.Builder builder = new VideoStream.Builder()
                        .setId(value.getString("tech", ID_UNKNOWN))
                        .setContent(value.getString("url"), true)
                        .setIsVideoOnly(false)
                        .setResolution(resolution);
                if ("hls".equals(entry.getKey())) {
                    builder.setDeliveryMethod(DeliveryMethod.HLS);
                } else {
                    builder.setMediaFormat(MediaFormat.getFromSuffix(entry.getKey()));
                }
                result.add(builder.build());
            }
        }
        return result;
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() { return List.of(); }

    @Override
    public StreamType getStreamType() { return StreamType.LIVE_STREAM; }

    @Nonnull
    @Override
    public String getCategory() { return group; }
}
