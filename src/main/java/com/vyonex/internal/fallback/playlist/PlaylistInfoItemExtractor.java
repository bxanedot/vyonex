// bxane
package com.vyonex.internal.fallback.playlist;

import com.vyonex.internal.fallback.InfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.Description;

import javax.annotation.Nonnull;

public interface PlaylistInfoItemExtractor extends InfoItemExtractor {

    /**
     * Get the uploader name
     * @return the uploader name
     */
    String getUploaderName() throws ParsingException;

    /**
     * Get the uploader url
     * @return the uploader url
     */
    String getUploaderUrl() throws ParsingException;

    /**
     * Get whether the uploader is verified
     * @return whether the uploader is verified
     */
    boolean isUploaderVerified() throws ParsingException;

    /**
     * Get the number of streams
     * @return the number of streams
     */
    long getStreamCount() throws ParsingException;

    /**
     * Get the description of the playlist if there is any.
     * Otherwise, an {@link Description#EMPTY_DESCRIPTION EMPTY_DESCRIPTION} is returned.
     * @return the playlist's description
     */
    @Nonnull
    default Description getDescription() throws ParsingException {
        return Description.EMPTY_DESCRIPTION;
    }

    /**
     * @return the type of this playlist, see {@link PlaylistInfo.PlaylistType} for a description
     *         of types. If not overridden always returns {@link PlaylistInfo.PlaylistType#NORMAL}.
     */
    @Nonnull
    default PlaylistInfo.PlaylistType getPlaylistType() throws ParsingException {
        return PlaylistInfo.PlaylistType.NORMAL;
    }
}
