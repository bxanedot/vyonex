// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem;

import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;

import javax.annotation.Nonnull;
import java.util.List;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId;

public class BandcampDiscographStreamInfoItemExtractor extends BandcampStreamInfoItemExtractor {

    private final JsonObject discograph;
    public BandcampDiscographStreamInfoItemExtractor(final JsonObject discograph,
                                                     final String uploaderUrl) {
        super(uploaderUrl);
        this.discograph = discograph;
    }

    @Override
    public String getUploaderName() {
        return discograph.getString("band_name");
    }

    @Override
    public String getName() {
        return discograph.getString("title");
    }

    @Override
    public String getUrl() throws ParsingException {
        return BandcampExtractorHelper.getStreamUrlFromIds(
                discograph.getLong("band_id"),
                discograph.getLong("item_id"),
                discograph.getString("item_type")
        );
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return getImagesFromImageId(discograph.getLong("art_id"), true);
    }

    @Override
    public long getDuration() {
        return -1;
    }
}
