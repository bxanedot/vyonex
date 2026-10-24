// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.google.protobuf.CodedOutputStream;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistUrl;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;

/** Reads playlist metadata and pages from YouTube's browse endpoint. */
public class YoutubePlaylistExtractor extends PlaylistExtractor {
    private static final String PLAYLIST_ID_PREFIX = "VL";
    private static final String UNAVAILABLE_VIDEO_PARAM = "wgYCCAA%3D";
    private static final String CONTINUATION_SETTINGS = "CADCBgIIAA%3D%3D";
    private static final String PLAYLIST_ITEM = "playlistVideoRenderer";
    private static final String LOCKUP_ITEM = "lockupViewModel";
    private static final String CONTINUATION_URL = YOUTUBEI_V1_URL + "browse?"
            + DISABLE_PRETTY_PRINT_PARAMETER;

    @Nullable private JsonObject metadata;
    @Nullable private JsonObject initialPage;
    private boolean headerLayout;

    public YoutubePlaylistExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String playlistId = getId();
        final Localization localization = getExtractorLocalization();
        final byte[] metadataBody = requestBody(localization, "browseId",
                PLAYLIST_ID_PREFIX + playlistId, "params", UNAVAILABLE_VIDEO_PARAM);
        metadata = getJsonPostResponse("browse",
                List.of("$fields=sidebar,header,microformat,alerts"), metadataBody, localization);
        YoutubeParsingHelper.defaultAlertsCheck(metadata);
        headerLayout = metadata.has("header") && !metadata.has("sidebar");

