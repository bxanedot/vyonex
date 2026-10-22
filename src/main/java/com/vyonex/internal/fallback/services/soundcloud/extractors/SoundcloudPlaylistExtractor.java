// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SoundcloudPlaylistExtractor extends PlaylistExtractor {
    private static final int IDS_PER_REQUEST = 15;
    private String playlistId;
    private JsonObject playlist;

    public SoundcloudPlaylistExtractor(final StreamingService service, final ListLinkHandler handler) {
        super(service, handler);
    }

    @Override public void onFetchPage(final Downloader downloader)
            throws IOException, ExtractionException {
        playlistId = getLinkHandler().getId();
        final String url = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL + "playlists/"
                + playlistId + "?client_id=" + SoundcloudParsingHelper.clientId()
                + "&representation=compact";
        final Response response = downloader.get(url, getExtractorLocalization());
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud playlist request failed with HTTP "
                    + response.responseCode());
        }
        try {
            playlist = JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid SoundCloud playlist response", e);
        }
    }

    @Override public String getId() { return playlistId; }
    @Override public String getName() { return playlist.getString("title", ""); }
    @Override public List<Image> getThumbnails() {
        try {
            return new SoundcloudPlaylistInfoItemExtractor(playlist).getThumbnails();
        } catch (final ParsingException e) {
            return List.of();
        }
    }
    @Override public String getUploaderUrl() { return SoundcloudParsingHelper.getUploaderUrl(playlist); }
    @Override public String getUploaderName() { return SoundcloudParsingHelper.getUploaderName(playlist); }
    @Override public List<Image> getUploaderAvatars() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(
                SoundcloudParsingHelper.getAvatarUrl(playlist));
    }
    @Override public boolean isUploaderVerified() {
        return playlist.getObject("user").getBoolean("verified");
    }
    @Override public long getStreamCount() { return playlist.getLong("track_count"); }
    @Override public Description getDescription() {
        final String description = playlist.getString("description", "");
        return description.isBlank() ? Description.EMPTY_DESCRIPTION
                : new Description(description, Description.PLAIN_TEXT);
    }

    @Override public InfoItemsPage<StreamInfoItem> getInitialPage() {
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        final List<String> unresolvedIds = new ArrayList<>();
        for (final Object value : playlist.getArray("tracks")) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject track = (JsonObject) value;
            if (track.getString("title") != null) {
                items.commit(new SoundcloudStreamInfoItemExtractor(track));
            } else if (track.get("id") != null) {
                unresolvedIds.add(track.get("id").toString());
            }
        }
        return new InfoItemsPage<>(items, unresolvedIds.isEmpty() ? null : new Page(unresolvedIds));
    }

    @Override public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getIds() == null || page.getIds().isEmpty()) {
            throw new IllegalArgumentException("Playlist page must contain track IDs");
        }
        final List<String> ids = page.getIds();
        final int pageEnd = Math.min(ids.size(), IDS_PER_REQUEST);
        final List<String> currentIds = ids.subList(0, pageEnd);
        final List<String> remainingIds = ids.subList(pageEnd, ids.size());
        final String endpoint = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL + "tracks?client_id="
                + SoundcloudParsingHelper.clientId() + "&ids=" + String.join(",", currentIds);
        final Response response = VyonEx.getDownloader().get(endpoint, getExtractorLocalization());
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud track lookup failed with HTTP " + response.responseCode());
        }
        final JsonArray tracks;
        try {
            tracks = JsonParser.array().from(response.responseBody());
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid SoundCloud track lookup response", e);
        }
        final Map<String, JsonObject> byId = new HashMap<>();
        for (final Object value : tracks) {
            if (value instanceof JsonObject) {
                final JsonObject track = (JsonObject) value;
                byId.put(Objects.toString(track.get("id"), ""), track);
            }
        }
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        for (final String id : currentIds) {
            final JsonObject track = byId.get(id);
            if (track == null) throw new ParsingException("SoundCloud omitted requested track " + id);
            items.commit(new SoundcloudStreamInfoItemExtractor(track));
        }
        return new InfoItemsPage<>(items, remainingIds.isEmpty() ? null : new Page(remainingIds));
    }
}
