// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL;
import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl;
import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.getAvatarUrl;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SoundcloudPlaylistExtractor extends PlaylistExtractor {
    private static final int STREAMS_PER_REQUESTED_PAGE = 15;

    private String playlistId;
    private JsonObject playlist;

    public SoundcloudPlaylistExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws IOException,
            ExtractionException {

        playlistId = getLinkHandler().getId();
        final String apiUrl = SOUNDCLOUD_API_V2_URL + "playlists/" + playlistId + "?client_id="
                + SoundcloudParsingHelper.clientId() + "&representation=compact";

        final String response = downloader.get(apiUrl, getExtractorLocalization()).responseBody();
        try {
            playlist = JsonParser.object().from(response);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }
    }

    @Nonnull
    @Override
    public String getId() {
        return playlistId;
    }

    @Nonnull
    @Override
    public String getName() {
        return playlist.getString("title");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        final String artworkUrl = playlist.getString("artwork_url");

        if (!isNullOrEmpty(artworkUrl)) {
            return getAllImagesFromArtworkOrAvatarUrl(artworkUrl);
        }

        // If the thumbnail is null or empty, traverse the items list and get a valid one
        // If it also fails, return an empty list
        try {
            final InfoItemsPage<StreamInfoItem> infoItems = getInitialPage();

            for (final StreamInfoItem item : infoItems.getItems()) {
                final List<Image> thumbnails = item.getThumbnails();
                if (!isNullOrEmpty(thumbnails)) {
                    return thumbnails;
                }
            }
        } catch (final Exception ignored) {
        }

        return List.of();
    }

    @Override
    public String getUploaderUrl() {
        return SoundcloudParsingHelper.getUploaderUrl(playlist);
    }

    @Override
    public String getUploaderName() {
        return SoundcloudParsingHelper.getUploaderName(playlist);
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        return getAllImagesFromArtworkOrAvatarUrl(getAvatarUrl(playlist));
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return playlist.getObject("user").getBoolean("verified");
    }

    @Override
    public long getStreamCount() {
        return playlist.getLong("track_count");
    }

    @Nonnull
    @Override
    public Description getDescription() throws ParsingException {
        final String description = playlist.getString("description");
        if (isNullOrEmpty(description)) {
            return Description.EMPTY_DESCRIPTION;
        }
        return new Description(description, Description.PLAIN_TEXT);
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() {
        final StreamInfoItemsCollector streamInfoItemsCollector =
                new StreamInfoItemsCollector(getServiceId());
        final List<String> ids = new ArrayList<>();

        playlist.getArray("tracks")
                .streamAsJsonObjects()
                .forEachOrdered(track -> {
                    // i.e. if full info is available
                    if (track.has("title")) {
                        streamInfoItemsCollector.commit(
                                new SoundcloudStreamInfoItemExtractor(track));
                    } else {
                        ids.add(String.valueOf(track.getLong("id")));
                    }
                });

        return new InfoItemsPage<>(streamInfoItemsCollector, new Page(ids));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) throws IOException,
            ExtractionException {
        if (page == null || isNullOrEmpty(page.getIds())) {
            throw new IllegalArgumentException("Page doesn't contain IDs");
        }

        final List<String> currentIds;
        final List<String> nextIds;
        if (page.getIds().size() <= STREAMS_PER_REQUESTED_PAGE) {
            // Fetch every remaining stream, there are less than the max
            currentIds = page.getIds();
            nextIds = null;
        } else {
            currentIds = page.getIds().subList(0, STREAMS_PER_REQUESTED_PAGE);
            nextIds = page.getIds().subList(STREAMS_PER_REQUESTED_PAGE, page.getIds().size());
        }

        final String currentPageUrl = SOUNDCLOUD_API_V2_URL + "tracks?client_id="
                + SoundcloudParsingHelper.clientId() + "&ids=" + String.join(",", currentIds);

        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        final String response = VyonEx.getDownloader().get(currentPageUrl,
                getExtractorLocalization()).responseBody();

        try {
            final JsonArray tracks = JsonParser.array().from(response);
            // Response may not contain tracks in the same order as currentIds.
            // The streams are displayed in the order which is used in currentIds on SoundCloud.
            final HashMap<Long, JsonObject> idToTrack = new HashMap<>();
            for (final Object track : tracks) {
                if (track instanceof JsonObject) {
                    final JsonObject o = (JsonObject) track;
                    idToTrack.put(o.getLong("id"), o);
                }
            }
            for (final String strId : currentIds) {
                final long id = Long.parseLong(strId);
                try {
                    collector.commit(new SoundcloudStreamInfoItemExtractor(
                        Objects.requireNonNull(
                                idToTrack.get(id),
                        "no track with id " + id + " in response"
                        )
                    ));
                } catch (final NullPointerException e) {
                    throw new ParsingException("Could not parse json response", e);
                }
            }
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }

        return new InfoItemsPage<>(collector, new Page(nextIds));
    }
}
