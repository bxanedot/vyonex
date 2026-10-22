// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
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
    private static final Pattern SIMPLE_START = Pattern.compile("(?:[?&#])start=(\\d+)(?:[&#]|$)");
    private static final Pattern TIME_START = Pattern.compile(
            "(?:^|[^a-zA-Z0-9_])(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s?)?(?:$|[^a-zA-Z0-9_])");

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
        final String publishedAt = getTextualUploadDate();
        return publishedAt == null ? null : DateWrapper.fromInstant(publishedAt);
    }
    @Override public List<Image> getThumbnails() {
        return getThumbnailsFromPlaylistOrVideoItem(baseUrl, video);
    }

    @Override
    public Description getDescription() throws ParsingException {
        String text = optionalString(video, "description");
        if (text == null || text.isEmpty()) return Description.EMPTY_DESCRIPTION;
        if (text.length() == 250 && text.endsWith("...")) {
            try {
                final Response response = getDownloader().get(videoApi("/description"));
                if (response != null && !response.responseBody().isBlank()) {
                    text = requiredString(JsonParser.object().from(response.responseBody()),
                            "description");
                }
            } catch (final IOException | ReCaptchaException | JsonParserException ignored) {
                // Keep the summary when the optional full description cannot be fetched.
            }
        }
        return new Description(text, Description.MARKDOWN);
    }

    @Override public int getAgeLimit() { return video.getBoolean("nsfw") ? 18 : NO_AGE_LIMIT; }
    @Override public long getLength() { return video.getLong("duration"); }
    @Override public long getViewCount() { return video.getLong("views", -1); }
    @Override public long getLikeCount() { return video.getLong("likes", -1); }
    @Override public long getDislikeCount() { return video.getLong("dislikes", -1); }

    @Override
    public long getTimeStamp() throws ParsingException {
        final String url = getOriginalUrl();
        final Matcher seconds = SIMPLE_START.matcher(url);
        if (seconds.find()) return Long.parseLong(seconds.group(1));
        final int keyIndex = url.indexOf("start=");
        if (keyIndex < 0) return 0;

        final int valueStart = keyIndex + "start=".length();
        int valueEnd = valueStart;
        while (valueEnd < url.length() && url.charAt(valueEnd) != '&'
                && url.charAt(valueEnd) != '#') valueEnd++;
        final Matcher time = TIME_START.matcher(url.substring(valueStart, valueEnd));
        if (!time.matches() || (time.group(1) == null && time.group(2) == null
                && time.group(3) == null)) return 0;
        try {
            final long hours = parsePart(time.group(1), 3600);
            final long minutes = parsePart(time.group(2), 60);
            final long secondsPart = Long.parseLong(time.group(3) == null ? "0" : time.group(3));
            return Math.addExact(Math.addExact(hours, minutes), secondsPart);
        } catch (final NumberFormatException | ArithmeticException error) {
            throw new ParsingException("Invalid PeerTube start time", error);
        }
    }

    private static long parsePart(final String value, final long factor) {
        return Math.multiplyExact(Long.parseLong(value == null ? "0" : value), factor);
    }

    @Override public String getUploaderUrl() throws ParsingException {
        final String accountId = "accounts/" + requiredString(video, "account.name")
                + "@" + requiredString(video, "account.host");
        return getService().getChannelLHFactory().fromId(accountId, baseUrl).getUrl();
    }
    @Override public String getUploaderName() throws ParsingException {
        return requiredString(video, "account.displayName");
    }
    @Override public List<Image> getUploaderAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, video.getObject("account"));
    }
    @Override public String getSubChannelUrl() throws ParsingException {
        return requiredString(video, "channel.url");
    }
    @Override public String getSubChannelName() throws ParsingException {
        return requiredString(video, "channel.displayName");
    }
    @Override public List<Image> getSubChannelAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, video.getObject("channel"));
    }

    @Override
    public String getHlsUrl() {
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
        return subtitles.stream().filter(track -> track.getFormat() == format).toList();
    }
    @Override public StreamType getStreamType() {
        return video.getBoolean("isLive") ? StreamType.LIVE_STREAM : StreamType.VIDEO_STREAM;
    }

    @Override
    public StreamInfoItemsCollector getRelatedItems() throws IOException, ExtractionException {
        final List<String> tags = getTags();
        final String url = tags.isEmpty() ? accountVideosUrl() : relatedUrl(tags);
        final Response response = getDownloader().get(url);
        if (response == null || response.responseBody().isBlank()) {
            throw new ExtractionException("PeerTube returned no related videos");
        }
        final JsonObject data = readObject(response.responseBody(), "related videos");
        PeertubeParsingHelper.validate(data);
        final Object entries = data.get("data");
        if (!(entries instanceof JsonArray)) {
            throw new ParsingException("Related video data is not an array");
        }

        final StreamInfoItemsCollector results = new StreamInfoItemsCollector(getServiceId());
        for (final Object entry : (JsonArray) entries) {
            if (!(entry instanceof JsonObject)) continue;
            final PeertubeStreamInfoItemExtractor related =
                    new PeertubeStreamInfoItemExtractor((JsonObject) entry, baseUrl);
            if (!getUrl().equals(related.getUrl())) results.commit(related);
        }
        return results;
    }

    @Override public List<String> getTags() {
        final List<String> values = new ArrayList<>();
        for (final Object tag : video.getArray("tags")) {
            if (tag instanceof String) values.add((String) tag);
        }
        return List.copyOf(values);
    }
    @Override public String getSupportInfo() { return video.getString("support", ""); }

    @Override
    public List<StreamSegment> getStreamSegments() throws ParsingException {
        final JsonObject data;
        try {
            data = fetchSubApiContent("chapters");
        } catch (final IOException | ReCaptchaException error) {
            throw new ParsingException("Could not get video chapters", error);
        }
        if (data == null) return List.of();

        final List<StreamSegment> segments = new ArrayList<>();
        for (final Object entry : data.getArray("chapters")) {
            if (entry instanceof JsonObject) {
                final JsonObject chapter = (JsonObject) entry;
                segments.add(new StreamSegment(chapter.getString("title", ""),
                        chapter.getInt("timecode")));
            }
        }
        return List.copyOf(segments);
    }

    @Override
    public List<Frameset> getFrames() throws ExtractionException {
        final JsonObject data;
        try {
            data = fetchSubApiContent("storyboards");
        } catch (final IOException | ReCaptchaException error) {
            throw new ExtractionException("Could not get video storyboards", error);
        }
        if (data == null) return List.of();

        final List<Frameset> frames = new ArrayList<>();
        for (final Object entry : data.getArray("storyboards")) {
            if (!(entry instanceof JsonObject)) continue;
            final JsonObject board = (JsonObject) entry;
            final int width = board.getInt("spriteWidth");
            final int height = board.getInt("spriteHeight");
            if (width <= 0 || height <= 0) continue;
            final int columns = board.getInt("totalWidth") / width;
            final int rows = board.getInt("totalHeight") / height;
            final String image = resolve(baseUrl, board.getString("storyboardPath", ""));
            frames.add(new Frameset(List.of(image), width, height, columns * rows,
                    board.getInt("spriteDuration") * 1000, columns, rows));
        }
        return List.copyOf(frames);
    }

    @Override
    public void onFetchPage(final Downloader downloader) throws IOException, ExtractionException {
        final Response response = downloader.get(videoApi(""));
        if (response == null || response.responseBody().isBlank()) {
            throw new ExtractionException("PeerTube returned no video data");
        }
        video = readObject(response.responseBody(), "video");
        PeertubeParsingHelper.validate(video);
        try {
            loadSubtitles();
        } catch (final Exception error) {
            subtitlesException = new ParsingException("Unable to load PeerTube captions", error);
        }
    }

    @Override public String getName() throws ParsingException { return requiredString(video, "name"); }
    @Override public String getHost() throws ParsingException { return requiredString(video, "account.host"); }
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
        for (final Object entry : captions) {
            if (!(entry instanceof JsonObject)) continue;
            final JsonObject caption = (JsonObject) entry;
            final String path = caption.getString("captionPath");
            final String language = optionalString(caption, "language.id");
            if (!hasText(path) || !hasText(language)) continue;
            final String url = resolve(baseUrl, path);
            final MediaFormat format = MediaFormat.getFromSuffix(extension(url));
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
                for (int index = 0; index < playlists.size(); index++) {
                    final JsonObject playlist = playlists.getObject(index);
                    final String url = playlist.getString("playlistUrl");
                    if (hasText(url)) {
                        final String id = playlist.getString("id", Integer.toString(index));
                        videoStreams.add(new VideoStream.Builder().setId(id).setContent(url, true)
                                .setIsVideoOnly(false).setResolution("")
                                .setMediaFormat(MediaFormat.MPEG_4)
                                .setDeliveryMethod(DeliveryMethod.HLS).build());
                    }
                }
            } else {
                addStreams(video.getArray("files"), null);
                for (final Object entry : video.getArray("streamingPlaylists")) {
                    if (entry instanceof JsonObject) {
                        final JsonObject playlist = (JsonObject) entry;
                        addStreams(playlist.getArray("files"), playlist.getString("playlistUrl"));
                    }
                }
            }
            streamsLoaded = true;
        } catch (final ParsingException error) {
            audioStreams.clear();
            videoStreams.clear();
            throw error;
        }
    }

    private void addStreams(final JsonArray files, final String playlistUrl) throws ParsingException {
        for (final Object entry : files) {
            if (!(entry instanceof JsonObject)) continue;
            final JsonObject file = (JsonObject) entry;
            final String url = file.getString("fileUrl", file.getString("fileDownloadUrl"));
            if (!hasText(url)) continue;

            final String resolution = file.getObject("resolution").getString("label", "");
            final String suffix = extension(url);
            final MediaFormat format = MediaFormat.getFromSuffix(suffix);
            final String id = hasText(resolution) ? resolution : "unknown";
            if (isAudio(file, resolution)) {
                addAudio(file, url, suffix, format, id, playlistUrl);
            } else {
                addVideo(file, url, suffix, format, id, resolution, playlistUrl);
            }
        }
    }

    private void addAudio(final JsonObject file, final String url, final String suffix,
                          final MediaFormat format, final String id, final String playlistUrl) {
        audioStreams.add(new AudioStream.Builder().setId(id + "-" + suffix)
                .setContent(url, true).setMediaFormat(format)
                .setAverageBitrate(AudioStream.UNKNOWN_BITRATE).build());
        if (hasText(playlistUrl)) {
            final String hlsUrl = renditionUrl(file, url, suffix, playlistUrl);
            final AudioStream hls = new AudioStream.Builder().setId(id + "-hls")
                    .setContent(hlsUrl, true).setDeliveryMethod(DeliveryMethod.HLS)
                    .setMediaFormat(format).setManifestUrl(playlistUrl)
                    .setAverageBitrate(AudioStream.UNKNOWN_BITRATE).build();
            addIfNew(audioStreams, hls);
        }
        final String torrent = file.getString("torrentUrl");
        if (hasText(torrent)) {
            audioStreams.add(new AudioStream.Builder().setId(id + "-torrent")
                    .setContent(torrent, true).setDeliveryMethod(DeliveryMethod.TORRENT)
                    .setMediaFormat(format).setAverageBitrate(AudioStream.UNKNOWN_BITRATE).build());
        }
    }

    private void addVideo(final JsonObject file, final String url, final String suffix,
                          final MediaFormat format, final String id, final String resolution,
                          final String playlistUrl) {
        final boolean videoOnly = file.has("hasAudio") && !file.getBoolean("hasAudio");
        videoStreams.add(new VideoStream.Builder().setId(id + "-" + suffix)
                .setContent(url, true).setIsVideoOnly(videoOnly).setResolution(resolution)
                .setMediaFormat(format).build());
        if (hasText(playlistUrl)) {
            final VideoStream hls = new VideoStream.Builder()
                    .setId(id + "-hls").setContent(renditionUrl(file, url, suffix, playlistUrl), true)
                    .setIsVideoOnly(videoOnly).setDeliveryMethod(DeliveryMethod.HLS)
                    .setResolution(resolution).setMediaFormat(format)
                    .setManifestUrl(playlistUrl).build();
            addIfNew(videoStreams, hls);
        }
        final String torrent = file.getString("torrentUrl");
        if (hasText(torrent)) {
            videoStreams.add(new VideoStream.Builder().setId(id + "-torrent")
                    .setContent(torrent, true).setIsVideoOnly(videoOnly)
                    .setDeliveryMethod(DeliveryMethod.TORRENT).setResolution(resolution)
                    .setMediaFormat(format).build());
        }
    }

    private static String renditionUrl(final JsonObject file, final String url,
                                       final String suffix, final String playlistUrl) {
        if (playlistUrl.endsWith("-master.m3u8")) {
            final String rendition = url.replace("-fragmented." + suffix, ".m3u8");
            if (!rendition.equals(url)) return rendition;
        }
        final Object resolutionId = file.getObject("resolution").get("id");
        return resolutionId == null ? playlistUrl : playlistUrl.replace("master", resolutionId.toString());
    }

    private static boolean isAudio(final JsonObject file, final String resolution) {
        return resolution.toLowerCase(Locale.ROOT).contains("audio")
                || file.getString("mimeType", "").startsWith("audio/");
    }

    private static <T extends Stream> void addIfNew(final List<T> streams, final T candidate) {
        if (!Stream.containSimilarStream(candidate, streams)) streams.add(candidate);
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

    private String accountVideosUrl() throws ParsingException {
        return baseUrl + "/api/v1/accounts/" + requiredString(video, "account.name") + "@"
                + requiredString(video, "account.host") + "/videos?start=0&count=8";
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

    private static JsonObject readObject(final String source, final String kind)
            throws ParsingException {
        try {
            return JsonParser.object().from(source);
        } catch (final JsonParserException error) {
            throw new ParsingException("Invalid PeerTube " + kind + " JSON", error);
        }
    }

    private static String resolve(final String base, final String path) {
        try {
            final String root = base.endsWith("/") ? base : base + "/";
            return URI.create(root).resolve(path).toString();
        } catch (final IllegalArgumentException ignored) {
            return base + (path.startsWith("/") ? "" : "/") + path;
        }
    }

    private static String extension(final String url) {
        try {
            final String path = URI.create(url).getPath();
            final int dot = path == null ? -1 : path.lastIndexOf('.');
            return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        } catch (final IllegalArgumentException ignored) {
            return "";
        }
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