        final String opaqueContinuation = Base64.getUrlEncoder().encodeToString(
                encodePlaylistContinuation(playlistId));
        initialPage = getJsonPostResponse("browse",
                requestBody(localization, "continuation", Utils.encodeUrlUtf8(opaqueContinuation)),
                localization);
    }

    static byte[] encodePlaylistContinuation(final String playlistId) throws IOException {
        final ByteArrayOutputStream fields = new ByteArrayOutputStream();
        final CodedOutputStream parameters = CodedOutputStream.newInstance(fields);
        parameters.writeString(2, PLAYLIST_ID_PREFIX + playlistId);
        parameters.writeString(3, CONTINUATION_SETTINGS);
        parameters.writeString(35, playlistId);
        parameters.flush();

        final ByteArrayOutputStream envelope = new ByteArrayOutputStream();
        final CodedOutputStream continuation = CodedOutputStream.newInstance(envelope);
        continuation.writeByteArray(80226972, fields.toByteArray());
        continuation.flush();
        return envelope.toByteArray();
    }

    @Nonnull
    private byte[] requestBody(final Localization localization, final String... fields)
            throws IOException, ExtractionException {
        final var builder = prepareDesktopJsonBuilder(localization, getExtractorContentCountry());
        for (int index = 0; index < fields.length; index += 2) {
            builder.value(fields[index], fields[index + 1]);
        }
        return JsonWriter.string(builder.done()).getBytes(StandardCharsets.UTF_8);
    }

    @Nonnull
    private JsonObject metadata() throws ParsingException {
        if (metadata == null) throw new ParsingException("Playlist metadata was not fetched");
        return metadata;
    }

    @Nonnull
    private JsonObject initialPage() throws ParsingException {
        if (initialPage == null) throw new ParsingException("Playlist items were not fetched");
        return initialPage;
    }

    @Nonnull
    private JsonObject sidebarPlaylistInfo() throws ParsingException {
        for (final JsonObject item : metadata().getObject("sidebar")
                .getObject("playlistSidebarRenderer").getArray("items").streamAsJsonObjects().toList()) {
            final JsonObject primary = item.getObject("playlistSidebarPrimaryInfoRenderer");
            if (!primary.isEmpty()) return primary;
        }
        throw new ParsingException("Playlist details are missing");
    }

    @Nonnull
    private JsonObject sidebarOwnerInfo() throws ParsingException {
        for (final JsonObject item : metadata().getObject("sidebar")
                .getObject("playlistSidebarRenderer").getArray("items").streamAsJsonObjects().toList()) {
            final JsonObject owner = item.getObject("playlistSidebarSecondaryInfoRenderer")
                    .getObject("videoOwner").getObject("videoOwnerRenderer");
            if (!owner.isEmpty()) return owner;
        }
        throw new ParsingException("Playlist owner is missing");
    }

    @Nonnull
    private JsonObject playlistHeader() throws ParsingException {
        final JsonObject header = metadata().getObject("header").getObject("playlistHeaderRenderer");
        if (header.isEmpty()) throw new ParsingException("Playlist header is missing");
        return header;
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        final String title = getTextFromObject(headerLayout
                ? playlistHeader().getObject("title") : sidebarPlaylistInfo().getObject("title"));
        if (title != null && !title.isBlank()) return title;
        final String fallback = metadata().getObject("microformat")
                .getObject("microformatDataRenderer").getString("title");
        if (fallback == null || fallback.isBlank()) throw new ParsingException("Playlist title is missing");
        return fallback;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        final JsonArray thumbnails = headerLayout
                ? playlistHeader().getObject("playlistHeaderBanner")
                .getObject("heroPlaylistThumbnailRenderer").getObject("thumbnail")
                .getArray("thumbnails")
                : sidebarPlaylistInfo().getObject("thumbnailRenderer")
                .getObject("playlistVideoThumbnailRenderer").getObject("thumbnail")
                .getArray("thumbnails");
        if (!thumbnails.isEmpty()) return getImagesFromThumbnailsArray(thumbnails);

        final JsonArray fallback = metadata().getObject("microformat")
                .getObject("microformatDataRenderer").getObject("thumbnail")
                .getArray("thumbnails");
        if (!fallback.isEmpty()) return getImagesFromThumbnailsArray(fallback);
        throw new ParsingException("Playlist thumbnails are missing");
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        final JsonObject endpoint;
        if (headerLayout) {
            endpoint = playlistHeader().getObject("ownerText").getArray("runs")
                    .getObject(0).getObject("navigationEndpoint");
        } else {
            endpoint = sidebarOwnerInfo().getObject("navigationEndpoint");
        }
        final String url = getUrlFromNavigationEndpoint(endpoint);
        if (url == null || url.isBlank()) throw new ParsingException("Playlist owner URL is missing");
        return url;
    }

    @Override
    public String getUploaderName() throws ParsingException {
        final String name = getTextFromObject(headerLayout
                ? playlistHeader().getObject("ownerText") : sidebarOwnerInfo().getObject("title"));
        if (name == null || name.isBlank()) throw new ParsingException("Playlist owner name is missing");
        return name;
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() throws ParsingException {
        if (headerLayout) return List.of();
        final JsonArray thumbnails = sidebarOwnerInfo().getObject("thumbnail").getArray("thumbnails");
        return getImagesFromThumbnailsArray(thumbnails);
    }

    @Override public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() throws ParsingException {
        final JsonObject source = headerLayout ? playlistHeader() : sidebarPlaylistInfo();
        final String direct = getTextFromObject(source.getObject("numVideosText"));
        final String byline = getTextFromObject(source.getArray("byline").getObject(0)
                .getObject("text"));
        for (final String text : List.of(direct == null ? "" : direct,
                byline == null ? "" : byline,
                orEmpty(statisticText(source.getArray("briefStats"))),
                orEmpty(statisticText(source.getArray("stats"))))) {
            final String digits = Utils.removeNonDigitCharacters(text);
            if (!digits.isBlank()) {
                try {
                    return Long.parseLong(digits);
                } catch (final NumberFormatException error) {
                    throw new ParsingException("Playlist video count is out of range", error);
                }
            }
        }
        return ITEM_COUNT_UNKNOWN;
    }

    @Nullable
    private static String statisticText(final JsonArray values) {
        return values.isEmpty() ? null : getTextFromObject(values.getObject(0));
    }

    private static String orEmpty(@Nullable final String value) {
        return value == null ? "" : value;
    }

    @Nonnull
    @Override
    public Description getDescription() throws ParsingException {
        final JsonObject value = headerLayout
                ? playlistHeader().getObject("descriptionText")
                : sidebarPlaylistInfo().getObject("description");
        return new Description(getTextFromObject(value, true), Description.HTML);
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        final JsonArray items = findContinuationItems(initialPage());
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        collectStreams(items, collector);
        return new InfoItemsPage<>(collector, nextPage(items));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getBody() == null) {
            throw new IllegalArgumentException("Playlist continuation body is missing");
        }
        final JsonObject response = getJsonPostResponse("browse", page.getBody(),
                getExtractorLocalization());
        final JsonArray items = findContinuationItems(response);
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        collectStreams(items, collector);
        return new InfoItemsPage<>(collector, nextPage(items));
    }

    @Nonnull
    private JsonArray findContinuationItems(final JsonObject response) {
        for (final JsonObject action : response.getArray("onResponseReceivedActions")
                .streamAsJsonObjects().toList()) {
            for (final String key : List.of("reloadContinuationItemsCommand",
                    "appendContinuationItemsAction")) {
                final JsonArray items = action.getObject(key).getArray("continuationItems");
                if (!items.isEmpty()) return items;
            }
        }
        return new JsonArray();
    }

    @Nullable
    private Page nextPage(final JsonArray items) throws IOException, ExtractionException {
        if (items.isEmpty()) return null;
        final JsonObject last = items.getObject(items.size() - 1);
        final String token = continuationToken(last);
        if (token == null || token.isBlank()) return null;
        return new Page(CONTINUATION_URL, requestBody(getExtractorLocalization(),
                "continuation", token));
    }

    @Nullable
    private String continuationToken(final JsonObject item) {
        if (item.has("continuationItemRenderer")) {
            final JsonObject endpoint = item.getObject("continuationItemRenderer")
                    .getObject("continuationEndpoint");
            final JsonArray commands = endpoint.getObject("commandExecutorCommand")
                    .getArray("commands");
            if (!commands.isEmpty()) {
                for (final JsonObject command : commands.streamAsJsonObjects().toList()) {
                    final String token = command.getObject("continuationCommand").getString("token");
                    if (token != null && !token.isBlank()) return token;
                }
            }
            return endpoint.getObject("continuationCommand").getString("token");
        }
        if (item.has("continuationItemViewModel")) {
            return item.getObject("continuationItemViewModel").getObject("continuationCommand")
                    .getObject("innertubeCommand").getObject("continuationCommand")
                    .getString("token");
        }
        return null;
    }

    private void collectStreams(final JsonArray entries, final StreamInfoItemsCollector collector)
            throws ParsingException {
        final TimeAgoParser dates = getTimeAgoParser();
        final boolean coursePlaylist = isCoursePlaylist();
        for (final JsonObject entry : entries.streamAsJsonObjects().toList()) {
            if (entry.has(PLAYLIST_ITEM)) {
                collector.commit(new YoutubeStreamInfoItemExtractor(
                        entry.getObject(PLAYLIST_ITEM), dates) {
                    @Override public String getUploaderName() throws ParsingException {
                        return coursePlaylist ? YoutubePlaylistExtractor.this.getUploaderName()
                                : super.getUploaderName();
                    }
                    @Override public String getUploaderUrl() throws ParsingException {
                        return coursePlaylist ? YoutubePlaylistExtractor.this.getUploaderUrl()
                                : super.getUploaderUrl();
                    }
                });
            } else if (entry.has("richItemRenderer")) {
                final JsonObject reel = entry.getObject("richItemRenderer")
                        .getObject("content").getObject("reelItemRenderer");
                if (!reel.isEmpty()) collector.commit(new YoutubeReelInfoItemExtractor(reel));
            } else if (entry.has(LOCKUP_ITEM)) {
                collector.commit(new YoutubeStreamInfoItemLockupExtractor(
                        entry.getObject(LOCKUP_ITEM), dates) {
                    @Override public boolean isChannelOrCoursePlaylistLockupItem() {
                        return coursePlaylist;
                    }
                    @Override public String getUploaderName() throws ParsingException {
                        return coursePlaylist ? YoutubePlaylistExtractor.this.getUploaderName()
                                : super.getUploaderName();
                    }
                    @Override public String getUploaderUrl() throws ParsingException {
                        return coursePlaylist ? YoutubePlaylistExtractor.this.getUploaderUrl()
                                : super.getUploaderUrl();
                    }
                });
            }
        }
    }

    private boolean isCoursePlaylist() throws ParsingException {
        final JsonObject tap = metadata().getObject("header").getObject("playlistHeaderRenderer")
                .getObject("onDescriptionTap").getObject("commandExecutorCommand");
        for (final JsonObject command : tap.getArray("commands").streamAsJsonObjects().toList()) {
            if ("engagement-panel-course-metadata".equals(command
                    .getObject("showEngagementPanelEndpoint").getObject("identifier")
                    .getString("tag"))) return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public PlaylistInfo.PlaylistType getPlaylistType() throws ParsingException {
        return extractPlaylistTypeFromPlaylistUrl(getUrl());
    }
}
