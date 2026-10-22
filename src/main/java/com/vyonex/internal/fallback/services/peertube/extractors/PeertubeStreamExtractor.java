// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeSearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.DeliveryMethod;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.Frameset;
import com.vyonex.internal.fallback.stream.Stream;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.fallback.stream.StreamSegment;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.SubtitlesStream;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.optionalString;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.requiredString;

public final class PeertubeStreamExtractor extends StreamExtractor {
    private static final Pattern START_VALUE = Pattern.compile(
            "(?:^|[^a-zA-Z0-9_])(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s?)?(?:$|[^a-zA-Z0-9_])");
    private static final Pattern SIMPLE_START = Pattern.compile("(?:[?&#])start=(\\d+)(?:[&#]|$)");

    private final String baseUrl;
    private JsonObject video;
    private final List<SubtitlesStream> subtitles = new ArrayList<>();
    private final List<AudioStream> audioStreams = new ArrayList<>();
    private final List<VideoStream> videoStreams = new ArrayList<>();
    private ParsingException subtitlesException;
    private boolean streamsLoaded;

    public PeertubeStreamExtractor(final StreamingService service, final LinkHandler handler)
            throws ParsingException {
        super(service, handler);
        baseUrl = getBaseUrl();
    }

    @Override public String getTextualUploadDate() { return optionalString(video, "publishedAt"); }

    @Override public DateWrapper getUploadDate() throws ParsingException {
        final String published = getTextualUploadDate();
        return published == null ? null : DateWrapper.fromInstant(published);
    }

    @Override public List<Image> getThumbnails() {
        return getThumbnailsFromPlaylistOrVideoItem(baseUrl, video);
    }

    @Override public Description getDescription() throws ParsingException {
        String description = optionalString(video, "description");
        if (description == null || description.isEmpty()) return Description.EMPTY_DESCRIPTION;
        if (description.length() == 250 && description.endsWith("...")) {
            try {
                final Response response = getDownloader().get(videoApi("/description"));
                if (response != null && !response.responseBody().isBlank()) {
                    description = requiredString(JsonParser.object().from(response.responseBody()),
                            "description");
                }
            } catch (final IOException | ReCaptchaException | JsonParserException ignored) {
                // Retain the summary when an instance cannot return the full description.
            }
        }
        return new Description(description, Description.MARKDOWN);
    }

    @Override public int getAgeLimit() { return video.getBoolean("nsfw") ? 18 : NO_AGE_LIMIT; }
    @Override public long getLength() { return video.getLong("duration"); }
    @Override public long getViewCount() { return video.getLong("views", -1); }
    @Override public long getLikeCount() { return video.getLong("likes", -1); }
    @Override public long getDislikeCount() { return video.getLong("dislikes", -1); }

    @Override public long getTimeStamp() throws ParsingException {
        final String url = getOriginalUrl();
        final Matcher simple = SIMPLE_START.matcher(url);
        if (simple.find()) return Long.parseLong(simple.group(1));
        final int start = url.indexOf("start=");
        if (start < 0) return 0;
        final int valueStart = start + 6;
        int valueEnd = valueStart;
        while (valueEnd < url.length() && url.charAt(valueEnd) != '&'
                && url.charAt(valueEnd) != '#') valueEnd++;
        final Matcher parts = START_VALUE.matcher(url.substring(valueStart, valueEnd));
        if (!parts.matches() || parts.group(1) == null && parts.group(2) == null
                && parts.group(3) == null) return 0;
        try {
            return Math.addExact(Math.addExact(
                    Math.multiplyExact(Long.parseLong(parts.group(1) == null ? "0" : parts.group(1)), 3600),
                    Math.multiplyExact(Long.parseLong(parts.group(2) == null ? "0" : parts.group(2)), 60)),
                    Long.parseLong(parts.group(3) == null ? "0" : parts.group(3)));
        } catch (final NumberFormatException | ArithmeticException e) {
            throw new ParsingException("Invalid PeerTube start time", e);
        }
    }

    @Override public String getUploaderUrl() throws ParsingException {
        return getService().getChannelLHFactory().fromId("accounts/"
                + requiredString(video, "account.name") + "@"
                + requiredString(video, "account.host"), baseUrl).getUrl();
    }

    @Override public String getUploaderName() throws ParsingException {
        return requiredString(video, "account.displayName");
    }

