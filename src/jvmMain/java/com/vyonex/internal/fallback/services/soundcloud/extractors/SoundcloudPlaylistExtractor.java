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
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudPlaylistInfoItem;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudPlaylistPayload;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudPlaylistPayloadParser;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudPlaylistTrackBatchParser;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudTrackInfoItem;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SoundcloudPlaylistExtractor extends PlaylistExtractor {
    private static final int IDS_PER_REQUEST = 15;
    private String playlistId;
    private SoundcloudPlaylistInfoItemExtractor playlistInfo;
    private String avatarUrl;
    private Description description;
    private List<StreamInfoItemExtractor> initialTracks = List.of();
    private List<String> unresolvedTrackIds = List.of();

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
            final SoundcloudPlaylistPayload payload = SoundcloudPlaylistPayloadParser.INSTANCE
                    .parse(response.responseBody());
            final SoundcloudPlaylistInfoItem playlist = payload.getPlaylist();
            playlistInfo = new SoundcloudPlaylistInfoItemExtractor(playlist);
            avatarUrl = SoundcloudParsingHelper.toHttpsUrl(playlist.getUser().getAvatarUrl());
            final String descriptionText = payload.getDescription();
            description = descriptionText.isBlank() ? Description.EMPTY_DESCRIPTION
                    : new Description(descriptionText, Description.PLAIN_TEXT);

            final List<StreamInfoItemExtractor> tracks = new ArrayList<>();
            for (final SoundcloudTrackInfoItem track : payload.getInitialTracks()) {
                tracks.add(new SoundcloudStreamInfoItemExtractor(track));
            }
            initialTracks = List.copyOf(tracks);
            unresolvedTrackIds = List.copyOf(payload.getUnresolvedTrackIds());
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("Invalid SoundCloud playlist response", e);
        }
    }

    @Override public String getId() { return playlistId; }
    @Override public String getName() { return playlistInfo.getName(); }
    @Override public List<Image> getThumbnails() {
        try {
            return playlistInfo.getThumbnails();
        } catch (final ParsingException e) {
            return List.of();
        }
    }
    @Override public String getUploaderUrl() { return playlistInfo.getUploaderUrl(); }
    @Override public String getUploaderName() { return playlistInfo.getUploaderName(); }
    @Override public List<Image> getUploaderAvatars() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(avatarUrl);
    }
    @Override public boolean isUploaderVerified() { return playlistInfo.isUploaderVerified(); }
    @Override public long getStreamCount() { return playlistInfo.getStreamCount(); }
    @Override public Description getDescription() { return description; }

    @Override public InfoItemsPage<StreamInfoItem> getInitialPage() {
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        initialTracks.forEach(items::commit);
        return new InfoItemsPage<>(items, unresolvedTrackIds.isEmpty()
                ? null : new Page(unresolvedTrackIds));
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

        final List<SoundcloudTrackInfoItem> tracks;
        try {
            tracks = SoundcloudPlaylistTrackBatchParser.INSTANCE.parse(response.responseBody());
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("Invalid SoundCloud track lookup response", e);
        }
        final Map<String, SoundcloudTrackInfoItem> byId = new HashMap<>();
        for (final SoundcloudTrackInfoItem track : tracks) byId.put(track.getId(), track);

        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        for (final String id : currentIds) {
            final SoundcloudTrackInfoItem track = byId.get(id);
            if (track == null) throw new ParsingException("SoundCloud omitted requested track " + id);
            items.commit(new SoundcloudStreamInfoItemExtractor(track));
        }
        return new InfoItemsPage<>(items, remainingIds.isEmpty() ? null : new Page(remainingIds));
    }
}
