// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.vyonex.internal.fallback.ListExtractor.ITEM_COUNT_UNKNOWN;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

public class YoutubeMusicAlbumOrPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject albumOrPlaylistInfoItem;
    private final JsonObject descriptionElementUploader;

    public YoutubeMusicAlbumOrPlaylistInfoItemExtractor(final JsonObject albumOrPlaylistInfoItem,
                                                        final JsonArray descriptionElements,
                                                        final String searchType) {
        this.albumOrPlaylistInfoItem = albumOrPlaylistInfoItem;

        this.descriptionElementUploader = descriptionElements.getObject(
                // For albums: "Album/Single/EP", " • ", uploader, " • ", year -> uploader is at 2
                // For playlists: uploader, " • ", view count -> uploader is at 0
                MUSIC_ALBUMS.equals(searchType) ? 2 : 0
        );
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        try {
            return getImagesFromThumbnailsArray(
                    albumOrPlaylistInfoItem.getObject("thumbnail")
                            .getObject("musicThumbnailRenderer")
                            .getObject("thumbnail")
                            .getArray("thumbnails"));
        } catch (final Exception e) {
            throw new ParsingException("Could not get thumbnails", e);
        }
    }

    @Override
    public String getName() throws ParsingException {
        final String name = getTextFromObject(albumOrPlaylistInfoItem.getArray("flexColumns")
                .getObject(0)
                .getObject("musicResponsiveListItemFlexColumnRenderer")
                .getObject("text"));

        if (!isNullOrEmpty(name)) {
            return name;
        }

        throw new ParsingException("Could not get name");
    }

    @Override
    public String getUrl() throws ParsingException {
        String playlistId = albumOrPlaylistInfoItem.getObject("menu")
                .getObject("menuRenderer")
                .getArray("items")
                .getObject(4)
                .getObject("toggleMenuServiceItemRenderer")
                .getObject("toggledServiceEndpoint")
                .getObject("likeEndpoint")
                .getObject("target")
                .getString("playlistId");

        if (isNullOrEmpty(playlistId)) {
            playlistId = albumOrPlaylistInfoItem.getObject("overlay")
                    .getObject("musicItemThumbnailOverlayRenderer")
                    .getObject("content")
                    .getObject("musicPlayButtonRenderer")
                    .getObject("playNavigationEndpoint")
                    .getObject("watchPlaylistEndpoint")
                    .getString("playlistId");
        }

        if (!isNullOrEmpty(playlistId)) {
            return "https://music.youtube.com/playlist?list=" + playlistId;
        }

        throw new ParsingException("Could not get URL");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        final String name = descriptionElementUploader.getString("text");

        if (!isNullOrEmpty(name)) {
            return name;
        }

        throw new ParsingException("Could not get uploader name");
    }

    @Nullable
    @Override
    public String getUploaderUrl() throws ParsingException {
        // first try obtaining the uploader from the menu (will not work for MUSIC_PLAYLISTS though)
        final JsonArray items = albumOrPlaylistInfoItem.getObject("menu")
                .getObject("menuRenderer")
                .getArray("items");
        for (final Object item : items) {
            final JsonObject menuNavigationItemRenderer =
                    ((JsonObject) item).getObject("menuNavigationItemRenderer");
            if (menuNavigationItemRenderer.getObject("icon")
                    .getString("iconType", "")
                    .equals("ARTIST")) {
                return getUrlFromNavigationEndpoint(
                        menuNavigationItemRenderer.getObject("navigationEndpoint"));
            }
        }

        // then try obtaining it from the uploader description element
        if (!descriptionElementUploader.has("navigationEndpoint")) {
            // if there is no navigationEndpoint for the uploader
            // then this playlist/album is likely autogenerated
            return null;
        }
        return getUrlFromNavigationEndpoint(
                descriptionElementUploader.getObject("navigationEndpoint"));
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        // YouTube Music album and playlist info items don't expose the stream count anywhere...
        return ITEM_COUNT_UNKNOWN;
    }
}