    @Override public List<Image> getUploaderAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl,
                video.getObject("account"));
    }

    @Override public String getSubChannelUrl() throws ParsingException {
        return requiredString(video, "channel.url");
    }

    @Override public String getSubChannelName() throws ParsingException {
        return requiredString(video, "channel.displayName");
    }

    @Override public List<Image> getSubChannelAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl,
                video.getObject("channel"));
    }

    @Override public String getHlsUrl() {
        assertPageFetched();
        final JsonObject files = video.getObject("files", null);
        if (files != null && hasText(files.getString("playlistUrl"))) {
            return files.getString("playlistUrl");
        }
        final JsonArray playlists = video.getArray("streamingPlaylists");
        return playlists.isEmpty() ? "" : playlists.getObject(0).getString("playlistUrl", "");
    }

    @Override public List<AudioStream> getAudioStreams() throws ParsingException {
        assertPageFetched();
        if (getStreamType() == StreamType.VIDEO_STREAM) loadStreams();
        return List.copyOf(audioStreams);
    }

    @Override public List<VideoStream> getVideoStreams() throws ExtractionException {
        assertPageFetched();
        loadStreams();
        return List.copyOf(videoStreams);
    }

    @Override public List<VideoStream> getVideoOnlyStreams() { return List.of(); }

    @Override public List<SubtitlesStream> getSubtitlesDefault() throws ParsingException {
        if (subtitlesException != null) throw subtitlesException;
        return List.copyOf(subtitles);
    }

    @Override public List<SubtitlesStream> getSubtitles(final MediaFormat format)
            throws ParsingException {
        if (subtitlesException != null) throw subtitlesException;
        return subtitles.stream().filter(stream -> stream.getFormat() == format).toList();
    }

    @Override public StreamType getStreamType() {
        return video.getBoolean("isLive") ? StreamType.LIVE_STREAM : StreamType.VIDEO_STREAM;
    }

    @Override public StreamInfoItemsCollector getRelatedItems()
            throws IOException, ExtractionException {
        final List<String> tags = getTags();
        final String url = tags.isEmpty()
                ? baseUrl + "/api/v1/accounts/" + requiredString(video, "account.name") + "@"
                    + requiredString(video, "account.host") + "/videos?start=0&count=8"
                : relatedUrl(tags);
        final Response response = getDownloader().get(url);
        if (response == null || response.responseBody().isBlank()) {
            throw new ExtractionException("PeerTube returned no related videos");
        }
        final JsonObject json = readObject(response.responseBody(), "related videos");
        PeertubeParsingHelper.validate(json);
        final Object data = json.get("data");
        if (!(data instanceof JsonArray)) throw new ParsingException("Related video data is not an array");

        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        for (final Object value : (JsonArray) data) {
            if (value instanceof JsonObject) {
                final PeertubeStreamInfoItemExtractor item =
                        new PeertubeStreamInfoItemExtractor((JsonObject) value, baseUrl);
                if (!getUrl().equals(item.getUrl())) items.commit(item);
            }
        }
        return items;
    }

    @Override public List<String> getTags() {
        final List<String> tags = new ArrayList<>();
        for (final Object tag : video.getArray("tags")) {
            if (tag instanceof String) tags.add((String) tag);
        }
        return List.copyOf(tags);
    }

    @Override public String getSupportInfo() { return video.getString("support", ""); }

    @Override public List<StreamSegment> getStreamSegments() throws ParsingException {
        final JsonObject response;
        try {
            response = fetchSubApiContent("chapters");
        } catch (final IOException | ReCaptchaException e) {
            throw new ParsingException("Could not get video chapters", e);
        }
        if (response == null) return List.of();
        final List<StreamSegment> segments = new ArrayList<>();
        for (final Object value : response.getArray("chapters")) {
            if (value instanceof JsonObject) {
                final JsonObject chapter = (JsonObject) value;
                segments.add(new StreamSegment(chapter.getString("title", ""),
                        chapter.getInt("timecode")));
            }
        }
        return List.copyOf(segments);
    }

    @Override public List<Frameset> getFrames() throws ExtractionException {
        final JsonObject response;
        try {
            response = fetchSubApiContent("storyboards");
        } catch (final IOException | ReCaptchaException e) {
            throw new ExtractionException("Could not get video storyboards", e);
        }
        if (response == null) return List.of();
        final List<Frameset> frames = new ArrayList<>();
        for (final Object value : response.getArray("storyboards")) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject board = (JsonObject) value;
            final int width = board.getInt("spriteWidth");
            final int height = board.getInt("spriteHeight");
            if (width <= 0 || height <= 0) continue;
            final int across = board.getInt("totalWidth") / width;
            final int down = board.getInt("totalHeight") / height;
            frames.add(new Frameset(List.of(resolve(baseUrl,
                    board.getString("storyboardPath", ""))), width, height, across * down,
                    board.getInt("spriteDuration") * 1000, across, down));
        }
        return List.copyOf(frames);
    }

    @Override public void onFetchPage(final Downloader downloader)
            throws IOException, ExtractionException {
        final Response response = downloader.get(videoApi(""));
        if (response == null || response.responseBody().isBlank()) {
            throw new ExtractionException("PeerTube returned no video data");
        }
        video = readObject(response.responseBody(), "video");
        PeertubeParsingHelper.validate(video);
        try {
            loadSubtitles();
        } catch (final Exception e) {
            subtitlesException = new ParsingException("Unable to load PeerTube captions", e);
        }
    }

    @Override public String getName() throws ParsingException {
        return requiredString(video, "name");
    }

    @Override public String getHost() throws ParsingException {
        return requiredString(video, "account.host");
    }

    @Override public Privacy getPrivacy() {
        return switch (video.getObject("privacy").getInt("id")) {
            case 1 -> Privacy.PUBLIC;
            case 2 -> Privacy.UNLISTED;
            case 3 -> Privacy.PRIVATE;
            case 4 -> Privacy.INTERNAL;
            default -> Privacy.OTHER;
        };
    }

    @Override public String getCategory() { return video.getObject("category").getString("label", ""); }
    @Override public String getLicence() { return video.getObject("licence").getString("label", ""); }

    @Override public Locale getLanguageInfo() {
        final String language = optionalString(video, "language.id");
        return language == null || language.isBlank() ? null : new Locale(language);
    }

    private void loadSubtitles() throws IOException, ReCaptchaException, ParsingException {
        final Response response = getDownloader().get(videoApi("/captions"));
        if (response == null || response.responseBody().isBlank()) {
            throw new ParsingException("PeerTube returned no captions");
        }
        final JsonArray captions = readObject(response.responseBody(), "captions").getArray("data");
        for (final Object value : captions) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject caption = (JsonObject) value;
            final String path = caption.getString("captionPath");
            final String language = optionalString(caption, "language.id");
            if (!hasText(path) || !hasText(language)) continue;
            final String url = resolve(baseUrl, path);
            final String extension = extension(url);
            final MediaFormat format = MediaFormat.getFromSuffix(extension);
            if (format != null) {
                subtitles.add(new SubtitlesStream.Builder().setContent(url, true)
                        .setMediaFormat(format).setLanguageCode(language)
                        .setAutoGenerated(false).build());
            }
        }
    }

    private void loadStreams() throws ParsingException {
        if (streamsLoaded) return;
        try {
            if (getStreamType() == StreamType.LIVE_STREAM) {
                final JsonArray playlists = video.getArray("streamingPlaylists");
                for (int i = 0; i < playlists.size(); i++) {
                    final JsonObject playlist = playlists.getObject(i);
                    final String url = playlist.getString("playlistUrl");
                    if (hasText(url)) videoStreams.add(new VideoStream.Builder()
                            .setId(playlist.getString("id", Integer.toString(i)))
                            .setContent(url, true).setIsVideoOnly(false).setResolution("")
                            .setMediaFormat(MediaFormat.MPEG_4)
                            .setDeliveryMethod(DeliveryMethod.HLS).build());
                }
            } else {
                addStreams(video.getArray("files"), null);
                for (final Object value : video.getArray("streamingPlaylists")) {
                    if (value instanceof JsonObject) {
                        final JsonObject playlist = (JsonObject) value;
                        addStreams(playlist.getArray("files"), playlist.getString("playlistUrl"));
                    }
                }
            }
            streamsLoaded = true;
        } catch (final ParsingException e) {
            audioStreams.clear();
            videoStreams.clear();
            throw e;
        }
    }

    private void addStreams(final JsonArray files, final String playlistUrl)
            throws ParsingException {
        for (final Object value : files) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject file = (JsonObject) value;
            final String url = file.getString("fileUrl", file.getString("fileDownloadUrl"));
            if (!hasText(url)) continue;
            final String resolution = file.getObject("resolution").getString("label", "");
            final String extension = extension(url);
            final MediaFormat format = MediaFormat.getFromSuffix(extension);
            final String id = hasText(resolution) ? resolution : "unknown";
            if (isAudio(file, resolution)) {
                addAudio(file, url, extension, format, id, playlistUrl);
            } else {
                addVideo(file, url, extension, format, id, resolution, playlistUrl);
            }
        }
    }

    private void addAudio(final JsonObject file, final String url, final String extension,
                          final MediaFormat format, final String id, final String playlistUrl) {
        final AudioStream progressive = new AudioStream.Builder().setId(id + "-" + extension)
                .setContent(url, true).setMediaFormat(format)
                .setAverageBitrate(AudioStream.UNKNOWN_BITRATE).build();
        audioStreams.add(progressive);
        if (hasText(playlistUrl)) {
            final String hlsUrl = renditionUrl(file, url, extension, playlistUrl);
            final AudioStream hls = new AudioStream.Builder().setId(id + "-hls")
                    .setContent(hlsUrl, true).setDeliveryMethod(DeliveryMethod.HLS)
                    .setMediaFormat(format).setManifestUrl(playlistUrl)
                    .setAverageBitrate(AudioStream.UNKNOWN_BITRATE).build();
            addIfNew(audioStreams, hls);
        }
        final String torrent = file.getString("torrentUrl");
        if (hasText(torrent)) audioStreams.add(new AudioStream.Builder()
                .setId(id + "-torrent").setContent(torrent, true)
                .setDeliveryMethod(DeliveryMethod.TORRENT).setMediaFormat(format)
                .setAverageBitrate(AudioStream.UNKNOWN_BITRATE).build());
    }

    private void addVideo(final JsonObject file, final String url, final String extension,
                          final MediaFormat format, final String id, final String resolution,
                          final String playlistUrl) {
        final boolean videoOnly = file.has("hasAudio") && !file.getBoolean("hasAudio");
        videoStreams.add(new VideoStream.Builder().setId(id + "-" + extension)
                .setContent(url, true).setIsVideoOnly(videoOnly).setResolution(resolution)
                .setMediaFormat(format).build());
        if (hasText(playlistUrl)) {
            final VideoStream hls = new VideoStream.Builder().setId(id + "-hls")
                    .setContent(renditionUrl(file, url, extension, playlistUrl), true)
                    .setIsVideoOnly(videoOnly).setDeliveryMethod(DeliveryMethod.HLS)
                    .setResolution(resolution).setMediaFormat(format)
                    .setManifestUrl(playlistUrl).build();
            addIfNew(videoStreams, hls);
        }
        final String torrent = file.getString("torrentUrl");
        if (hasText(torrent)) videoStreams.add(new VideoStream.Builder()
                .setId(id + "-torrent").setContent(torrent, true).setIsVideoOnly(videoOnly)
                .setDeliveryMethod(DeliveryMethod.TORRENT).setResolution(resolution)
                .setMediaFormat(format).build());
    }

    private static String renditionUrl(final JsonObject file, final String url,
                                       final String extension, final String playlistUrl) {
        if (playlistUrl.endsWith("-master.m3u8")) {
            final String hls = url.replace("-fragmented." + extension, ".m3u8");
            if (!hls.equals(url)) return hls;
        }
        final Object resolutionId = file.getObject("resolution").get("id");
        return resolutionId == null ? playlistUrl
                : playlistUrl.replace("master", resolutionId.toString());
    }

    private static boolean isAudio(final JsonObject file, final String resolution) {
        return resolution.toLowerCase(Locale.ROOT).contains("audio")
                || file.getString("mimeType", "").startsWith("audio/");
    }

    private static <T extends Stream> void addIfNew(final List<T> streams, final T stream) {
        if (!Stream.containSimilarStream(stream, streams)) streams.add(stream);
    }

    private JsonObject fetchSubApiContent(final String path)
            throws IOException, ReCaptchaException, ParsingException {
        final Response response = getDownloader().get(videoApi("/" + path));
        if (response == null) throw new ParsingException("PeerTube returned no " + path + " data");
        if (response.responseCode() == 400) return null;
        if (response.responseCode() != 200) {
            throw new ParsingException("PeerTube " + path + " request failed with HTTP "
                    + response.responseCode());
        }
        return readObject(response.responseBody(), path);
    }

    private String relatedUrl(final List<String> tags) {
        final StringBuilder url = new StringBuilder(baseUrl)
                .append(PeertubeSearchQueryHandlerFactory.SEARCH_ENDPOINT_VIDEOS)
                .append("?start=0&count=8&sort=-createdAt");
        for (final String tag : tags) {
            url.append("&tagsOneOf=").append(URLEncoder.encode(tag, StandardCharsets.UTF_8));
        }
        return url.toString();
    }

    private String videoApi(final String suffix) throws ParsingException {
        return baseUrl + PeertubeStreamLinkHandlerFactory.VIDEO_API_ENDPOINT + getId() + suffix;
    }

    private static JsonObject readObject(final String json, final String kind)
            throws ParsingException {
        try {
            return JsonParser.object().from(json);
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid PeerTube " + kind + " JSON", e);
        }
    }

    private static String resolve(final String base, final String path) {
        try {
            return URI.create(base.endsWith("/") ? base : base + "/").resolve(path).toString();
        } catch (final IllegalArgumentException e) {
            return base + (path.startsWith("/") ? "" : "/") + path;
        }
    }

    private static String extension(final String url) {
        try {
            final String path = URI.create(url).getPath();
            final int dot = path == null ? -1 : path.lastIndexOf('.');
            return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        } catch (final IllegalArgumentException e) {
            return "";
        }
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
