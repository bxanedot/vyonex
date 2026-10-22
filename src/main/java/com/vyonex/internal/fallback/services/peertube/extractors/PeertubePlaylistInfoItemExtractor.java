// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.grack.nanojson.JsonObject;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.stream.Description;

import javax.annotation.Nonnull;
import java.util.List;

import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem;

public class PeertubePlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {

    private final JsonObject item;
    private final JsonObject uploader;
    private final String baseUrl;

    public PeertubePlaylistInfoItemExtractor(@Nonnull final JsonObject item,
                                             @Nonnull final String baseUrl) {
        this.item = item;
        this.uploader = item.getObject("uploader");
        this.baseUrl = baseUrl;
    }

    @Override
    public String getName() throws ParsingException {
        return item.getString("displayName");
    }

    @Override
    public String getUrl() throws ParsingException {
        return item.getString("url");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return getThumbnailsFromPlaylistOrVideoItem(baseUrl, item);
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return uploader.getString("displayName");
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        return uploader.getString("url");
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        return item.getInt("videosLength");
    }

    @Nonnull
    @Override
    public Description getDescription() throws ParsingException {
        final String description = item.getString("description");
        if (isNullOrEmpty(description)) {
            return Description.EMPTY_DESCRIPTION;
        }
        return new Description(description, Description.PLAIN_TEXT);
    }
}
