// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistId;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment;

/** Maps YouTube's playlist-card layout into VyonEx playlist items. */
public class YoutubeMixOrPlaylistLockupInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject item;

    public YoutubeMixOrPlaylistLockupInfoItemExtractor(@Nonnull final JsonObject item) {
        this.item = Objects.requireNonNull(item, "playlist card is required");
    }

    @Override
    public String getName() throws ParsingException {
        final String name = item.getObject("metadata").getObject("lockupMetadataViewModel")
                .getObject("title").getString("content");
        if (name == null || name.isBlank()) {
            throw new ParsingException("Playlist card title is missing");
        }
        return name;
    }

    @Override
    public String getUrl() throws ParsingException {
        final String playlistId = playlistId();
        if (playlistType() == PlaylistInfo.PlaylistType.NORMAL && playlistId != null) {
            return YoutubePlaylistLinkHandlerFactory.getInstance().getUrl(playlistId);
        }

        final String url = getUrlFromNavigationEndpoint(item.getObject("rendererContext")
                .getObject("commandContext").getObject("onTap").getObject("innertubeCommand"));
        if (url == null || url.isBlank()) {
            throw new ParsingException("Playlist card URL is missing");
        }
        return url;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return getImagesFromThumbnailsArray(thumbnailViewModel().getObject("image").getArray("sources"));
    }

    @Override
    public String getUploaderName() throws ParsingException {
        final String name = firstMetadataPart().getObject("text").getString("content");
        if (name == null || name.isBlank()) {
            throw new ParsingException("Playlist card uploader is missing");
        }
        return name;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        if (playlistType() != PlaylistInfo.PlaylistType.NORMAL) return null;

        final JsonArray runs = firstMetadataPart().getObject("text").getArray("commandRuns");
        if (runs.isEmpty()) return null;
        final JsonObject command = runs.getObject(0).getObject("onTap").getObject("innertubeCommand");
        return getUrlFromNavigationEndpoint(command);
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        if (playlistType() != PlaylistInfo.PlaylistType.NORMAL) return false;
        return hasArtistOrVerifiedIconBadgeAttachment(firstMetadataPart().getObject("text")
                .getArray("attachmentRuns"));
    }

    @Override
    public long getStreamCount() throws ParsingException {
        if (playlistType() != PlaylistInfo.PlaylistType.NORMAL) {
            return ListExtractor.ITEM_COUNT_INFINITE;
        }

        final String count = badgeText();
        if (count == null || count.isBlank()) {
            throw new ParsingException("Playlist card count is missing");
        }
        try {
            final String upperCase = count.toUpperCase(Locale.ROOT);
            return upperCase.indexOf('K') >= 0 || upperCase.indexOf('M') >= 0 || upperCase.indexOf('B') >= 0
                    ? Utils.mixedNumberWordToLong(count)
                    : Long.parseLong(Utils.removeNonDigitCharacters(count));
        } catch (final NumberFormatException | ParsingException error) {
            throw new ParsingException("Could not parse playlist card count", error);
        }
    }

    @Nonnull
    @Override
    public PlaylistInfo.PlaylistType getPlaylistType() {
        return playlistType();
    }

    private JsonObject thumbnailViewModel() {
        return item.getObject("contentImage").getObject("collectionThumbnailViewModel")
                .getObject("primaryThumbnail").getObject("thumbnailViewModel");
    }

    private JsonObject firstMetadataPart() throws ParsingException {
        final JsonArray rows = item.getObject("metadata").getObject("lockupMetadataViewModel")
                .getObject("metadata").getObject("contentMetadataViewModel").getArray("metadataRows");
        if (rows.isEmpty()) throw new ParsingException("Playlist card metadata is missing");

        final JsonArray parts = rows.getObject(0).getArray("metadataParts");
        if (parts.isEmpty()) throw new ParsingException("Playlist card uploader metadata is missing");
        return parts.getObject(0);
    }

    private String badgeText() {
        for (final JsonObject overlay : thumbnailViewModel().getArray("overlays")
                .streamAsJsonObjects().toList()) {
            final JsonArray badges = overlay.getObject("thumbnailOverlayBadgeViewModel")
                    .getArray("thumbnailBadges");
            for (final JsonObject badge : badges.streamAsJsonObjects().toList()) {
                final String text = badge.getObject("thumbnailBadgeViewModel").getString("text");
                if (text != null && !text.isBlank()) return text;
            }
        }
        return null;
    }

    private String playlistId() {
        final String id = item.getString("contentId");
        if (id != null && !id.isBlank()) return id;
        final String endpointId = item.getObject("rendererContext").getObject("commandContext")
                .getObject("watchEndpoint").getString("playlistId");
        return endpointId == null || endpointId.isBlank() ? null : endpointId;
    }

    private PlaylistInfo.PlaylistType playlistType() {
        try {
            return extractPlaylistTypeFromPlaylistId(playlistId());
        } catch (final ParsingException ignored) {
            return PlaylistInfo.PlaylistType.NORMAL;
        }
    }
}
